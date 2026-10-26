package com.varanauskasd.mytube

data class ChannelVideo(
    val title: String,
    val url: String,
    val uploadDateText: String?,
    val uploadEpochSecond: Long?,
    val thumbnailUrl: String?,
)
