import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class HomeView: View() {
    override val root = form {
        button("Start crawling") {
            action {
                replaceWith<SetCrawlingParamsView>()
            }
        }

        button("Get information") {
            action {
                replaceWith<SelectPageView>()
            }
        }
    }
}