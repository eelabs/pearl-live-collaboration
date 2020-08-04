import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.refactoring.suggested.newRange
import com.intellij.refactoring.suggested.oldRange
import org.jivesoftware.smack.packet.Message
import smack.Smack
import smack.Subjects


class Document(private val project: Project, private val editor: Editor?, private val smack: Smack,
               private val users: Users, private val isReadOnly: Boolean) : Disposable {
    private var docChange: DocChange? = null
    private var documentListener: DocumentListener? = null
    fun register() {
        documentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (!isReadOnly) {
                    val message = Message()
                    message.type = Message.Type.chat
                    val docChange = DocChange(event.oldRange.startOffset, event.oldRange.endOffset,
                            event.newRange.startOffset, event.newRange.endOffset, event.oldFragment.toString(), event.newFragment.toString())
                    message.body = GsonSerializer.toJson(docChange)
                    message.subject = Subjects.document_text_change
                    Utility.sendToAllUsers(smack, message, users)
                }
            }
        }
        editor?.document?.addDocumentListener(documentListener!!)
    }

    override fun dispose() {
        editor?.document?.setReadOnly(false)
        deregister()
    }

    fun deregister() {
        documentListener?.let {
            editor?.document?.removeDocumentListener(it)
            documentListener = null
        }
    }
    
    fun modifyDocument(message: String) {
        Utility.invokeLater {
            Utility.write(project) {
                val docChange = GsonSerializer.fromJson<DocChange>(message)
                Utility.suppressException {
                    editor?.document?.setReadOnly(false)
                    editor?.document?.replaceString(docChange.oldStart, docChange.oldEnd, docChange.newText)
                    this.docChange = docChange
                    editor?.document?.setReadOnly(isReadOnly)
                }
            }
        }

    }
}







