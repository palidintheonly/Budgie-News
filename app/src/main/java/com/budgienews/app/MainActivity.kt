package com.budgienews.app

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricPrompt
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.messaging
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
private val Paper = Color(0xFF0B0F14)
private val SurfaceDark = Color(0xFF151B22)
private val SurfaceRaised = Color(0xFF1D252E)
private val Muted = Color(0xFFAAB4BF)
private val Accent = Color(0xFF37C6A3)
private val AccentSoft = Color(0xFF203A36)
private val Alert = Color(0xFFFFB86B)

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

class MainActivity : ComponentActivity() {
    private var isUnlocked by mutableStateOf(false)
    private var authMessage by mutableStateOf("Unlock Budgie News to continue.")
    private var authInProgress = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
        BudgieFirebase.setup()
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
            requestNotificationPermissionIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        requestNotificationPermissionIfNeeded()
    }
}

private object BudgieFirebase {
    fun setup() {
        Firebase.analytics.logEvent("budgie_app_open", null)
        FirebaseCrashlytics.getInstance().setCustomKey("budgie_version", "0.0.6-alpha")
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        Firebase.remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            },
        )
        Firebase.remoteConfig.fetchAndActivate()
        Firebase.messaging.token.addOnSuccessListener { token ->
            FirebaseCrashlytics.getInstance().setCustomKey("fcm_token_ready", token.isNotBlank())
        }.addOnFailureListener { error ->
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }
}

private object BudgieNotifications {
    const val BREAKING_CHANNEL_ID = "budgie_news_breaking"
    const val IMPORTANT_CHANNEL_ID = "budgie_news_important"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    BREAKING_CHANNEL_ID,
                    "Breaking news",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Breaking Budgie News alerts"
                    setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
                },
                NotificationChannel(
                    IMPORTANT_CHANNEL_ID,
                    "Important news",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Important Budgie News alerts"
                    setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, audioAttributes)
                },
            ),
        )
    }

    fun notifyFor(context: Context, section: NewsSection, item: FeedItem) {
        if (section == NewsSection.HEADLINES) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = when (section) {
            NewsSection.BREAKING -> BREAKING_CHANNEL_ID
            NewsSection.IMPORTANT -> IMPORTANT_CHANNEL_ID
            NewsSection.HEADLINES -> return
        }
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            section.ordinal,
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
}

private object BudgiePrefs {
    private const val PREFS = "budgie_news_settings"
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_BREAKING = "breaking_notifications"
    private const val KEY_IMPORTANT = "important_notifications"
    private const val KEY_SECTION = "default_section"
    private const val KEY_SOURCE = "default_source"
    private const val KEY_LAST_BREAKING = "last_breaking_link"
    private const val KEY_LAST_IMPORTANT = "last_important_link"

    fun load(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppSettings(
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, true),
            breakingNotificationsEnabled = prefs.getBoolean(KEY_BREAKING, true),
            importantNotificationsEnabled = prefs.getBoolean(KEY_IMPORTANT, true),
            defaultSection = prefs.getString(KEY_SECTION, null)?.let { runCatching { NewsSection.valueOf(it) }.getOrNull() } ?: NewsSection.HEADLINES,
            defaultSource = prefs.getString(KEY_SOURCE, null)?.let { runCatching { SourceFilter.valueOf(it) }.getOrNull() } ?: SourceFilter.ALL,
        )
    }

    fun save(context: Context, settings: AppSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BIOMETRIC, settings.biometricEnabled)
            .putBoolean(KEY_BREAKING, settings.breakingNotificationsEnabled)
            .putBoolean(KEY_IMPORTANT, settings.importantNotificationsEnabled)
            .putString(KEY_SECTION, settings.defaultSection.name)
            .putString(KEY_SOURCE, settings.defaultSource.name)
            .apply()
    }

    fun shouldNotify(context: Context, section: NewsSection, item: FeedItem, settings: AppSettings): Boolean {
        val enabled = when (section) {
            NewsSection.BREAKING -> settings.breakingNotificationsEnabled
            NewsSection.IMPORTANT -> settings.importantNotificationsEnabled
            NewsSection.HEADLINES -> false
        }
        if (!enabled || item.link.isBlank()) return false
        val key = if (section == NewsSection.BREAKING) KEY_LAST_BREAKING else KEY_LAST_IMPORTANT
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(key, "") == item.link) return false
        prefs.edit().putString(key, item.link).apply()
        return true
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

private data class FeedItem(
    val title: String,
    val description: String,
    val link: String,
    val source: String,
    val publishedAt: String,
    val imageUrl: String?,
    val coverageSources: List<String> = listOf(source),
)

private data class FeedSource(
    val name: String,
    val url: String,
)

private data class AppSettings(
    val biometricEnabled: Boolean = true,
    val breakingNotificationsEnabled: Boolean = true,
    val importantNotificationsEnabled: Boolean = true,
    val defaultSection: NewsSection = NewsSection.HEADLINES,
    val defaultSource: SourceFilter = SourceFilter.ALL,
)

private enum class SourceFilter(val label: String, val sourceName: String?) {
    ALL("All", null),
    BBC("BBC", "BBC UK"),
    SKY("Sky", "Sky News UK"),
    SKY_POLITICS("Sky Politics", "Sky Politics"),
    GUARDIAN("Guardian", "Guardian UK"),
    GUARDIAN_POLITICS("Guardian Politics", "Guardian Politics"),
    INDEPENDENT("Independent", "Independent UK"),
    DAILY_MAIL("Daily Mail", "Daily Mail News"),
    SUN("The Sun", "The Sun News"),
    FT("FT", "Financial Times UK"),
}

private enum class NewsSection(
    val label: String,
    val tagline: String,
    val emptyText: String,
) {
    HEADLINES(
        "Headlines",
        "Curated GB headlines",
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

private sealed interface FeedState {
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
    val scope = rememberCoroutineScope()

    BackHandler(enabled = selectedItem != null || settingsOpen) {
        selectedItem = null
        settingsOpen = false
    }

    fun saveSettings(updated: AppSettings) {
        settings = updated
        BudgiePrefs.save(context, updated)
        onBiometricSettingChanged(updated.biometricEnabled)
    }

    fun refresh() {
        state = FeedState.Loading
        scope.launch {
            state = fetchFeeds(context, selectedSection, selectedSource, settings)
        }
    }

    LaunchedEffect(refreshToken, selectedSection, selectedSource, settings.breakingNotificationsEnabled, settings.importantNotificationsEnabled) {
        state = FeedState.Loading
        state = fetchFeeds(context, selectedSection, selectedSource, settings)
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
                            Text("Budgie News", color = Ink, fontWeight = FontWeight.SemiBold)
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
        } else if (detailItem != null) {
            StoryDetail(detailItem, modifier = Modifier.padding(padding))
        } else {
            when (val value = state) {
                FeedState.Loading -> LoadingNews(Modifier.padding(padding))
                is FeedState.Error -> ErrorNews(value.message, ::refresh, Modifier.padding(padding))
                is FeedState.Ready -> NewsList(
                    items = value.items,
                    selectedSection = selectedSection,
                    selectedSource = selectedSource,
                    onSectionSelected = { selectedSection = it },
                    onSourceSelected = { selectedSource = it },
                    onStorySelected = { selectedItem = it },
                    modifier = Modifier.padding(padding),
                )
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SettingsToggle(
                title = "Biometric login",
                description = "Require device authentication when opening Budgie News.",
                checked = settings.biometricEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(biometricEnabled = it)) },
            )
        }
        item {
            SettingsToggle(
                title = "Breaking alerts",
                description = "Notify only when the Breaking feed finds a new story.",
                checked = settings.breakingNotificationsEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(breakingNotificationsEnabled = it)) },
            )
        }
        item {
            SettingsToggle(
                title = "Important alerts",
                description = "Notify only when the Important feed finds a new story.",
                checked = settings.importantNotificationsEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(importantNotificationsEnabled = it)) },
            )
        }
        item {
            SettingsPicker(
                title = "Default section",
                value = settings.defaultSection.label,
                options = NewsSection.entries.map { it.label to { onSettingsChanged(settings.copy(defaultSection = it)) } },
            )
        }
        item {
            SettingsPicker(
                title = "Default outlet",
                value = settings.defaultSource.label,
                options = SourceFilter.entries.map { it.label to { onSettingsChanged(settings.copy(defaultSource = it)) } },
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, AccentSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingsPicker(
    title: String,
    value: String,
    options: List<Pair<String, () -> Unit>>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, AccentSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$title: $value", color = Ink, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { option ->
                    AssistChip(
                        onClick = option.second,
                        label = { Text(option.first) },
                        shape = RoundedCornerShape(6.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (option.first == value) Accent else SurfaceRaised,
                            labelColor = if (option.first == value) Paper else Ink,
                        ),
                        border = BorderStroke(1.dp, if (option.first == value) Accent else AccentSoft),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingNews(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Accent, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            TypewriterText("Fetching latest headlines", color = Ink, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
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
            Text("Budgie News", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Muted, lineHeight = 20.sp)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = Alert, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(14.dp))
            Text("Could not load the feed", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = Muted, lineHeight = 20.sp)
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

@Composable
private fun NewsList(
    items: List<FeedItem>,
    selectedSection: NewsSection,
    selectedSource: SourceFilter,
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
                StoryCard(item, onStorySelected)
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
    Text(
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
        shape = RoundedCornerShape(8.dp),
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Coverage Lens", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Compare source framing before opening a story.",
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeedSources.forEach { source ->
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
        Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun EmptySection(section: NewsSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, AccentSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = Accent, modifier = Modifier.size(28.dp))
            Text(section.emptyText, color = Ink, fontWeight = FontWeight.SemiBold)
            Text("Try refresh, or switch to another menu section.", color = Muted, lineHeight = 20.sp)
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
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
            border = BorderStroke(1.dp, AccentSoft),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column {
                RemoteImage(item.imageUrl, Modifier.fillMaxWidth().height(190.dp))
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Top Story | ${section.label}", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(item.title, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                    if (item.description.isNotBlank()) {
                        Text(item.description, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    }
                    CoverageRow(item)
                    StoryMeta(item)
                }
            }
        }
    }
}

@Composable
private fun StoryCard(item: FeedItem, onStorySelected: (FeedItem) -> Unit) {
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
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, Color(0xFF26313B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RemoteImage(item.imageUrl, Modifier.size(92.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                    if (item.description.isNotBlank()) {
                        Text(item.description, color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
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
            RemoteImage(item.imageUrl, Modifier.fillMaxWidth().height(220.dp))
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, AccentSoft),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SourcePill(item.source.shortSourceName(), active = true)
                        Text(item.publishedAt, color = Muted, fontSize = 12.sp)
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
private fun QuickRead(item: FeedItem) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Quick read", color = Accent, fontWeight = FontWeight.SemiBold)
        item.quickReadPoints().forEach { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("-", color = Muted, lineHeight = 21.sp)
                Text(point, color = Muted, lineHeight = 21.sp, modifier = Modifier.weight(1f))
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
    Text(
        label,
        color = if (active) Paper else Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(if (active) Accent else AccentSoft, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
private fun StoryMeta(item: FeedItem) {
    Text(
        listOf(item.source, item.publishedAt).filter { it.isNotBlank() }.joinToString("  |  "),
        color = Muted,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
    Box(modifier.background(AccentSoft, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
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
        shape = RoundedCornerShape(8.dp),
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
): FeedState = withContext(Dispatchers.IO) {
    runCatching {
        val sources = FeedSources
            .filter { sourceFilter.sourceName == null || it.name == sourceFilter.sourceName }
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
        val filteredItems = availableItems.filterFor(section).take(24)
            .withCoverageContext()
        filteredItems.firstOrNull()?.let { firstItem ->
            if (BudgiePrefs.shouldNotify(context, section, firstItem, settings)) {
                BudgieNotifications.notifyFor(context, section, firstItem)
            }
        }
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
    SourceFilter.ALL -> "Using ${FeedSources.size} curated UK/GB feeds"
    else -> "Showing $label stories only"
}

private fun String.shortSourceName(): String = when {
    contains("BBC", ignoreCase = true) -> "BBC"
    contains("Sky", ignoreCase = true) -> "Sky"
    contains("Guardian", ignoreCase = true) -> "Guardian"
    contains("Independent", ignoreCase = true) -> "Independent"
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
    if (next() != XmlPullParser.TEXT) return ""
    val result = text.orEmpty()
    nextTag()
    return result.trim()
}

private fun XmlPullParser.skipTag() {
    if (eventType != XmlPullParser.START_TAG) return
    var depth = 1
    while (depth != 0) {
        when (next()) {
            XmlPullParser.END_TAG -> depth--
            XmlPullParser.START_TAG -> depth++
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

@Composable
private fun BudgieNewsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
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
                    title = "Markets and politics drive a busy morning briefing",
                    description = "A compact summary of the biggest stories available from the test RSS feed.",
                    link = "https://www.bbc.co.uk/news/uk",
                    source = "BBC UK",
                    publishedAt = "Jun 23, 7:10 PM",
                    imageUrl = null,
                    coverageSources = listOf("BBC UK", "Sky News UK"),
                ),
                FeedItem(
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
            onSectionSelected = {},
            onSourceSelected = {},
            onStorySelected = {},
            modifier = Modifier.background(Paper),
        )
    }
}
