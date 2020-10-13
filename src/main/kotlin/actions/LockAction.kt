package actions

import FileOp
import Registration
import Users
import Utility
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import org.jivesoftware.smack.packet.Message
import smack.Smack
import smack.Subjects
import windows.DialogBox


class LockAction : AnAction() {
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
                        LockStatus.lockTaken(project, "you")
                        sendLockStatus(smack, true, users)
                        ActionEnabler.enableReleaseLock(e)
                        false
                    }
                }
                "Release Lock" -> {
                    LockStatus.lockOpen(project)
                    sendLockStatus(smack, false, users)
                    ActionEnabler.enableTakeLock(e)
                    true
                }
                else -> true
            }
            registration.isReadOnly = isReadOnly
            smack.loggedInUser?.hasTakenLock = !isReadOnly
            registration.changeEditor(editor!!)
            val file = FileEditorManager.getInstance(project).selectedEditor?.file
            registration.fileOps.sendFileChange(file!!, editor.document.text, FileOp.Modification, isReadOnly)
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
        fun changeCollaboratorLockStatus(project: Project, user: String, message: String) {
            val lockTaken = message.toBoolean()
            val users = Users.get(project)!!
            val lockTakenByUser = users.getUser(user)!!
            lockTakenByUser.hasTakenLock = lockTaken
            if (lockTaken)
                LockStatus.lockTaken(project, lockTakenByUser.name)
            else
                LockStatus.lockOpen(project)

        }


    }
}
