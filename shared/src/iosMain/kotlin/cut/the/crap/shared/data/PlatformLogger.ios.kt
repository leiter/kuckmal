package cut.the.crap.shared.data

import platform.Foundation.NSLog

actual object PlatformLogger {
    actual fun debug(tag: String, message: String) {
        NSLog("D/$tag: $message")
        IosFileLogger.log("DEBUG", tag, message)
    }

    actual fun info(tag: String, message: String) {
        NSLog("I/$tag: $message")
        IosFileLogger.log("INFO", tag, message)
    }

    actual fun warn(tag: String, message: String) {
        NSLog("W/$tag: $message")
        IosFileLogger.log("WARN", tag, message)
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        val fullMessage = if (throwable != null) {
            "$message - ${throwable.message}\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        NSLog("E/$tag: $fullMessage")
        IosFileLogger.log("ERROR", tag, fullMessage)
    }
}
