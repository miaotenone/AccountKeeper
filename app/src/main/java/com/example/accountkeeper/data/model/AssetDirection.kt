package com.example.accountkeeper.data.model

enum class AssetDirection {
    POSITIVE,
    NEGATIVE;

    companion object {
        fun fromStoredValue(value: String?): AssetDirection = when (value) {
            "NEGATIVE", "VIRTUAL" -> NEGATIVE
            else -> POSITIVE
        }
    }
}