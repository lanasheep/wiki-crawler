import java.util.Queue
import java.util.LinkedList
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.*
import java.net.URL
import javax.imageio.ImageIO
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

const val WIKI_HTTPS ="https://ru.wikipedia.org/"
const val IMAGES_DIR = "images"
const val ARTICLES_DIR = "articles"

class WikiCrawler(private val database: DB) {
    private var cntImages = 0
    private var cntPages = 0
    private var path = ""
    private var cntPagesMax = 5
    private val queue: Queue<Pair<String, Int>> = LinkedList<Pair<String, Int>>()

    private fun saveImageFromUrl(url: String, id: Int) {
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

    private fun saveArticle(article: String, id: Int) {
        database.addArticle(article, id)
        if (path == "") {
            return
        }
        val file = File(path + "/" + ARTICLES_DIR + "/" + cntPages.toString() + ".txt")
        FileWriter(file).use { it.write(article) }
    }

    private fun extractWikiPageUrls(doc: Document): List<String> {
        doc.select("td.mbox-text, div.mw-references-wrap, div.navbox").remove()
        return doc.select("div.mw-parser-output")
                  .select("ul, p, h2, table.wikitable")
                  .select("a[href]")
                  .map { it.attr("href") }
                  .filter { it.startsWith("/wiki/") }
    }

    private fun extractImageUrls(doc: Document): List<String> {
        return doc.select("div.thumb, div.thumbinner")
                  .select("img")
                  .map { it.attr("src") }
                  .map { if (it.startsWith("https:")) it else "https:" + it }
                  .filter { '.' in it.takeLast(7) }
    }

    private fun extractArticle(doc: Document): String {
        doc.select("span.mw-editsection, div.toc, sup.reference, span[id=Примечания]").remove()
        return doc.select("div.mw-parser-output").text()
    }

    fun addUrl(url: String): Int? {
        val id = database.addUrl(url)
        if (id != null) {
            queue.add(Pair(url, id))
            return id
        } else {
            return null
        }
    }

    fun setPath(path: String) {
        this.path = path
        File(path + "/" + IMAGES_DIR).mkdir()
        File(path + "/" + ARTICLES_DIR).mkdir()
    }

    fun setCntPagesMax(cntPagesMax: Int) {
        this.cntPagesMax = cntPagesMax
    }

    fun crawl() {
        while (!queue.isEmpty() && cntPages < cntPagesMax) {
            val (url, id) = queue.poll()
            val doc = Jsoup.connect(url).get()
            val pages = extractWikiPageUrls(doc)
            val images = extractImageUrls(doc)
            val article = extractArticle(doc)
            pages.forEach { val idTo = addUrl(WIKI_HTTPS + it)
                            if (idTo != null) database.addLink(id, idTo) }
            images.forEach { saveImageFromUrl(it, id) }
            saveArticle(article, id)
            cntPages++
            cntImages = 0
        }
    }
}