import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.project.Project
import org.jivesoftware.smack.packet.Message
import smack.Smack
import smack.Subjects

class Selection(val editor: Editor?, val project: Project, private val smack: Smack, private val users: Users) : Disposable {
    fun show(startOffset: Int, endOffset: Int, user: User) {
        Utility.invokeLater {
            editor?.let {
                Utility.suppressException {
                    Utility.invokeLater {
                        try {
                            user.selection?.let {
                                removeSelection(editor, users)
                                if (editor.markupModel.allHighlighters.contains(it))
                                    editor.markupModel.removeHighlighter(it)
                            }

                        } finally {
                            user.selection = Highlighter.highlightSelection(editor.markupModel, user.color, startOffset, endOffset)
                        }

                    }
                }
            }

        }
    }

    private var selectionListener: SelectionListener? = null
    fun register() {
        deregister()
        selectionListener = object : SelectionListener {
            override fun selectionChanged(event: SelectionEvent) {
                val start = editor?.selectionModel?.selectionStartPosition
                val end = editor?.selectionModel?.selectionEndPosition
                if (start != end) {
                    val message = Message()
                    message.type = Message.Type.chat
                    message.body = event.newRange.startOffset.toString() + "," + event.newRange.endOffset
                    message.subject = Subjects.selection_change
                    Utility.sendToAllUsers(smack, message, users)

                }
            }
        }
        editor?.selectionModel?.addSelectionListener(selectionListener!!)
    }

    private fun deregister() {
        selectionListener?.let {
            editor?.selectionModel?.removeSelectionListener(it)
            selectionListener = null
        }
    }

    override fun dispose() {
        deregister()
    }

    companion object {
        fun removeSelection(editor: Editor?, users: Users) {
            val userSelections = users.getUsers().flatMap { listOf(it.selection, it.cursor) }
            val highlighterToBeDeleted = editor?.markupModel?.allHighlighters?.filter { !userSelections.contains(it) }
            highlighterToBeDeleted?.forEach {
                editor.markupModel.removeHighlighter(it)
            }
        }
    }
}
