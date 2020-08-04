import Utility.relativePath
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.psi.PsiManager
import com.intellij.util.messages.MessageBusConnection
import com.thaiopensource.util.OptionParser
import org.jivesoftware.smack.packet.Message
import smack.Smack
import smack.Subjects
import windows.DialogBox
import java.nio.file.Files
import java.nio.file.Path


class FileOps(private val project: Project, private val editor: Editor?, private val smack: Smack,
              private val users: Users) {
    fun registerFileSync(messageBusConnection: MessageBusConnection) {
        messageBusConnection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                val isReadOnly = Registration.get(project)?.isReadOnly!!
                events.forEach { e ->
                    run {
                        e.file?.let {
                            val isInProject = ProjectRootManager.getInstance(project).fileIndex.isInContent(it)
                            when (e) {
                                is VFileCreateEvent -> {
                                    val op = when {
                                        it.isDirectory -> FileOp.DirectoryCreation
                                        else -> FileOp.Creation
                                    }
                                    sendFileChange(it, "", op, isReadOnly)
                                }
                                is VFileDeleteEvent -> {
                                    val op = when {
                                        it.isDirectory -> FileOp.DirectoryDeletion
                                        else -> FileOp.Deletion
                                    }
                                    sendFileChange(it, "", op, isReadOnly)
                                }
                                is VFileMoveEvent -> {
                                    if (isInProject) {
                                        val fileMoveDetails = FileMoveDetails(e.oldPath.relativePath(project)!!,
                                                e.newParent.path.relativePath(project)!!, null, FileOp.MoveFile)
                                        sendFileMovement(isReadOnly, fileMoveDetails)
                                    }
                                }
                                is VFileCopyEvent -> {
                                    if (isInProject) {
                                        val fileMoveDetails = FileMoveDetails(it.path.relativePath(project)!!,
                                                e.newParent.path.relativePath(project)!!, e.newChildName, FileOp.CopyFile)
                                        sendFileMovement(isReadOnly, fileMoveDetails)
                                    }
                                }
                                is VFilePropertyChangeEvent -> {
                                    if (isInProject) {
                                        var filePath = it.path.relativePath(project)!!
                                        if (e.propertyName == VirtualFile.PROP_NAME)
                                            filePath = e.oldPath.relativePath(project)!!
                                        sendPropertyChange(filePath, isReadOnly, e.propertyName, e.newValue as String)
                                    }
                                }
                                else -> {
                                    if (isInProject) {
                                        sendFileForSync(it, isReadOnly)
                                    }

                                }
                            }
                        }
                    }
                }
            }

        })


    }

    private fun sendPropertyChange(filePath: String, isReadOnly: Boolean, propertyName: String, propertyValue: String) {
        if (!isReadOnly) {
            val message = Message()
            message.type = Message.Type.chat
            message.body = GsonSerializer.toJson(FilePropChange(filePath, propertyName, propertyValue))
            message.subject = Subjects.file_property_change
            Utility.sendToAllUsers(smack, message, users)
        }
    }

    fun makePropertyChange(message: String) {
        val filePropChange = GsonSerializer.fromJson<FilePropChange>(message)
        val filePath = project.basePath + filePropChange.filePath
        val file = LocalFileSystem.getInstance().findFileByPath(filePath)
        Utility.write(project)
        {
            when (filePropChange.attributeName) {
                VirtualFile.PROP_NAME -> {
                    file?.rename(this, filePropChange.attribuetValue)
                }
                VirtualFile.PROP_HIDDEN -> Files.setAttribute(Path.of(filePath), filePropChange.attributeName,
                        filePropChange.attribuetValue.toBoolean())
            }
            file?.refresh(false, false)
        }
    }

    fun sendFileForSync(file: VirtualFile?, isReadOnly: Boolean) {
        if (!isReadOnly) {
            val message = Message()
            message.type = Message.Type.chat
            val filepath = file?.path?.relativePath(project)
            message.body = GsonSerializer.toJson(FileDetails(filepath!!, file.name, String(file.contentsToByteArray())))
            message.subject = Subjects.sync_file
            Utility.sendToAllUsers(smack, message, users)
        }

    }

    fun syncFile(message: String) {
        val fileDetails = GsonSerializer.fromJson<FileDetails>(message)
        val filePath = project.basePath + fileDetails.filePath
        val file = LocalFileSystem.getInstance().findFileByPath(filePath)
        if (file == null) {
            Utility.invokeLater {
                DialogBox("File not exists:${filePath}", "Ok").show()
            }
        } else
            Utility.write(project) {
                file.setBinaryContent(fileDetails.docText.toByteArray(file.charset))
            }
    }

    fun sendFileChange(file: VirtualFile, fileText: String, fileOp: FileOp, isReadOnly: Boolean) {
        if (!isReadOnly) {
            val message = Message()
            message.type = Message.Type.chat
            val fileDetails = GsonSerializer.toJson(FileDetails(file.parent.path.relativePath(project)!!, file.name, fileText, fileOp))
            message.body = fileDetails
            message.subject = Subjects.file_change
            Utility.sendToAllUsers(smack, message, users)
        }
    }

    fun acceptFileChange(message: String) {
        val fileDetails = GsonSerializer.fromJson<FileDetails>(message)
        val filePath = project.basePath + fileDetails.filePath
        val existingFilePath = Utility.getFilePath(editor)
        val file = LocalFileSystem.getInstance().findFileByPath(filePath + "/" + fileDetails.filename)
        when (fileDetails.fileOp) {
            FileOp.DirectoryCreation -> {
                Utility.write(project) {
                    val directory = LocalFileSystem.getInstance().findFileByPath(filePath)
                    val psiDirectory = PsiManager.getInstance(project).findDirectory(directory!!)
                    psiDirectory?.createSubdirectory(fileDetails.filename)
                }
            }
            FileOp.Creation -> {
                Utility.write(project) {
                    val directory = LocalFileSystem.getInstance().findFileByPath(filePath)
                    directory?.let {
                        val psiDirectory = PsiManager.getInstance(project).findDirectory(it)
                        val psiFile = psiDirectory?.createFile(fileDetails.filename)
                        openFile(psiFile?.virtualFile!!) {}
                    }
                }
            }
            FileOp.Modification -> {
                when {
                    file == null -> {
                        Utility.invokeLater {
                            DialogBox("File not exists:${fileDetails.filename}", "Ok").show()
                            editor?.document?.setReadOnly(true)
                        }
                    }
                    existingFilePath != file.path -> {
                        openFile(file) {
                            Utility.writeText(project, it, fileDetails.docText)
                        }
                    }
                    fileDetails.docText != editor?.document?.text -> {
                        Utility.writeText(project, editor, fileDetails.docText)
                    }
                }

            }
            FileOp.Deletion -> {
                Utility.write(project) {
                    val directory = LocalFileSystem.getInstance().findFileByPath(filePath)
                    directory?.let {
                        val psiDirectory = PsiManager.getInstance(project).findDirectory(it)
                        psiDirectory?.findFile(fileDetails.filename)?.delete()
                    }


                }

            }
            FileOp.DirectoryDeletion -> {
                Utility.write(project) {
                    val directory = LocalFileSystem.getInstance().findFileByPath(filePath)
                    directory?.let {
                        val psiDirectory = PsiManager.getInstance(project).findDirectory(it)
                        psiDirectory?.findSubdirectory(fileDetails.filename)?.delete()
                    }


                }

            }
            else -> throw OptionParser.InvalidOptionException()
        }
    }


    private fun openFile(virtualFile: VirtualFile, func: (Editor?) -> Unit) {
        Utility.invokeLater {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
            val newEditor = FileEditorManager.getInstance(project).selectedTextEditor
            val registration = Registration.get(project)
            registration?.changeEditor(newEditor!!)
            func(newEditor)
        }
    }

    fun sendFileMovement(isReadOnly: Boolean, fileMoveDetails: FileMoveDetails) {
        if (!isReadOnly) {
            val message = Message()
            message.type = Message.Type.chat
            val fileMoveDetailsJson = GsonSerializer.toJson(fileMoveDetails)
            message.body = fileMoveDetailsJson
            message.subject = Subjects.copy_or_move_file
            Utility.sendToAllUsers(smack, message, users)
        }
    }

    fun copyOrMoveFile(message: String) {
        val moveFileDetails = GsonSerializer.fromJson<FileMoveDetails>(message)
        val oldFilePath = project.basePath + moveFileDetails.oldFilePath
        val newParentFilePath = project.basePath + moveFileDetails.newParentPath
        val oldFile = LocalFileSystem.getInstance().findFileByPath(oldFilePath)
        val newParentFile = LocalFileSystem.getInstance().findFileByPath(newParentFilePath)
        Utility.write(project)
        {
            when (moveFileDetails.fileOp) {
                FileOp.CopyFile -> oldFile?.copy(this, newParentFile!!, moveFileDetails.newFilename!!)
                FileOp.MoveFile -> oldFile?.move(this, newParentFile!!)
                else -> throw OptionParser.InvalidOptionException()
            }
            VirtualFileManager.getInstance().syncRefresh()
        }

    }
}