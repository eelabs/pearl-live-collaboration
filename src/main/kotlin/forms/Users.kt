import Utility.toEntityBear
import actions.LockStatus
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.RegisterToolWindowTask
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import org.jxmpp.jid.Jid
import smack.Smack
import windows.DialogBox
import windows.PearlWindowFactory
import java.awt.Component
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.concurrent.ConcurrentHashMap
import javax.swing.*


class Users(private val presence: Presence, private val smack: Smack, private val project: Project) {
    private var userList: JList<User>? = null
    private var userContent: JPanel? = null
    private var addUserText: JTextField? = null
    private var addUserButton: JButton? = null
    private var close: JButton? = null
    private val userListModel = DefaultListModel<User>()

    init {
        userList?.model = userListModel
        userList?.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                val user = value as User
                foreground = user.color
                if (user.hasTakenLock)
                    icon = AllIcons.Diff.Lock
                text = user.name
                return component
            }
        }
        addUserButton?.addActionListener {
            checkIfUserOnline()
        }
        addUserText?.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                super.keyPressed(e)
                if (e.keyCode == 10 && !addUserText?.text.isNullOrBlank()) {
                    checkIfUserOnline()
                }
            }
        })

        close?.addActionListener {
            val dialogBox = DialogBox("Hope you enjoyed pairing.Do you want to end collaboration?",
                    "Yes", "No", "Collaboration")
            if (dialogBox.showAndGet()) {
                ToolWindowManager.getInstance(project).getToolWindow("Collaborators")?.remove()
                val toolWindowManager = ToolWindowManager.getInstance(project)
                toolWindowManager.registerToolWindow(RegisterToolWindowTask("Pearl", ToolWindowAnchor.RIGHT, contentFactory = PearlWindowFactory()))
            }
        }


    }

    private fun checkIfUserOnline() {
        if (compareValues(addUserText?.text?.toLowerCase(), smack.loggedInUser?.name?.toLowerCase()) != 0) {
            presence.checkPresence(addUserText?.text!!)
        }
        addUserText?.text = ""
    }

    fun getContent(): JPanel? {
        return userContent
    }

    fun getUsers(): List<User> {
        return userListModel.elements()?.toList()!!
    }

    fun getUserNames(): List<Jid> {
        return getUsers().map { it.name.toEntityBear() }
    }

    fun getUser(name: String): User? {
        return getUsers().find { it.name == name }
    }

    fun removeUser(name: String) {
        userListModel.removeElement(getUser(name))
        changeLockStatusOnUserChange()
    }

    fun addUser(name: String, hasTakenLock: Boolean) {
        if (getUser(name) == null && name.toLowerCase() != smack.loggedInUser?.name?.toLowerCase()) {
            val user = User(name, hasTakenLock)
            user.setColor(userListModel.size())
            userListModel.addElement(user)
        }
        changeLockStatusOnUserChange()
    }

    private fun changeLockStatusOnUserChange() {
        val anyDriver = getUsers().find { it.hasTakenLock }
        if (anyDriver != null) {
            LockStatus.lockTaken(project, anyDriver.name)
        } else {
            LockStatus.lockOpen(project)
        }
    }


    companion object {
        private var projectMap = ConcurrentHashMap<Project, Users>()
        fun set(project: Project, users: Users) {
            projectMap[project] = users
        }

        fun get(project: Project): Users? {
            return projectMap[project]
        }
    }

}



