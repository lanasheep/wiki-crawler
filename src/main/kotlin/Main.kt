import java.io.*
import mu.KotlinLogging

fun main(args: Array<String>) {
    // launch<WikiCrawlerApp>()
    val logger = KotlinLogging.logger {}
    if (args.size < 2 || args.size > 2) {
        logger.error("Wrong number of arguments.\nProgram terminated.")
        return
    }
    val fileUrls = args[0]
    if (!File(fileUrls).exists()) {
        logger.error("File $fileUrls does not exist.\nProgram terminated.")
        return
    }
    val urls = mutableListOf<String>()
    FileReader(fileUrls).use { urls.addAll(it.readLines()) }
    var path: String? = null
    if (args.size == 2) {
        val filePath = args[1]
        if (!File(filePath).exists()) {
            logger.error("File $filePath does not exist.\nProgram terminated.")
            return
        }
        path = FileReader(filePath).use { it.readText() }
    }
    val launcher = Launcher(urls, 3, 10, path)
    launcher.launch()
}