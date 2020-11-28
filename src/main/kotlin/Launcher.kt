import java.util.concurrent.Executors
import kotlin.math.min

class Launcher(private val urls: List<String>,
               private var concurrencyLevel: Int,
               private val cntPagesMax: Int,
               private val path: String? = null) {
    private val database: DB
    init {
        concurrencyLevel = min(concurrencyLevel, urls.size)
        database = DB()
    }
    fun launch() {
        val executor = Executors.newFixedThreadPool(concurrencyLevel)
        for (i in 0..concurrencyLevel - 1) {
            val worker = Runnable {
                val crawler = WikiCrawler(database)
                if (path != null) {
                    crawler.setPath(path)
                }
                crawler.setCntPagesMax(cntPagesMax)
                val from = i * (urls.size / concurrencyLevel)
                val to = if (i + 1 != concurrencyLevel) (i + 1) * (urls.size / concurrencyLevel) else urls.size
                urls.subList(from, to).forEach { crawler.addUrl(it) }
                crawler.crawl()
            }
            executor.execute(worker)
        }
        executor.shutdown()
        while (!executor.isTerminated) {
        }
    }
}