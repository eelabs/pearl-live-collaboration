import actions.LockAction
import actions.LockStatus
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jivesoftware.smack.packet.Message
import smack.Smack
import smack.Subjects
import java.util.concurrent.ConcurrentHashMap


class Registration constructor(private val project: Project, private var smack: Smack, private val users: Users,
                               private val presence: Presence) : Disposable {
    private var editor = FileEditorManager.getInstance(project).selectedTextEditor
    var isReadOnly: Boolean = true
    private var document = Document(project, editor, smack, users, isReadOnly)
    private var invitation = Invitation(project, smack, users)
    private var cursor = Cursor(editor, project, smack, users)
    private var selection = Selection(editor, project, smack, users)
    var fileOps = FileOps(project, editor, smack, users)
        private set
    private val messageBusConnection = project.messageBus.connect()


    fun changeEditor(editor: Editor?) {
        deregister()
        this.editor = editor
        editor?.document?.setReadOnly(isReadOnly)
        this.document = Document(project, editor, smack, users, isReadOnly)
        this.cursor = Cursor(editor, project, smack, users)
        this.selection = Selection(editor, project, smack, users)
        register()
    }

    override fun dispose() {
        LockStatus.dispose(project)
        editor?.markupModel?.removeAllHighlighters()
        presence.sendDisconnectedMessage(users)
        this.document.dispose()
        this.selection.dispose()
        this.cursor.dispose()
        messageBusConnection.dispose()
        smack.dispose()
    }

    private fun deregister() {
        this.document.deregister()
        this.selection.dispose()
        this.cursor.dispose()
    }

    init {
        editor?.document?.setReadOnly(true)
        fileOps.registerFileSync(messageBusConnection)
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                super.fileOpened(source, file)
                changeEditor(source.selectedTextEditor)
            }

            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                source.selectedTextEditor?.document?.setReadOnly(false)
            }

            override fun selectionChanged(event: FileEditorManagerEvent) {
                super.selectionChanged(event)
                changeEditor(event.manager.selectedTextEditor)
                fileOps.sendFileChange(event.newFile!!, event.manager.selectedTextEditor?.document?.text!!, FileOp.Modification, isReadOnly)

            }
        })
        register()
    }

    private fun register() {
        cursor.register()
        selection.register()
        document.register()
    }


    fun listen() {
        smack.listen {
            val packet = it as Message?
            val from = it.from.toString().split("/")[0]
            val message = packet?.body!!
            if (message.isNotEmpty()) {
                when (packet.subject) {
                    Subjects.caretChange -> {
                        val user = users.getUser(from)
                        val (line, column) = parse(message)
                        cursor.showCursor(line, column, user!!)
                    }
                    Subjects.selection_change -> {
                        val user = users.getUser(from)
                        val (line, column) = parse(message)
                        selection.show(line, column, user!!)
                    }
                    Subjects.collaboration_invitation -> {
                        invitation.acceptInvitation(from, message)
                    }
                    Subjects.document_text_change -> {
                        document.modifyDocument(message)
                    }
                    Subjects.sync_file -> {
                        fileOps.syncFile(editor?.document!!, message)
                    }
                    Subjects.user_availability -> {
                        presence.sendPresence(from)
                    }
                    Subjects.availability_status -> {
                        invitation.sendInvite(from)
                    }
                    Subjects.invitation_acknowledgment -> {
                        users.addUser(from, false)
                    }
                    Subjects.file_change -> {
                        fileOps.acceptFileChange(message)
                    }
                    Subjects.lock_status -> {
                        LockAction.changeCollaboratorLockStatus(project, from, message)
                    }
                    Subjects.user_status -> {
                        Users.get(project)?.removeUser(from)
                        Utility.invokeLater {
                            Selection.removeSelection(editor, Users.get(project)!!)
                        }
                    }
                    Subjects.copy_or_move_file -> {
                        fileOps.copyOrMoveFile(message)
                    }
                    Subjects.file_property_change -> {
                        fileOps.makePropertyChange(message)
                    }
                    Subjects.new_collaborator_joined -> {
                        invitation.addConnectedUser(message)
                    }

                }
            }
        }
    }


    private fun parse(message: String): Pair<Int, Int> {
        val line = message.split(",")[0].toInt()
        val column = message.split(",")[1].toInt()
        return Pair(line, column)
    }

    companion object {
        private var projectMap = ConcurrentHashMap<Project, Registration>()
        fun set(project: Project, registration: Registration) {
            projectMap[project] = registration
        }

        fun get(project: Project): Registration? {
            return projectMap[project]
        }
    }

}