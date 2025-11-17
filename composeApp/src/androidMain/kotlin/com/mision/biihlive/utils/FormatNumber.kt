package com.mision.biihlive.utils

fun formatNumber(number: Int): String {
    return when {
        number >= 1000_000_000 -> {
            val millions = number / 1000_000_000f
            if (millions.rem(1) == 0f) {
                "${millions.toInt()}B"
            } else {
                "%.1fM".format(millions)
            }
        }
        number >= 1_000_000 -> {
            val millions = number / 1_000_000f
            if (millions.rem(1) == 0f) {
                "${millions.toInt()}M"
            } else {
                "%.1fM".format(millions)
            }
        }
        number >= 100_000 -> {
            val thousands = number / 1_000f
            if (thousands.rem(1) == 0f) {
                "${thousands.toInt()}K"
            } else {
                "%.1fK".format(thousands)
            }
        }
        else -> number.toString()
    }
}