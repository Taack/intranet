package attachment

import crew.User
import grails.util.Pair
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import taack.domain.TaackAttachmentService
import taack.ssh.vfs.FileCallback
import taack.ssh.vfs.FileTree
import taack.ssh.vfs.FolderCallback
import taack.ssh.vfs.INode
import taack.ssh.vfs.impl.VfsPath
import taack.ssh.vfs.impl.VfsPosixFileAttributes

import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.StandardCopyOption

@Slf4j
@CompileStatic
abstract class SshAttachmentFolder implements FolderCallback, FileCallback {
    final String attachmentVfsUploadFolder
    final String storePath
    final TaackAttachmentService taackAttachmentService

    final User user

    SshAttachmentFolder(TaackAttachmentService taackSimpleAttachmentService, User user, String storePath, String attachmentVfsUploadFolder) {
        this.attachmentVfsUploadFolder = attachmentVfsUploadFolder
        this.storePath = storePath
        this.taackAttachmentService = taackSimpleAttachmentService
        this.user = user
    }

    abstract Iterable<Pair<Long, String>> getAttachmentAndNames()

    abstract boolean isUpdatable(Attachment attachment)

    @Override
    void onGetFolder(FileTree fileTree, FileTree.Folder folder) {
        log.info "onGetFolder $fileTree $folder"
        def b = fileTree.createBuilder()
        Attachment.withNewSession {
            try {
                for (Pair<Long, String> an : attachmentAndNames) {
                    if (an && an.aValue && an.bValue) {
                        Attachment a = Attachment.read(an.aValue)
                        b.addFile(this, an.bValue, isUpdatable(a), taackAttachmentService.attachmentPath(a), an.aValue)
                    }
                }
            } catch (e) {
                log.error "${e.message}"
                e.printStackTrace()
            }
        }
        b.toFolder(folder)
    }

    @Override
    SeekableByteChannel onFolderFileCreate(FileTree fileTree, FileTree.Folder folder, VfsPath vfsPath, String handle, Set<? extends OpenOption> options) {
        log.trace "onFolderFileCreate $fileTree, $folder , $vfsPath, $handle, $options"
        String realPath = "${attachmentVfsUploadFolder}/$handle-${user.username}-${vfsPath.fileName}"
        def b = fileTree.createBuilder()
        b.addFile(this, vfsPath.fileName.toString(), true, realPath)
        b.toFolder(folder)
        File tmp = new File(realPath)
        Files.newByteChannel tmp.toPath(), options
    }

    @Override
    void onFolderFileCreateClose(FileTree fileTree, FileTree.Folder folder, VfsPath vfsPath, String handle) {
        log.info "onFolderFileCreateClose $fileTree, $folder, $vfsPath, $handle"
    }

    @Override
    VfsPosixFileAttributes onGetFileAttributes(FileTree fileTree, FileTree.File file) {
        log.trace "onGetFileAttributes ${file.name}"
        try {
            def a = file.getAttributes(0)
            return a
        } catch (e) {
            log.error "cannot generate attributes for file: ${file.name}: ${e.message}"
            e.printStackTrace()
        }
    }

    @Override
    SeekableByteChannel onFileOpen(FileTree fileTree, FileTree.File file, String s, Set<? extends OpenOption> options) {
        log.trace "onFileOpen $fileTree $file ${options}"
        File tmp = new File(file.realFilePath)
        Files.newByteChannel tmp.toPath(), options
    }

    @Override
    void onFileClose(FileTree fileTree, FileTree.File file, String s) {

    }

    @Override
    void onRenameFile(FileTree fileTree, FileTree.File file, VfsPath oldPath, VfsPath newPath) {
        log.info "onRenameFile $fileTree, $file, $oldPath, $newPath"
        FileTree.File oldFile = null
        Attachment a = null
        try {
            Attachment.withNewSession {
                def itChildren = file.parent.children.iterator()
                while (itChildren.hasNext()) {
                    INode i = itChildren.next()
                    if (i instanceof FileTree.File) {
                        FileTree.File f = i as FileTree.File
                        if (f.fileName == newPath.fileName.toString()) {
                            if (f.internalId) {
                                a = Attachment.read(f.internalId)
                                if (isUpdatable(a)) {
                                    oldFile = f
                                    //file.parent.children.remove()
                                    break
                                }
                            }
                        }
                    }
                }

                FileTree.File newFile = null
                itChildren = file.parent.children.iterator()
                while (itChildren.hasNext()) {
                    INode i = itChildren.next()
                    if (i instanceof FileTree.File) {
                        FileTree.File f = i as FileTree.File
                        if (f.fileName == oldPath.fileName.toString()) {
                            newFile = f
                            itChildren.remove()
                            break
                        }
                    }
                }
                Files.copy(new File(oldFile.realFilePath).toPath(), new File(newFile.realFilePath).toPath(), StandardCopyOption.REPLACE_EXISTING)
                def newFileReplace = new File(newFile.realFilePath)
// TODO                taackAttachmentService.updateAttachment(newFileReplace, newPath.fileName.toString(), a)
            }

        } catch (e) {
            log.warn "cannot rename file ${e.message}... ${a} ${oldFile}"
            e.printStackTrace()
            throw e
        }
    }
}
