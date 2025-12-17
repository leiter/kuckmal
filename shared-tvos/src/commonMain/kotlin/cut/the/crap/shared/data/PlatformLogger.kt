package cut.the.crap.shared.data

/**
 * Platform-specific logging.
 */
expect object PlatformLogger {
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
