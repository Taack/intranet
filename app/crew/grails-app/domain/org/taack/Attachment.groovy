package org.taack

import app.config.AttachmentContentType
import app.config.AttachmentContentTypeCategory
import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum
import taack.domain.IDomainHistory

import java.nio.file.Files

@TaackFieldEnum
@GrailsCompileStatic
class Attachment implements IDomainHistory<Attachment> {

    User userCreated
    Date dateCreated
    User userUpdated
    Date lastUpdated

    String filePath
    String originalName
    Integer version
    String contentType

    Boolean active = true

    Long fileSize = 0

    String contentShaOne
    Attachment nextVersion
    AttachmentDescriptor attachmentDescriptor
    AttachmentContentType contentTypeEnum
    AttachmentContentTypeCategory contentTypeCategoryEnum


    static constraints = {
        userUpdated nullable: true
        attachmentDescriptor nullable: true
        filePath widget: "filePath"
        lastUpdated nullable: true
        nextVersion nullable: true, unique: true
        active validator: { boolean val, Attachment obj ->
            if (val && obj.nextVersion)
                return "attachment.active.hasNextVersion.error"
        }
    }

    static mapping = {
        filePath type: 'text'
        originalName type: 'text'
    }

    String getName() {
        attachmentDescriptor.publicName ?: originalName
    }

    String getExtension() {
        originalName.substring(originalName.lastIndexOf('.') + 1)
    }

    String getOriginalNameWithoutExtension() {
        if (originalName.contains('.')) originalName.substring(0, originalName.lastIndexOf('.'))
        else originalName
    }

    @Override
    String toString() {
        return getName() ?: "[$id]"
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
        return null
    }

}
