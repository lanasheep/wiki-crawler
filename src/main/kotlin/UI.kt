import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class WikiCrawlerApp: App(WikiCrawlerView::class)

class WikiCrawlerView: View() {
    val controller: WikiCrawlerController by inject()

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
            }
        }
    }
}


class WikiCrawlerController: Controller() {
    private val urls = mutableListOf<String>()
    private var concurrencyLevel: Int = 1
    private var cntPagesMax: Int = 1
    private var path: String? = null

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

    fun go() {
        val launcher = Launcher(urls, concurrencyLevel, cntPagesMax, path)
        launcher.launch()
    }
}