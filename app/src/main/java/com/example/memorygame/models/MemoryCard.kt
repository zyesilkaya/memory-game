package com.example.memorygame.models

data class MemoryCard(
    val identifier: Int,
    var isFaceUp: Boolean = false,
    var isMatced: Boolean=false
)