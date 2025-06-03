package com.dawitf.akahidegn

import androidx.compose.runtime.Stable

@Stable
data class ChatMessage(
    val messageId: String? = null,
    val senderId: String? = null,
    var senderName: String? = null,
    val text: String? = null,
    val timestamp: Long? = null
) {
    constructor() : this(null, null, null, null, null)
}
