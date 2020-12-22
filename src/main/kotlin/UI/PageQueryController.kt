import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class PageQueryController: Controller() {
    private val database = WikiPagesDB()
    private var idQuery: Int? = null

    fun setIdQuery(url: String) {
        this.idQuery = database.getUrlId(url)
    }

    fun setIdQuery(id: Int) {
        this.idQuery = id
    }

    fun getHeadingsList(): List<Pair<Int, String>> {
        return database.getHeadingsList()
    }

    fun pageQueryViewed(): Boolean {
        return (idQuery != null && database.getTimeLastView(idQuery) != null)
    }

    fun pageQueryChanged(): Boolean {
        return (idQuery != null && database.getTimeLastChange(idQuery) != null)
    }

    fun getTimeLastView() = database.getTimeLastView(idQuery)
    fun getTimeLastChange() = database.getTimeLastChange(idQuery)
    fun getLinksFromCnt() = database.getLinksFromCnt(idQuery)
    fun getImagesCnt() = database.getImagesCnt(idQuery)
    fun getArticleLen() = database.getArticleLen(idQuery)
}