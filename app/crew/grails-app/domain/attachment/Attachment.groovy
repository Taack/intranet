package attachment

import attachment.config.AttachmentContentType
import attachment.config.AttachmentContentTypeCategory
import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum
import taack.domain.IDomainHistory

@TaackFieldEnum
@GrailsCompileStatic
class Attachment extends TaackDocument implements IDomainHistory<Attachment> {

    String filePath

    String originalName
    Long version
    String contentType
    Boolean active = true

    Long fileSize = 0

    String contentShaOne
    Attachment nextVersion
    AttachmentContentType contentTypeEnum
    AttachmentContentTypeCategory contentTypeCategoryEnum

    static constraints = {
        userUpdated nullable: true
        documentAccess nullable: true
        contentTypeEnum nullable: true
        contentTypeCategoryEnum nullable: true
        filePath widget: "filePath"
        lastUpdated nullable: true
        nextVersion nullable: true
        active validator: { boolean val, Attachment obj ->
            if (val && obj.nextVersion)
                return "attachment.active.hasNextVersion.error"
        }
    }

    def beforeValidate() {
        if (isDirty("filePath")) {
            if (filePath == null)  {
                filePath = this.getPersistentValue("filePath")
                contentTypeEnum = this.getPersistentValue("contentTypeEnum") as AttachmentContentType
                contentTypeCategoryEnum = this.getPersistentValue("contentTypeCategoryEnum") as AttachmentContentTypeCategory
            }
        }
    }

    static mapping = {
        filePath type: 'text'
        originalName type: 'text'
    }

    String getExtension() {
        originalName.substring(originalName.lastIndexOf('.') + 1)
    }

    String getName() {
        originalName
    }

    @Override
    String toString() {
        return filePath + "[$id]"
    }

    @Override
    Attachment cloneDirectObjectData() {
        if (this.id) {
            Attachment oldValue = new Attachment()
            oldValue.userCreated = userUpdated
            log.info "Attachment::cloneDirectObjectData ${version} ${userCreated}: ${dateCreated}, ${userUpdated}: ${lastUpdated} for ${name}"
            oldValue.filePath = filePath
            oldValue.originalName = originalName
            oldValue.contentTypeEnum = contentTypeEnum
            oldValue.contentTypeCategoryEnum = contentTypeCategoryEnum
            oldValue.contentType = contentType
            oldValue.active = active
            oldValue.fileSize = fileSize
            oldValue.contentShaOne = contentShaOne

            oldValue.active = false
            oldValue.nextVersion = this
            return oldValue
        }
        return null
    }

    @Override
    List<Attachment> getHistory() {
        try {
            return [this] + Attachment.findAllByNextVersion(this).sort { (it as Attachment).id }.reverse() // from recent to old
        } catch(e) {
            println "Attachment  ${e}"
            return null
        }
    }

}
