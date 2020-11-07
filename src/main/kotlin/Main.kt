import java.io.FileReader

fun main(args: Array<String>) {
    val fileUrls = args[0]
    val filePath = args[1]
    val crawler = WikiCrawler()
    crawler.setPath(FileReader(filePath).use { it.readText() })
    FileReader(fileUrls).use { it.readLines().forEach { crawler.addUrl(it) } }
    crawler.crawl()
}