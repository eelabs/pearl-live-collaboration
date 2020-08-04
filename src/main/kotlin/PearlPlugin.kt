import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import windows.PearlWindowFactory


class Activity : StartupActivity {
    override fun runActivity(project: Project) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        toolWindowManager.registerToolWindow(RegisterToolWindowTask("Pearl", ToolWindowAnchor.RIGHT,
                contentFactory = PearlWindowFactory()))
    }
}

