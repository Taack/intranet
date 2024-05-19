package attachement

import crew.AttachmentController
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import org.codehaus.groovy.runtime.MethodClosure as MC
import attachment.Attachment
import crew.User
import taack.render.TaackUiEnablerService

import javax.annotation.PostConstruct

@GrailsCompileStatic
class AttachmentSecurityService {

    static lazyInit = false

    SpringSecurityService springSecurityService

    private securityClosure(Long id, Map p) {
        if (!id && !p) return true
        if (!id) return true
        canDownloadFile(Attachment.read(id), springSecurityService.currentUser as User)
    }

    @PostConstruct
    void init() {
        TaackUiEnablerService.securityClosure(
                this.&securityClosure,
                AttachmentController.&downloadAttachment as MC,
                AttachmentController.&extensionForAttachment as MC)
    }

    boolean canDownloadFile(Attachment attachment) {
        canDownloadFile(attachment, springSecurityService.currentUser as User)
    }

    boolean canDownloadFile(Attachment attachment, User user) {
        if (user == attachment.userCreated) return true
        if (attachment.documentAccess.isRestrictedToMyBusinessUnit && !attachment.documentAccess.isRestrictedToMySubsidiary && attachment.userCreated.businessUnit == user.businessUnit) return true
        if (attachment.documentAccess.isRestrictedToMySubsidiary && !attachment.documentAccess.isRestrictedToMySubsidiary && attachment.userCreated.subsidiary == user.subsidiary) return true
        if (attachment.documentAccess.isRestrictedToMySubsidiary && attachment.documentAccess.isRestrictedToMyBusinessUnit && attachment.userCreated.businessUnit == user.businessUnit && attachment.userCreated.subsidiary == user.subsidiary) return true
        if (attachment.documentAccess.isRestrictedToMyManagers && user.managedUsers.contains(attachment.userCreated)) return true
        return !attachment.documentAccess.isRestrictedToMyBusinessUnit && !attachment.documentAccess.isRestrictedToMySubsidiary && !attachment.documentAccess.isRestrictedToMyManagers
    }
}
