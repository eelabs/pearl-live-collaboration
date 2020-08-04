package actions

import Registration
import Utility
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditorManager

class SyncFileAction : AnAction("SyncFile", "Sync files with collaborators", null) {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = FileEditorManager.getInstance(e.project!!).selectedTextEditor
        editor?.document?.isWritable?.let {
            val file = Utility.getFile(editor)
            Registration.get(e.project!!)?.fileOps?.sendFileForSync(file, Registration.get(e.project!!)!!.isReadOnly)
        }
    }

    override fun update(e: AnActionEvent) {
        ActionEnabler.changeVisibility(e)
    }

    companion object {
        fun register() {
            val matchingActions = ActionManager.getInstance().getActionIds("Sync")
            if (!matchingActions.contains("Sync")) {
                val fileSyncAction = SyncFileAction()
                ActionManager.getInstance().registerAction("Sync", fileSyncAction)
                val group = ActionManager.getInstance().getAction("EditorPopupMenu") as DefaultActionGroup
                group.add(fileSyncAction, Constraints(Anchor.AFTER, "Lock"))
            }
        }
    }
}