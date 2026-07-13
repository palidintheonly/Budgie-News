package com.budgienews.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Html
import android.util.Xml
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.messaging
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URLDecoder
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val Ink = Color(0xFFF8FAFC)
private val Paper = Color(0xFF09090B)
private val SurfaceDark = Color(0xFF111113)
private val SurfaceRaised = Color(0xFF18181B)
private val Muted = Color(0xFFA1A1AA)
private val Accent = Color(0xFF3F3F46)
private val AccentSoft = Color(0xFF27272A)
private val Alert = Color(0xFFD4D4D8)

internal val FeedSources = listOf(
    FeedSource("BBC UK", "https://feeds.bbci.co.uk/news/uk/rss.xml", NewsEdition.GB),
    FeedSource("Sky News UK", "https://feeds.skynews.com/feeds/rss/uk.xml", NewsEdition.GB),
    FeedSource("Sky Politics", "https://feeds.skynews.com/feeds/rss/politics.xml", NewsEdition.GB),
    FeedSource("Guardian UK", "https://www.theguardian.com/uk/rss", NewsEdition.GB),
    FeedSource("Guardian Politics", "https://www.theguardian.com/politics/rss", NewsEdition.GB),
    FeedSource("The Sun News", "https://www.thesun.co.uk/news/feed/", NewsEdition.GB),
    FeedSource("NPR News", "https://feeds.npr.org/1001/rss.xml", NewsEdition.USA),
    FeedSource("NPR Politics", "https://feeds.npr.org/1014/rss.xml", NewsEdition.USA),
    FeedSource("CBS News", "https://www.cbsnews.com/latest/rss/main", NewsEdition.USA),
    FeedSource("CBS Politics", "https://www.cbsnews.com/latest/rss/politics", NewsEdition.USA),
    FeedSource("ABC News", "https://abcnews.go.com/abcnews/topstories", NewsEdition.USA),
    FeedSource("ABC Politics", "https://abcnews.go.com/abcnews/politicsheadlines", NewsEdition.USA),
    FeedSource("CNN News", "http://rss.cnn.com/rss/cnn_topstories.rss", NewsEdition.USA),
    FeedSource("CNN Politics", "http://rss.cnn.com/rss/cnn_allpolitics.rss", NewsEdition.USA),
    FeedSource("Fox News", "https://moxie.foxnews.com/google-publisher/latest.xml", NewsEdition.USA),
    FeedSource("Fox Politics", "https://moxie.foxnews.com/google-publisher/politics.xml", NewsEdition.USA),
    FeedSource("NYT News", "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml", NewsEdition.USA),
    FeedSource("NYT Politics", "https://rss.nytimes.com/services/xml/rss/nyt/Politics.xml", NewsEdition.USA),
)

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        BudgieNotifications.ensureChannels(this)
        handleArticleIntent(intent)
        BudgieFirebase.setup(this)
        BudgieVersionCheck.startMonitoring(this)
        setContent {
            BudgieNewsTheme {
                NewsApp()
            }
        }
        requestRequiredPermissionsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        BudgieVersionCheck.startMonitoring(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleArticleIntent(intent)
    }

    private fun handleArticleIntent(intent: Intent?) {
        val articleId = (
            intent?.getStringExtra(BudgieNotifications.EXTRA_ARTICLE_ID)
                ?: intent?.getStringExtra("articleId")
            ).orEmpty()
        if (articleId.isNotBlank()) {
            ArticleSignals.open(articleId)
        }
    }

    private fun requestRequiredPermissionsIfNeeded() {
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

internal fun Throwable.isExpectedFirestoreMissingError(): Boolean {
    if (this is com.google.firebase.firestore.FirebaseFirestoreException) {
        return code == com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND ||
            code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ||
            code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED
    }
    if (this is java.net.SocketTimeoutException || this is java.net.UnknownHostException || this is java.net.ConnectException || this is java.io.InterruptedIOException) {
        return true
    }
    val msg = message.orEmpty() + " " + (cause?.message.orEmpty())
    return (msg.contains("NOT_FOUND", ignoreCase = true) && msg.contains("database", ignoreCase = true)) ||
        msg.contains("The database (default) does not exist", ignoreCase = true) ||
        msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
        msg.contains("UNAVAILABLE", ignoreCase = true) ||
        msg.contains("No stories loaded from", ignoreCase = true) ||
        msg.contains("returned HTTP", ignoreCase = true)
}

private object BudgieFirebase {
    @Suppress("DEPRECATION")
    fun setup(context: Context) {
        Firebase.analytics.logEvent("budgie_app_open", null)
        FirebaseCrashlytics.getInstance().setCustomKey("budgie_version", "1.0.0")
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                BudgieAccountApi.ensureSession()
                BudgieAccountApi.startLiveArticles(context)
                BudgiePrefs.deviceToken(context).takeIf { it.isNotBlank() }?.let { token ->
                    BudgieAccountApi.registerDevice(context, token)
                }
            }.onFailure { if (!it.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(it) }
        }
        Firebase.remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            },
        )
        Firebase.remoteConfig.fetchAndActivate()
        Firebase.messaging.token.addOnSuccessListener { token ->
            FirebaseCrashlytics.getInstance().setCustomKey("fcm_token_ready", token.isNotBlank())
            if (token.isNotBlank()) {
                BudgiePrefs.saveDeviceToken(context, token)
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    runCatching {
                        BudgieAccountApi.ensureSession()
                        BudgieAccountApi.registerDevice(context, token)
                    }
                        .onFailure { if (!it.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(it) }
                }
            }
        }.addOnFailureListener { error ->
            if (!error.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(error)
        }
    }
}

internal object BudgieNotifications {
    const val DEFAULT_CHANNEL_ID = "channel_budgie_default"
    const val BREAKING_CHANNEL_ID = "budgie_news_breaking"
    const val IMPORTANT_CHANNEL_ID = "budgie_news_important"
    const val HEADLINES_CHANNEL_ID = "budgie_news_headlines"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val customSoundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.sound_chirp}")
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    DEFAULT_CHANNEL_ID,
                    "Budgie News Alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Default Budgie News custom alerts"
                    setSound(customSoundUri, audioAttributes)
                },
                NotificationChannel(
                    BREAKING_CHANNEL_ID,
                    "Breaking news",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Breaking Budgie News alerts"
                    setSound(customSoundUri, audioAttributes)
                },
                NotificationChannel(
                    IMPORTANT_CHANNEL_ID,
                    "Important news",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Important Budgie News alerts"
                    setSound(customSoundUri, audioAttributes)
                },
                NotificationChannel(
                    HEADLINES_CHANNEL_ID,
                    "Headlines",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Regular Budgie News alerts"
                    setSound(null, null)
                },
            ),
        )
    }

    const val EXTRA_ARTICLE_ID = "com.budgienews.app.extra.ARTICLE_ID"
    const val EXTRA_ARTICLE_CATEGORY = "com.budgienews.app.extra.ARTICLE_CATEGORY"

    fun notifyNewArticle(
        context: Context,
        item: FeedItem,
        section: NewsSection = NewsSection.HEADLINES,
        isPush: Boolean = false,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val settings = BudgiePrefs.load(context)
        val enabled = when (section) {
            NewsSection.BREAKING -> settings.breakingNotificationsEnabled
            NewsSection.IMPORTANT -> settings.importantNotificationsEnabled
            NewsSection.HEADLINES, NewsSection.GENERAL -> settings.headlinesNotificationsEnabled
            NewsSection.SAVED -> false
        }
        if (!enabled) return

        val articleId = item.link.ifBlank { item.id }.ifBlank { item.title }
        if (articleId.isBlank()) return

        val prefs = context.getSharedPreferences("budgie_news_settings", Context.MODE_PRIVATE)
        val key = when (section) {
            NewsSection.BREAKING -> "last_breaking_link"
            NewsSection.IMPORTANT -> "last_important_link"
            NewsSection.HEADLINES -> "last_headlines_link"
            NewsSection.GENERAL -> "last_general_link"
            NewsSection.SAVED -> "last_saved_link"
        }
        if (prefs.getString(key, "") == articleId && !isPush) return
        prefs.edit().putString(key, articleId).apply()

        if (AppVisibility.isForeground) return

        val channelId = when (section) {
            NewsSection.BREAKING -> BREAKING_CHANNEL_ID
            NewsSection.IMPORTANT -> IMPORTANT_CHANNEL_ID
            NewsSection.HEADLINES, NewsSection.GENERAL, NewsSection.SAVED -> DEFAULT_CHANNEL_ID
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ARTICLE_ID, articleId)
            putExtra(EXTRA_ARTICLE_CATEGORY, section.label)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            articleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.budgie_icon)
            .setContentTitle("${section.label}: ${item.source.shortSourceName()}")
            .setContentText(item.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.title))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (section == NewsSection.IMPORTANT || section == NewsSection.BREAKING) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(section.ordinal + articleId.hashCode(), notification)
    }
}

internal object BudgiePrefs {
    private const val PREFS = "budgie_news_settings"
    private const val KEY_BREAKING = "breaking_notifications"
    private const val KEY_IMPORTANT = "important_notifications"
    private const val KEY_HEADLINES = "headlines_notifications"
    private const val KEY_SECTION = "default_section"
    private const val KEY_SOURCE = "default_source"
    private const val KEY_GB_SOURCE = "default_gb_source"
    private const val KEY_USA_SOURCE = "default_usa_source"
    private const val KEY_LOCATION = "uk_location"
    private const val KEY_SEND_STATS = "send_stats"
    private const val KEY_DEVICE_TOKEN = "device_token"
    private const val KEY_LAST_BREAKING = "last_breaking_link"
    private const val KEY_LAST_IMPORTANT = "last_important_link"

    fun load(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppSettings(
            breakingNotificationsEnabled = prefs.getBoolean(KEY_BREAKING, true),
            importantNotificationsEnabled = prefs.getBoolean(KEY_IMPORTANT, true),
            headlinesNotificationsEnabled = prefs.getBoolean(KEY_HEADLINES, true),
            defaultSection = prefs.getString(KEY_SECTION, null)?.let { runCatching { NewsSection.valueOf(it) }.getOrNull() } ?: NewsSection.HEADLINES,
            defaultSource = prefs.getString(KEY_SOURCE, null)?.let { runCatching { SourceFilter.valueOf(it) }.getOrNull() } ?: SourceFilter.ALL,
            defaultGbSource = prefs.getString(KEY_GB_SOURCE, null)?.let { runCatching { SourceFilter.valueOf(it) }.getOrNull() }
                ?: prefs.getString(KEY_SOURCE, null)?.let { runCatching { SourceFilter.valueOf(it) }.getOrNull() }
                ?: SourceFilter.ALL,
            defaultUsaSource = prefs.getString(KEY_USA_SOURCE, null)?.let { runCatching { SourceFilter.valueOf(it) }.getOrNull() } ?: SourceFilter.ALL,
            ukLocation = prefs.getString(KEY_LOCATION, "United Kingdom").orEmpty(),
            sendAppStatistics = prefs.getBoolean(KEY_SEND_STATS, true),
        )
    }

    fun save(context: Context, settings: AppSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BREAKING, settings.breakingNotificationsEnabled)
            .putBoolean(KEY_IMPORTANT, settings.importantNotificationsEnabled)
            .putBoolean(KEY_HEADLINES, settings.headlinesNotificationsEnabled)
            .putString(KEY_SECTION, settings.defaultSection.name)
            .putString(KEY_SOURCE, settings.defaultSource.name)
            .putString(KEY_GB_SOURCE, settings.defaultGbSource.name)
            .putString(KEY_USA_SOURCE, settings.defaultUsaSource.name)
            .putString(KEY_LOCATION, settings.ukLocation)
            .putBoolean(KEY_SEND_STATS, settings.sendAppStatistics)
            .apply()
    }

    fun saveDeviceToken(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DEVICE_TOKEN, token)
            .apply()
    }

    fun deviceToken(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DEVICE_TOKEN, "").orEmpty()

    suspend fun saveAndSync(context: Context, settings: AppSettings) {
        save(context, settings)
    }
}

internal object BudgieAccountApi {
    private var liveArticleRegistration: ListenerRegistration? = null

    suspend fun ensureSession() {
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously().await()
        }
    }

    suspend fun registerDevice(context: Context, token: String) {
        if (token.isBlank()) return
        ensureSession()
        val settings = BudgiePrefs.load(context)
        val uid = Firebase.auth.currentUser?.uid ?: return
        val data = mapOf(
            "token" to token,
            "uid" to uid,
            "ukLocation" to settings.ukLocation,
            "breakingNotificationsEnabled" to settings.breakingNotificationsEnabled,
            "importantNotificationsEnabled" to settings.importantNotificationsEnabled,
            "headlinesNotificationsEnabled" to settings.headlinesNotificationsEnabled,
            "defaultGbSource" to settings.defaultGbSource.name,
            "defaultUsaSource" to settings.defaultUsaSource.name,
            "updatedAt" to FieldValue.serverTimestamp(),
            "platform" to "android",
        )
        Firebase.firestore.collection("deviceTokens")
            .document(token.safeFirestoreId())
            .set(data, SetOptions.merge())
            .await()
    }

    fun startLiveArticles(context: Context) {
        if (liveArticleRegistration != null) return
        val newestAllowed = BudgieTime.minAllowedMillis()
        liveArticleRegistration = Firebase.firestore.collection("articles")
            .whereGreaterThanOrEqualTo("publishedAtMillis", newestAllowed)
            .orderBy("publishedAtMillis", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.isExpectedFirestoreMissingError()) {
                        liveArticleRegistration?.remove()
                        liveArticleRegistration = null
                        return@addSnapshotListener
                    }
                    FirebaseCrashlytics.getInstance().recordException(error)
                    return@addSnapshotListener
                }
                val articles = snapshot?.documents
                    ?.mapNotNull { document ->
                        val publishedAtMillis = document.getLong("publishedAtMillis") ?: return@mapNotNull null
                        if (publishedAtMillis < newestAllowed) return@mapNotNull null
                        LocalArticle(
                            articleId = document.getString("articleId") ?: document.id,
                            title = document.getString("title").orEmpty(),
                            description = document.getString("description").orEmpty(),
                            link = document.getString("link").orEmpty(),
                            source = document.getString("source") ?: "Budgie News",
                            publishedAt = document.getString("publishedAt").orEmpty(),
                            imageUrl = document.getString("imageUrl")?.takeIf { it.isNotBlank() },
                            category = document.getString("category") ?: NewsSection.HEADLINES.label,
                            isRead = document.getBoolean("isRead") == true,
                        )
                    }
                    .orEmpty()
                val filteredArticles = articles.filter { isFreeNewsSource(it.source) }
                if (filteredArticles.isNotEmpty()) {
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        filteredArticles.forEach { article ->
                            BudgieArticleDatabase.get(context).upsertArticle(article)
                        }
                        ArticleSignals.changed()
                    }
                }
            }
    }
}

internal fun String.safeFirestoreId(): String =
    replace(Regex("[^A-Za-z0-9_-]"), "_").take(140).ifBlank { "unknown-token" }

internal data class AppUpdateConfig(
    val minRequiredVersion: String = "0.1.0-beta",
    val updateMessage: String = "An important update for Budgie News is available. Please update your app to continue reading news.",
    val updateUrl: String = "https://budgienews.com",
    val isOutdated: Boolean = false,
)

internal object BudgieVersionCheck {
    private val _config = MutableStateFlow(AppUpdateConfig())
    val config = _config.asStateFlow()

    private var registration: ListenerRegistration? = null

    fun startMonitoring(context: Context) {
        val currentVersion = context.appVersionText()
        if (registration == null) {
            registration = Firebase.firestore.collection("config")
                .document("version")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        if (error.isExpectedFirestoreMissingError()) {
                            registration?.remove()
                            registration = null
                            return@addSnapshotListener
                        }
                        FirebaseCrashlytics.getInstance().recordException(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.exists()) {
                        runCatching {
                            Firebase.firestore.collection("config")
                                .document("version")
                                .set(
                                    mapOf(
                                        "minRequiredVersion" to "0.1.0-beta",
                                        "updateMessage" to "An important update for Budgie News is available. Please update your app to continue reading news.",
                                        "updateUrl" to "https://budgienews.com",
                                        "forceLock" to false,
                                    ),
                                    SetOptions.merge()
                                )
                        }
                    }
                    val minVersion = snapshot?.getString("minRequiredVersion") ?: "0.1.0-beta"
                    val message = snapshot?.getString("updateMessage") ?: "A new version of Budgie News is required. Please update your app to continue reading news."
                    val url = snapshot?.getString("updateUrl") ?: "https://budgienews.com"
                    val forceLock = snapshot?.getBoolean("forceLock") == true

                    val outdated = forceLock || compareVersions(currentVersion, minVersion) < 0
                    _config.value = AppUpdateConfig(
                        minRequiredVersion = minVersion,
                        updateMessage = message,
                        updateUrl = url,
                        isOutdated = outdated,
                    )
                }
        }
    }

    private fun compareVersions(current: String, minRequired: String): Int {
        if (current == minRequired) return 0
        val currentParts = current.substringBefore("-").split(".").mapNotNull { it.toIntOrNull() }
        val minParts = minRequired.substringBefore("-").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currentParts.size, minParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val m = minParts.getOrElse(i) { 0 }
            if (c != m) return c.compareTo(m)
        }
        val currentSuffix = current.substringAfter("-", "")
        val minSuffix = minRequired.substringAfter("-", "")
        if (currentSuffix == minSuffix) return 0
        if (currentSuffix.isEmpty()) return 1
        if (minSuffix.isEmpty()) return -1
        return currentSuffix.compareTo(minSuffix)
    }
}


private object BudgieCache {
    private const val PREFS = "budgie_news_cache"
    private const val KEY_ITEMS = "items"

    fun save(context: Context, items: List<FeedItem>) {
        val array = JSONArray()
        items.take(80).forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("description", item.description)
                    .put("link", item.link)
                    .put("source", item.source)
                    .put("publishedAt", item.publishedAt)
                    .put("imageUrl", item.imageUrl.orEmpty()),
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }

    fun checkReset(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastReset = prefs.getLong("last_reset_millis", 0L)
        if (lastReset < BudgieTime.RESET_EPOCH_MILLIS) {
            prefs.edit().clear().putLong("last_reset_millis", BudgieTime.RESET_EPOCH_MILLIS).apply()
        }
    }

    fun load(context: Context): List<FeedItem> {
        checkReset(context)
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                FeedItem(
                    id = item.optString("id").ifBlank { item.optString("link") },
                    title = item.optString("title"),
                    description = item.optString("description"),
                    link = item.optString("link"),
                    source = item.optString("source"),
                    publishedAt = item.optString("publishedAt"),
                    imageUrl = item.optString("imageUrl").takeIf { it.isNotBlank() },
                )
            }
        }.getOrElse {
            if (!it.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(it)
            emptyList()
        }
    }
}

internal data class FeedItem(
    val id: String,
    val title: String,
    val description: String,
    val link: String,
    val source: String,
    val publishedAt: String,
    val imageUrl: String?,
    val coverageSources: List<String> = listOf(source),
)

enum class NewsEdition(
    val title: String,
    val label: String,
    val subtitle: String,
    val flag: String,
    val emptyMessage: String,
) {
    GB(
        title = "GB News",
        label = "GB News",
        subtitle = "United Kingdom Edition",
        flag = "🇬🇧",
        emptyMessage = "No UK news stories matched the current filters."
    ),
    USA(
        title = "USA News",
        label = "USA News",
        subtitle = "United States Edition",
        flag = "🇺🇸",
        emptyMessage = "No USA news stories matched the current filters."
    )
}

internal data class FeedSource(
    val name: String,
    val url: String,
    val edition: NewsEdition = NewsEdition.GB,
)

internal data class AppSettings(
    val breakingNotificationsEnabled: Boolean = true,
    val importantNotificationsEnabled: Boolean = true,
    val headlinesNotificationsEnabled: Boolean = true,
    val defaultSection: NewsSection = NewsSection.HEADLINES,
    val defaultSource: SourceFilter = SourceFilter.ALL,
    val defaultGbSource: SourceFilter = SourceFilter.ALL,
    val defaultUsaSource: SourceFilter = SourceFilter.ALL,
    val ukLocation: String = "United Kingdom",
    val sendAppStatistics: Boolean = true,
)

internal enum class SourceFilter(val label: String, val sourceName: String?, val edition: NewsEdition? = null) {
    ALL("All", null, null),
    BBC("BBC", "BBC UK", NewsEdition.GB),
    SKY("Sky", "Sky News UK", NewsEdition.GB),
    SKY_POLITICS("Sky Pol", "Sky Politics", NewsEdition.GB),
    GUARDIAN("Guardian", "Guardian UK", NewsEdition.GB),
    GUARDIAN_POLITICS("Guard Pol", "Guardian Politics", NewsEdition.GB),
    SUN("Sun", "The Sun News", NewsEdition.GB),
    NPR("NPR", "NPR News", NewsEdition.USA),
    NPR_POLITICS("NPR Pol", "NPR Politics", NewsEdition.USA),
    CBS("CBS", "CBS News", NewsEdition.USA),
    CBS_POLITICS("CBS Pol", "CBS Politics", NewsEdition.USA),
    ABC("ABC", "ABC News", NewsEdition.USA),
    ABC_POLITICS("ABC Pol", "ABC Politics", NewsEdition.USA),
    CNN("CNN", "CNN News", NewsEdition.USA),
    CNN_POLITICS("CNN Pol", "CNN Politics", NewsEdition.USA),
    FOX("Fox", "Fox News", NewsEdition.USA),
    FOX_POLITICS("Fox Pol", "Fox Politics", NewsEdition.USA),
    NYT("NYT", "NYT News", NewsEdition.USA),
    NYT_POLITICS("NYT Pol", "NYT Politics", NewsEdition.USA),
}

internal fun availableSourcesForEdition(edition: NewsEdition?): List<FeedSource> {
    if (edition == null) return FeedSources
    return FeedSources.filter { it.edition == edition }
}

internal fun availableFiltersForEdition(edition: NewsEdition?): List<SourceFilter> {
    if (edition == null) return SourceFilter.entries
    return SourceFilter.entries.filter { it.edition == null || it.edition == edition }
}

internal enum class NewsSection(
    val label: String,
    val tagline: String,
    val emptyText: String,
) {
    HEADLINES(
        "Headlines",
        "The latest top stories from across the UK",
        "No headlines were found in the selected feeds.",
    ),
    BREAKING(
        "Breaking",
        "Live, urgent, and developing stories",
        "No breaking stories matched the current filters.",
    ),
    IMPORTANT(
        "Important",
        "High-impact UK public-interest stories",
        "No important stories matched the current filters.",
    ),
    GENERAL(
        "General",
        "Everyday UK news, culture, and general reporting",
        "No general news stories matched the current filters.",
    ),
    SAVED(
        "Saved Stories",
        "Your offline bookmarks for reading anywhere",
        "No saved stories yet. Tap the bookmark icon on any story to read offline.",
    ),
}

internal sealed interface FeedState {
    data object Loading : FeedState
    data class Ready(val items: List<FeedItem>) : FeedState
    data class Error(val message: String) : FeedState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsApp() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(BudgiePrefs.load(context)) }
    var refreshToken by remember { mutableStateOf(0) }
    var selectedEdition by remember { mutableStateOf<NewsEdition?>(null) }
    var selectedSection by remember { mutableStateOf(settings.defaultSection) }
    var selectedSource by remember { mutableStateOf(settings.defaultSource) }
    var selectedItem by remember { mutableStateOf<FeedItem?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var defaultOutletsOpen by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<FeedState>(FeedState.Loading) }
    var houseAd by remember { mutableStateOf<HouseAd?>(null) }
    var houseAds by remember { mutableStateOf<List<HouseAd>>(emptyList()) }
    val articleSignal by ArticleSignals.version.collectAsState()
    val openArticleId by ArticleSignals.openArticleId.collectAsState()
    val updateConfig by BudgieVersionCheck.config.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    val bookmarkedIds by produceState<Set<String>>(initialValue = emptySet(), articleSignal) {
        value = withContext(Dispatchers.IO) { BudgieArticleDatabase.get(context).bookmarkedIds() }
    }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = selectedItem != null || defaultOutletsOpen || settingsOpen || searchOpen || selectedEdition != null) {
        if (selectedItem != null) {
            selectedItem = null
            BudgieAudioReader.stop()
        } else if (defaultOutletsOpen) {
            defaultOutletsOpen = false
        } else if (settingsOpen) {
            settingsOpen = false
        } else if (searchOpen) {
            searchOpen = false
            searchQuery = ""
        } else if (selectedEdition != null) {
            selectedEdition = null
            selectedSource = SourceFilter.ALL
            searchQuery = ""
        }
    }

    fun saveSettings(updated: AppSettings) {
        settings = updated
        scope.launch {
            BudgiePrefs.saveAndSync(context, updated)
            BudgiePrefs.deviceToken(context).takeIf { it.isNotBlank() }?.let { token ->
                runCatching { BudgieAccountApi.registerDevice(context, token) }
                    .onFailure { if (!it.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(it) }
            }
        }
    }

    fun refresh() {
        val edition = selectedEdition ?: return
        state = FeedState.Loading
        scope.launch {
            state = fetchFeeds(context, selectedSection, selectedSource, settings, searchQuery, edition)
        }
        scope.launch {
            val fetched = HouseAdRepository.fetchHouseAds(context)
            houseAds = fetched
            houseAd = fetched.filter { it.isActive }.randomOrNull()
        }
    }

    LaunchedEffect(refreshToken, selectedEdition, selectedSection, selectedSource, settings.breakingNotificationsEnabled, settings.importantNotificationsEnabled, articleSignal, searchQuery) {
        val edition = selectedEdition ?: return@LaunchedEffect
        state = FeedState.Loading
        if (houseAds.isEmpty()) {
            val fetched = HouseAdRepository.fetchHouseAds(context)
            houseAds = fetched
            houseAd = fetched.filter { it.isActive }.randomOrNull()
        }
        state = fetchFeeds(context, selectedSection, selectedSource, settings, searchQuery, edition)
    }

    LaunchedEffect(refreshToken) {
        val fetched = HouseAdRepository.fetchHouseAds(context)
        houseAds = fetched
        houseAd = fetched.filter { it.isActive }.randomOrNull()
    }

    LaunchedEffect(openArticleId, state) {
        val articleId = openArticleId ?: return@LaunchedEffect
        if (selectedEdition == null) {
            selectedEdition = NewsEdition.GB
        }
        val feedItem = when (val value = state) {
            is FeedState.Ready -> value.items.firstOrNull { item ->
                item.id == articleId || item.link == articleId
            }
            else -> null
        } ?: withContext(Dispatchers.IO) {
            BudgieArticleDatabase.get(context).articleById(articleId)?.toFeedItem()
        }
        if (feedItem != null) {
            selectedItem = feedItem
            settingsOpen = false
            ArticleSignals.clearOpenRequest(articleId)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Paper,
        topBar = {
            TopAppBar(
                title = {
                    if (searchOpen && selectedItem == null && !settingsOpen && selectedEdition != null) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search news by keyword or topic...", fontSize = 14.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            textStyle = TextStyle(fontSize = 14.sp, color = Ink),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = AccentSoft,
                                focusedContainerColor = SurfaceRaised,
                                unfocusedContainerColor = SurfaceRaised,
                            ),
                            shape = RoundedCornerShape(6.dp),
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BudgieMark()
                            Spacer(Modifier.size(10.dp))
                            Column {
                                val currentEdition = selectedEdition
                                if (defaultOutletsOpen) {
                                    TypewriterText("Default News Outlets", color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    TypewriterText("Regional startup preferences", color = Muted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1)
                                } else if (currentEdition == null) {
                                    TypewriterText("Budgie News", color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    TypewriterText("Select Regional Edition", color = Muted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1)
                                } else {
                                    TypewriterText("Budgie News • ${currentEdition.flag} ${currentEdition.title}", color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    TypewriterText(selectedSource.tagline(selectedSection, currentEdition), color = Muted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (selectedEdition == null && !settingsOpen && !defaultOutletsOpen) {
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Ink)
                        }
                    } else if (selectedItem == null && !settingsOpen && !defaultOutletsOpen && selectedEdition != null) {
                        IconButton(onClick = {
                            if (searchOpen) {
                                searchOpen = false
                                searchQuery = ""
                            } else {
                                searchOpen = true
                            }
                        }) {
                            Icon(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = "Search", tint = Ink)
                        }
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Ink)
                        }
                        IconButton(onClick = { refreshToken++ }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh news", tint = Ink)
                        }
                    }
                },
                navigationIcon = {
                    if (selectedItem != null || defaultOutletsOpen || settingsOpen || searchOpen || selectedEdition != null) {
                        IconButton(onClick = {
                            if (selectedItem != null) {
                                selectedItem = null
                                BudgieAudioReader.stop()
                            } else if (defaultOutletsOpen) {
                                defaultOutletsOpen = false
                            } else if (settingsOpen) {
                                settingsOpen = false
                            } else if (searchOpen) {
                                searchOpen = false
                                searchQuery = ""
                            } else if (selectedEdition != null) {
                                selectedEdition = null
                                selectedSource = SourceFilter.ALL
                                searchQuery = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Ink)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
            )
        },
    ) { padding ->
        val detailItem = selectedItem
        if (defaultOutletsOpen) {
            DefaultOutletsScreen(
                settings = settings,
                onSettingsChanged = ::saveSettings,
                selectedEdition = selectedEdition,
                modifier = Modifier.padding(padding),
            )
        } else if (settingsOpen) {
            SettingsScreen(
                settings = settings,
                onSettingsChanged = ::saveSettings,
                onOpenDefaultOutlets = { defaultOutletsOpen = true },
                modifier = Modifier.padding(padding),
            )
        } else if (selectedEdition == null) {
            EditionSelectionScreen(
                onEditionSelected = { edition ->
                    selectedEdition = edition
                    selectedSource = if (edition == NewsEdition.GB) settings.defaultGbSource else settings.defaultUsaSource
                },
                onOpenSettings = { settingsOpen = true },
                modifier = Modifier.padding(padding),
            )
        } else {
            BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
                val useTwoPane = maxWidth >= 720.dp
                if (!useTwoPane && detailItem != null) {
                    StoryDetail(
                        item = detailItem,
                        houseAd = houseAd,
                        houseAds = houseAds,
                        isBookmarked = bookmarkedIds.contains(detailItem.id),
                        onToggleBookmark = { item -> scope.launch(Dispatchers.IO) { BudgieArticleDatabase.get(context).toggleBookmark(item) } },
                    )
                } else {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(if (useTwoPane && detailItem != null) 0.48f else 1f).fillMaxSize()) {
                            when (val value = state) {
                                FeedState.Loading -> LoadingNews()
                                is FeedState.Error -> ErrorNews(value.message, ::refresh)
                                is FeedState.Ready -> NewsList(
                                    items = value.items,
                                    selectedSection = selectedSection,
                                    selectedSource = selectedSource,
                                    selectedItem = detailItem,
                                    selectedEdition = selectedEdition ?: NewsEdition.GB,
                                    houseAd = houseAd,
                                    houseAds = houseAds,
                                    bookmarkedIds = bookmarkedIds,
                                    onToggleBookmark = { item -> scope.launch(Dispatchers.IO) { BudgieArticleDatabase.get(context).toggleBookmark(item) } },
                                    onSectionSelected = { selectedSection = it },
                                    onSourceSelected = { selectedSource = it },
                                    onStorySelected = { selectedItem = it },
                                )
                            }
                        }
                        if (useTwoPane) {
                            Spacer(Modifier.width(1.dp).fillMaxSize().background(AccentSoft))
                            Box(Modifier.weight(0.52f).fillMaxSize()) {
                                if (detailItem != null) {
                                    StoryDetail(
                                        item = detailItem,
                                        houseAd = houseAd,
                                        houseAds = houseAds,
                                        isBookmarked = bookmarkedIds.contains(detailItem.id),
                                        onToggleBookmark = { item -> scope.launch(Dispatchers.IO) { BudgieArticleDatabase.get(context).toggleBookmark(item) } },
                                    )
                                } else {
                                    DetailPlaceholder()
                                }
                            }
                        }
                    }
                }
            }
        }

        if (updateConfig.isOutdated) {
            AlertDialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                ),
                modifier = Modifier.fillMaxWidth(0.9f),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.PriorityHigh, contentDescription = null, tint = Color(0xFFE53935))
                        BudgieText("Update Required", color = Ink, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        BudgieText(
                            text = updateConfig.updateMessage,
                            color = Muted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    BudgieText("Current Version", color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
                                    BudgieText(context.appVersionText(), color = Alert, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    BudgieText("Required Version", color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
                                    BudgieText(updateConfig.minRequiredVersion, color = Color(0xFF4CAF50), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { context.openUrl(updateConfig.updateUrl) },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        BudgieText("Update Now", color = Color.White, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Paper,
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    onOpenDefaultOutlets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showLibrariesDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Paper),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            SettingsRow(
                title = "Default news outlets",
                description = "Configure separate startup newsrooms for GB News and USA News editions",
                onClick = onOpenDefaultOutlets,
            )
        }
        item {
            SettingsChoiceRow(
                title = "Default section",
                description = "Choose the news section shown first",
                value = settings.defaultSection.label,
                options = NewsSection.entries.map { it.label to { onSettingsChanged(settings.copy(defaultSection = it)) } },
            )
        }
        item {
            SettingsRow(
                title = "Notification settings",
                description = "Choose which Budgie News notifications you want to receive",
            )
        }
        item {
            SettingsSwitchRow(
                title = "Breaking alerts",
                description = "Notify only when the Breaking feed finds a new story.",
                checked = settings.breakingNotificationsEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(breakingNotificationsEnabled = it)) },
            )
        }
        item {
            SettingsSwitchRow(
                title = "Important alerts",
                description = "Notify only when the Important feed finds a new story.",
                checked = settings.importantNotificationsEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(importantNotificationsEnabled = it)) },
            )
        }
        item {
            SettingsSwitchRow(
                title = "Headlines alerts",
                description = "Notify when the Headlines feed finds a new story.",
                checked = settings.headlinesNotificationsEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(headlinesNotificationsEnabled = it)) },
            )
        }
        item {
            SettingsSwitchRow(
                title = "Send app statistics",
                description = "Budgie News uses this to analyse crashes, feed failures, and app quality.",
                checked = settings.sendAppStatistics,
                onCheckedChange = { onSettingsChanged(settings.copy(sendAppStatistics = it)) },
            )
        }
        item {
            SettingsRow(
                title = "Send technical feedback",
                description = "Send feedback and bug reports.",
                onClick = { showFeedbackDialog = true },
            )
        }
        item {
            SettingsRow(
                title = "What's new in v1.2.0",
                description = "View the latest release highlights and performance improvements.",
                onClick = { showChangelogDialog = true },
            )
        }
        item {
            SettingsRow(
                title = "Third party libraries",
                description = "Firebase, Coil, AndroidX, Kotlin, and Jetpack Compose.",
                onClick = { showLibrariesDialog = true },
            )
        }
        item {
            VersionFooter()
        }
    }

    if (showLibrariesDialog) {
        val libraries = listOf(
            "Firebase" to "https://firebase.google.com/",
            "Coil" to "https://coil-kt.github.io/coil/",
            "AndroidX" to "https://developer.android.com/jetpack/androidx",
            "Kotlin" to "https://kotlinlang.org/",
            "Jetpack Compose" to "https://developer.android.com/compose",
        )
        AlertDialog(
            onDismissRequest = { showLibrariesDialog = false },
            title = { BudgieText("Third party libraries", color = Ink, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BudgieText("Tap any library below to visit its official website and documentation:", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
                    libraries.forEach { (name, url) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { context.openUrl(url) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                BudgieText(name, color = Ink, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
                                BudgieText(url, color = Accent, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLibrariesDialog = false }) {
                    BudgieText("Close", color = Accent, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Paper,
        )
    }

    if (showFeedbackDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Bug") }
        var isSubmitting by remember { mutableStateOf(false) }
        var submitStatus by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showFeedbackDialog = false },
            title = { BudgieText("Send technical feedback", color = Ink, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    BudgieText("Report a bug, issue, or general feedback directly to our development team.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf("Bug", "Issue", "Feedback").forEach { option ->
                            Row(
                                modifier = Modifier.clickable { type = option },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = type == option,
                                    onClick = { type = option },
                                    colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = Muted)
                                )
                                BudgieText(option, color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { BudgieText("Title", color = Muted, fontSize = 12.sp, lineHeight = 16.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = AccentSoft,
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { BudgieText("Description / Content", color = Muted, fontSize = 12.sp, lineHeight = 16.sp) },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = AccentSoft,
                            focusedTextColor = Ink,
                            unfocusedTextColor = Ink,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (submitStatus != null) {
                        BudgieText(
                            text = submitStatus!!,
                            color = if (submitStatus!!.startsWith("Error") || submitStatus!!.startsWith("Please")) Color(0xFFE53935) else Accent,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isBlank() || content.isBlank()) {
                            submitStatus = "Please fill in both title and content."
                            return@Button
                        }
                        isSubmitting = true
                        submitStatus = "Sending..."
                        scope.launch(Dispatchers.IO) {
                            val success = sendDiscordWebhook(
                                webhookUrl = "https://discord.com/api/webhooks/1522933849234083841/6zzq9_TBDgjte6Ihxz53sfaWXHI0TN_Sen2VzxvcdbgveXQyYX_bMsr5BYZvQYE8QSZ_",
                                title = title,
                                content = content,
                                type = type,
                                version = context.appVersionText(),
                            )
                            withContext(Dispatchers.Main) {
                                isSubmitting = false
                                if (success) {
                                    submitStatus = "Sent successfully! Thank you."
                                    delay(1200)
                                    showFeedbackDialog = false
                                } else {
                                    submitStatus = "Error sending feedback. Please try again."
                                }
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
                ) {
                    BudgieText(if (isSubmitting) "Sending..." else "Send", color = Color.White, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }, enabled = !isSubmitting) {
                    BudgieText("Cancel", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
                }
            },
            containerColor = Paper,
        )
    }

    if (showChangelogDialog) {
        val changelogItems = listOf(
            "6 Free USA News Outlets & Push Notifications" to "Wired up live coverage from 6 major free US newsrooms (NPR, CBS News, ABC News, CNN, Fox News, NYT News) across Main and Politics RSS feeds (FeedSources and SourceFilter). Integrated USA background polling (FeedNotificationWorker) and device push notifications (BudgieMessagingService & BudgieAccountApi.registerDevice), syncing regional default choices (defaultUsaSource & defaultGbSource) to cloud push services.",
            "Separate Regional Background Workers" to "Split background feed polling into distinct regional workers (GbFeedNotificationWorker and UsaFeedNotificationWorker) so GB and USA feeds check independently and fast without scanning all 12 outlets sequentially.",
            "USA Image Thumbnail & Enclosure Sanitization" to "Filtered out audio/podcast .mp3/.mp4 enclosures on NPR/CBS, rejected 1x1 tracking GIFs, and retained Turner/CNN cleartext HTTP endpoints for reliable thumbnail extraction across all USA feeds.",
            "Badge Sizing & Full USA Brand Color Palette" to "Upgraded source pills from capped raw text (92.dp max) to uniform surface badges (minHeight = 24.dp) and added custom brand colors across NPR, CBS, ABC, CNN, Fox News, and NYT.",
            "Dedicated Default Outlets Page (DefaultOutletsScreen)" to "Created a separate, dedicated regional startup settings screen accessible from Shared App Settings → Default news outlets to configure startup newsroom feeds independently for GB and USA.",
            "Location Tracking & Permission Removal" to "Completely removed ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION permissions from AndroidManifest.xml and stripped out internal LocationManager providers.",
        )
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = { BudgieText("What's new in v1.2.0", color = Ink, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                ) {
                    items(changelogItems) { (feature, details) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BudgieText("• $feature", color = Ink, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                            BudgieText(details, color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChangelogDialog = false }) {
                    BudgieText("Got it", color = Accent, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Paper,
        )
    }
}

@Composable
private fun DefaultOutletsScreen(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    selectedEdition: NewsEdition?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Paper),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BudgieText("Regional Startup Feeds", color = Ink, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                BudgieText(
                    "Choose which newsroom outlet loads automatically when you launch each regional edition of Budgie News.",
                    color = Muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }
            HorizontalDivider(color = Accent, thickness = 1.dp)
        }
        item {
            SettingsChoiceRow(
                title = "🇬🇧 GB News Default Outlet",
                description = "Choose the UK newsroom shown first when you open the GB edition",
                value = settings.defaultGbSource.label,
                options = availableFiltersForEdition(NewsEdition.GB).map { filter ->
                    filter.label to {
                        onSettingsChanged(
                            settings.copy(
                                defaultGbSource = filter,
                                defaultSource = if (selectedEdition == NewsEdition.GB) filter else settings.defaultSource,
                            ),
                        )
                    }
                },
            )
        }
        item {
            SettingsChoiceRow(
                title = "🇺🇸 USA News Default Outlet",
                description = "Choose the US newsroom shown first when you open the USA edition",
                value = settings.defaultUsaSource.label,
                options = availableFiltersForEdition(NewsEdition.USA).map { filter ->
                    filter.label to {
                        onSettingsChanged(
                            settings.copy(
                                defaultUsaSource = filter,
                                defaultSource = if (selectedEdition == NewsEdition.USA) filter else settings.defaultSource,
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
) {
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BudgieText(title, color = Ink, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            BudgieText(description, color = Muted, fontSize = 15.sp, lineHeight = 22.sp, maxLines = 3)
        }
        HorizontalDivider(color = Accent, thickness = 1.dp)
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BudgieText(title, color = Ink, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                BudgieText(description, color = Muted, fontSize = 15.sp, lineHeight = 22.sp, maxLines = 4)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        HorizontalDivider(color = Accent, thickness = 1.dp)
    }
}

@Composable
private fun SettingsChoiceRow(
    title: String,
    description: String,
    value: String,
    options: List<Pair<String, () -> Unit>>,
) {
    Column {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BudgieText(title, color = Ink, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            BudgieText(description, color = Muted, fontSize = 15.sp, lineHeight = 22.sp, maxLines = 3)
            BudgieText(value, color = Accent, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { option ->
                    val selected = option.first == value
                    AssistChip(
                        onClick = option.second,
                        label = { Text(option.first, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        shape = RoundedCornerShape(6.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) Accent else SurfaceRaised,
                            labelColor = if (selected) Paper else Ink,
                        ),
                        border = BorderStroke(1.dp, if (selected) Accent else AccentSoft),
                    )
                }
            }
        }
        HorizontalDivider(color = Accent, thickness = 1.dp)
    }
}



@Composable
private fun VersionFooter() {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 36.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BudgieMark()
        Spacer(Modifier.size(14.dp))
        Column {
            BudgieText("Version", color = Ink, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            BudgieText(context.appVersionText(), color = Ink, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 1)
        }
    }
}

@Composable
private fun LoadingNews(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(Modifier.size(44.dp))
                repeat(3) { SkeletonBlock(Modifier.weight(1f).height(44.dp)) }
            }
        }
        item { SkeletonBlock(Modifier.fillMaxWidth().height(128.dp)) }
        item { SkeletonBlock(Modifier.fillMaxWidth().aspectRatio(1.7f)) }
        items(4) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(Modifier.size(92.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBlock(Modifier.fillMaxWidth().height(18.dp))
                    SkeletonBlock(Modifier.fillMaxWidth(0.76f).height(18.dp))
                    SkeletonBlock(Modifier.fillMaxWidth(0.52f).height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1100), repeatMode = RepeatMode.Restart),
        label = "skeleton_shimmer",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (shimmer < 0.5f) SurfaceDark else SurfaceRaised),
    )
}

@Composable
private fun BudgieText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
private fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 3,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun NewsList(
    items: List<FeedItem>,
    selectedSection: NewsSection,
    selectedSource: SourceFilter,
    selectedItem: FeedItem?,
    selectedEdition: NewsEdition = NewsEdition.GB,
    houseAd: HouseAd? = null,
    houseAds: List<HouseAd> = emptyList(),
    bookmarkedIds: Set<String> = emptySet(),
    onToggleBookmark: (FeedItem) -> Unit = {},
    onSectionSelected: (NewsSection) -> Unit,
    onSourceSelected: (SourceFilter) -> Unit,
    onStorySelected: (FeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "section_menu") {
            SectionMenu(
                selectedSection = selectedSection,
                selectedSource = selectedSource,
                selectedEdition = selectedEdition,
                onSectionSelected = onSectionSelected,
                onSourceSelected = onSourceSelected,
            )
        }
        item(key = "coverage_overview") {
            CoverageOverview(items, selectedSource, selectedEdition)
        }
        item(key = "feed_source_note") {
            FeedSourceNote(selectedSource, selectedEdition)
        }
        val activeAds = (houseAds + listOfNotNull(houseAd)).filter { it.isActive }.distinctBy { it.id }
        if (activeAds.isNotEmpty()) {
            val ad = activeAds.random()
            item(key = "house_ad_top_${ad.id}") {
                HouseAdBanner(ad)
            }
        }
        if (items.isEmpty()) {
            item(key = "empty_section") {
                EmptySection(selectedSection, selectedEdition)
            }
        } else {
            item(key = "lead_story_${items.first().id}") {
                LeadStory(
                    item = items.first(),
                    section = selectedSection,
                    isBookmarked = bookmarkedIds.contains(items.first().id),
                    onToggleBookmark = onToggleBookmark,
                    onStorySelected = onStorySelected,
                )
            }
            val remaining = items.drop(1)
            remaining.forEachIndexed { index, item ->
                item(key = "story_${item.id}") {
                    StoryCard(
                        item = item,
                        selected = item.id == selectedItem?.id,
                        isBookmarked = bookmarkedIds.contains(item.id),
                        onToggleBookmark = onToggleBookmark,
                        onStorySelected = onStorySelected,
                    )
                }
                val itemCount = index + 2
                if (activeAds.isNotEmpty() && itemCount % 4 == 0) {
                    val ad = activeAds.random()
                    item(key = "house_ad_${ad.id}_after_item_$itemCount") {
                        HouseAdBanner(ad)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorNews(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, AccentSoft),
        ) {
        Column(
            Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = Alert, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(14.dp))
            TypewriterText("Could not load the feed", color = Ink, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            TypewriterText(message, color = Muted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
            ) {
                Text("Try again", fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        }
    }
}

@Composable
private fun SectionMenu(
    selectedSection: NewsSection,
    selectedSource: SourceFilter,
    selectedEdition: NewsEdition = NewsEdition.GB,
    onSectionSelected: (NewsSection) -> Unit,
    onSourceSelected: (SourceFilter) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        items(NewsSection.entries) { section ->
            val selected = section == selectedSection
            AssistChip(
                onClick = { onSectionSelected(section) },
                label = { Text(section.label) },
                leadingIcon = {
                    Icon(
                        when (section) {
                            NewsSection.IMPORTANT -> Icons.Rounded.PriorityHigh
                            NewsSection.SAVED -> Icons.Rounded.Bookmark
                            else -> Icons.AutoMirrored.Rounded.Article
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                shape = RoundedCornerShape(6.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected) Accent else SurfaceDark,
                    labelColor = if (selected) Paper else Ink,
                    leadingIconContentColor = if (selected) Paper else Muted,
                ),
                border = BorderStroke(1.dp, if (selected) Accent else AccentSoft),
            )
        }
        items(availableFiltersForEdition(selectedEdition).filter { it != SourceFilter.ALL }) { source ->
            val selected = source == selectedSource
            AssistChip(
                onClick = { onSourceSelected(if (selected) SourceFilter.ALL else source) },
                label = { Text(source.label) },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Rounded.Article,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                shape = RoundedCornerShape(6.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (selected) Accent else SurfaceDark,
                    labelColor = if (selected) Paper else Ink,
                    leadingIconContentColor = if (selected) Paper else Muted,
                ),
                border = BorderStroke(1.dp, if (selected) Accent else AccentSoft),
            )
        }
    }
}

@Composable
private fun FeedSourceNote(selectedSource: SourceFilter, selectedEdition: NewsEdition = NewsEdition.GB) {
    TypewriterText(
        selectedSource.sourceNote(selectedEdition),
        color = Muted,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun CoverageOverview(items: List<FeedItem>, selectedSource: SourceFilter, selectedEdition: NewsEdition = NewsEdition.GB) {
    val visibleSources = items.map { it.source }.distinct().sorted()
    val editionSources = availableSourcesForEdition(selectedEdition)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, AccentSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                MetricTile("Stories", items.size.toString(), Modifier.weight(1f))
                MetricTile(
                    "Outlets",
                    if (selectedSource == SourceFilter.ALL) "${visibleSources.size}/${editionSources.size}" else "1/${editionSources.size}",
                    Modifier.weight(1f),
                )
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    TypewriterText("Coverage Lens", color = Ink, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    TypewriterText(
                        "Compare source framing before opening a story.",
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(editionSources) { source ->
                        val active = source.name in visibleSources
                        SourcePill(source.name.shortSourceName(), active)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(AccentSoft, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        TypewriterText(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, maxLines = 1)
        TypewriterText(label, color = Muted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1)
    }
}

@Composable
private fun EmptySection(section: NewsSection, edition: NewsEdition = NewsEdition.GB) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, AccentSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
            if (edition == NewsEdition.USA && section != NewsSection.SAVED) {
                TypewriterText(edition.emptyMessage, color = Ink, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 3)
                TypewriterText("Check back shortly as US news outlets go live, or use the back arrow above to return to the edition start screen.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 2)
            } else {
                TypewriterText(section.emptyText, color = Ink, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                TypewriterText("Try refresh, or switch to another menu section.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun EditionSelectionScreen(
    onEditionSelected: (NewsEdition) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BudgieMark()
                Spacer(Modifier.width(12.dp))
                Column {
                    TypewriterText("Welcome to Budgie News", color = Ink, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    TypewriterText("Select your regional edition to continue", color = Muted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(4.dp))
            TypewriterText(
                "Choose between regional editions. Both editions share your saved stories, audio reader preferences, and app notification settings.",
                color = Muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(8.dp))

            EditionCard(
                edition = NewsEdition.GB,
                statusTag = "Explore GB News Feeds →",
                description = "Live breaking alerts, headlines, and political coverage from BBC UK, Sky News, The Guardian, and The Sun.",
                onClick = { onEditionSelected(NewsEdition.GB) },
            )

            EditionCard(
                edition = NewsEdition.USA,
                statusTag = "Explore USA News Feeds →",
                description = "Live breaking alerts, headlines, and political coverage from NPR, CBS, ABC, CNN, Fox News, and NYT.",
                onClick = { onEditionSelected(NewsEdition.USA) },
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSettings),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, AccentSoft),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
                    Column {
                        TypewriterText("Shared App Settings", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        TypewriterText("Configure notifications & defaults", color = Muted, fontSize = 12.sp, maxLines = 1)
                    }
                }
                TypewriterText("Open →", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EditionCard(
    edition: NewsEdition,
    statusTag: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.5.dp, AccentSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(edition.flag, fontSize = 28.sp)
                    Column {
                        TypewriterText(edition.title, color = Ink, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                        TypewriterText(edition.subtitle, color = Accent, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                AssistChip(
                    onClick = onClick,
                    label = { Text(statusTag, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = AccentSoft,
                        labelColor = Ink,
                    ),
                    border = BorderStroke(1.dp, Accent),
                    shape = RoundedCornerShape(6.dp),
                )
            }
            TypewriterText(description, color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TypewriterText("Explore ${edition.label} Feeds →", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LeadStory(
    item: FeedItem?,
    section: NewsSection,
    isBookmarked: Boolean = false,
    onToggleBookmark: (FeedItem) -> Unit = {},
    onStorySelected: (FeedItem) -> Unit,
) {
    if (item == null) return
    var visible by remember(item.link) { mutableStateOf(false) }
    LaunchedEffect(item.link) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350)) { it / 8 },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStorySelected(item) },
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
            border = BorderStroke(1.dp, AccentSoft),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column {
                RemoteImage(item.imageUrl, Modifier.fillMaxWidth().aspectRatio(1.72f))
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypewriterText("Top Story | ${section.label}", color = Accent, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    TypewriterText(item.title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp, maxLines = 4)
                    if (item.description.isNotBlank()) {
                        TypewriterText(item.description, color = Muted, fontSize = 14.sp, maxLines = 3, lineHeight = 21.sp)
                    }
                    CoverageRow(item)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StoryMeta(item)
                        IconButton(onClick = { onToggleBookmark(item) }) {
                            Icon(
                                if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) Accent else Muted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseAdBanner(ad: HouseAd, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var visible by remember(ad.id) { mutableStateOf(false) }
    LaunchedEffect(ad.id) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350)) { it / 8 },
        modifier = modifier,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { context.openCustomTab(ad.targetUrl) },
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, AccentSoft),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!ad.iconUrl.isNullOrBlank()) {
                        RemoteImage(
                            url = ad.iconUrl,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TypewriterText(
                            text = ad.headline,
                            color = Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                        if (!ad.advertiser.isNullOrBlank()) {
                            TypewriterText(
                                text = ad.advertiser,
                                color = Muted,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AccentSoft,
                        border = BorderStroke(1.dp, Accent),
                    ) {
                        Text(
                            text = "Promo",
                            color = Accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (!ad.body.isNullOrBlank()) {
                    TypewriterText(
                        text = ad.body,
                        color = Muted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3
                    )
                }

                if (ad.starRating != null && ad.starRating > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "★ %.1f", ad.starRating),
                            color = Accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (ad.mediaUrl.isNotBlank()) {
                    RemoteImage(
                        url = ad.mediaUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2.2f)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }

                val ctaText = ad.callToAction?.takeIf { it.isNotBlank() } ?: "Learn More"
                Button(
                    onClick = { context.openCustomTab(ad.targetUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentSoft,
                        contentColor = Accent
                    ),
                    border = BorderStroke(1.dp, Accent),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = ctaText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryCard(
    item: FeedItem,
    selected: Boolean,
    isBookmarked: Boolean = false,
    onToggleBookmark: (FeedItem) -> Unit = {},
    onStorySelected: (FeedItem) -> Unit,
) {
    var visible by remember(item.link) { mutableStateOf(false) }
    LaunchedEffect(item.link) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { it / 10 },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onStorySelected(item) },
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = if (selected) SurfaceRaised else SurfaceDark),
            border = BorderStroke(1.dp, if (selected) Ink else Accent),
            elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp),
        ) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RemoteImage(item.imageUrl, Modifier.size(92.dp).clip(RoundedCornerShape(6.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypewriterText(item.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, lineHeight = 23.sp)
                    if (item.description.isNotBlank()) {
                        TypewriterText(item.description, color = Muted, fontSize = 13.sp, maxLines = 2, lineHeight = 19.sp)
                    }
                    CoverageRow(item)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            StoryMeta(item)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onToggleBookmark(item) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = "Bookmark",
                                    tint = if (isBookmarked) Accent else Muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open story", tint = Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryDetail(
    item: FeedItem,
    houseAd: HouseAd? = null,
    houseAds: List<HouseAd> = emptyList(),
    isBookmarked: Boolean = false,
    onToggleBookmark: (FeedItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val activeAds = (houseAds + listOfNotNull(houseAd)).filter { it.isActive }.distinctBy { it.id }
        if (activeAds.isNotEmpty()) {
            val ad = activeAds.random()
            item(key = "story_detail_ad_${ad.id}") {
                HouseAdBanner(ad)
            }
        }
        item {
            RemoteImage(item.imageUrl, Modifier.fillMaxWidth().aspectRatio(1.78f))
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, AccentSoft),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SourcePill(item.source.shortSourceName(), active = true)
                        TypewriterText(item.publishedAt, color = Muted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1)
                    }
                    TypewriterText(item.title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp)
                    QuickRead(item)
                    CoverageRow(item)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isAudioPlaying by BudgieAudioReader.isPlaying.collectAsState()
                        val isAudioLoading by BudgieAudioReader.isLoading.collectAsState()
                        val audioProgress by BudgieAudioReader.playbackProgress.collectAsState()
                        val remainingSec by BudgieAudioReader.remainingTimeSeconds.collectAsState()
                        val activeAudioId by BudgieAudioReader.currentArticleId.collectAsState()
                        val currentRate by BudgieAudioReader.speechRate.collectAsState()
                        val playingThis = isAudioPlaying && activeAudioId == item.id
                        val loadingThis = isAudioLoading && activeAudioId == item.id

                        Button(
                            onClick = { BudgieAudioReader.togglePlay(context, item) },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (playingThis || loadingThis) AccentSoft else SurfaceRaised,
                                contentColor = if (playingThis || loadingThis) Accent else Ink,
                            ),
                            border = BorderStroke(1.dp, if (playingThis || loadingThis) Accent else AccentSoft),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (loadingThis) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Accent,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = "Connecting...",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 1,
                                )
                            } else if (playingThis) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
                                    CircularProgressIndicator(
                                        progress = { audioProgress },
                                        modifier = Modifier.size(20.dp),
                                        color = Accent,
                                        trackColor = Accent.copy(alpha = 0.25f),
                                        strokeWidth = 2.2.dp,
                                    )
                                    Icon(
                                        Icons.Rounded.Stop,
                                        contentDescription = "Stop",
                                        modifier = Modifier.size(12.dp),
                                        tint = Accent,
                                    )
                                }
                                Spacer(Modifier.size(8.dp))
                                val remText = if (remainingSec > 0) "-${remainingSec / 60}:${(remainingSec % 60).toString().padStart(2, '0')}" else ""
                                Text(
                                    text = if (remText.isNotEmpty()) "Listening (${currentRate}x) • $remText" else "Listening (${currentRate}x)",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 1,
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = "Listen to Summary",
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 1,
                                )
                            }
                        }
                        if (playingThis) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SurfaceRaised,
                                border = BorderStroke(1.dp, AccentSoft),
                                modifier = Modifier.clickable { BudgieAudioReader.cycleSpeed() },
                            ) {
                                Text(
                                    text = "${currentRate}x",
                                    color = Ink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                )
                            }
                        }
                        IconButton(
                            onClick = { onToggleBookmark(item) },
                            modifier = Modifier.background(SurfaceRaised, RoundedCornerShape(6.dp)).padding(2.dp),
                        ) {
                            Icon(
                                if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Bookmark story",
                                tint = if (isBookmarked) Accent else Ink,
                            )
                        }
                    }
                    val isAudioPlayingAfter by BudgieAudioReader.isPlaying.collectAsState()
                    val isAudioLoadingAfter by BudgieAudioReader.isLoading.collectAsState()
                    val audioProgressAfter by BudgieAudioReader.playbackProgress.collectAsState()
                    val remainingSecAfter by BudgieAudioReader.remainingTimeSeconds.collectAsState()
                    val activeAudioIdAfter by BudgieAudioReader.currentArticleId.collectAsState()
                    if ((isAudioPlayingAfter || isAudioLoadingAfter) && activeAudioIdAfter == item.id) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { if (isAudioLoadingAfter) 0f else audioProgressAfter },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp)),
                                color = Accent,
                                trackColor = SurfaceRaised,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = if (isAudioLoadingAfter) "Connecting to neural voice engine..." else "${(audioProgressAfter * 100).toInt()}% complete",
                                    color = Muted,
                                    fontSize = 11.sp,
                                )
                                if (isAudioPlayingAfter && remainingSecAfter > 0) {
                                    Text(
                                        text = "${remainingSecAfter / 60}:${(remainingSecAfter % 60).toString().padStart(2, '0')} remaining",
                                        color = Muted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { context.openUrl(item.link) },
                        enabled = item.link.isNotBlank(),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Read official source", fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailPlaceholder() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, AccentSoft),
        ) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = Accent, modifier = Modifier.size(36.dp))
                TypewriterText("Select a story", color = Ink, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                TypewriterText("Article details will stay open beside the feed on larger screens.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3)
            }
        }
    }
}

@Composable
private fun QuickRead(item: FeedItem) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        TypewriterText("Quick read", color = Accent, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        item.quickReadPoints().forEach { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("-", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
                TypewriterText(point, color = Muted, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f), maxLines = 5)
            }
        }
    }
}

internal fun FeedItem.quickReadPoints(): List<String> {
    val points = mutableListOf<String>()
    points += "Main story: $title"
    if (description.isNotBlank()) points += description
    points += "Source: $source${publishedAt.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()}"
    points += "Coverage: ${coverageSources.joinToString { it.shortSourceName() }}"
    return points
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoverageRow(item: FeedItem) {
    val totalSources = FeedSources.count { source -> source.name in item.coverageSources || source.edition == (FeedSources.firstOrNull { it.name == item.source }?.edition ?: NewsEdition.GB) }.coerceAtLeast(1)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            "Covered by ${item.coverageSources.size}/$totalSources",
            color = Accent,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        item.coverageSources.take(2).forEach { source ->
            SourcePill(source.shortSourceName(), active = true)
        }
    }
}

@Composable
private fun SourcePill(label: String, active: Boolean) {
    val (bgColor, textColor) = if (active) brandColors(label) else AccentSoft to Muted
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, if (active) bgColor else AccentSoft),
        modifier = Modifier.heightIn(min = 24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                label,
                color = textColor,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun brandColors(label: String): Pair<Color, Color> = when {
    label.contains("BBC", ignoreCase = true) -> Color(0xFFB80000) to Ink
    label.contains("Sky", ignoreCase = true) -> Color(0xFF003399) to Ink
    label.contains("Guard", ignoreCase = true) -> Color(0xFF052962) to Ink
    label.contains("Sun", ignoreCase = true) -> Color(0xFFED1C24) to Ink
    label.contains("NPR", ignoreCase = true) -> Color(0xFF1A5E9A) to Ink
    label.contains("CBS", ignoreCase = true) -> Color(0xFF003366) to Ink
    label.contains("ABC", ignoreCase = true) -> Color(0xFF1E4E79) to Ink
    label.contains("CNN", ignoreCase = true) -> Color(0xFFCC0000) to Ink
    label.contains("Fox", ignoreCase = true) -> Color(0xFF002244) to Ink
    label.contains("NYT", ignoreCase = true) || label.contains("Times", ignoreCase = true) -> Color(0xFF222222) to Ink
    else -> Accent to Ink
}

@Composable
private fun StoryMeta(item: FeedItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TypewriterText(
            listOf(item.source, item.publishedAt).filter { it.isNotBlank() }.joinToString("  |  "),
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
        )
    }
}

@Composable
private fun RemoteImage(url: String?, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "imagePlaceholderPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "imagePlaceholderAlpha",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.Article,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier
                .size(30.dp)
                .graphicsLayer(alpha = pulse),
        )
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun BudgieMark() {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(6.dp),
        color = SurfaceRaised,
        border = BorderStroke(1.dp, AccentSoft),
    ) {
        Image(
            painter = painterResource(R.drawable.budgie_news_reader),
            contentDescription = null,
            modifier = Modifier.padding(5.dp),
        )
    }
}

private suspend fun fetchFeeds(
    context: Context,
    section: NewsSection,
    sourceFilter: SourceFilter,
    settings: AppSettings,
    query: String = "",
    edition: NewsEdition = NewsEdition.GB,
): FeedState = withContext(Dispatchers.IO) {
    BudgieCache.checkReset(context)
    runCatching {
        val sources = availableSourcesForEdition(edition)
            .filter { sourceFilter.sourceName == null || it.name == sourceFilter.sourceName }
        val localItems = BudgieArticleDatabase.get(context)
            .recentArticles()
            .map { it.toFeedItem() }
            .filter { isFreeNewsSource(it.source) }
            .filter { item ->
                val feedSource = FeedSources.firstOrNull { it.name == item.source }
                feedSource?.edition == null || feedSource.edition == edition
            }
            .filter { sourceFilter.sourceName == null || it.source == sourceFilter.sourceName }
        val loadedItems = if (section == NewsSection.SAVED) {
            emptyList()
        } else if (sources.isEmpty()) {
            emptyList()
        } else {
            sources
                .map { source ->
                    runCatching { fetchFeed(source).take(12) }
                        .onFailure { if (!it.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(it) }
                        .getOrDefault(emptyList())
                }
                .interleaved()
                .distinctBy { it.link.ifBlank { it.title } }
        }
        val availableItems = if (loadedItems.isNotEmpty()) {
            BudgieCache.save(context, loadedItems)
            loadedItems
        } else if (section == NewsSection.SAVED) {
            emptyList()
        } else {
            BudgieCache.load(context)
                .filter { isFreeNewsSource(it.source) }
                .filter { item ->
                    val feedSource = FeedSources.firstOrNull { it.name == item.source }
                    feedSource?.edition == null || feedSource.edition == edition
                }
                .filter { item -> sourceFilter.sourceName == null || item.source == sourceFilter.sourceName }
                .distinctBy { it.link.ifBlank { it.title } }
        }
        val allItems = if (section == NewsSection.SAVED) {
            BudgieArticleDatabase.get(context).allBookmarks().map { it.toFeedItem() }
        } else {
            (localItems + availableItems).distinctBy { it.link.ifBlank { it.title } }
        }
        val filteredItems = if (section == NewsSection.SAVED) {
            allItems
        } else {
            allItems.filterFor(section).take(24).withCoverageContext()
        }
        val searchedItems = if (query.isNotBlank()) {
            filteredItems.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true) ||
                item.source.contains(query, ignoreCase = true)
            }
        } else {
            filteredItems
        }
        if (searchedItems.isEmpty() && section != NewsSection.SAVED && query.isBlank()) {
            if (sources.isEmpty()) {
                return@withContext FeedState.Ready(emptyList())
            }
            val sourceNames = sources.joinToString { it.name }
            error("No stories loaded from $sourceNames")
        }
        searchedItems
    }.fold(
        onSuccess = { items ->
            FeedState.Ready(items)
        },
        onFailure = {
            if (!it.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(it)
            FeedState.Error(it.message ?: "Unexpected feed error")
        },
    )
}

internal fun isFreeNewsSource(source: String): Boolean {
    if (source.contains("NYT", ignoreCase = true) || source.contains("New York Times", ignoreCase = true)) return true
    val paywalled = listOf("Financial Times", "FT", "Independent", "Indy", "Daily Mail", "Mail Online", "Mail+", "Telegraph", "The Times", "Times UK")
    return paywalled.none { source.contains(it, ignoreCase = true) }
}

internal fun LocalArticle.toFeedItem(): FeedItem =
    FeedItem(
        id = articleId,
        title = title,
        description = description,
        link = link,
        source = source,
        publishedAt = publishedAt,
        imageUrl = imageUrl,
        coverageSources = listOf(source),
    )

private fun List<List<FeedItem>>.interleaved(): List<FeedItem> {
    val result = mutableListOf<FeedItem>()
    val maxSize = maxOfOrNull { it.size } ?: return result
    for (index in 0 until maxSize) {
        forEach { sourceItems ->
            sourceItems.getOrNull(index)?.let(result::add)
        }
    }
    return result
}

private fun List<FeedItem>.withCoverageContext(): List<FeedItem> = map { item ->
    val relatedSources = filter { candidate ->
        candidate.source == item.source || item.relatedTo(candidate)
    }
        .map { it.source }
        .distinct()
        .sorted()
    item.copy(coverageSources = relatedSources.ifEmpty { listOf(item.source) })
}

internal fun fetchFeed(source: FeedSource): List<FeedItem> {
    val connection = (URL(source.url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 15_000
        setRequestProperty("User-Agent", "Budgie News Android")
    }
    try {
        if (connection.responseCode !in 200..299) {
            error("${source.name} returned HTTP ${connection.responseCode}")
        }
        return connection.inputStream.use { stream ->
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(stream, null)
            }
            parseRss(parser, source.name)
        }
    } finally {
        connection.disconnect()
    }
}

private fun parseRss(parser: XmlPullParser, fallbackSource: String): List<FeedItem> {
    val items = mutableListOf<FeedItem>()
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG &&
            (parser.name.equals("item", ignoreCase = true) || parser.name.equals("entry", ignoreCase = true))
        ) {
            readItem(parser, fallbackSource, parser.name)?.let { items += it }
        }
        event = parser.next()
    }
    return items.take(40)
}

private fun readItem(parser: XmlPullParser, fallbackSource: String, containerTag: String): FeedItem? {
    var title = ""
    var description = ""
    var link = ""
    var source = fallbackSource
    var pubDate = ""
    var rawPubDate = ""
    var imageUrl: String? = null

    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.END_TAG && parser.name.equals(containerTag, ignoreCase = true)) break
        if (parser.eventType != XmlPullParser.START_TAG) continue

        when (parser.name.lowercase()) {
            "title" -> title = parser.readText()
            "description", "summary" -> {
                val rawText = parser.readText()
                val descImg = rawText.firstImageUrl()
                if (descImg != null && (imageUrl == null || imageUrl.endsWith(".mp3", ignoreCase = true) || imageUrl.endsWith(".mp4", ignoreCase = true) || descImg.contains("large", ignoreCase = true) || descImg.contains("high", ignoreCase = true))) {
                    imageUrl = descImg
                }
                description = rawText.stripHtml()
            }
            "link" -> {
                val href = parser.attributeValue("href")
                if (href.isNullOrBlank()) {
                    link = parser.readText()
                } else {
                    link = href
                    parser.skipTag()
                }
            }
            "pubdate", "published", "updated" -> {
                rawPubDate = parser.readText()
                pubDate = rawPubDate.formatNewsDate()
            }
            "source" -> source = parser.readText().ifBlank { source }
            "media:group", "image" -> {
                // Do not skipTag; enter tag to find nested media:content, media:thumbnail, or url tags
            }
            "enclosure", "thumbnail", "media:thumbnail", "media:content", "url" -> {
                val typeAttr = parser.attributeValue("type") ?: ""
                val mediumAttr = parser.attributeValue("medium") ?: ""
                if (!typeAttr.contains("audio", ignoreCase = true) && !typeAttr.contains("video", ignoreCase = true) &&
                    !mediumAttr.equals("audio", ignoreCase = true) && !mediumAttr.equals("video", ignoreCase = true)) {
                    val extractedUrl = parser.attributeValue("url")
                        ?: parser.attributeValue("href")
                        ?: parser.attributeValue("src")
                        ?: runCatching { parser.readText() }.getOrNull()?.takeIf { it.startsWith("http", ignoreCase = true) }
                    if (extractedUrl != null && !extractedUrl.endsWith(".mp3", ignoreCase = true) &&
                        !extractedUrl.endsWith(".mp4", ignoreCase = true) && !extractedUrl.endsWith(".m4a", ignoreCase = true) &&
                        !extractedUrl.endsWith(".wav", ignoreCase = true) && !extractedUrl.endsWith(".aac", ignoreCase = true) &&
                        !extractedUrl.contains("audio/", ignoreCase = true) && !extractedUrl.contains("video/", ignoreCase = true) &&
                        !extractedUrl.contains("pixel", ignoreCase = true) && !extractedUrl.contains("tracking", ignoreCase = true) &&
                        !extractedUrl.contains("beacon", ignoreCase = true) && !extractedUrl.endsWith(".gif", ignoreCase = true)) {
                        
                        val cleanExtracted = if (extractedUrl.startsWith("http://", ignoreCase = true) && !extractedUrl.contains("turner.com", ignoreCase = true) && !extractedUrl.contains("cnn.com", ignoreCase = true)) {
                            extractedUrl.replaceFirst("http://", "https://")
                        } else {
                            extractedUrl
                        }
                        
                        val current = imageUrl
                        if (current == null || current.endsWith(".mp3", ignoreCase = true) || current.endsWith(".mp4", ignoreCase = true) ||
                            cleanExtracted.contains("large", ignoreCase = true) || cleanExtracted.contains("high", ignoreCase = true) ||
                            cleanExtracted.contains("1024", ignoreCase = true) || cleanExtracted.contains("1280", ignoreCase = true) ||
                            cleanExtracted.contains("1920", ignoreCase = true) || cleanExtracted.contains("3000", ignoreCase = true)) {
                            imageUrl = cleanExtracted
                        }
                    }
                }
                if (parser.eventType == XmlPullParser.START_TAG) parser.skipTag()
            }
            "content:encoded", "content" -> {
                val encodedContent = parser.readText()
                val encodedImg = encodedContent.firstImageUrl()
                if (encodedImg != null && (imageUrl == null || imageUrl.endsWith(".mp3", ignoreCase = true) || imageUrl.endsWith(".mp4", ignoreCase = true) || encodedImg.contains("large", ignoreCase = true) || encodedImg.contains("high", ignoreCase = true))) {
                    imageUrl = encodedImg
                }
                if (description.isBlank()) description = encodedContent.stripHtml()
            }
            else -> parser.skipTag()
        }
    }

    val pubMillis = rawPubDate.toEpochMillisOrNull()
    if (pubMillis != null && pubMillis < BudgieTime.minAllowedMillis()) return null

    val cleanImageUrl = imageUrl
        ?.takeIf {
            !it.contains("pixel", ignoreCase = true) &&
            !it.contains("tracking", ignoreCase = true) &&
            !it.contains("beacon", ignoreCase = true) &&
            !it.endsWith(".gif", ignoreCase = true) &&
            !it.endsWith(".mp3", ignoreCase = true) &&
            !it.endsWith(".mp4", ignoreCase = true) &&
            !it.endsWith(".m4a", ignoreCase = true)
        }
        ?.let { url ->
            if (url.startsWith("http://", ignoreCase = true) && !url.contains("turner.com", ignoreCase = true) && !url.contains("cnn.com", ignoreCase = true)) {
                url.replaceFirst("http://", "https://")
            } else {
                url
            }
        }

    return FeedItem(
        id = link.ifBlank { title },
        title = title.stripHtml().ifBlank { "Untitled story" },
        description = description,
        link = link,
        source = source.stripHtml(),
        publishedAt = pubDate,
        imageUrl = cleanImageUrl,
    )
}

internal fun List<FeedItem>.filterFor(section: NewsSection): List<FeedItem> = when (section) {
    NewsSection.HEADLINES, NewsSection.SAVED -> this
    NewsSection.BREAKING -> filter { item ->
        item.matchesAny(
            "breaking",
            "live",
            "urgent",
            "developing",
            "latest",
            "alert",
            "updates",
        )
    }
    NewsSection.IMPORTANT -> filter { item ->
        item.matchesAny(
            "world",
            "government",
            "election",
            "parliament",
            "president",
            "minister",
            "court",
            "war",
            "conflict",
            "climate",
            "health",
            "economy",
            "security",
        )
    }
    NewsSection.GENERAL -> {
        val general = filter { !it.matchesAny("breaking", "live", "urgent", "developing", "alert", "updates") }
        general.ifEmpty { this }
    }
}

private fun FeedItem.matchesAny(vararg keywords: String): Boolean {
    val haystack = "$title $description $source".lowercase()
    return keywords.any { keyword -> haystack.contains(keyword) }
}

private fun FeedItem.relatedTo(other: FeedItem): Boolean {
    if (this === other) return true
    val mine = title.storyTokens()
    val theirs = other.title.storyTokens()
    if (mine.size < 3 || theirs.size < 3) return false
    val overlap = mine.intersect(theirs).size
    return overlap >= 3
}

private fun SourceFilter.tagline(section: NewsSection, edition: NewsEdition = NewsEdition.GB): String = when (this) {
    SourceFilter.ALL -> if (edition == NewsEdition.USA) "Live breaking alerts, headlines & reporting across the US" else section.tagline
    else -> "${label} ${section.label.lowercase()}"
}

private fun SourceFilter.sourceNote(edition: NewsEdition = NewsEdition.GB): String = when (this) {
    SourceFilter.ALL -> if (edition == NewsEdition.USA) "Reporting from ${availableSourcesForEdition(edition).size} leading US newsrooms" else "Reporting from ${availableSourcesForEdition(edition).size} leading UK newsrooms"
    else -> "Showing $label stories only"
}

private fun String.shortSourceName(): String = when {
    contains("BBC", ignoreCase = true) -> "BBC"
    contains("Sky", ignoreCase = true) -> "Sky"
    contains("Guardian", ignoreCase = true) -> "Guardian"
    contains("Sun", ignoreCase = true) -> "Sun"
    contains("NPR", ignoreCase = true) -> "NPR"
    contains("CBS", ignoreCase = true) -> "CBS"
    contains("ABC", ignoreCase = true) -> "ABC"
    contains("CNN", ignoreCase = true) -> "CNN"
    contains("Fox", ignoreCase = true) -> "Fox"
    contains("NYT", ignoreCase = true) || contains("Times", ignoreCase = true) -> "NYT"
    else -> this
}

private fun String.storyTokens(): Set<String> =
    lowercase()
        .split(Regex("""[^a-z0-9]+"""))
        .filter { it.length >= 4 && it !in StoryStopWords }
        .toSet()

private val StoryStopWords = setOf(
    "after",
    "from",
    "have",
    "into",
    "latest",
    "more",
    "news",
    "over",
    "says",
    "that",
    "their",
    "this",
    "with",
)

private fun XmlPullParser.readText(): String {
    var result = ""
    var depth = 1
    while (depth > 0) {
        when (next()) {
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.START_TAG -> depth++
            XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (depth == 1) result += text.orEmpty()
            XmlPullParser.END_DOCUMENT -> return result.trim()
        }
    }
    return result.trim()
}

private fun XmlPullParser.skipTag() {
    if (eventType != XmlPullParser.START_TAG) return
    var depth = 1
    while (depth != 0) {
        when (next()) {
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.START_TAG -> depth++
            XmlPullParser.END_DOCUMENT -> return
        }
    }
}

private fun XmlPullParser.attributeValue(name: String): String? {
    for (index in 0 until attributeCount) {
        if (getAttributeName(index).equals(name, ignoreCase = true)) return getAttributeValue(index)
    }
    return null
}

private fun String.stripHtml(): String =
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace(Regex("\\r\\n|\\r|\\n"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun String.firstImageUrl(): String? {
    val matches = Regex("""<(?:img|media|figure)[^>]+(?:src|data-src|data-original|url)=['"]?([^'"\s>]+)['"]?""", RegexOption.IGNORE_CASE).findAll(this)
    for (match in matches) {
        val rawUrl = match.groupValues.getOrNull(1)?.replace("&amp;", "&")?.trim() ?: continue
        if (rawUrl.contains("pixel", ignoreCase = true) || rawUrl.contains("tracking", ignoreCase = true) ||
            rawUrl.contains("beacon", ignoreCase = true) || rawUrl.contains("1x1", ignoreCase = true) ||
            rawUrl.endsWith(".gif", ignoreCase = true) || rawUrl.endsWith(".mp3", ignoreCase = true) ||
            rawUrl.endsWith(".mp4", ignoreCase = true) || rawUrl.endsWith(".m4a", ignoreCase = true) ||
            rawUrl.contains("audio/", ignoreCase = true) || rawUrl.contains("video/", ignoreCase = true)) {
            continue
        }
        val cleanUrl = if (rawUrl.contains("url=", ignoreCase = true) && rawUrl.contains("http", ignoreCase = true) &&
            !rawUrl.contains("brightspotcdn", ignoreCase = true) && !rawUrl.contains("dims3", ignoreCase = true) &&
            !rawUrl.contains("npr.org", ignoreCase = true) && !rawUrl.contains("cbsnews", ignoreCase = true) &&
            !rawUrl.contains("cbsistatic", ignoreCase = true) && !rawUrl.contains("abcnews", ignoreCase = true) &&
            !rawUrl.contains("turner.com", ignoreCase = true) && !rawUrl.contains("cnn.com", ignoreCase = true) &&
            !rawUrl.contains("foxnews", ignoreCase = true) && !rawUrl.contains("nytimes", ignoreCase = true) &&
            !rawUrl.contains("nyt.com", ignoreCase = true) && !rawUrl.contains("bbci", ignoreCase = true) &&
            !rawUrl.contains("skynews", ignoreCase = true) && !rawUrl.contains("theguardian", ignoreCase = true) &&
            !rawUrl.contains("thesun", ignoreCase = true)) {
            rawUrl.extractNestedImageUrl()
        } else {
            rawUrl
        }
        return if (cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.contains("turner.com", ignoreCase = true) && !cleanUrl.contains("cnn.com", ignoreCase = true)) {
            cleanUrl.replaceFirst("http://", "https://")
        } else {
            cleanUrl
        }
    }
    return null
}

private fun String.extractNestedImageUrl(): String {
    val nestedUrl = runCatching { Uri.parse(this).getQueryParameter("url") }.getOrNull()
    return if (nestedUrl.isNullOrBlank()) this else URLDecoder.decode(nestedUrl, Charsets.UTF_8.name())
}

private fun String.toEpochMillisOrNull(): Long? =
    runCatching {
        ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    }.recoverCatching {
        ZonedDateTime.parse(this).toInstant().toEpochMilli()
    }.getOrNull()

private fun String.formatNewsDate(): String =
    runCatching {
        DateTimeFormatter.ofPattern("MMM d, h:mm a").format(ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME))
    }.recoverCatching {
        DateTimeFormatter.ofPattern("MMM d, h:mm a").format(ZonedDateTime.parse(this))
    }.getOrDefault(this)

private fun android.content.Context.openUrl(url: String) {
    if (url.isBlank()) return
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

private fun android.content.Context.openCustomTab(url: String) {
    if (url.isBlank()) return
    runCatching {
        val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
    }.onFailure {
        openUrl(url)
    }
}

private fun Context.appVersionText(): String {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return packageInfo.versionName ?: "0.1.0-beta"
}

private fun sendDiscordWebhook(
    webhookUrl: String,
    title: String,
    content: String,
    type: String,
    version: String,
): Boolean = runCatching {
    val url = java.net.URL(webhookUrl)
    val conn = url.openConnection() as java.net.HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
    conn.doOutput = true
    conn.connectTimeout = 8000
    conn.readTimeout = 8000

    val payload = org.json.JSONObject().apply {
        put("content", "**New Budgie News Feedback** ($version)")
        put("embeds", org.json.JSONArray().put(
            org.json.JSONObject().apply {
                put("title", "[$type] $title")
                put("description", content)
                put("color", when (type) {
                    "Bug" -> 0xE53935
                    "Issue" -> 0xFB8C00
                    else -> 0x1E88E5
                })
                put("fields", org.json.JSONArray().apply {
                    put(org.json.JSONObject().put("name", "Type").put("value", type).put("inline", true))
                    put(org.json.JSONObject().put("name", "Version").put("value", version).put("inline", true))
                    put(org.json.JSONObject().put("name", "Android / Device").put("value", "Android ${Build.VERSION.RELEASE} (${Build.MODEL})").put("inline", false))
                })
            }
        ))
    }.toString()

    conn.outputStream.use { os ->
        os.write(payload.toByteArray(Charsets.UTF_8))
        os.flush()
    }

    val responseCode = conn.responseCode
    conn.disconnect()
    responseCode in 200..299
}.onFailure {
    if (!it.isExpectedFirestoreMissingError()) FirebaseCrashlytics.getInstance().recordException(it)
}.getOrDefault(false)

@Composable
private fun BudgieNewsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Paper,
            background = Paper,
            onBackground = Ink,
            surface = SurfaceDark,
            onSurface = Ink,
        ),
        content = content,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NewsPreview() {
    BudgieNewsTheme {
        NewsList(
            listOf(
                FeedItem(
                    id = "preview-bbc",
                    title = "Markets and politics drive a busy morning briefing",
                    description = "A compact summary of the biggest stories available from the test RSS feed.",
                    link = "https://www.bbc.co.uk/news/uk",
                    source = "BBC UK",
                    publishedAt = "Jun 23, 7:10 PM",
                    imageUrl = null,
                    coverageSources = listOf("BBC UK", "Sky News UK"),
                ),
                FeedItem(
                    id = "preview-sky",
                    title = "UK leaders respond as major policy announcements continue",
                    description = "Latest updates are displayed in a scrollable modern news feed.",
                    link = "https://news.sky.com/uk",
                    source = "Sky News UK",
                    publishedAt = "Jun 23, 6:42 PM",
                    imageUrl = null,
                    coverageSources = listOf("Sky News UK"),
                ),
            ),
            selectedSection = NewsSection.HEADLINES,
            selectedSource = SourceFilter.ALL,
            selectedItem = null,
            houseAd = null,
            houseAds = emptyList(),
            onSectionSelected = {},
            onSourceSelected = {},
            onStorySelected = {},
            modifier = Modifier.background(Paper),
        )
    }
}
