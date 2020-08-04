package windows

import UserDetails
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.content.ContentFactory

class PearlWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val userDetailsWindow = UserDetails(project)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(userDetailsWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
        toolWindow.setIcon(AllIcons.General.Modified)
        val toolWindowEx = toolWindow as ToolWindowEx
        toolWindowEx.stretchHeight(300 - toolWindowEx.decorator.height);
        toolWindowEx.stretchWidth(300 - toolWindowEx.decorator.width);
        toolWindow.isAutoHide = true
    }


}
