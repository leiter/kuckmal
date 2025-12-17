package cut.the.crap.shared.data

import platform.Foundation.NSLog

actual object PlatformLogger {
    actual fun debug(tag: String, message: String) {
        NSLog("D/$tag: $message")
    }

    actual fun info(tag: String, message: String) {
        NSLog("I/$tag: $message")
    }

    actual fun warn(tag: String, message: String) {
        NSLog("W/$tag: $message")
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            NSLog("E/$tag: $message - ${throwable.message}")
        } else {
            NSLog("E/$tag: $message")
        }
    }
}
