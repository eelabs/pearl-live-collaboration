import Utility.toEntityBear
import org.jivesoftware.smack.packet.Message
import smack.Smack
import smack.Subjects

class Presence(private val smack: Smack) {
    fun checkPresence(user: String) {
        val message = Message(user.toEntityBear(), Message.Type.chat)
        message.subject = Subjects.user_availability
        message.body = "Are you available?"
        Utility.sendPacket(smack, message)
    }

    fun sendPresence(user: String) {
        val message = Message(user.toEntityBear(), Message.Type.chat)
        message.subject = Subjects.availability_status
        message.body = "I am available"
        Utility.sendPacket(smack, message)
    }

    fun sendDisconnectedMessage(users: Users) {
        val message = Message()
        message.type = Message.Type.chat
        message.subject = Subjects.user_status
        message.body = "I am disconnecting"
        Utility.sendToAllUsers(smack, message, users)
    }
}