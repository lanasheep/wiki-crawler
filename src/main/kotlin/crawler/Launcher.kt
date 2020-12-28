import mu.KotlinLogging
import org.joda.time.DateTime
import java.util.concurrent.Executors
import kotlin.math.min

class Launcher(private val urls: List<String>,
               private var concurrencyLevel: Int,
               private val cntPagesMax: Int,
               private val path: String? = null) {
    private val logger = KotlinLogging.logger {}
    private var database = WikiPagesDB()

    init {
        concurrencyLevel = min(concurrencyLevel, urls.size)
    }

    fun launch(): List<ChangedPage> {
        Helper.info(logger, "Launch started\n")
        val executor = Executors.newFixedThreadPool(concurrencyLevel)
        val changedPages = mutableListOf<ChangedPage>()
        var block = urls.size / concurrencyLevel
        if (urls.size % concurrencyLevel != 0) {
            block++
        }
        val urlsDivided = urls.sortedBy { database.getLinksToCnt(database.getUrlId(it)) }.withIndex()
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
                changedPages += crawler.crawl(timeStart)
            }
            executor.execute(worker)
        }
        executor.shutdown()
        while (!executor.isTerminated) {
        }
        Helper.info(logger, "Launch completed\n")
        return changedPages
    }
}