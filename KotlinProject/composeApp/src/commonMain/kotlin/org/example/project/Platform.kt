package org.example.project

interface Platform {
    val name: String
    val embedsHtmlMap: Boolean get() = false
}

expect fun getPlatform(): Platform
