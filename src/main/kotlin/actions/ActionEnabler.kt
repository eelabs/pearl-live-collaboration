package actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.wm.ToolWindowManager

object ActionEnabler {
    fun changeVisibility(e: AnActionEvent) {
        e.project?.let {
            if (ToolWindowManager.getInstance(it).getToolWindow("Collaborators") == null) {
                if (e.presentation.isEnabledAndVisible)
                    e.presentation.isEnabledAndVisible = false
            } else
                if (!e.presentation.isEnabledAndVisible)
                    e.presentation.isEnabledAndVisible = true
        }
    }

    fun changeActionText(e: AnActionEvent) {
        e.project?.let {
            val editor = FileEditorManager.getInstance(it).selectedTextEditor
            when (editor?.document?.isWritable) {
                true -> e.presentation.text = "Release Lock"
                false -> e.presentation.text = "Take Lock"
            }
        }
    }
}