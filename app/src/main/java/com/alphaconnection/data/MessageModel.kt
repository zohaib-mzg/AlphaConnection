package com.alphaconnection.data

data class MessageModel(
    val sender: String,
    val text: String,
    val timestamp: String,
    val isSent: Boolean
)
