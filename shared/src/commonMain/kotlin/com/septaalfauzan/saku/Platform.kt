package com.septaalfauzan.saku

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
