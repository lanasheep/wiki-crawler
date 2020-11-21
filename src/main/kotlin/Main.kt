import java.io.*
//import mu.KotlinLogging

fun main(args: Array<String>) {
    //val logger = KotlinLogging.logger {}
    if (args.size < 2 || args.size > 2) {
        //logger.error("Wrong number of arguments.\nProgram terminated.")
        return
    }
    val fileUrls = args[0]
    if (!File(fileUrls).exists()) {
        //logger.error("File $fileUrls does not exist.\nProgram terminated.")
        return
    }
    val database = DB()
    val crawler = WikiCrawler(database)
    if (args.size == 2) {
        val filePath = args[1]
        if (!File(filePath).exists()) {
            //logger.error("File $filePath does not exist.\nProgram terminated.")
            return
        }
        crawler.setPath(FileReader(filePath).use { it.readText() })
    }
    try {
        FileReader(fileUrls).use { it.readLines().forEach { crawler.addUrl(it) } }
        crawler.crawl()
    }
    catch (e: Exception) {
        //logger.error(e.message + "\nProgram terminated.")
        return
    }
    //logger.info("Program completed successfully.")
}