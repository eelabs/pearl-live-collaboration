import com.intellij.openapi.editor.markup.RangeHighlighter
import java.awt.Color

data class InviteData(val connectedUsers: List<ConnectedUser>)
data class FileDetails(val filePath: String, val filename: String, val docText: String, val fileOp: FileOp = FileOp.Modification)

data class User(val name: String, var hasTakenLock: Boolean, var color: Color = colors[0],
                var cursor: RangeHighlighter? = null, var selection: RangeHighlighter? = null,
                var docChange: DocChange? = null) {
    companion object {
        val colors = listOf(Color.CYAN, Color.ORANGE, Color.MAGENTA, Color.BLUE)
    }

    fun setColor(index: Int) {
        color = colors[index]
    }
}

data class DocChange(val oldStart: Int, val oldEnd: Int, val newStart: Int, val newEnd: Int, val oldText: String,
                     val newText: String)


data class ConnectedUser(val name: String, var hasTakenLock: Boolean)

data class FileMoveDetails(val oldFilePath: String, val newParentPath: String, val newFilename: String? = null, val fileOp: FileOp)

enum class FileOp {
    Modification,
    Creation,
    Deletion,
    DirectoryCreation,
    DirectoryDeletion,
    CopyFile,
    MoveFile
}

data class FilePropChange(val filePath: String, val attributeName: String, val attribuetValue: String)