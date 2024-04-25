package attachement

import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import org.taack.Attachment
import org.taack.User

@GrailsCompileStatic
class AttachmentSecurityService {
    SpringSecurityService springSecurityService

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
