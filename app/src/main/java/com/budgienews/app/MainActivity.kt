package com.budgienews.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Xml
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.rounded.Article
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
    FeedSource("Yahoo Headlines", "https://www.yahoo.com/news/rss"),
    FeedSource("BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BudgieNewsTheme { NewsApp() } }
    }
}

private data class FeedItem(
    val title: String,
    val description: String,
    val link: String,
    val source: String,
    val publishedAt: String,
    val imageUrl: String?,
)

private data class FeedSource(
    val name: String,
    val url: String,
)

private enum class NewsSection(
    val label: String,
    val tagline: String,
    val emptyText: String,
) {
    HEADLINES(
        "Headlines",
        "Curated from two test feeds",
        "No headlines were found in the selected feeds.",
    ),
    BREAKING(
        "Breaking",
        "Live, urgent, and developing stories",
        "No breaking stories matched the current filters.",
    ),
    IMPORTANT(
        "Important",
        "High-impact world and public-interest stories",
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
    var state by remember { mutableStateOf<FeedState>(FeedState.Loading) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        state = FeedState.Loading
        scope.launch {
            state = fetchFeeds(selectedSection)
        }
    }

    LaunchedEffect(refreshToken, selectedSection) {
        state = FeedState.Loading
        state = fetchFeeds(selectedSection)
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
                            Text(selectedSection.tagline, color = Muted, fontSize = 12.sp)
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
                onSectionSelected = { selectedSection = it },
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
    onSectionSelected: (NewsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionMenu(selectedSection, onSectionSelected)
        }
        item {
            FeedSourceNote()
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
    onSectionSelected: (NewsSection) -> Unit,
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
    }
}

@Composable
private fun FeedSourceNote() {
    Text(
        "Using 2 curated feeds: Yahoo Headlines and BBC World",
        color = Muted,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
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
                Text(section.label, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(item.title, color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
                if (item.description.isNotBlank()) {
                    Text(item.description, color = Muted, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                }
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
                StoryMeta(item)
            }
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open story", tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
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

private suspend fun fetchFeeds(section: NewsSection): FeedState = withContext(Dispatchers.IO) {
    runCatching {
        FeedSources
            .flatMap { source -> fetchFeed(source).take(12) }
            .distinctBy { it.link.ifBlank { it.title } }
            .filterFor(section)
            .take(24)
    }.fold(
        onSuccess = { items ->
            FeedState.Ready(items)
        },
        onFailure = { FeedState.Error(it.message ?: "Unexpected feed error") },
    )
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
            "thumbnail", "content" -> imageUrl = parser.attributeValue("url") ?: imageUrl
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
                    link = "https://www.yahoo.com/news/",
                    source = "Yahoo News",
                    publishedAt = "Jun 23, 7:10 PM",
                    imageUrl = null,
                ),
                FeedItem(
                    title = "World leaders meet as new policy announcements continue",
                    description = "Latest updates are displayed in a scrollable modern news feed.",
                    link = "https://www.yahoo.com/news/",
                    source = "Yahoo News",
                    publishedAt = "Jun 23, 6:42 PM",
                    imageUrl = null,
                ),
            ),
            selectedSection = NewsSection.HEADLINES,
            onSectionSelected = {},
            modifier = Modifier.background(Paper),
        )
    }
}
