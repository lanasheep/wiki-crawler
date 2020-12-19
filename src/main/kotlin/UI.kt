import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class WikiCrawlerApp: App(HomeView::class)

class HomeView: View() {
    override val root = form {
        button("Start crawling") {
            action {
                replaceWith<StartView>()
            }
        }

        button("Get information") {
            action {
                replaceWith<SelectView>()
            }
        }
    }
}

class StartView: View() {
    val controller: StartController by inject()

    override val root = form {
        fieldset {
            val input = SimpleStringProperty()
            field("URL") {
                textfield(input)
            }

            button("Add") {
                action {
                    controller.add(input.value)
                }
            }
        }

        fieldset {
            val input = SimpleStringProperty()
            field("Concurrency level") {
                textfield(input)
            }

            button("OK") {
                action {
                    controller.setConcurrencyLevel(input.value.toInt())
                }
            }
        }

        fieldset {
            val input = SimpleStringProperty()
            field("Мaximum number of viewed pages\n") {
                textfield(input)
            }

            button("OK") {
                action {
                    controller.setCntPagesMax(input.value.toInt())
                }
            }
        }

        fieldset {
            val input = SimpleStringProperty()
            field("Path to storage folder") {
                textfield(input)
            }

            button("OK") {
                action {
                    controller.setPath(input.value)
                }
            }
        }

        button("Go!") {
            action {
                controller.go()
                replaceWith<ChangeView>()
            }
        }
    }
}

class SelectView: View() {
    val controller: SelectController by inject()

    override val root = form {
        text("Select a page from the list\n")
        listview(controller.getHeadingsList().asObservable()) {
            cellFormat {
                text = it.second
            }
            onUserSelect(1) {
                controller.setIdQuery(it.first)
            }
        }
        button("Get info") {
            action {
                replaceWith<InfoView>()
            }
        }

        fieldset {
            val input = SimpleStringProperty()
            field("Or enter URL") {
                textfield(input)
            }

            button("Get info") {
                action {
                    controller.setIdQuery(input.value)
                    replaceWith<InfoView>()
                }
            }
        }
    }
}

class InfoView: View() {
    val controller: SelectController by inject()

    override val root = form {
        textflow {
            if (controller.pageQueryViewed()) {
                text("This page was last viewed on ${controller.getTimeLastView()}\n")
                text("Links to other pages: ${controller.getLinksFromCnt()}\n")
                text("Images: ${controller.getImagesCnt()}\n")
                text("Text (in characters): ${controller.getArticleLen()}\n")
                if (controller.pageQueryChanged()) {
                    text("This page was last changed on ${controller.getTimeLastChange()}")
                }

            }
            else {
                text("Sorry, this page hasn't been viewed yet\n")
            }
        }
        button("Home") {
            action {
                replaceWith<HomeView>()
            }
        }
    }
}

class ChangeView: View() {
    val controller: StartController by inject()

    override val root = form {
        textflow {
            val changedPages = controller.getChangedPages()
            if (!changedPages.isEmpty()) {
                text("Сhanges were found\n")
            }
            else {
                text("No changes were found among the viewed pages\n")
            }
        }
        button("Home") {
            action {
                replaceWith<HomeView>()
            }
        }
    }
}

class StartController: Controller() {
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

class SelectController: Controller() {
    private val database = DB()
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