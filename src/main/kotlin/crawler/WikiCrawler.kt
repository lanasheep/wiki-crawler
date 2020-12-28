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

class WikiCrawler(private val database: WikiPagesDB) {
    private val logger = KotlinLogging.logger {}
    private var cntImages = 0
    private var cntPages = 0
    private var path = ""
    private var cntPagesMax = 30
    private val queue = PriorityQueue({(_, a): Pair<String, Int>, (_, b): Pair<String, Int> ->
                                        database.getLinksToCnt(b) - database.getLinksToCnt(a)})

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
            File("${path}${File.separator}${IMAGES_DIR}").mkdir()
            File("${path}${File.separator}${ARTICLES_DIR}").mkdir()
        }
        catch (e: Exception) {
            Helper.error(logger, e)
        }
    }

    fun setCntPagesMax(cntPagesMax: Int) {
        this.cntPagesMax = cntPagesMax
    }

    fun crawl(timeStart: DateTime): List<ChangedPage> {
        val changedPages = mutableListOf<ChangedPage>()
        while (!queue.isEmpty() && cntPages < cntPagesMax) {
            val (url, id) = queue.poll()
            Helper.info(logger, "Start processing $url\n")
            val doc = getDoc(url)
            if (doc == null) {
                continue
            }
            val pages = extractWikiPageUrls(doc)
            val images = extractImageUrls(doc)
            val (content, heading) = extractArticle(doc)
            if (pageViewed(id)) {
                val diff = getDifference(id, pages.size, images.size, content.length + heading.length, content)
                if (diff != null) {
                    changedPages.add(ChangedPage(url, heading, diff))
                }
            }
            database.updTimeLastView(id)
            pages.forEach { val idTo = addUrl(WIKI_HTTPS + it, timeStart)
                if (idTo != null) database.addLink(id, idTo) }
            images.forEach { saveImageFromUrl(it, id) }
            saveArticle(content, heading, id)
            cntPages++
            cntImages = 0
            Helper.info(logger, "Finish processing $url\n")
        }
        return changedPages
    }

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
            Helper.error(logger, e)
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
            Helper.error(logger, e)
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
        doc.select("span.mw-editsection, div.toc, sup.reference, span[id=Примечания]").remove()
        return Pair(doc.select("div.mw-parser-output").text(), heading)
    }

    private fun getDoc(url: String): Document? {
        var doc: Document? = null
        try {
            doc = Jsoup.connect(url).get()
        }
        catch (e: Exception) {
            Helper.error(logger, e)
        }
        return doc
    }

    private fun pageViewed(id: Int): Boolean {
        return database.getTimeLastView(id) != null
    }

    private fun getFirstContentMismatch(id: Int, content: String): Int {
        val contentPrev = database.getArticleContent(id)!!
        val matchLen = content.commonPrefixWith(contentPrev).length
        if (matchLen == content.length) {
            return 0
        }
        else {
            return matchLen + 1
        }
    }

    private fun getDifference(id: Int, cntLinks: Int, cntImages: Int, articleLen: Int, content: String): Difference? {
        val cntPagesDiff = cntLinks - database.getLinksFromCnt(id)
        val cntImagesDiff = cntImages - database.getImagesCnt(id)
        val articleLenDiff = articleLen - database.getArticleLen(id)!!
        val firstContentMismatch = getFirstContentMismatch(id, content)
        if (cntPagesDiff != 0 || cntImagesDiff != 0 || articleLenDiff != 0 || firstContentMismatch != 0) {
            return Difference(cntPagesDiff, cntImagesDiff, articleLenDiff, firstContentMismatch)
        }
        else {
            return null
        }
    }
}