package com.septaalfauzan.saku.util
import platform.Foundation.NSLog

actual object Logger {
    actual fun d(tag: String, message: String) {
        NSLog("[%@] %@", tag, message)
    }
}
