package com.septaalfauzan.saku

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getAppVersion(): String =
    (NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String) ?: "?"
