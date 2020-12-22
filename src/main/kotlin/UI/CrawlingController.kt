import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class CrawlingController: Controller() {
    private val urls = mutableListOf<String>()
    private var concurrencyLevel: Int = 1
    private var cntPagesMax: Int = 1
    private var path: String? = null
    private var changedPages = listOf<ChangedPage>()

    fun add(url: String) {
        urls += url
    }

    fun setPath(path: String) {
        this.path = path
    }

    fun setConcurrencyLevel(concurrencyLevel: Int) {
        this.concurrencyLevel = concurrencyLevel
    }

    fun setCntPagesMax(cntPagesMax: Int) {
        this.cntPagesMax = cntPagesMax
    }

    fun getChangedPages(): List<ChangedPage> {
        return changedPages
    }

    fun go() {
        if (urls.isEmpty()) {
            return
        }
        val launcher = Launcher(urls, concurrencyLevel, cntPagesMax, path)
        changedPages = launcher.launch()
    }
}