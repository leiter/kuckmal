package cut.the.crap.shared.data

actual object PlatformLogger {
    actual fun debug(tag: String, message: String) {
        println("D/$tag: $message")
    }

    actual fun info(tag: String, message: String) {
        println("I/$tag: $message")
    }

    actual fun warn(tag: String, message: String) {
        println("W/$tag: $message")
    }

    actual fun error(tag: String, message: String, throwable: Throwable?) {
        System.err.println("E/$tag: $message")
        throwable?.printStackTrace()
    }
}
