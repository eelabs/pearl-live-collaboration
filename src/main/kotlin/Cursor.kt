import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project
import org.jivesoftware.smack.packet.Message
import smack.Smack
import smack.Subjects

class Cursor(private val editor: Editor?, val project: Project, private val smack: Smack, private val users: Users) : Disposable {

    fun showCursor(line: Int, column: Int, user: User) {
        val location = VisualPosition(line, column);
        Utility.invokeLater {
            editor?.let {
                it.caretModel.addCaret(location, false)
                        ?.let { caret ->
                            try {
                                user.cursor?.let {
                                    Utility.suppressException {
                                        Utility.invokeLater {
                                            removeSelection(user)
                                            if (editor.markupModel.allHighlighters.contains(it))
                                                editor.markupModel.removeHighlighter(it)
                                        }

                                    }

                                }
                            } finally {
                                user.cursor = Highlighter.highlight(caret, user.color)
                                it.caretModel.removeCaret(caret)
                            }

                        }
            }
        }
    }

    private fun removeSelection(user: User) {
        Selection.removeSelection(editor, users)
        val lowerEnd = if (compareValues(user.selection?.endOffset, user.selection?.startOffset) < 0) user.selection?.endOffset else user.selection?.startOffset
        val higherEnd = if (compareValues(user.selection?.endOffset, user.selection?.startOffset) > 0) user.selection?.endOffset else user.selection?.startOffset


        if (compareValues(user.cursor?.startOffset, lowerEnd) < 0 || compareValues(user.cursor?.startOffset, higherEnd) > 0) {
            if (editor!!.markupModel.allHighlighters.contains(user.selection))
                editor.markupModel.removeHighlighter(user.selection!!)
        }
    }

    private var caretListener: CaretListener? = null
    fun register() {
        deregister()
        caretListener = object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                val message = Message()
                message.type = Message.Type.chat
                val line = event.caret?.visualPosition?.line
                val column = event.caret?.visualPosition?.column
                message.body = line!!.toString() + "," + column!!.toString()
                message.subject = Subjects.caretChange
                Utility.sendToAllUsers(smack, message, users)

            }
        }
        editor?.caretModel?.addCaretListener(caretListener!!, project)
    }

    override fun dispose() {
        deregister()
    }

    private fun deregister() {
        caretListener?.let {
            editor?.caretModel?.removeCaretListener(it)
            caretListener = null
        }
    }
}