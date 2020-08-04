package windows

import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.Dimension
import javax.annotation.Nullable
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel


class DialogBox(private val message: String, buttonText: String, cancelButtonText: String = "Cancel", private val title: String = "Collaboration request") : DialogWrapper(true) {

    @Nullable
    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel(BorderLayout())
        val label = JLabel(message)
        label.preferredSize = Dimension(100, 100)
        dialogPanel.add(label, BorderLayout.CENTER)
        return dialogPanel
    }

    override fun getTitle(): String {
        return title
    }

    init {
        init()
        setOKButtonText(buttonText)
        setCancelButtonText(cancelButtonText)
    }
}