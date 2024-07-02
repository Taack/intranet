package crew.ssh.helper

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import crew.User
import taack.ssh.vfs.FileCallback
import taack.ssh.vfs.FileTree
import taack.ssh.vfs.FolderCallback
import taack.ssh.vfs.impl.VfsPath
import taack.ssh.vfs.impl.VfsPosixFileAttributes

import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.StandardCopyOption

@Slf4j
@CompileStatic
class RealFoldersCallback implements FolderCallback, FileCallback {
    final User user
    final String realBaseDir

    RealFoldersCallback(User user, String userBaseDir) {
        this.user = user
        this.realBaseDir = userBaseDir
        def f = new File(realBaseDir)
        if (!f.exists()) Files.createDirectory(f.toPath())
    }

    @Override
    VfsPosixFileAttributes onGetFileAttributes(FileTree fileTree, FileTree.File file) {
        log.trace "onGetFileAttributes $fileTree, ${file.name}"
        file.getAttributes 0
    }

    @Override
    SeekableByteChannel onFileOpen(FileTree fileTree, FileTree.File file, String handle, Set<? extends OpenOption> options) {
        log.info "onFileOpen $fileTree, ${file.name}, $handle, $options"

        File tmp = new File(file.realFilePath)
        Files.newByteChannel tmp.toPath(), options
    }

    @Override
    void onFileClose(FileTree fileTree, FileTree.File file, String handle) {
        log.info "onGetFileAttributes $fileTree, ${file.name}, $handle"

    }

    @Override
    void onRenameFile(FileTree fileTree, FileTree.File file, VfsPath oldPath, VfsPath newPath) {
        log.info "onRenameFile $fileTree, ${file.name}, $oldPath, $newPath"
        Files.move(new File("$realBaseDir/${oldPath.getFileName()}").toPath(), new File("$realBaseDir/${newPath.getFileName()}").toPath(), StandardCopyOption.REPLACE_EXISTING)
        file.fileName = newPath.getFileName()
        file.realFilePath = "${realBaseDir}/${file.fileName}"
    }

    @Override
    void onGetFolder(FileTree fileTree, FileTree.Folder folder) {
        log.info "onGetFolder $fileTree, ${folder.name}"
        def b = fileTree.createBuilder()
        b.addFileFromRealFolder(this, realBaseDir, true)
        b.toFolder(folder)
    }

    @Override
    SeekableByteChannel onFolderFileCreate(FileTree fileTree, FileTree.Folder folder, VfsPath path, String handle, Set<? extends OpenOption> options) {
        log.info "onFolderFileCreate $fileTree, ${folder.name}, $path, $handle, $options"
        File test = new File("$realBaseDir/${path.getFileName()}")
        fileTree.createBuilder()
                .addFiles(this, true, path.last().toString())
                .toFolder(folder)
        Files.newByteChannel test.toPath(), options
    }

    @Override
    void onFolderFileCreateClose(FileTree fileTree, FileTree.Folder folder, VfsPath vfsPath, String s) {
        log.info "onFolderFileCreateClose $fileTree, ${folder.name}, $vfsPath, $s"

    }
}
