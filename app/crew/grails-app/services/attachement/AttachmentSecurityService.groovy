package attachement

import crew.AttachmentController
import crew.CrewController
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.taack.Attachment
import org.taack.User
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
        if (attachment.attachmentDescriptor.isRestrictedToMyBusinessUnit && !attachment.attachmentDescriptor.isRestrictedToMySubsidiary && attachment.userCreated.businessUnit == user.businessUnit) return true
        if (attachment.attachmentDescriptor.isRestrictedToMySubsidiary && !attachment.attachmentDescriptor.isRestrictedToMySubsidiary && attachment.userCreated.subsidiary == user.subsidiary) return true
        if (attachment.attachmentDescriptor.isRestrictedToMySubsidiary && attachment.attachmentDescriptor.isRestrictedToMyBusinessUnit && attachment.userCreated.businessUnit == user.businessUnit && attachment.userCreated.subsidiary == user.subsidiary) return true
        if (attachment.attachmentDescriptor.isRestrictedToMyManagers && user.managedUsers.contains(attachment.userCreated)) return true
        return !attachment.attachmentDescriptor.isRestrictedToMyBusinessUnit && !attachment.attachmentDescriptor.isRestrictedToMySubsidiary && !attachment.attachmentDescriptor.isRestrictedToMyManagers
    }
}
