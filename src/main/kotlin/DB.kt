import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class DB {
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
        Database.connect("jdbc:h2:mem:test;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(Urls, Links, Images, Articles)
        }
    }

    fun addLink(idFrom: Int, idTo: Int) {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
        transaction {
            Links.insert {
                it[Links.idFrom] = idFrom
                it[Links.idTo] = idTo
            }
        }
    }

    fun addUrl(url: String): Int? {
        var exist = false
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
        transaction {
            val select = Urls.select { Urls.url eq url }
            if (select.count() > 0) {
                exist = true
            }
        }
        if (exist) {
            return null
        }
        var id: Int? = null
        transaction {
            id = Urls.insert {
                it[Urls.url] = url
            } get Urls.id
        }
        return id
    }

    fun addImage(image: ExposedBlob, idFrom: Int) {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
        transaction {
            Images.insert {
                it[Images.content] = image
                it[Images.idFrom] = idFrom
            }
        }
    }

    fun addArticle(article: String, idFrom: Int) {
        Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")
        transaction {
            Articles.insert {
                it[Articles.content] = ExposedBlob(article.toByteArray())
                it[Articles.idFrom] = idFrom
            }
        }
    }
}