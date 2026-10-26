package com.varanauskasd.mytube

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private var initialized = false

private fun ensureNewPipeInitialized() {
    if (!initialized) {
        NewPipe.init(NewPipeDownloader())
        initialized = true
    }
}

data class ChannelSnapshot(val avatarUrl: String?, val videos: List<ChannelVideo>)

/**
 * Resolves a channel URL (e.g. https://www.youtube.com/@budgetforlifers) to its
 * avatar and most recent uploads, via NewPipeExtractor — no API key, no account.
 */
object ChannelRepository {

    fun fetchChannelSnapshot(channelUrl: String, limit: Int = 10): ChannelSnapshot {
        ensureNewPipeInitialized()

        val service = ServiceList.YouTube
        val channelHandler = service.channelLHFactory.fromUrl(channelUrl.trim())
        val channelExtractor: ChannelExtractor = service.getChannelExtractor(channelHandler)
        channelExtractor.fetchPage()

        val avatarUrl = channelExtractor.avatars.lastOrNull()?.url

        val videosTab = channelExtractor.tabs.firstOrNull {
            it.contentFilters.contains(ChannelTabs.VIDEOS)
        }
        val videos = if (videosTab == null) {
            emptyList()
        } else {
            val tabExtractor = service.getChannelTabExtractor(videosTab)
            tabExtractor.fetchPage()
            tabExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .take(limit)
                .map { item ->
                    ChannelVideo(
                        title = item.name,
                        url = item.url,
                        uploadDateText = item.textualUploadDate,
                        uploadEpochSecond = item.uploadDate?.offsetDateTime()?.toEpochSecond(),
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url,
                    )
                }
        }

        return ChannelSnapshot(avatarUrl, videos)
    }
}
