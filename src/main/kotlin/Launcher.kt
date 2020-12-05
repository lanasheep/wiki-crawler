import mu.KotlinLogging
import org.joda.time.DateTime
import java.util.concurrent.Executors
import kotlin.math.min

class Launcher(private val urls: List<String>,
               private var concurrencyLevel: Int,
               private val cntPagesMax: Int,
               private val path: String? = null) {
    private var database: DB

    init {
        concurrencyLevel = min(concurrencyLevel, urls.size)
        database = DB()
    }

    fun launch() {
        val executor = Executors.newFixedThreadPool(concurrencyLevel)
        var block = urls.size / concurrencyLevel
        if (urls.size % concurrencyLevel != 0) {
            block++
        }
        val urlsDivided = urls.sortedBy { database.getLinksCnt(database.getUrlId(it)) }.withIndex()
                              .groupBy { it.index % block }
        val timeStart = DateTime.now()
        for ((_, lst) in urlsDivided) {
            val worker = Runnable {
                val crawler = WikiCrawler(database)
                if (path != null) {
                    crawler.setPath(path)
                }
                crawler.setCntPagesMax(cntPagesMax)
                lst.forEach { crawler.addUrl(it.value, timeStart) }
                crawler.crawl(timeStart)
            }
            executor.execute(worker)
        }
        executor.shutdown()
        while (!executor.isTerminated) {
        }
    }
}