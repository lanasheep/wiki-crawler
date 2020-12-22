import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class SetCrawlingParamsView: View() {
    val controller: CrawlingController by inject()

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
                replaceWith<DiffInfoView>()
            }
        }
    }
}