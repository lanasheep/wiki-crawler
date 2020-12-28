import mu.KLogger
import java.io.*

class Helper {
    companion object {
        fun info(logger: KLogger, message: String) {
            logger.info(message)
        }

        fun error(logger: KLogger, message: String) {
            logger.error(message)
        }

        fun error(logger: KLogger, e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
    }
}