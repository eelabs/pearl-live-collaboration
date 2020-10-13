package actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import java.util.concurrent.ConcurrentHashMap

object LockStatus {
    private var balloonMap: ConcurrentHashMap<Project, Balloon> = ConcurrentHashMap()

    fun lockTaken(project: Project, user: String) {
        createBalloon(project, "Lock taken by $user")
    }

    fun lockOpen(project: Project) {
        createBalloon(project, "Nobody has taken lock");
    }

    private fun createBalloon(project: Project, text: String) {
        dispose(project)
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(text, MessageType.INFO, null)
                .setHideOnAction(false)
                .setHideOnClickOutside(false)
                .createBalloon()

        balloon.show(RelativePoint.getNorthEastOf(statusBar.component), Balloon.Position.atRight)
        balloonMap[project] = balloon
    }

    fun dispose(project: Project) {
        balloonMap[project]?.dispose()
    }

}