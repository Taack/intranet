package crew.ssh

import app.config.AttachmentType
import attachment.SshAttachmentFolder
import crew.ssh.helper.RealFoldersCallback
import grails.compiler.GrailsCompileStatic
import grails.util.Pair
import org.apache.commons.io.FileUtils
import org.postgresql.PGConnection
import org.postgresql.copy.CopyManager
import org.postgresql.core.BaseConnection
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.taack.Attachment
import org.taack.User
import taack.base.TaackSimpleAttachmentService
import taack.ssh.SshEventRegistry
import taack.ssh.vfs.FileCallback
import taack.ssh.vfs.FileTree
import taack.ssh.vfs.FolderCallback
import taack.ssh.vfs.impl.VfsPath
import taack.ssh.vfs.impl.VfsPosixFileAttributes

import javax.annotation.PostConstruct
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.OpenOption

@GrailsCompileStatic
final class CrewSshService implements SshEventRegistry.VfsEvent {

    static lazyInit = false

    private final String VFS_FILE_NAME = "crew"

    @Value('${intranet.root}')
    String intranetRoot

    String getStorePath() {
        intranetRoot + "/attachment/store"
    }

    String getTmpUploadFolder() {
        intranetRoot + '/crew/tmp'
    }

    String getCrewFilesSharingPath() {
        intranetRoot + '/crew/sharing'
    }

    TransactionAwareDataSourceProxy dataSource
    TaackSimpleAttachmentService taackSimpleAttachmentService

    /*
    TODO: implement generic CSV export / import, based on AST and getProperties
    */

    @PostConstruct
    def initVfs() {
        log.info "initVfs start"
        FileUtils.forceMkdir(new File(tmpUploadFolder))
        FileUtils.forceMkdir(new File(storePath))
        FileUtils.forceMkdir(new File(crewFilesSharingPath))
        SshEventRegistry.VfsProvider.initVfsEventProvider(VFS_FILE_NAME, this)
        log.info "initVfs ends"
    }

    // Real File Folder
    // action files

    private final class CrewCsvFiles implements FileCallback {

        final User user

        final File activeUsers
        final File activeUserUpdated
        final File roles

        CrewCsvFiles(User user) {
            this.user = user
            activeUsers = new File(tmpUploadFolder + "/" + CrewCsvFiles.simpleName + "-" + "activeUsers.csv")
            activeUserUpdated = new File(tmpUploadFolder + "/" + CrewCsvFiles.simpleName + "-" + "activeUsersNew.csv")
            roles = new File(tmpUploadFolder + "/" + CrewCsvFiles.simpleName + "-" + "roles.csv")

        }

        @Override
        VfsPosixFileAttributes onGetFileAttributes(FileTree fileTree, FileTree.File file) {
            log.trace "CrewCsvFiles onGetFileAttributes $fileTree ${file.fileName}"
            if (file.fileName == "activeUsers.csv") {
                if (!activeUsers.exists()) {
                    try {
                        if (dataSource.connection.isWrapperFor(PGConnection.class)) {
                            PGConnection pgConnection = dataSource.getConnection().unwrap(PGConnection.class)
                            def cm = new CopyManager(pgConnection as BaseConnection)
                            def fos = new FileOutputStream(activeUsers)
                            cm.copyOut("COPY (select * from taacksec_user u where u.enabled = true) TO STDOUT WITH (FORMAT CSV, HEADER)", fos)
                            fos.close()
                        } else {
                            log.warn "Seems to not be PGConnection ..."
                            throw new RuntimeException("Seems to not be PGConnection ...")
                        }
                    } catch (e) {
                        log.warn("Cannot create copy manager: ${e.message}")
                        e.printStackTrace()
                    }
                }
                file.realFilePath = activeUsers.path
                return file.getAttributes(0)
            } else if (file.fileName == "roles.csv") {
                if (!roles.exists()) {
                    try {
                        if (dataSource.connection.isWrapperFor(PGConnection.class)) {
                            PGConnection pgConnection = dataSource.getConnection().unwrap(PGConnection.class)
                            def cm = new CopyManager(pgConnection as BaseConnection)
                            def fos = new FileOutputStream(roles)
                            cm.copyOut("COPY (select * from role u) TO STDOUT WITH (FORMAT CSV, HEADER)", new FileOutputStream(roles))
                            fos.close()
                        } else {
                            log.warn "Seems to not be PGConnection ..."
                            throw new RuntimeException("Seems to not be PGConnection ...")
                        }
                    } catch (e) {
                        log.warn("Cannot create copy manager: ${e.message}")
                        e.printStackTrace()
                    }
                }
                file.realFilePath = roles.path
                return file.getAttributes(0)
            } else if (file.fileName.startsWith("activeUsers.csv")) {
                return new VfsPosixFileAttributes(activeUserUpdated, user.username, true)
            }
            log.warn "onGetFileAttributes for file ${file.fileName} fails"
            return null
        }

        @Override
        SeekableByteChannel onFileOpen(FileTree fileTree, FileTree.File file, String s, Set<? extends OpenOption> options) {
            log.trace "CrewCsvFiles onFileOpen $fileTree ${file.fileName} $options"

            if (file.fileName == "activeUsers.csv") {
                return Files.newByteChannel(activeUsers.toPath())
            } else if (file.fileName == "roles.csv") {
                return Files.newByteChannel(roles.toPath())
            } else if (file.fileName.startsWith("activeUsers.csv")) {
                return Files.newByteChannel(activeUserUpdated.toPath())
            }
            log.warn "onGetFileAttributes for file ${file.fileName} fails"
            return null
        }

        @Override
        void onFileClose(FileTree fileTree, FileTree.File file, String s) {

        }

        @Override
        void onRenameFile(FileTree fileTree, FileTree.File file, VfsPath vfsPath, VfsPath vfsPath1) {

        }
    }

    private final class PicturesFolder extends SshAttachmentFolder {

        PicturesFolder(TaackSimpleAttachmentService taackSimpleAttachmentService, User user, String storePath, String attachmentVfsUploadFolder) {
            super(taackSimpleAttachmentService, user, storePath, attachmentVfsUploadFolder)
        }

        @Override
        Iterable<Pair<Long, String>> getAttachmentAndNames() {
            List<Pair<Long, String>> res = []
            for (User u : User.findAllByEnabled(true) as List<User>) {
                Attachment pic = u.attachments.find {
                    it.type == AttachmentType.mainPicture && it.active
                }
                if (pic) res.add new Pair<Long, String>(pic.id, u.username + "${pic.originalName.substring(pic.originalName.lastIndexOf('.'))}")
            }
            res
        }

        @Override
        boolean isUpdatable(Attachment attachment) {
            return attachment.userCreated.username == user.username
        }
    }

    static final class UserFiles extends RealFoldersCallback {

        UserFiles(User user, String realBaseDir) {
            super(user, realBaseDir)
        }
    }

    private final class SharedFiles implements FolderCallback {
        @Override
        void onGetFolder(FileTree fileTree, FileTree.Folder folder) {
            log.info "onGetFolder $fileTree $folder"
            try {
                def b = fileTree.createBuilder()
                User.withNewTransaction {
                    for (User uit : User.findAllByEnabled(true) as List<User>) {
                        String userShareFolder = crewFilesSharingPath + "/${uit.username}"
                        b.addNode(fileTree.createFolder(new UserFiles(uit, userShareFolder), uit.username, false, userShareFolder))
                    }
                }
                b.toFolder(folder)
            } catch(e) {
                log.error "ex: ${e.message}"
                e.printStackTrace()
                throw e
            }
        }

        @Override
        SeekableByteChannel onFolderFileCreate(FileTree fileTree, FileTree.Folder folder, VfsPath vfsPath, String handle, Set<? extends OpenOption> options) {
            log.info "onFolderFileCreate $fileTree $folder $vfsPath $handle $options"
            return null
        }

        @Override
        void onFolderFileCreateClose(FileTree fileTree, FileTree.Folder folder, VfsPath vfsPath, String handle) {
            log.info "onFolderFileCreateClose $fileTree $folder $vfsPath $handle"

        }
    }

    @Override
    FileTree initVfsAppEvent(String username) {
        log.info "initVfsAppEvent $username on Crew"
        User.withNewSession {
            try {
                User u = User.findByUsernameAndEnabled(username, true)
                if (u) {
                    PicturesFolder picturesFolder = new PicturesFolder(taackSimpleAttachmentService, u, storePath, tmpUploadFolder)
                    FileTree fs = new FileTree(username)
                    CrewCsvFiles crewCsvFiles = new CrewCsvFiles(u)
                    String userShareFolder = crewFilesSharingPath + "/${username}"
                    fs.root = fs.createBuilder(VFS_FILE_NAME)
                            .addNode(fs.createFolder(picturesFolder, "pictures"))
                            .addNode(fs.createFolder(new UserFiles(u, userShareFolder), "myViewableFiles", true, userShareFolder))
                            .addNode(fs.createFolder(new SharedFiles(), "sharedFiles"))
                            .addFiles(crewCsvFiles, true, "activeUsers.csv")
                            .addFiles(crewCsvFiles, false, "roles.csv")
                            .toFolder()
                    return fs
                }
            } catch(e) {
                log.error "cannot create FileTree: ${e.message}"
                e.printStackTrace()
                throw e
            }

            log.warn "initVfsAppEvent failed for $username"
            return null
        }
    }

    @Override
    void closeVfsConnection(String username) {
        log.info "closeVfsConnection $username"
    }
}

// Git LFS not working
// git clone --progress --verbose ssh://localhost:22222//home/auo/AUO32
// ssh -p 22222 auo@localhost
// https://programmingtechie.com/2019/08/18/how-to-implement-an-sftp-server-in-java-spring-boot-using-apache-mina-sshd-part-2-using-public-key-authentication/
// https://stackoverflow.com/questions/15372360/apache-sshd-public-key-authentication
// ssh-keygen -t ed25519
// https://phabricator.wikimedia.org/T276486
// ssh -4 -p 22222 -o PubkeyAcceptedKeyTypes=-ssh-rsa -o IdentityFile=~/.ssh/id_ed25519 10.109.55.95
// https://cryptsus.com/blog/how-to-secure-your-ssh-server-with-public-key-elliptic-curve-ed25519-crypto.html
// sftp -4 -oPubkeyAcceptedKeyTypes=-ssh-rsa -oIdentityFile=~/.ssh/id_ed25519 -P 22222 auo4ever
// export GIT_SSH_COMMAND="ssh -4 -i ~/.ssh/id_ed25519 " ; git lfs clone --progress --verbose ssh://auo4ever:22222//home/auo/AUO56
// ssh-keygen -m pem -t ed25519
