import mu.KotlinLogging
import java.io.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.jodatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import tornadofx.timeline

class DB {
    private val logger = KotlinLogging.logger {}

    object Urls : Table() {
        val id = integer("id").autoIncrement()
        val url = varchar("url", 1000000)
        val timeLastView = datetime("timeLastView")
        override val primaryKey = PrimaryKey(id)
    }

    object Links : Table() {
        val id = integer("id").autoIncrement()
        val idFrom = integer("idUrlFrom") references Urls.id
        val idTo = integer("idUrlTo") references Urls.id
        override val primaryKey = PrimaryKey(id)
    }

    object Images : Table() {
        val id = integer("id").autoIncrement()
        val content = blob("content")
        val idFrom = integer("idUrl") references Urls.id
        override val primaryKey = PrimaryKey(id)
    }

    object Articles : Table() {
        val id = integer("id").autoIncrement()
        val content = blob("content")
        val idFrom = integer("idUrl") references Urls.id
        override val primaryKey = PrimaryKey(id)
    }

    init {
        Database.connect("jdbc:h2:./test;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Urls, Links, Images, Articles)
        }
    }

    fun addUrl(url: String, timeStart: DateTime): Int? {
        var id = getUrlId(url)
        if (id != null) {
            var timeLastView: DateTime? = null
            transaction {
                timeLastView = Urls.select { Urls.id eq id!!.toInt() }.single()[Urls.timeLastView]
            }
            if (timeStart < timeLastView) {
                return null
            }
            else {
                return id
            }
        }
        try {
            transaction {
                id = Urls.insert {
                    it[Urls.url] = url
                    it[Urls.timeLastView] = timeStart
                } get Urls.id
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return id
    }

    fun addLink(idFrom: Int, idTo: Int) {
        try {
            transaction {
                Links.insert {
                    it[Links.idFrom] = idFrom
                    it[Links.idTo] = idTo
                }
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
    }

    fun addImage(image: ExposedBlob, idFrom: Int) {
        try {
            transaction {
                Images.insert {
                    it[Images.content] = image
                    it[Images.idFrom] = idFrom
                }
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
    }

    fun addArticle(article: String, idFrom: Int) {
        try {
            transaction {
                Articles.insert {
                    it[Articles.content] = ExposedBlob(article.toByteArray())
                    it[Articles.idFrom] = idFrom
                }
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
    }

    fun getUrlId(url: String): Int? {
        var id: Int? = null
        try {
            transaction {
                val select = Urls.select { Urls.url eq url }
                if (select.count() > 0) {
                    id = select.single()[Urls.id]
                }
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return id
    }

    fun getLinksCnt(id: Int?): Int {
        if (id == null) {
            return 0
        }
        var cnt: Int = 0
        try {
            transaction {
                cnt = Links.select { Links.idTo eq id }.count().toInt()
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return cnt
    }

    fun updTimeLastView(id: Int?) {
        if (id == null) {
            return
        }
        transaction {
            Urls.update({ Urls.id eq id }) { it[Urls.timeLastView] = DateTime.now() }
        }
    }
}