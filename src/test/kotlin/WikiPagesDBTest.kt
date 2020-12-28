import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.joda.time.DateTime
import org.junit.Before
import java.io.*
import java.net.URL
import javax.imageio.ImageIO
import kotlin.test.*

class WikiPagesDBTest {
    @Before
    fun clear() {
        WikiPagesDB.deleteTables()
    }

    @Test
    fun addUrlTest() {
        val database = WikiPagesDB()
        assertNotNull(database.addUrl("aaa.com", DateTime.now()))
    }

    @Test
    fun addImageTest() {
        val database = WikiPagesDB()
        val id = database.addUrl("333.com", DateTime.now())!!
        val image = ImageIO.read(URL("https://upload.wikimedia.org/wikipedia/commons/3/3f/Sundogs_-_New_Ulm-Edit1.JPG"))
        val byteOutputStream = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", byteOutputStream)
        database.addImage(ExposedBlob(byteOutputStream.toByteArray()), id)
        assert(database.getImages(id).isNotEmpty())
    }

    @Test
    fun addArticleTest() {
        val database = WikiPagesDB()
        val id = database.addUrl("333.com", DateTime.now())!!
        database.addArticle("content", "heading", id)
        assertEquals(Pair("content", "heading"), database.getArticle(id))
    }

    @Test
    fun getUrlIdTest() {
        val database = WikiPagesDB()
        val id = database.addUrl("aaa.com", DateTime.now())
        assertEquals(id, database.getUrlId("aaa.com"))
        assertNull(database.getUrlId("bbb.com"))
    }

    @Test
    fun getLinksToCntTest() {
        val database = WikiPagesDB()
        val idFrom1 = database.addUrl("from1.com", DateTime.now())!!
        val idFrom2 = database.addUrl("from2.com", DateTime.now())!!
        val idTo = database.addUrl("to.com", DateTime.now())!!
        database.addLink(idFrom1, idTo)
        database.addLink(idFrom2, idTo)
        assertEquals(2, database.getLinksToCnt(idTo))
    }

    @Test
    fun getLinksFromCntTest() {
        val database = WikiPagesDB()
        val idFrom = database.addUrl("from.com", DateTime.now())!!
        val idTo1 = database.addUrl("to1.com", DateTime.now())!!
        val idTo2 = database.addUrl("to2.com", DateTime.now())!!
        database.addLink(idFrom, idTo1)
        database.addLink(idFrom, idTo2)
        assertEquals(2, database.getLinksFromCnt(idFrom))
    }

    @Test
    fun getImagesCntTest() {
        val database = WikiPagesDB()
        val id = database.addUrl("333.com", DateTime.now())!!
        val image = ImageIO.read(URL("https://upload.wikimedia.org/wikipedia/commons/3/3f/Sundogs_-_New_Ulm-Edit1.JPG"))
        val byteOutputStream = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", byteOutputStream)
        database.addImage(ExposedBlob(byteOutputStream.toByteArray()), id)
        assertEquals(1, database.getImagesCnt(id))
    }

    @Test
    fun getArticleLenTest() {
        val database = WikiPagesDB()
        val id = database.addUrl("aaa.com", DateTime.now())!!
        database.addArticle("aaa", "aa", id)
        assertEquals(5, database.getArticleLen(id))
    }

    @Test
    fun getArticleContentTest() {
        val database = WikiPagesDB()
        val id = database.addUrl("aaa.com", DateTime.now())!!
        database.addArticle("abcd", "_", id)
        assertEquals("abcd", database.getArticleContent(id))
    }

    @Test
    fun getHeadingsListTest() {
        val database = WikiPagesDB()
        val id1 = database.addUrl("111.com", DateTime.now())!!
        val id2 = database.addUrl("222.com", DateTime.now())!!
        database.addArticle("aaa", "111", id1)
        database.addArticle("bbb", "222", id2)
        assertEquals(listOf(Pair(id1, "111"), Pair(id2, "222")), database.getHeadingsList().sortedBy { it.second })
    }
}
