package com.mision.biihlive

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform