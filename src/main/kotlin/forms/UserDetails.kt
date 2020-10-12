import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import smack.Smack
import windows.CollaboratorWindowFactory
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextField

class UserDetails(project: Project) {

    var windowContent: JPanel? = null
    var username: JTextField? = null
    var password: JPasswordField? = null
    var domain: JTextField? = null
    var login: JButton? = null

    fun getContent(): JPanel? {
        return windowContent
    }

    init {
        Utility.suppressException {
            val inputStream = this::class.java.getResourceAsStream("local.properties")
            if (inputStream != null) {
                val properties = Properties()
                properties.load(inputStream)
                username?.text = properties["userName"] as String
                password?.text = properties["password"] as String
                domain?.text = properties["domain"] as String
            }
        }

        login!!.addActionListener { _: ActionEvent? ->
            val smack = Smack()
            Smack.set(project, smack)
            smack.connect(username?.text!!, String(password?.password!!), domain?.text!!)
            val pearlWindow = ToolWindowManager.getInstance(project).getToolWindow("Pearl")
            pearlWindow?.remove()
            val toolWindowManager = ToolWindowManager.getInstance(project)
            toolWindowManager.registerToolWindow(RegisterToolWindowTask("Collaborators", ToolWindowAnchor.RIGHT,
                    contentFactory = CollaboratorWindowFactory(smack)))
        }
    }

}