package com.budgienews.app

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricPrompt
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.text.Html
import android.util.Xml
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URLDecoder
import java.net.URL
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
        setContent {
            BudgieNewsTheme {
                if (isUnlocked) {
                    NewsApp()
                } else {
                    LockedApp(authMessage, onUnlock = { authenticate() })
                }
            }
        }
        window.decorView.post { authenticate() }
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

private object BudgieNotifications {
    const val BREAKING_CHANNEL_ID = "budgie_news_breaking"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            BREAKING_CHANNEL_ID,
            "Breaking news",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Important Budgie News alerts"
        }
        manager.createNotificationChannel(channel)
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

private enum class SourceFilter(val label: String, val sourceName: String?) {
    ALL("All", null),
    BBC("BBC", "BBC UK"),
    SKY("Sky", "Sky News UK"),
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
private fun NewsApp() {
    var refreshToken by remember { mutableStateOf(0) }
    var selectedSection by remember { mutableStateOf(NewsSection.HEADLINES) }
    var selectedSource by remember { mutableStateOf(SourceFilter.ALL) }
    var state by remember { mutableStateOf<FeedState>(FeedState.Loading) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        state = FeedState.Loading
        scope.launch {
            state = fetchFeeds(selectedSection, selectedSource)
        }
    }

    LaunchedEffect(refreshToken, selectedSection, selectedSource) {
        state = FeedState.Loading
        state = fetchFeeds(selectedSection, selectedSource)
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
                            Text(selectedSource.tagline(selectedSection), color = Muted, fontSize = 12.sp)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { refreshToken++ }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh news", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Paper),
            )
        },
    ) { padding ->
        when (val value = state) {
            FeedState.Loading -> LoadingNews(Modifier.padding(padding))
            is FeedState.Error -> ErrorNews(value.message, ::refresh, Modifier.padding(padding))
            is FeedState.Ready -> NewsList(
                items = value.items,
                selectedSection = selectedSection,
                selectedSource = selectedSource,
                onSectionSelected = { selectedSection = it },
                onSourceSelected = { selectedSource = it },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun LoadingNews(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Accent, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text("Fetching latest headlines", color = Ink, fontWeight = FontWeight.Medium)
        }
    }
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
                LeadStory(items.firstOrNull(), selectedSection)
            }
            items(items.drop(1)) { item ->
                StoryCard(item)
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
private fun LeadStory(item: FeedItem?, section: NewsSection) {
    if (item == null) return
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { context.openUrl(item.link) },
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

@Composable
private fun StoryCard(item: FeedItem) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { context.openUrl(item.link) },
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
    val bitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.IO) { url?.let(::loadBitmap) }
    }
    Box(modifier.background(AccentSoft, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(Icons.AutoMirrored.Rounded.Article, contentDescription = null, tint = Accent, modifier = Modifier.size(30.dp))
        }
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

private suspend fun fetchFeeds(section: NewsSection, sourceFilter: SourceFilter): FeedState = withContext(Dispatchers.IO) {
    runCatching {
        val loadedItems = FeedSources
            .filter { sourceFilter.sourceName == null || it.name == sourceFilter.sourceName }
            .flatMap { source -> runCatching { fetchFeed(source).take(12) }.getOrDefault(emptyList()) }
            .distinctBy { it.link.ifBlank { it.title } }
        if (loadedItems.isEmpty()) {
            val sourceNames = FeedSources
                .filter { sourceFilter.sourceName == null || it.name == sourceFilter.sourceName }
                .joinToString { it.name }
            error("No stories loaded from $sourceNames")
        }
        loadedItems.filterFor(section).take(24)
            .withCoverageContext()
    }.fold(
        onSuccess = { items ->
            FeedState.Ready(items)
        },
        onFailure = { FeedState.Error(it.message ?: "Unexpected feed error") },
    )
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
        if (event == XmlPullParser.START_TAG && parser.name.equals("item", ignoreCase = true)) {
            items += readItem(parser, fallbackSource)
        }
        event = parser.next()
    }
    return items.take(40)
}

private fun readItem(parser: XmlPullParser, fallbackSource: String): FeedItem {
    var title = ""
    var description = ""
    var link = ""
    var source = fallbackSource
    var pubDate = ""
    var imageUrl: String? = null

    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.END_TAG && parser.name.equals("item", ignoreCase = true)) break
        if (parser.eventType != XmlPullParser.START_TAG) continue

        when (parser.name.lowercase()) {
            "title" -> title = parser.readText()
            "description" -> description = parser.readText().stripHtml()
            "link" -> link = parser.readText()
            "pubdate" -> pubDate = parser.readText().formatRssDate()
            "source" -> source = parser.readText().ifBlank { source }
            "enclosure" -> imageUrl = parser.attributeValue("url") ?: imageUrl
            "thumbnail", "media:thumbnail", "media:content" -> imageUrl = parser.attributeValue("url") ?: imageUrl
            "content:encoded" -> {
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
    SourceFilter.ALL -> "Using 2 curated GB feeds: BBC UK and Sky News UK"
    SourceFilter.BBC -> "Showing BBC UK stories only"
    SourceFilter.SKY -> "Showing Sky News UK stories only"
}

private fun String.shortSourceName(): String = when {
    contains("BBC", ignoreCase = true) -> "BBC"
    contains("Sky", ignoreCase = true) -> "Sky"
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

private fun String.formatRssDate(): String = try {
    DateTimeFormatter.ofPattern("MMM d, h:mm a").format(ZonedDateTime.parse(this, DateTimeFormatter.RFC_1123_DATE_TIME))
} catch (_: DateTimeParseException) {
    this
}

private fun loadBitmap(url: String): Bitmap? = runCatching {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8_000
        readTimeout = 10_000
        setRequestProperty("User-Agent", "Budgie News Android")
    }
    try {
        connection.inputStream.use(BitmapFactory::decodeStream)
    } finally {
        connection.disconnect()
    }
}.getOrNull()

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
            modifier = Modifier.background(Paper),
        )
    }
}
