import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class PageInfoView: View() {
    val controller: PageQueryController by inject()

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