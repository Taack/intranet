package attachement

import attachment.WriteAccess
import crew.AttachmentController
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import jakarta.annotation.PostConstruct
import org.codehaus.groovy.runtime.MethodClosure as MC
import attachment.Attachment
import crew.User
import taack.app.TaackApp
import taack.app.TaackAppRegisterService
import taack.render.TaackUiEnablerService


@GrailsCompileStatic
class AttachmentSecurityService {

    static lazyInit = false

    SpringSecurityService springSecurityService

    private securityCanDownloadClosure(Long id, Map p) {
        if (!id && !p) return true
        if (!id) return true
        canDownloadFile(Attachment.get(id), springSecurityService.currentUser as User)
    }

    private securityCanEditClosure(Long id, Map p) {
        if (!id && !p) return true
        if (!id) return true
        canEditFile(Attachment.get(id), springSecurityService.currentUser as User)
    }

    @PostConstruct
    void init() {
        TaackUiEnablerService.securityClosure(
                this.&securityCanDownloadClosure,
                AttachmentController.&downloadBinAttachment as MC,
                AttachmentController.&showAttachmentIFrame as MC,
                AttachmentController.&downloadBinExtensionForAttachment as MC)
        TaackUiEnablerService.securityClosure(
                this.&securityCanEditClosure,
                AttachmentController.&editAttachment as MC,
                AttachmentController.&inlineEdition as MC,
                AttachmentController.&saveAttachment as MC
        )
        TaackAppRegisterService.register(new TaackApp(AttachmentController.&index as MC, new String(AttachmentSecurityService.getResourceAsStream("/att/att.svg").readAllBytes())))
    }

    boolean canEditFile(Attachment attachment, User user) {
        switch (attachment.writeAccess) {
            case WriteAccess.OWNERS:
                return attachment.userCreated.id == user.id || attachment.userCreated.allManagers*.id.contains(user.id)
                break
            case WriteAccess.READ_ONLY:
                return false
                break
            case WriteAccess.READERS:
                return canDownloadFile(attachment, user)
                break
        }
        return attachment.userCreated.id == user.id
    }

    boolean canDownloadFile(Attachment attachment) {
        canDownloadFile(attachment, springSecurityService.currentUser as User)
    }

    boolean canDownloadFile(Attachment attachment, User user) {
        if (attachment.nextVersion) attachment = attachment.nextVersion
        if (user == attachment.userCreated) return true
        if (attachment.documentAccess.isRestrictedToMyBusinessUnit && !attachment.documentAccess.isRestrictedToMySubsidiary && attachment.userCreated.businessUnit == user.businessUnit) return true
        if (attachment.documentAccess.isRestrictedToMySubsidiary && !attachment.documentAccess.isRestrictedToMySubsidiary && attachment.userCreated.subsidiary == user.subsidiary) return true
        if (attachment.documentAccess.isRestrictedToMySubsidiary && attachment.documentAccess.isRestrictedToMyBusinessUnit && attachment.userCreated.businessUnit == user.businessUnit && attachment.userCreated.subsidiary == user.subsidiary) return true
        if (attachment.documentAccess.isRestrictedToMyManagers && user.managedUsers.contains(attachment.userCreated)) return true
        return !attachment.documentAccess.isRestrictedToMyBusinessUnit && !attachment.documentAccess.isRestrictedToMySubsidiary && !attachment.documentAccess.isRestrictedToMyManagers
    }
}
