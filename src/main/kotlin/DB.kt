import mu.KotlinLogging
import java.io.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class DB {
    private val logger = KotlinLogging.logger {}

    object Urls : Table() {
        val id = integer("id").autoIncrement()
        val url = varchar("url", 1000000)
        override val primaryKey = PrimaryKey(id)
    }

    object Links : Table() {
        val id = integer("id").autoIncrement()
        val idFrom = (integer("idUrlFrom") references Urls.id)
        val idTo = (integer("idUrlTo") references Urls.id)
        override val primaryKey = PrimaryKey(id)
    }

    object Images : Table() {
        val id = integer("id").autoIncrement()
        val content = blob("content")
        val idFrom = (integer("idUrl") references Urls.id)
        override val primaryKey = PrimaryKey(id)
    }

    object Articles : Table() {
        val id = integer("id").autoIncrement()
        val content = blob("content")
        val idFrom = (integer("idUrl") references Urls.id)
        override val primaryKey = PrimaryKey(id)
    }

    init {
        Database.connect("jdbc:h2:mem:testdb;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Urls, Links, Images, Articles)
        }
    }

    fun addUrl(url: String): Int? {
        var id: Int? = null
        try {
            var exist = false
            Database.connect("jdbc:h2:mem:testdb;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
            transaction {
                val select = Urls.select { Urls.url eq url }
                if (select.count() > 0) {
                    exist = true
                }
            }
            if (exist) {
                return null
            }
            transaction {
                id = Urls.insert {
                    it[Urls.url] = url
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
            Database.connect("jdbc:h2:mem:testdb;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
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
            Database.connect("jdbc:h2:mem:testdb;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
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
            Database.connect("jdbc:h2:mem:testdb;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
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
}