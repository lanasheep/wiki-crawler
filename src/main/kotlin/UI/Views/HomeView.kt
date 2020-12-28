import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import tornadofx.*

class HomeView: View() {
    override val root = vbox(alignment = Pos.CENTER) {
        prefWidth = 400.0
        prefHeight = 400.0
        button("Start crawling") {
            prefWidth = 150.0
            action {
                replaceWith<SetCrawlingParamsView>()
            }
        }

        button("Get information") {
            prefWidth = 150.0
            action {
                replaceWith<SelectPageView>()
            }
        }
    }
}