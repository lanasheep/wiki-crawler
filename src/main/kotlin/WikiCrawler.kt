import java.util.Queue
import java.util.LinkedList
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.*
import java.net.URL
import javax.imageio.ImageIO
import java.io.FileWriter

const val MAX_PAGES_CNT = 30
const val WIKI_HTTPS = "https://ru.wikipedia.org/"
const val IMG_DIR = "img"
const val TXT_DIR = "txt"

class WikiCrawler {
    private var cntImages = 0
    private var cntPages = 0
    private var path = ""
    private val queue: Queue<String> = LinkedList<String>()
    private val index: MutableSet<String> = mutableSetOf()

    private fun saveImageFromUrl(url: String) {
        val image = ImageIO.read(URL(url))
        val formatName = url.takeLastWhile { it != '.' }.toLowerCase()
        val file = File(path + "/" + IMG_DIR + "/" + cntPages.toString() +
                                  "_" + cntImages.toString() + "." + formatName)
        ImageIO.write(image, formatName, file)
        cntImages++
    }

    private fun saveText(text: String) {
        val file = File(path + "/" + TXT_DIR + "/" + cntPages.toString() + ".txt")
        FileWriter(file).use { it.write(text) }
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

    private fun extractText(doc: Document): String {
        doc.select("span.mw-editsection, div.toc, sup.reference, span[id=Примечания]").remove()
        return doc.select("div.mw-parser-output").text()
    }

    fun setPath(path: String) {
        this.path = path
    }

    fun addUrl(url: String)
    {
        if (url in index) {
            return
        }
        queue.add(url)
        index.add(url)
    }

    fun crawl() {
        File(path + "/" + IMG_DIR).mkdir()
        File(path + "/" + TXT_DIR).mkdir()
        while (!queue.isEmpty() && cntPages < MAX_PAGES_CNT) {
            val url = queue.poll()
            val doc = Jsoup.connect(url).get()
            val pages = extractWikiPageUrls(doc)
            val images = extractImageUrls(doc)
            val text = extractText(doc)
            pages.forEach { addUrl(WIKI_HTTPS + it) }
            images.forEach { saveImageFromUrl(it) }
            saveText(text)
            cntPages++
            cntImages = 0
        }
    }
}