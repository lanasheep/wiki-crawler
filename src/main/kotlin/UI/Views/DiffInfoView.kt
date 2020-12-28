import javafx.beans.property.SimpleStringProperty
import tornadofx.*
import kotlin.math.abs

class DiffInfoView: View() {
    val controller: CrawlingController by inject()

    override val root = form {
        textflow {
            val changedPages = controller.getChangedPages()
            if (changedPages.isEmpty()) {
                text("No changes were found among the viewed pages\n")
                return@textflow
            }
            text("The following changes were found:\n")
            for (page in changedPages) {
                text("On the page ${page.heading} (${page.url}):\n")
                val cntPagesDiff = page.diff.cntPagesDiff
                val cntImagesDiff = page.diff.cntTmagesDiff
                val articleLenDiff = page.diff.articleLenDiff
                val firstContentMismatch = page.diff.firstContentMismatch
                if (cntPagesDiff != 0) {
                    if (cntPagesDiff > 0) {
                        text("The number of links to other wiki pages increased by ${abs(cntPagesDiff)}\n")
                    }
                    else {
                        text("The number of links to other wiki pages decreased by ${abs(cntPagesDiff)}\n")
                    }
                }
                if (cntImagesDiff != 0) {
                    if (cntPagesDiff > 0) {
                        text("The number of images increased by ${abs(cntImagesDiff)}\n")
                    }
                    else {
                        text("The number of images decreased by ${abs(cntImagesDiff)}\n")
                    }
                }
                if (articleLenDiff != 0) {
                    if (cntPagesDiff > 0) {
                        text("Article length increased by ${abs(articleLenDiff)} characters\n")
                    }
                    else {
                        text("Article length decreased by ${abs(articleLenDiff)} characters\n")
                    }
                }
                if (firstContentMismatch != 0) {
                    text("The content of the articles does not match starting from the $firstContentMismatch character\n")
                }
            }
        }
        button("Home") {
            action {
                replaceWith<HomeView>()
            }
        }
    }
}
