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
        val url = varchar("url", 1000000).uniqueIndex()
        val timeLastCheck = datetime("timeLastCheck")
        val timeLastView = datetime("timeLastView").nullable()
        val timeLastChange = datetime("timeLastChange").nullable()
        override val primaryKey = PrimaryKey(id)
    }

    object Links : Table() {
        val id = integer("id").autoIncrement()
        val idFrom = integer("idFrom") references Urls.id
        val idTo = integer("idTo") references Urls.id
        override val primaryKey = PrimaryKey(id)
    }

    object Images : Table() {
        val id = integer("id").autoIncrement()
        val content = blob("content")
        val idFrom = integer("idFrom") references Urls.id
        override val primaryKey = PrimaryKey(id)
    }

    object Articles : Table() {
        val id = integer("id").autoIncrement()
        val content = text("content")
        val heading = varchar("heading", 500)
        val idFrom = integer("idFrom") references Urls.id
        override val primaryKey = PrimaryKey(id)
    }

    init {
        Database.connect("jdbc:h2:./www;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Urls, Links, Images, Articles)
        }
    }

    fun addUrl(url: String, timeStart: DateTime): Int? {
        var id = getUrlId(url)
        if (id != null) {
            val timeLastCheck = getTimeLastCheck(id)
            if (timeStart < timeLastCheck) {
                return null
            }
            else {
                updTimeLastCheck(id)
                return id
            }
        }
        try {
            transaction {
                id = Urls.insert {
                    it[Urls.url] = url
                    it[Urls.timeLastCheck] = DateTime.now()
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

    fun addArticle(content: String, heading: String, idFrom: Int) {
        try {
            transaction {
                Articles.insert {
                    it[Articles.content] = content
                    it[Articles.heading] = heading
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

    fun getLinksToCnt(id: Int?): Int {
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

    fun getLinksFromCnt(id: Int?): Int {
        if (id == null) {
            return 0
        }
        var cnt: Int = 0
        try {
            transaction {
                cnt = Links.select { Links.idFrom eq id }.count().toInt()
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return cnt
    }

    fun getImagesCnt(id: Int?): Int {
        if (id == null) {
            return 0
        }
        var cnt: Int = 0
        try {
            transaction {
                cnt = Images.select { Images.idFrom eq id }.count().toInt()
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return cnt
    }

    fun getArticleLen(id: Int?): Int? {
        if (id == null) {
            return null
        }
        var len = 0
        try {
            transaction {
                val content = Articles.select { Articles.idFrom eq id.toInt() }.single()[Articles.content]
                val heading = Articles.select { Articles.idFrom eq id.toInt() }.single()[Articles.heading]
                len = content.count() + heading.count()
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return len
    }
    
    fun getHeadingsList(): List<Pair<Int, String>> {
        var headings = listOf<Pair<Int, String>>()
        try {
            transaction {
                headings = Articles.selectAll().map { Pair(it[Articles.idFrom], it[Articles.heading]) }
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return headings
    }

    fun getTimeLastCheck(id: Int?): DateTime? {
        if (id == null) {
            return null
        }
        var timeLastCheck: DateTime? = null
        try {
            transaction {
                timeLastCheck = Urls.select { Urls.id eq id.toInt() }.single()[Urls.timeLastCheck]
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return timeLastCheck
    }

    fun getTimeLastView(id: Int?): DateTime? {
        if (id == null) {
            return null
        }
        var timeLastView: DateTime? = null
        try {
            transaction {
                timeLastView = Urls.select { Urls.id eq id.toInt() }.single()[Urls.timeLastView]
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return timeLastView
    }

    fun getTimeLastChange(id: Int?): DateTime? {
        if (id == null) {
            return null
        }
        var timeLastChange: DateTime? = null
        try {
            transaction {
                timeLastChange = Urls.select { Urls.id eq id.toInt() }.single()[Urls.timeLastChange]
            }
        }
        catch (e: Exception) {
            val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()
            logger.error("Exception caught: $stacktrace\n")
        }
        return timeLastChange
    }

    fun updTimeLastCheck(id: Int?) {
        if (id == null) {
            return
        }
        transaction {
            Urls.update({ Urls.id eq id }) { it[Urls.timeLastCheck] = DateTime.now() }
        }
    }

    fun updTimeLastView(id: Int?) {
        if (id == null) {
            return
        }
        transaction {
            Urls.update({ Urls.id eq id }) { it[Urls.timeLastView] = DateTime.now() }
        }
    }

    fun updTimeLastChange(id: Int?) {
        if (id == null) {
            return
        }
        transaction {
            Urls.update({ Urls.id eq id }) { it[Urls.timeLastChange] = DateTime.now() }
        }
    }
}