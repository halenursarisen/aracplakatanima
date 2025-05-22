package com.example.kullaniciapp

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val isOption: Boolean = false
)

