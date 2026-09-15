package com.septaalfauzan.saku

import android.os.Build
import com.septaalfauzan.saku.di.AndroidContextHolder

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getAppVersion(): String {
    val ctx = AndroidContextHolder.applicationContext
    @Suppress("DEPRECATION")
    return ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
}
