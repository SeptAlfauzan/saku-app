package com.septaalfauzan.saku.util
import platform.os.Logger as IOSLogger

actual object Logger {
    private val logger = IOSLogger(
        subsystem = "com.septalfauzan",
        category = "Saku"
    )
    actual fun d(tag: String, message: String) {
        logger.debug("[$tag] $message")
    }
}