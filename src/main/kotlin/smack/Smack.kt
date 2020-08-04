package smack

import ConnectedUser
import Utility
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jivesoftware.smack.ReconnectionManager
import org.jivesoftware.smack.StanzaListener
import org.jivesoftware.smack.chat2.ChatManager
import org.jivesoftware.smack.filter.StanzaFilter
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.packet.Stanza
import org.jivesoftware.smack.tcp.XMPPTCPConnection
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration
import java.util.concurrent.ConcurrentHashMap


class Smack : Disposable {
    private var chatManagerInstance: ChatManager? = null
    private var connection: XMPPTCPConnection? = null
    private var stanzaListener: StanzaListener? = null
    var loggedInUser: ConnectedUser? = null
        private set
    val chatManager: ChatManager
        get() = chatManagerInstance!!

    fun connect(userName: String, password: String, domain: String) {
        val configBuilder = XMPPTCPConnectionConfiguration.builder()
        configBuilder.setUsernameAndPassword(userName, password)
        configBuilder.setXmppDomain(domain)
        configBuilder.setConnectTimeout(60000)
        val connection = XMPPTCPConnection(configBuilder.build())
        connection.replyTimeout = 30000
        connection.setUseStreamManagement(true)
        connection.setPreferredResumptionTime(1)
        connection.connect();
        connection.login()
        connection.let {
            if (it.isConnected) {
                this.connection = connection
                chatManagerInstance = ChatManager.getInstanceFor(connection)
                loggedInUser = ConnectedUser("$userName@$domain", false)
                ReconnectionManager.getInstanceFor(connection).enableAutomaticReconnection()
            }
        }
    }

    fun listen(func: (Stanza) -> Unit) {
        if (stanzaListener != null) {
            connection?.removeSyncStanzaListener(stanzaListener)
        }
        stanzaListener = StanzaListener(func)
        this.connection?.addSyncStanzaListener(stanzaListener, StanzaFilter {
            if (it is Message) {
                System.currentTimeMillis() - Utility.getMessageTimeStamp(it) < 60000
            } else
                false
        })
    }

    override fun dispose() {
        connection?.disconnect()
        connection?.removeSyncStanzaListener(stanzaListener)
    }

    companion object {
        private var projectMap = ConcurrentHashMap<Project, Smack>()
        fun set(project: Project, smack: Smack) {
            projectMap[project] = smack
        }

        fun get(project: Project): Smack? {
            return projectMap[project]
        }
    }


}

