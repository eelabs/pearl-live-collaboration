package windows

import Cleanup
import actions.LockAction
import Presence
import Registration
import smack.Smack
import actions.SyncFileAction
import Users
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory


class CollaboratorWindowFactory(private val smack: Smack) : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val presence = Presence(smack)
        val collaboratorWindow = Users(presence, smack, project)
        Users.set(project, collaboratorWindow)
        val contentFactory = ContentFactory.SERVICE.getInstance();
        val content = contentFactory.createContent(collaboratorWindow.getContent(), "", true);
        toolWindow.contentManager.addContent(content);
        toolWindow.isAvailable = true
        toolWindow.isAutoHide = false
        val toolWindowEx = toolWindow as ToolWindowEx
        toolWindowEx.stretchHeight(300 - toolWindowEx.decorator.height);
        toolWindowEx.stretchWidth(300 - toolWindowEx.decorator.width);
        toolWindow.show()
        toolWindow.setIcon(AllIcons.General.User)
        toolWindow.isAutoHide = false
        registerActions(project, collaboratorWindow, presence, content)

    }

    private fun registerActions(project: Project, collaboratorWindow: Users, presence: Presence, content: Content) {
        val registration = Registration(project, smack, collaboratorWindow, presence)
        registration.listen()
        Registration.set(project, registration)
        LockAction.register()
        SyncFileAction.register()
        Disposer.register(content, Cleanup(project))
    }

}
