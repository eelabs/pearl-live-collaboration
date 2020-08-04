import Utility.toEntityBear
import com.intellij.openapi.project.Project
import org.jivesoftware.smack.packet.Message
import smack.Smack
import smack.Subjects
import windows.DialogBox


class Invitation(val project: Project, private val smack: Smack,
                 private val users: Users) {
    fun sendInvite(user: String) {
        val message = Message(user.toEntityBear(), Message.Type.chat)
        message.subject = Subjects.collaboration_invitation
        var connectedUsers = users.getUsers().map { ConnectedUser(it.name, it.hasTakenLock) }
        connectedUsers = connectedUsers + smack.loggedInUser!!
        message.body = GsonSerializer.toJson(InviteData(connectedUsers))
        Utility.sendPacket(smack, message)
    }

    fun acceptInvitation(user: String, message: String) {
        Utility.invokeLater {
            val invitationDialog = DialogBox("Incoming request from $user. Accept to collaborate.",
                    "Accept", "Reject")
            if (invitationDialog.showAndGet()) {
                val inviteData = GsonSerializer.fromJson<InviteData>(message)
                acknowledgeInvitationAcceptance(user, inviteData.connectedUsers)
            }
        }

    }

    private fun acknowledgeInvitationAcceptance(user: String, connectedUsers: List<ConnectedUser>) {
        acknowledgeAcceptance(user)
        connectedUsers.forEach { users.addUser(it.name, it.hasTakenLock) }
        notifyConnectedUser(connectedUsers)
    }


    private fun acknowledgeAcceptance(user: String) {
        val message = Message(user.toEntityBear(), Message.Type.chat)
        message.subject = Subjects.invitation_acknowledgment
        message.body = "accepted"
        Utility.sendPacket(smack, message)
    }

    private fun notifyConnectedUser(connectedUsers: List<ConnectedUser>) {
        connectedUsers.forEach {
            val message = Message(it.name.toEntityBear(), Message.Type.chat)
            message.subject = Subjects.new_collaborator_joined
            message.body = GsonSerializer.toJson(smack.loggedInUser!!)
            Utility.sendPacket(smack, message)
        }

    }

    fun addConnectedUser(message: String) {
        val connectedUser = GsonSerializer.fromJson<ConnectedUser>(message)
        users.addUser(connectedUser.name, connectedUser.hasTakenLock)
    }


}
