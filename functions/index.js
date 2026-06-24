const admin = require("firebase-admin");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

exports.pushArticleCreated = onDocumentCreated("articles/{articleId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const article = snapshot.data();
  const articleId = article.articleId || event.params.articleId;
  const category = article.category || "Headlines";
  const publishedAtMillis = Number(article.publishedAtMillis || Date.now());
  const newestAllowed = Date.now() - 86_400_000;

  if (!articleId || publishedAtMillis < newestAllowed) return;

  const isBreaking = category.toLowerCase() === "breaking";
  const isImportant = category.toLowerCase() === "important";
  if (!isBreaking && !isImportant) return;

  const tokensSnapshot = await db.collection("deviceTokens").limit(500).get();
  const tokens = [];

  tokensSnapshot.forEach((tokenDoc) => {
    const device = tokenDoc.data();
    if (isBreaking && device.breakingNotificationsEnabled === false) return;
    if (isImportant && device.importantNotificationsEnabled === false) return;
    if (typeof device.token === "string" && device.token.length > 0) {
      tokens.push(device.token);
    }
  });

  if (tokens.length === 0) return;

  const response = await messaging.sendEachForMulticast({
    tokens,
    data: {
      articleId,
      category,
      source: article.source || "Budgie News"
    },
    android: {
      priority: "high"
    }
  });

  const invalidTokens = [];
  response.responses.forEach((sendResponse, index) => {
    if (!sendResponse.success) {
      const code = sendResponse.error && sendResponse.error.code;
      if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
        invalidTokens.push(tokens[index]);
      }
      logger.warn("FCM send failed", { code, articleId });
    }
  });

  await Promise.all(
    invalidTokens.map((token) => db.collection("deviceTokens").doc(token.replace(/[^A-Za-z0-9_-]/g, "_").slice(0, 140)).delete())
  );
});
