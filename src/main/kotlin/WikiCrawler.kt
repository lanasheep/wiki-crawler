import java.util.PriorityQueue
import java.util.LinkedList
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import mu.KotlinLogging
import java.io.*
import java.net.URL
import javax.imageio.ImageIO
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.joda.time.DateTime

const val WIKI_HTTPS ="https://ru.wikipedia.org/"
const val IMAGES_DIR = "images"
const val ARTICLES_DIR = "articles"

data class ChangedPage(val url: String, val heading: String,
                       val valuesBefore: Triple<Int, Int, Int>, val valuesAfter: Triple<Int, Int, Int>)

class WikiCrawler(private val database: DB) {
    private val logger = KotlinLogging.logger {}
    private var cntImages = 0
    private var cntPages = 0
    private var path = ""
    private var cntPagesMax = 30
    private val queue = PriorityQueue({(_, a): Pair<String, Int>, (_, b): Pair<String, Int> ->
                                        database.getLinksToCnt(b) - database.getLinksToCnt(a)})

    private fun saveImageFromUrl(url: String, id: Int) {
        try {
            val image = ImageIO.read(URL(url))
            val formatName = url.takeLastWhile { it != '.' }.toLowerCase()
            val byteOutputStream = ByteArrayOutputStream()
            ImageIO.write(image, formatName, byteOutputStream)
            database.addImage(ExposedBlob(byteOutputStream.toByteArray()), id)
            if (path == "") {
                return
            }
            val file = File(path + "/" + IMAGES_DIR + "/" + cntPages.toString() +
                    "_" + cntImages.toString() + "." + formatName)
            ImageIO.write(image, formatName, file)
            cntImages++
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
    }

    private fun saveArticle(content: String, heading: String, id: Int) {
        database.addArticle(content, heading, id)
        if (path == "") {
            return
        }
        try {
            val file = File(path + "/" + ARTICLES_DIR + "/" + cntPages.toString() + ".txt")
            FileWriter(file).use { it.write(heading + "\n" + content) }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
    }

    private fun extractWikiPageUrls(doc: Document): List<String> {
        doc.select("td.mbox-text, div.mw-references-wrap, div.navbox").remove()
        return doc.select("div.mw-parser-output")
                  .select("ul, p, h2, table.wikitable")
                  .select("a[href]")
                  .map { it.attr("href") }
                  .filter { it.startsWith("/wiki/") }
                  .filter { '.' !in it.takeLast(7) }
    }

    private fun extractImageUrls(doc: Document): List<String> {
        return doc.select("div.thumb, div.thumbinner")
                  .select("img")
                  .map { it.attr("src") }
                  .map { if (it.startsWith("https:")) it else "https:" + it }
                  .filter { '.' in it.takeLast(7) }
    }

    private fun extractArticle(doc: Document): Pair<String, String> {
        val heading = doc.select("div[id=content]").select("h1[id=firstHeading]").text()
        //println(heading)
        doc.select("span.mw-editsection, div.toc, sup.reference, span[id=Примечания]").remove()
        return Pair(doc.select("div.mw-parser-output").text(), heading)
    }

    private fun getDoc(url: String): Document? {
        var doc: Document? = null
        try {
            doc = Jsoup.connect(url).get()
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return doc
    }

    private fun pageChanged(id: Int, cntLinks: Int, cntImages: Int, articleLen: Int): Boolean {
        return database.getTimeLastView(id) != null && (cntLinks != database.getLinksFromCnt(id) || cntImages != database.getImagesCnt(id) ||
               articleLen != database.getArticleLen(id))
    }

    fun addUrl(url: String, timeStart: DateTime): Int? {
        val id = database.addUrl(url, timeStart)
        if (id != null) {
            queue.add(Pair(url, id))
            return id
        } else {
            return null
        }
    }

    fun setPath(path: String) {
        this.path = path
        try {
            File(path + "/" + IMAGES_DIR).mkdir()
            File(path + "/" + ARTICLES_DIR).mkdir()
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
    }

    fun setCntPagesMax(cntPagesMax: Int) {
        this.cntPagesMax = cntPagesMax
    }

    fun crawl(timeStart: DateTime): List<ChangedPage> {
        val changedPages = mutableListOf<ChangedPage>()
        while (!queue.isEmpty() && cntPages < cntPagesMax) {
            val (url, id) = queue.poll()
            logger.info("Start processing $url\n")
            val doc = getDoc(url)
            if (doc == null) {
                continue
            }
            val pages = extractWikiPageUrls(doc)
            val images = extractImageUrls(doc)
            val (content, heading) = extractArticle(doc)
            //println(url)
            if (pageChanged(id, pages.size, images.size, content.length + heading.length)) {
                database.updTimeLastChange(id)
                changedPages.add(
                    ChangedPage(url, heading,
                    Triple(database.getLinksFromCnt(id), database.getImagesCnt(id), database.getArticleLen(id)!!),
                    Triple(pages.size, images.size, content.length + heading.length))
                )
            }
            database.updTimeLastView(id)
            pages.forEach { val idTo = addUrl(WIKI_HTTPS + it, timeStart)
                            if (idTo != null) database.addLink(id, idTo) }
            images.forEach { saveImageFromUrl(it, id) }
            saveArticle(content, heading, id)
            cntPages++
            cntImages = 0
            logger.info("Finish processing $url\n")
        }
        return changedPages
    }
}