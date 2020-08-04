import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smackx.delay.packet.DelayInformation
import org.jxmpp.jid.EntityBareJid
import org.jxmpp.jid.impl.JidCreate
import smack.Smack
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*


object Utility {
    fun <T> suppressException(func: () -> T): T? {
        try {
            return func()
        } catch (ex: Exception) {
            val sw = StringWriter()
            ex.printStackTrace(PrintWriter(sw))
            val exceptionAsString = sw.toString()
            println(exceptionAsString)

        }
        return null
    }


    fun sendPacket(smack: Smack, packet: Message) {
        suppressException {
            packet.addExtension(DelayInformation(Date()))
            smack.chatManager.chatWith(packet.to as EntityBareJid)?.send(packet)
        }
    }

    fun getMessageTimeStamp(msg: Message): Long {
        val timestamp = msg.getExtension("delay", "urn:xmpp:delay") as DelayInformation?
        return timestamp?.stamp?.time!!
    }

    fun sendToAllUsers(smack: Smack, packet: Message, users: Users) {
        suppressException {
            users.getUserNames().forEach {
                val message = Message(it, packet.type)
                message.body = packet.body
                message.subject = packet.subject
                sendPacket(smack, message)
            }

        }
    }

    fun write(project: Project, func: () -> Unit) {
        suppressException {
            WriteCommandAction.runWriteCommandAction(project) {
                func()
            }
        }
    }


    fun writeText(project: Project, editor: Editor?, message: String) {
        write(project) {
            if (editor?.isDisposed == false) {
                editor.document.setReadOnly(false)
                editor.document.setText(message.replace("\n", System.getProperty("line.separator")))
                editor.document.setReadOnly(true)
            }
        }
    }

    fun invokeLater(func: () -> Unit) {
        ApplicationManager.getApplication().invokeLater {
            suppressException {
                func()
            }
        }
    }


    fun getFilePath(editor: Editor?): String? {
        return if (editor?.isDisposed == false) {
            getFile(editor)?.path
        } else null
    }

    fun getFile(editor: Editor?): VirtualFile? {
        return if (editor?.isDisposed == false) {
            FileDocumentManager.getInstance().getFile(editor.document)
        } else null

    }

    fun String?.relativePath(project: Project): String? {
        return this?.replace(project.basePath.toString(), "")
    }

    fun String.toEntityBear(): EntityBareJid {
        return JidCreate.entityBareFrom(this)
    }
}