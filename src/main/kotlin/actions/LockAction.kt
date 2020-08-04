package actions

import windows.DialogBox
import FileOp
import Registration
import smack.Smack
import Users
import Utility
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import org.jivesoftware.smack.packet.Message
import smack.Subjects

class LockAction : AnAction("Take Lock", "Get or Release lock for editing", null) {
    override fun update(e: AnActionEvent) {
        ActionEnabler.changeVisibility(e)
        ActionEnabler.changeActionText(e)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        val editor = FileEditorManager.getInstance(e.project!!).selectedTextEditor
        val registration = Registration.get(project)
        val users = Users.get(project)!!
        val smack = Smack.get(project)!!
        registration?.let {
            val isReadOnly = when (e.presentation.text) {
                "Take Lock" -> {
                    val driver = users.getUsers().find { it.hasTakenLock }
                    if (driver != null) {
                        DialogBox("User ${driver.name} has taken lock", "Ok", title = "Lock Status").show()
                        true
                    } else {
                        sendLockStatus(smack, true, users)
                        this.templatePresentation.text = "Release Lock"
                        false
                    }
                }
                "Release Lock" -> {
                    sendLockStatus(smack, false, users)
                    this.templatePresentation.text = "Take Lock"
                    true
                }
                else -> true
            }
            registration.isReadOnly = isReadOnly
            smack.loggedInUser?.hasTakenLock = !isReadOnly
            registration.changeEditor(editor!!)
            val file = FileEditorManager.getInstance(project).selectedEditor?.file
            registration.fileOps.sendFileChange(file!!, editor.document.text, FileOp.Modification,isReadOnly)
        }
    }

    private fun sendLockStatus(smack: Smack, lockTaken: Boolean, users: Users) {
        val message = Message()
        message.type = Message.Type.chat
        message.subject = Subjects.lock_status
        message.body = lockTaken.toString()
        Utility.sendToAllUsers(smack, message, users)
    }


    companion object {
        fun register() {
            val matchingActions = ActionManager.getInstance().getActionIds("Lock")
            if (!matchingActions.contains("Lock")) {
                val lockAction = LockAction()
                ActionManager.getInstance().registerAction("Lock", lockAction)
                val group = ActionManager.getInstance().getAction("EditorPopupMenu") as DefaultActionGroup
                group.add(lockAction, Constraints(Anchor.FIRST, null))
            }

        }

        fun changeCollaboratorLockStatus(project: Project, user: String, message: String) {
            val lockTaken = message.toBoolean()
            val users = Users.get(project)!!
            users.getUser(user)!!.hasTakenLock = lockTaken

        }


    }
}
