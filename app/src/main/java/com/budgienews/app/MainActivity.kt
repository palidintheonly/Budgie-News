package com.budgienews.app

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricPrompt
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.Settings
import android.text.Html
import android.util.Xml
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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

private val FeedSources = listOf(
    FeedSource("BBC UK", "https://feeds.bbci.co.uk/news/uk/rss.xml"),
    FeedSource("Sky News UK", "https://feeds.skynews.com/feeds/rss/uk.xml"),
    FeedSource("Sky Politics", "https://feeds.skynews.com/feeds/rss/politics.xml"),
    FeedSource("Guardian UK", "https://www.theguardian.com/uk/rss"),
    FeedSource("Guardian Politics", "https://www.theguardian.com/politics/rss"),
    FeedSource("Independent UK", "https://www.independent.co.uk/news/uk/rss"),
    FeedSource("Daily Mail News", "https://www.dailymail.co.uk/news/index.rss"),
    FeedSource("The Sun News", "https://www.thesun.co.uk/news/feed/"),
    FeedSource("Financial Times UK", "https://www.ft.com/uk?format=rss"),
)

private val UkLocationOptions = listOf(
    "United Kingdom",
    "England",
    "Scotland",
    "Wales",
    "Northern Ireland",
    "London",
    "North West",
    "North East",
    "Yorkshire",
    "Midlands",
    "South East",
    "South West",
)

class MainActivity : ComponentActivity() {
    private var isUnlocked by mutableStateOf(false)
    private var authMessage by mutableStateOf("Unlock Budgie News to continue.")
    private var authInProgress = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updateLocationPreference()
        }

    private val credentialLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            authInProgress = false
            if (result.resultCode == Activity.RESULT_OK) {
                unlockApp()
            } else {
                authMessage = "Authentication cancelled."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BudgieNotifications.ensureChannels(this)
        handleArticleIntent(intent)
        BudgieFirebase.setup(this)
        if (!BudgiePrefs.load(this).biometricEnabled) {
            isUnlocked = true
        }
        setContent {
            BudgieNewsTheme {
                if (isUnlocked) {
                    NewsApp(onBiometricSettingChanged = { enabled ->
                        if (enabled) authenticate()
                    })
                } else {
                    LockedApp(authMessage, onUnlock = { authenticate() })
                }
            }
        }
        if (!isUnlocked) {
            window.decorView.post { authenticate() }
        } else {
            requestRequiredPermissionsIfNeeded()
        }
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
        requestLocationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestLocationPermissionIfNeeded() {
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasCoarse || hasFine) {
            updateLocationPreference()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    private fun updateLocationPreference() {
        val location = bestLastKnownLocation() ?: return
        val current = BudgiePrefs.load(this)
        BudgiePrefs.save(this, current.copy(ukLocation = location.toUkRegion()))
    }

    private fun bestLastKnownLocation(): Location? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return null

        val manager = getSystemService(LocationManager::class.java)
        return manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun authenticate() {
        if (isUnlocked || authInProgress) return
        authInProgress = true
        authMessage = "Waiting for authentication..."
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            authenticateWithBiometricPrompt()
        } else {
            authenticateWithDeviceCredential()
        }
    }

    @Suppress("DEPRECATION")
    private fun authenticateWithBiometricPrompt() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (!keyguardManager.isDeviceSecure) {
            authInProgress = false
            authMessage = "Set up biometrics or a screen lock to use Budgie News."
            return
        }

        val prompt = BiometricPrompt.Builder(this)
            .setTitle("Unlock Budgie News")
            .setSubtitle("Use biometrics or your screen lock")
            .setDescription("Authentication keeps your news app private.")
            .setDeviceCredentialAllowed(true)
            .build()

        prompt.authenticate(
            CancellationSignal(),
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authInProgress = false
                    unlockApp()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authInProgress = false
                    authMessage = errString.toString().ifBlank { "Authentication failed." }
                }

                override fun onAuthenticationFailed() {
                    authMessage = "Authentication did not match. Try again."
                }
            },
        )
    }

    @Suppress("DEPRECATION")
    private fun authenticateWithDeviceCredential() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (!keyguardManager.isDeviceSecure) {
            authInProgress = false
            authMessage = "Set up a screen lock to use Budgie News."
            return
        }
        val intent = keyguardManager.createConfirmDeviceCredentialIntent(
            "Unlock Budgie News",
            "Confirm your screen lock to continue.",
        )
        if (intent == null) {
            authInProgress = false
            authMessage = "Device authentication is unavailable."
        } else {
            credentialLauncher.launch(intent)
        }
    }

    private fun unlockApp() {
        isUnlocked = true
        authMessage = "Unlocked."
        requestRequiredPermissionsIfNeeded()
    }
}

private object BudgieFirebase {
    fun setup(context: Context) {
        Firebase.analytics.logEvent("budgie_app_open", null)
        FirebaseCrashlytics.getInstance().setCustomKey("budgie_version", "0.0.14-alpha")
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                BudgieAccountApi.ensureSession()
                BudgieAccountApi.startLiveArticles(context)
                BudgiePrefs.deviceToken(context).takeIf { it.isNotBlank() }?.let { token ->
                    BudgieAccountApi.registerDevice(context, token)
                }
            }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
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
                        .onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
                }
            }
        }.addOnFailureListener { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
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

    fun notifyFor(context: Context, section: NewsSection, item: FeedItem, articleId: String = item.link) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = when (section) {
            NewsSection.BREAKING -> BREAKING_CHANNEL_ID
            NewsSection.IMPORTANT -> IMPORTANT_CHANNEL_ID
            NewsSection.HEADLINES -> DEFAULT_CHANNEL_ID
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
            .setPriority(if (section == NewsSection.IMPORTANT) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(section.ordinal + item.link.hashCode(), notification)
    }

    fun notifyForPush(
        context: Context,
        articleId: String,
        category: String,
        title: String,
        source: String,
    ) {
        val section = runCatching { NewsSection.valueOf(category.uppercase()) }.getOrDefault(NewsSection.HEADLINES)
        notifyFor(
            context = context,
            section = section,
            item = FeedItem(
                id = articleId,
                title = title.ifBlank { "${section.label} story" },
                description = "",
                link = articleId,
                source = source.ifBlank { "Budgie News" },
                publishedAt = "",
                imageUrl = null,
            ),
            articleId = articleId,
        )
    }
}

internal object BudgiePrefs {
    private const val PREFS = "budgie_news_settings"
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_BREAKING = "breaking_notifications"
    private const val KEY_IMPORTANT = "important_notifications"
    private const val KEY_HEADLINES = "headlines_notifications"
    private const val KEY_SECTION = "default_section"
    private const val KEY_SOURCE = "default_source"
    private const val KEY_ACCOUNT_ENABLED = "account_enabled"
    private const val KEY_ACCOUNT_NAME = "account_name"
    private const val KEY_ACCOUNT_EMAIL = "account_email"
    private const val KEY_ACCOUNT_PASSWORD = "account_password"
    private const val KEY_LOCATION = "uk_location"
    private const val KEY_SEND_STATS = "send_stats"
    private const val KEY_DEVICE_TOKEN = "device_token"
    private const val KEY_LAST_BREAKING = "last_breaking_link"
    private const val KEY_LAST_IMPORTANT = "last_important_link"

    fun load(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppSettings(
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, true),
            breakingNotificationsEnabled = prefs.getBoolean(KEY_BREAKING, true),
            importantNotificationsEnabled = prefs.getBoolean(KEY_IMPORTANT, true),
            headlinesNotificationsEnabled = prefs.getBoolean(KEY_HEADLINES, true),
            defaultSection = prefs.getString(KEY_SECTION, null)?.let { runCatching { NewsSection.valueOf(it) }.getOrNull() } ?: NewsSection.HEADLINES,
            defaultSource = prefs.getString(KEY_SOURCE, null)?.let { runCatching { SourceFilter.valueOf(it) }.getOrNull() } ?: SourceFilter.ALL,
            accountEnabled = prefs.getBoolean(KEY_ACCOUNT_ENABLED, false),
            accountName = prefs.getString(KEY_ACCOUNT_NAME, "").orEmpty(),
            accountEmail = prefs.getString(KEY_ACCOUNT_EMAIL, "").orEmpty(),
            accountPassword = prefs.getString(KEY_ACCOUNT_PASSWORD, "").orEmpty(),
            ukLocation = prefs.getString(KEY_LOCATION, "United Kingdom").orEmpty(),
            sendAppStatistics = prefs.getBoolean(KEY_SEND_STATS, true),
        )
    }

    fun save(context: Context, settings: AppSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BIOMETRIC, settings.biometricEnabled)
            .putBoolean(KEY_BREAKING, settings.breakingNotificationsEnabled)
            .putBoolean(KEY_IMPORTANT, settings.importantNotificationsEnabled)
            .putBoolean(KEY_HEADLINES, settings.headlinesNotificationsEnabled)
            .putString(KEY_SECTION, settings.defaultSection.name)
            .putString(KEY_SOURCE, settings.defaultSource.name)
            .putBoolean(KEY_ACCOUNT_ENABLED, settings.accountEnabled)
            .putString(KEY_ACCOUNT_NAME, settings.accountName)
            .putString(KEY_ACCOUNT_EMAIL, settings.accountEmail)
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
        if (!settings.accountEnabled || settings.accountEmail.isBlank()) return
        withContext(Dispatchers.IO) {
            runCatching {
                BudgieAccountApi.sync(settings)
            }.onFailure { error ->
                FirebaseCrashlytics.getInstance().recordException(error)
            }
        }
    }

    fun shouldNotify(context: Context, section: NewsSection, item: FeedItem, settings: AppSettings): Boolean {
        val enabled = when (section) {
            NewsSection.BREAKING -> settings.breakingNotificationsEnabled
            NewsSection.IMPORTANT -> settings.importantNotificationsEnabled
            NewsSection.HEADLINES -> settings.headlinesNotificationsEnabled
        }
        if (!enabled || item.link.isBlank()) return false
        val key = when (section) {
            NewsSection.BREAKING -> KEY_LAST_BREAKING
            NewsSection.IMPORTANT -> KEY_LAST_IMPORTANT
            NewsSection.HEADLINES -> "last_headlines_link"
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(key, "") == item.link) return false
        prefs.edit().putString(key, item.link).apply()
        return true
    }
}

internal object BudgieAccountApi {
    private var liveArticleRegistration: ListenerRegistration? = null

    suspend fun ensureSession() {
        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously().await()
        }
    }

    suspend fun register(settings: AppSettings) {
        require(settings.accountEmail.isNotBlank()) { "Email is required" }
        require(settings.accountPassword.length >= 6) { "Password must be at least 6 characters" }
        val credential = EmailAuthProvider.getCredential(settings.accountEmail, settings.accountPassword)
        val currentUser = Firebase.auth.currentUser
        if (currentUser?.isAnonymous == true) {
            runCatching {
                currentUser.linkWithCredential(credential).await()
            }.recoverCatching { error ->
                if (error is FirebaseAuthUserCollisionException) {
                    Firebase.auth.signInWithEmailAndPassword(settings.accountEmail, settings.accountPassword).await()
                } else {
                    throw error
                }
            }.getOrThrow()
        } else {
            Firebase.auth.createUserWithEmailAndPassword(settings.accountEmail, settings.accountPassword).await()
        }
        sync(settings.copy(accountEnabled = true))
    }

    suspend fun login(settings: AppSettings) {
        require(settings.accountEmail.isNotBlank()) { "Email is required" }
        require(settings.accountPassword.isNotBlank()) { "Password is required" }
        Firebase.auth.signInWithEmailAndPassword(settings.accountEmail, settings.accountPassword).await()
        sync(settings.copy(accountEnabled = true))
    }

    suspend fun sync(settings: AppSettings) {
        val user = Firebase.auth.currentUser ?: return
        val data = mapOf(
            "uid" to user.uid,
            "email" to settings.accountEmail,
            "displayName" to settings.accountName.ifBlank { "Budgie reader" },
            "ukLocation" to settings.ukLocation,
            "defaultSection" to settings.defaultSection.name,
            "defaultSource" to settings.defaultSource.name,
            "biometricEnabled" to settings.biometricEnabled,
            "breakingNotificationsEnabled" to settings.breakingNotificationsEnabled,
            "importantNotificationsEnabled" to settings.importantNotificationsEnabled,
            "headlinesNotificationsEnabled" to settings.headlinesNotificationsEnabled,
            "sendAppStatistics" to settings.sendAppStatistics,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        Firebase.firestore.collection("users")
            .document(user.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun registerDevice(context: Context, token: String) {
        if (token.isBlank()) return
        ensureSession()
        val settings = BudgiePrefs.load(context)
        val uid = Firebase.auth.currentUser?.uid ?: return
        val data = mapOf(
            "token" to token,
            "uid" to uid,
            "email" to settings.accountEmail,
            "ukLocation" to settings.ukLocation,
            "breakingNotificationsEnabled" to settings.breakingNotificationsEnabled,
            "importantNotificationsEnabled" to settings.importantNotificationsEnabled,
            "headlinesNotificationsEnabled" to settings.headlinesNotificationsEnabled,
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
        val newestAllowed = System.currentTimeMillis() - 86_400_000L
        liveArticleRegistration = Firebase.firestore.collection("articles")
            .whereGreaterThanOrEqualTo("publishedAtMillis", newestAllowed)
            .orderBy("publishedAtMillis", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
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
                    .filter { it.title.isNotBlank() && it.link.isNotBlank() }
                val settings = BudgiePrefs.load(context)
                articles.forEach { article ->
                    BudgieArticleDatabase.get(context).upsertArticle(article)
                    val section = article.category.toNewsSection() ?: return@forEach
                    val item = article.toFeedItem()
                    if (BudgiePrefs.shouldNotify(context, section, item, settings)) {
                        BudgieNotifications.notifyFor(context, section, item, articleId = article.articleId)
                    }
                }
            }
    }

    private fun String.safeFirestoreId(): String =
        replace(Regex("[^A-Za-z0-9_-]"), "_").take(140).ifBlank { "unknown-token" }
}

private fun Throwable.userFacingAuthMessage(action: String): String {
    val authCode = (this as? FirebaseAuthException)?.errorCode.orEmpty()
    return when (authCode) {
        "ERROR_CONFIGURATION_NOT_FOUND" -> "$action failed: Firebase Auth is not initialized for project moneybytes-apk. Enable Authentication, then enable Email/Password sign-in. If Firebase asks for billing, billing must be enabled before Auth can initialize."
        "ERROR_OPERATION_NOT_ALLOWED" -> "$action failed: Email/Password sign-in is disabled in Firebase Console."
        "ERROR_INVALID_EMAIL" -> "$action failed: enter a valid email address."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "$action failed: email is already registered. Use Login."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "$action failed: email or password is wrong."
        "ERROR_USER_NOT_FOUND" -> "$action failed: no account exists for that email."
        "ERROR_WEAK_PASSWORD" -> "$action failed: password must be at least 6 characters."
        else -> "$action failed: ${message ?: "unknown Firebase Auth error"}"
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

    fun load(context: Context): List<FeedItem> {
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
            FirebaseCrashlytics.getInstance().recordException(it)
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

internal data class FeedSource(
    val name: String,
    val url: String,
)

internal data class AppSettings(
    val biometricEnabled: Boolean = true,
    val breakingNotificationsEnabled: Boolean = true,
    val importantNotificationsEnabled: Boolean = true,
    val headlinesNotificationsEnabled: Boolean = true,
    val defaultSection: NewsSection = NewsSection.HEADLINES,
    val defaultSource: SourceFilter = SourceFilter.ALL,
    val accountEnabled: Boolean = false,
    val accountName: String = "",
    val accountEmail: String = "",
    val accountPassword: String = "",
    val ukLocation: String = "United Kingdom",
    val sendAppStatistics: Boolean = true,
)

internal enum class SourceFilter(val label: String, val sourceName: String?) {
    ALL("All", null),
    BBC("BBC", "BBC UK"),
    SKY("Sky", "Sky News UK"),
    SKY_POLITICS("Sky Pol", "Sky Politics"),
    GUARDIAN("Guardian", "Guardian UK"),
    GUARDIAN_POLITICS("Guard Pol", "Guardian Politics"),
    INDEPENDENT("Indy", "Independent UK"),
    DAILY_MAIL("Mail", "Daily Mail News"),
    SUN("Sun", "The Sun News"),
    FT("FT", "Financial Times UK"),
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
}

internal sealed interface FeedState {
    data object Loading : FeedState
    data class Ready(val items: List<FeedItem>) : FeedState
    data class Error(val message: String) : FeedState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsApp(onBiometricSettingChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(BudgiePrefs.load(context)) }
    var refreshToken by remember { mutableStateOf(0) }
    var selectedSection by remember { mutableStateOf(settings.defaultSection) }
    var selectedSource by remember { mutableStateOf(settings.defaultSource) }
    var selectedItem by remember { mutableStateOf<FeedItem?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<FeedState>(FeedState.Loading) }
    val articleSignal by ArticleSignals.version.collectAsState()
    val openArticleId by ArticleSignals.openArticleId.collectAsState()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = selectedItem != null || settingsOpen) {
        selectedItem = null
        settingsOpen = false
    }

    fun saveSettings(updated: AppSettings) {
        settings = updated
        scope.launch {
            BudgiePrefs.saveAndSync(context, updated)
            BudgiePrefs.deviceToken(context).takeIf { it.isNotBlank() }?.let { token ->
                runCatching { BudgieAccountApi.registerDevice(context, token) }
                    .onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
            }
        }
        onBiometricSettingChanged(updated.biometricEnabled)
    }

    fun refresh() {
        state = FeedState.Loading
        scope.launch {
            state = fetchFeeds(context, selectedSection, selectedSource, settings)
        }
    }

    LaunchedEffect(refreshToken, selectedSection, selectedSource, settings.breakingNotificationsEnabled, settings.importantNotificationsEnabled, articleSignal) {
        state = FeedState.Loading
        state = fetchFeeds(context, selectedSection, selectedSource, settings)
    }

    LaunchedEffect(openArticleId, state) {
        val articleId = openArticleId ?: return@LaunchedEffect
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BudgieMark()
                        Spacer(Modifier.size(10.dp))
                        Column {
                            TypewriterText("Budgie News", color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            TypewriterText(selectedSource.tagline(selectedSection), color = Muted, fontSize = 12.sp)
                        }
                    }
                },
                actions = {
                    if (selectedItem == null && !settingsOpen) {
                        IconButton(onClick = { settingsOpen = true }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = Ink)
                        }
                        IconButton(onClick = { refreshToken++ }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh news", tint = Ink)
                        }
                    }
                },
                navigationIcon = {
                    if (selectedItem != null || settingsOpen) {
                        IconButton(onClick = {
                            selectedItem = null
                            settingsOpen = false
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to news", tint = Ink)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
            )
        },
    ) { padding ->
        val detailItem = selectedItem
        if (settingsOpen) {
            SettingsScreen(
                settings = settings,
                onSettingsChanged = ::saveSettings,
                modifier = Modifier.padding(padding),
            )
        } else {
            BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
                val useTwoPane = maxWidth >= 720.dp
                if (!useTwoPane && detailItem != null) {
                    StoryDetail(detailItem)
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
                                    StoryDetail(detailItem)
                                } else {
                                    DetailPlaceholder()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var accountMessage by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Paper),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            SettingsRow(
                title = "Account",
                description = if (settings.accountEnabled) {
                    settings.accountEmail.ifBlank { "Budgie account enabled" }
                } else {
                    "Register or log in to sync settings and live-news preferences"
                },
            )
        }
        item {
            SettingsSwitchRow(
                title = "Use account",
                description = "Register or log in with Firebase Auth.",
                checked = settings.accountEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(accountEnabled = it)) },
            )
        }
        if (settings.accountEnabled) {
            item {
                SettingsTextField(
                    title = "Display name",
                    value = settings.accountName,
                    placeholder = "Budgie reader",
                    onValueChange = { onSettingsChanged(settings.copy(accountName = it)) },
                )
            }
            item {
                SettingsTextField(
                    title = "Email",
                    value = settings.accountEmail,
                    placeholder = "name@example.com",
                    onValueChange = { onSettingsChanged(settings.copy(accountEmail = it)) },
                )
            }
            item {
                SettingsTextField(
                    title = "Password",
                    value = settings.accountPassword,
                    placeholder = "Required for register and login",
                    onValueChange = { onSettingsChanged(settings.copy(accountPassword = it)) },
                )
            }
            item {
                SettingsAuthActions(
                    message = accountMessage,
                    onRegister = {
                        accountMessage = "Registering..."
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                BudgieAccountApi.register(settings)
                                BudgiePrefs.saveAndSync(context, settings)
                            }.fold(
                                onSuccess = { accountMessage = "Account registered and synced." },
                                onFailure = {
                                    FirebaseCrashlytics.getInstance().recordException(it)
                                    accountMessage = it.userFacingAuthMessage("Register")
                                },
                            )
                        }
                    },
                    onLogin = {
                        accountMessage = "Logging in..."
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                BudgieAccountApi.login(settings)
                                BudgiePrefs.saveAndSync(context, settings)
                            }.fold(
                                onSuccess = { accountMessage = "Logged in and synced." },
                                onFailure = {
                                    FirebaseCrashlytics.getInstance().recordException(it)
                                    accountMessage = it.userFacingAuthMessage("Login")
                                },
                            )
                        }
                    },
                )
            }
        }
        item {
            SettingsChoiceRow(
                title = "Location",
                description = "GB/UK only location-based news preference",
                value = settings.ukLocation,
                options = UkLocationOptions.map { location ->
                    location to { onSettingsChanged(settings.copy(ukLocation = location)) }
                },
            )
        }
        item {
            SettingsChoiceRow(
                title = "Default outlet",
                description = "Choose the outlet shown first when Budgie News opens",
                value = settings.defaultSource.label,
                options = SourceFilter.entries.map { it.label to { onSettingsChanged(settings.copy(defaultSource = it)) } },
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
                title = "Biometric login",
                description = "Require device authentication when opening Budgie News.",
                checked = settings.biometricEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(biometricEnabled = it)) },
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
                description = "Prepared for future account-backed feedback sync.",
            )
        }
        item {
            SettingsRow(
                title = "Third party libraries",
                description = "Firebase, Coil, AndroidX, Kotlin, and Jetpack Compose.",
            )
        }
        item {
            VersionFooter()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    description: String,
) {
    Column {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TypewriterText(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            TypewriterText(description, color = Muted, fontSize = 18.sp, lineHeight = 25.sp, maxLines = 3)
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
                TypewriterText(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                TypewriterText(description, color = Muted, fontSize = 18.sp, lineHeight = 25.sp, maxLines = 4)
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
            TypewriterText(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            TypewriterText(description, color = Muted, fontSize = 18.sp, lineHeight = 25.sp, maxLines = 3)
            TypewriterText(value, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { option ->
                    val selected = option.first == value
                    AssistChip(
                        onClick = option.second,
                        label = { Text(option.first, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
private fun SettingsTextField(
    title: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Column {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TypewriterText(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text(placeholder, color = Muted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider(color = Accent, thickness = 1.dp)
    }
}

@Composable
private fun SettingsAuthActions(
    message: String,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
) {
    Column {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRegister,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
                ) {
                    Text("Register")
                }
                Button(
                    onClick = onLogin,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised, contentColor = Ink),
                ) {
                    Text("Login")
                }
            }
            if (message.isNotBlank()) {
                TypewriterText(message, color = Muted, fontSize = 13.sp, maxLines = 2)
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
            TypewriterText("Version", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            TypewriterText(context.appVersionText(), color = Ink, fontSize = 16.sp, maxLines = 1)
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
private fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = 3,
) {
    var visibleText by remember(text) { mutableStateOf("") }
    LaunchedEffect(text) {
        visibleText = ""
        text.indices.forEach { index ->
            visibleText = text.take(index + 1)
            delay(18)
        }
    }
    Text(
        visibleText,
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
private fun LockedApp(message: String, onUnlock: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BudgieMark()
            TypewriterText("Budgie News", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            TypewriterText(message, color = Muted, lineHeight = 20.sp, maxLines = 3)
            Button(
                onClick = onUnlock,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
            ) {
                Text("Unlock")
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
            TypewriterText("Could not load the feed", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            TypewriterText(message, color = Muted, lineHeight = 20.sp, maxLines = 3)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
            ) {
                Text("Try again")
            }
        }
        }
    }
}

@Composable
private fun NewsList(
    items: List<FeedItem>,
    selectedSection: NewsSection,
    selectedSource: SourceFilter,
    selectedItem: FeedItem?,
    onSectionSelected: (NewsSection) -> Unit,
    onSourceSelected: (SourceFilter) -> Unit,
    onStorySelected: (FeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionMenu(
                selectedSection = selectedSection,
                selectedSource = selectedSource,
                onSectionSelected = onSectionSelected,
                onSourceSelected = onSourceSelected,
            )
        }
        item {
            CoverageOverview(items, selectedSource)
        }
        item {
            FeedSourceNote(selectedSource)
        }
        if (items.isEmpty()) {
            item {
                EmptySection(selectedSection)
            }
        } else {
            item {
                LeadStory(items.firstOrNull(), selectedSection, onStorySelected)
            }
            items(items.drop(1)) { item ->
                StoryCard(item, selected = item.id == selectedItem?.id, onStorySelected)
            }
        }
    }
}

@Composable
private fun SectionMenu(
    selectedSection: NewsSection,
    selectedSource: SourceFilter,
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
                        if (section == NewsSection.IMPORTANT) Icons.Rounded.PriorityHigh else Icons.AutoMirrored.Rounded.Article,
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
        items(SourceFilter.entries.filter { it != SourceFilter.ALL }) { source ->
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
private fun FeedSourceNote(selectedSource: SourceFilter) {
    TypewriterText(
        selectedSource.sourceNote(),
        color = Muted,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun CoverageOverview(items: List<FeedItem>, selectedSource: SourceFilter) {
    val visibleSources = items.map { it.source }.distinct().sorted()
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
                    if (selectedSource == SourceFilter.ALL) "${visibleSources.size}/${FeedSources.size}" else "1/${FeedSources.size}",
                    Modifier.weight(1f),
                )
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    TypewriterText("Coverage Lens", color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    TypewriterText(
                        "Compare source framing before opening a story.",
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(FeedSources) { source ->
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
        TypewriterText(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
        TypewriterText(label, color = Muted, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun EmptySection(section: NewsSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, AccentSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
            TypewriterText(section.emptyText, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 2)
            TypewriterText("Try refresh, or switch to another menu section.", color = Muted, lineHeight = 20.sp, maxLines = 2)
        }
    }
}

@Composable
private fun LeadStory(item: FeedItem?, section: NewsSection, onStorySelected: (FeedItem) -> Unit) {
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
                    TypewriterText("Top Story | ${section.label}", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    TypewriterText(item.title, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp, maxLines = 4)
                    if (item.description.isNotBlank()) {
                        TypewriterText(item.description, color = Muted, maxLines = 3, lineHeight = 20.sp)
                    }
                    CoverageRow(item)
                    StoryMeta(item)
                }
            }
        }
    }
}

@Composable
private fun StoryCard(item: FeedItem, selected: Boolean, onStorySelected: (FeedItem) -> Unit) {
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
                    TypewriterText(item.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 3, lineHeight = 20.sp)
                    if (item.description.isNotBlank()) {
                        TypewriterText(item.description, color = Muted, fontSize = 13.sp, maxLines = 2, lineHeight = 18.sp)
                    }
                    CoverageRow(item)
                    StoryMeta(item)
                }
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open story", tint = Muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StoryDetail(item: FeedItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                        TypewriterText(item.publishedAt, color = Muted, fontSize = 12.sp, maxLines = 1)
                    }
                    TypewriterText(item.title, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                    QuickRead(item)
                    CoverageRow(item)
                    Button(
                        onClick = { context.openUrl(item.link) },
                        enabled = item.link.isNotBlank(),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Paper),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Read official source")
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
                TypewriterText("Select a story", color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1)
                TypewriterText("Article details will stay open beside the feed on larger screens.", color = Muted, lineHeight = 20.sp, maxLines = 3)
            }
        }
    }
}

@Composable
private fun QuickRead(item: FeedItem) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        TypewriterText("Quick read", color = Accent, fontWeight = FontWeight.SemiBold, maxLines = 1)
        item.quickReadPoints().forEach { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("-", color = Muted, lineHeight = 21.sp)
                TypewriterText(point, color = Muted, lineHeight = 21.sp, modifier = Modifier.weight(1f), maxLines = 5)
            }
        }
    }
}

private fun FeedItem.quickReadPoints(): List<String> {
    val points = mutableListOf<String>()
    points += "Main story: $title"
    if (description.isNotBlank()) points += description
    points += "Source: $source${publishedAt.takeIf { it.isNotBlank() }?.let { " | $it" }.orEmpty()}"
    points += "Coverage: ${coverageSources.joinToString { it.shortSourceName() }}"
    return points
}

@Composable
private fun CoverageRow(item: FeedItem) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Covered by ${item.coverageSources.size}/${FeedSources.size}",
            color = Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        item.coverageSources.take(2).forEach { source ->
            SourcePill(source.shortSourceName(), active = true)
        }
    }
}

@Composable
private fun SourcePill(label: String, active: Boolean) {
    val (bgColor, textColor) = if (active) brandColors(label) else AccentSoft to Muted
    Text(
        label,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = 92.dp)
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

private fun brandColors(label: String): Pair<Color, Color> = when {
    label.contains("BBC", ignoreCase = true) -> Color(0xFFB80000) to Ink
    label.contains("Sky", ignoreCase = true) -> Color(0xFF0015A8) to Ink
    label.contains("Guard", ignoreCase = true) -> Color(0xFF052962) to Ink
    label.contains("Indy", ignoreCase = true) -> Color(0xFFE31B22) to Ink
    label.contains("Mail", ignoreCase = true) -> Color(0xFF004D99) to Ink
    label.contains("Sun", ignoreCase = true) -> Color(0xFFED1C24) to Ink
    label.contains("FT", ignoreCase = true) -> Color(0xFFF3D5B9) to Paper
    else -> Accent to Ink
}

@Composable
private fun StoryMeta(item: FeedItem) {
    TypewriterText(
        listOf(item.source, item.publishedAt).filter { it.isNotBlank() }.joinToString("  |  "),
        color = Muted,
        fontSize = 12.sp,
        maxLines = 1,
    )
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

internal suspend fun loadAllFeedItems(context: Context): List<FeedItem> = withContext(Dispatchers.IO) {
    val loadedItems = FeedSources
        .map { source ->
            runCatching { fetchFeed(source).take(12) }
                .onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
                .getOrDefault(emptyList())
        }
        .interleaved()
        .distinctBy { it.link.ifBlank { it.title } }
    if (loadedItems.isNotEmpty()) {
        BudgieCache.save(context, loadedItems)
        loadedItems
    } else {
        BudgieCache.load(context).distinctBy { it.link.ifBlank { it.title } }
    }
}

internal fun notifyForNewFeedStories(
    context: Context,
    items: List<FeedItem>,
    settings: AppSettings = BudgiePrefs.load(context),
) {
    listOf(NewsSection.BREAKING, NewsSection.IMPORTANT).forEach { section ->
        items.filterFor(section).firstOrNull()?.let { item ->
            if (BudgiePrefs.shouldNotify(context, section, item, settings)) {
                BudgieNotifications.notifyFor(context, section, item)
            }
        }
    }
}

private fun String.toNewsSection(): NewsSection? = when (lowercase()) {
    "breaking" -> NewsSection.BREAKING
    "important" -> NewsSection.IMPORTANT
    else -> null
}

private suspend fun fetchFeeds(
    context: Context,
    section: NewsSection,
    sourceFilter: SourceFilter,
    settings: AppSettings,
): FeedState = withContext(Dispatchers.IO) {
    runCatching {
        val sources = FeedSources
            .filter { sourceFilter.sourceName == null || it.name == sourceFilter.sourceName }
        val localItems = BudgieArticleDatabase.get(context)
            .recentArticles()
            .map { it.toFeedItem() }
            .filter { sourceFilter.sourceName == null || it.source == sourceFilter.sourceName }
        val loadedItems = sources
            .map { source ->
                runCatching { fetchFeed(source).take(12) }
                    .onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
                    .getOrDefault(emptyList())
            }
            .interleaved()
            .distinctBy { it.link.ifBlank { it.title } }
        val availableItems = if (loadedItems.isNotEmpty()) {
            BudgieCache.save(context, loadedItems)
            loadedItems
        } else {
            BudgieCache.load(context)
                .filter { item -> sourceFilter.sourceName == null || item.source == sourceFilter.sourceName }
                .distinctBy { it.link.ifBlank { it.title } }
        }
        if (availableItems.isEmpty()) {
            val sourceNames = sources.joinToString { it.name }
            error("No stories loaded from $sourceNames")
        }
        val allItems = (localItems + availableItems).distinctBy { it.link.ifBlank { it.title } }
        notifyForNewFeedStories(context, allItems, settings)
        val filteredItems = allItems
            .filterFor(section)
            .take(24)
            .withCoverageContext()
        filteredItems
    }.fold(
        onSuccess = { items ->
            FeedState.Ready(items)
        },
        onFailure = {
            FirebaseCrashlytics.getInstance().recordException(it)
            FeedState.Error(it.message ?: "Unexpected feed error")
        },
    )
}

private fun LocalArticle.toFeedItem(): FeedItem =
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

private fun Location.toUkRegion(): String {
    val lat = latitude
    val lon = longitude
    return when {
        lat in 55.0..59.0 && lon in -8.5..-0.5 -> "Scotland"
        lat in 51.2..53.6 && lon in -5.8..-2.5 -> "Wales"
        lat in 54.0..55.4 && lon in -8.3..-5.3 -> "Northern Ireland"
        lat in 51.0..51.8 && lon in -0.8..0.4 -> "London"
        lat in 53.0..55.0 && lon in -3.8..-1.5 -> "North West"
        lat in 54.5..55.9 && lon in -2.4..-0.5 -> "North East"
        lat in 53.3..54.6 && lon in -2.5..0.2 -> "Yorkshire"
        lat in 52.0..53.4 && lon in -3.2..-0.5 -> "Midlands"
        lat in 50.6..52.2 && lon in -1.8..1.8 -> "South East"
        lat in 49.8..52.1 && lon in -6.5..-1.8 -> "South West"
        else -> "United Kingdom"
    }
}

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

private fun fetchFeed(source: FeedSource): List<FeedItem> {
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
            items += readItem(parser, fallbackSource, parser.name)
        }
        event = parser.next()
    }
    return items.take(40)
}

private fun readItem(parser: XmlPullParser, fallbackSource: String, containerTag: String): FeedItem {
    var title = ""
    var description = ""
    var link = ""
    var source = fallbackSource
    var pubDate = ""
    var imageUrl: String? = null

    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.END_TAG && parser.name.equals(containerTag, ignoreCase = true)) break
        if (parser.eventType != XmlPullParser.START_TAG) continue

        when (parser.name.lowercase()) {
            "title" -> title = parser.readText()
            "description" -> description = parser.readText().stripHtml()
            "summary" -> description = parser.readText().stripHtml()
            "link" -> {
                val href = parser.attributeValue("href")
                if (href.isNullOrBlank()) {
                    link = parser.readText()
                } else {
                    link = href
                    parser.skipTag()
                }
            }
            "pubdate", "published", "updated" -> pubDate = parser.readText().formatNewsDate()
            "source" -> source = parser.readText().ifBlank { source }
            "enclosure" -> imageUrl = parser.attributeValue("url") ?: imageUrl
            "thumbnail", "media:thumbnail", "media:content" -> imageUrl = parser.attributeValue("url") ?: imageUrl
            "content:encoded", "content" -> {
                val encodedContent = parser.readText()
                imageUrl = encodedContent.firstImageUrl() ?: imageUrl
                if (description.isBlank()) description = encodedContent.stripHtml()
            }
            else -> parser.skipTag()
        }
    }

    return FeedItem(
        id = link.ifBlank { title },
        title = title.stripHtml().ifBlank { "Untitled story" },
        description = description,
        link = link,
        source = source.stripHtml(),
        publishedAt = pubDate,
        imageUrl = imageUrl,
    )
}

private fun List<FeedItem>.filterFor(section: NewsSection): List<FeedItem> = when (section) {
    NewsSection.HEADLINES -> this
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

private fun SourceFilter.tagline(section: NewsSection): String = when (this) {
    SourceFilter.ALL -> section.tagline
    else -> "${label} ${section.label.lowercase()}"
}

private fun SourceFilter.sourceNote(): String = when (this) {
    SourceFilter.ALL -> "Reporting from ${FeedSources.size} leading UK newsrooms"
    else -> "Showing $label stories only"
}

private fun String.shortSourceName(): String = when {
    contains("BBC", ignoreCase = true) -> "BBC"
    contains("Sky", ignoreCase = true) -> "Sky"
    contains("Guardian", ignoreCase = true) -> "Guardian"
    contains("Independent", ignoreCase = true) -> "Indy"
    contains("Daily Mail", ignoreCase = true) -> "Mail"
    contains("Sun", ignoreCase = true) -> "Sun"
    contains("Financial Times", ignoreCase = true) -> "FT"
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
    Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString().trim()

private fun String.firstImageUrl(): String? {
    val match = Regex("""<img[^>]+src=['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE).find(this)
    val rawUrl = match?.groupValues?.getOrNull(1)?.replace("&amp;", "&") ?: return null
    return rawUrl.extractNestedImageUrl()
}

private fun String.extractNestedImageUrl(): String {
    val nestedUrl = Uri.parse(this).getQueryParameter("url")
    return if (nestedUrl.isNullOrBlank()) this else URLDecoder.decode(nestedUrl, Charsets.UTF_8.name())
}

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

private fun Context.appVersionText(): String {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else @Suppress("DEPRECATION") packageInfo.versionCode.toLong()
    return "${packageInfo.versionName} [$code]"
}

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
            onSectionSelected = {},
            onSourceSelected = {},
            onStorySelected = {},
            modifier = Modifier.background(Paper),
        )
    }
}
