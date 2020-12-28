import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class SelectPageView: View() {
    val controller: PageQueryController by inject()

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
                replaceWith<PageInfoView>()
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
                    replaceWith<PageInfoView>()
                }
            }
        }
    }
}