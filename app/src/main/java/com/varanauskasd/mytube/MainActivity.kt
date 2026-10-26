package com.varanauskasd.mytube

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val INITIAL_PER_CHANNEL = 3
private const val LOAD_MORE_STEP = 3

data class FeedEntry(val channelUrl: String, val video: ChannelVideo)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FeedScreen()
            }
        }
    }
}

private data class FeedFetchResult(
    val feed: List<FeedEntry>,
    val avatarUrls: Map<String, String?>,
    val failedChannels: List<String>,
)

private suspend fun fetchFeed(channels: List<String>, perChannelLimit: Int): FeedFetchResult =
    withContext(Dispatchers.IO) {
        val failed = mutableListOf<String>()
        val snapshots = channels.map { url ->
            url to runCatching { ChannelRepository.fetchChannelSnapshot(url, perChannelLimit) }
                .getOrElse { failed.add(channelDisplayName(url)); null }
        }
        val feed = snapshots
            .flatMap { (url, snapshot) -> snapshot?.videos.orEmpty().map { FeedEntry(url, it) } }
            .sortedByDescending { it.video.uploadEpochSecond ?: Long.MIN_VALUE }
        val avatarUrls = snapshots.associate { (url, snapshot) -> url to snapshot?.avatarUrl }
        FeedFetchResult(feed, avatarUrls, failed)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreen() {
    val context = LocalContext.current

    var subscriptions by remember { mutableStateOf(SubscriptionsStore.load(context)) }
    var perChannelLimit by remember { mutableStateOf(INITIAL_PER_CHANNEL) }
    var feed by remember { mutableStateOf<List<FeedEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilterChannel by remember { mutableStateOf<String?>(null) }

    var showAddPanel by remember { mutableStateOf(false) }
    var newChannelUrl by remember { mutableStateOf("") }
    var showSidebar by remember { mutableStateOf(false) }
    var avatarUrls by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(subscriptions, perChannelLimit, refreshTrigger) {
        if (subscriptions.isEmpty()) {
            feed = emptyList()
            errorMessage = null
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        val result = fetchFeed(subscriptions, perChannelLimit)
        feed = result.feed
        // Keep a channel's last-known avatar if this fetch failed to refresh it,
        // rather than letting a transient failure blank out a working icon.
        avatarUrls = avatarUrls + result.avatarUrls.filterValues { it != null }
        if (result.failedChannels.isNotEmpty()) {
            errorMessage = "Couldn't load: ${result.failedChannels.joinToString(", ")}"
        }
        isLoading = false
    }

    fun resetAndReload() {
        selectedFilterChannel = null
        perChannelLimit = INITIAL_PER_CHANNEL
        refreshTrigger++
    }

    fun addSubscription(input: String) {
        if (input.isBlank()) return
        val url = normalizeChannelInput(input)
        if (subscriptions.contains(url)) return
        subscriptions = subscriptions + url
        SubscriptionsStore.save(context, subscriptions)
    }

    fun removeSubscription(url: String) {
        subscriptions = subscriptions.filterNot { it == url }
        SubscriptionsStore.save(context, subscriptions)
        if (selectedFilterChannel == url) selectedFilterChannel = null
    }

    val topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val headerRed = Color(0xFFE57373)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(topBarScrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = { Text("MyTube", modifier = Modifier.clickable { resetAndReload() }) },
                    scrollBehavior = topBarScrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = headerRed,
                        scrolledContainerColor = headerRed,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { showSidebar = !showSidebar }) {
                            Icon(Icons.Default.Menu, contentDescription = "Subscriptions")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddPanel = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add channel")
                        }
                    },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                    PullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = { refreshTrigger++ },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        if (subscriptions.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "Welcome.\nAdd some channels.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else if (isLoading && feed.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val visibleFeed = selectedFilterChannel?.let { filter ->
                                feed.filter { it.channelUrl == filter }
                            } ?: feed

                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(visibleFeed) { entry ->
                                    VideoRow(entry.video) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.video.url)))
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Button(
                                            onClick = { perChannelLimit += LOAD_MORE_STEP },
                                            enabled = !isLoading && subscriptions.isNotEmpty(),
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Load more")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showSidebar,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { showSidebar = false },
                    )
                }

                AnimatedVisibility(
                    visible = showSidebar,
                    enter = slideInHorizontally(initialOffsetX = { -it }),
                    exit = slideOutHorizontally(targetOffsetX = { -it }),
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    SubscriptionsSidebar(
                        subscriptions = subscriptions,
                        selectedUrl = selectedFilterChannel,
                        avatarUrls = avatarUrls,
                        onSelect = { url ->
                            selectedFilterChannel = if (selectedFilterChannel == url) null else url
                            showSidebar = false
                        },
                        onRemove = { removeSubscription(it) },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showAddPanel,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showAddPanel = false },
            )
        }

        AnimatedVisibility(
            visible = showAddPanel,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            AddChannelPanel(
                value = newChannelUrl,
                onValueChange = { newChannelUrl = it },
                onDismiss = {
                    showAddPanel = false
                    newChannelUrl = ""
                },
                onAdd = {
                    addSubscription(newChannelUrl)
                    newChannelUrl = ""
                    showAddPanel = false
                },
            )
        }
    }
}

@Composable
private fun AddChannelPanel(
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 72.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Add channel",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("Channel handle") },
                    placeholder = { Text("@handle") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onAdd, enabled = value.isNotBlank(), modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add channel")
                }
            }
        }
    }
}

@Composable
private fun SubscriptionsSidebar(
    subscriptions: List<String>,
    selectedUrl: String?,
    avatarUrls: Map<String, String?>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "Subscriptions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(subscriptions) { url ->
                    val isSelected = url == selectedUrl
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        AsyncImage(
                            model = avatarUrls[url],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            channelDisplayName(url),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelect(url) },
                        )
                        IconButton(onClick = { onRemove(url) }, modifier = Modifier.size(32.dp)) {
                            Text("×", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoRow(video: ChannelVideo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        Spacer(Modifier.height(8.dp))
        Text(video.title, style = MaterialTheme.typography.titleMedium)
        video.uploadDateText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onClick) { Text("Open on YouTube") }
        HorizontalDivider()
    }
}
