package org.taack

import app.config.AttachmentContentType
import app.config.AttachmentContentTypeCategory
import app.config.AttachmentType
import app.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import taack.ast.annotation.TaackFieldEnum

@CompileStatic
enum AttachmentStatus {
    proposal,
    valid,
    toFix,
    obsolete
}

@TaackFieldEnum
@GrailsCompileStatic
class Attachment {

    User userCreated
    Date dateCreated
    User userUpdated
    Date lastUpdated

    AttachmentType type
    String fileOrigin

    AttachmentStatus status = AttachmentStatus.proposal
    SupportedLanguage declaredLanguage

    String filePath
    String originalName
    String publicName
    Integer version
    String contentType
    AttachmentContentType contentTypeEnum
    AttachmentContentTypeCategory contentTypeCategoryEnum

    Boolean active = true
    Boolean isInternal
    Boolean isRestrictedToMyBusinessUnit
    Boolean isRestrictedToMySubsidiary
    Boolean isRestrictedToMyManagers
    Boolean isRestrictedToEmbeddingObjects

    Long fileSize = 0

    String contentShaOne
    Attachment nextVersion

    Set<Term> tags

    static constraints = {
        userUpdated nullable: true
        filePath widget: "filePath"
        isRestrictedToMyBusinessUnit nullable: true
        isRestrictedToMySubsidiary nullable: true
        isRestrictedToMyManagers nullable: true
        isRestrictedToEmbeddingObjects nullable: true
        contentTypeEnum nullable: true
        contentTypeCategoryEnum nullable: true
        publicName nullable: true
        type nullable: true
        status nullable: true
        lastUpdated nullable: true
        fileOrigin nullable: true
        declaredLanguage nullable: true
        nextVersion nullable: true, unique: true//, validator: { Attachment val, Attachment obj ->
        active validator: { boolean val, Attachment obj ->
            if (val && obj.nextVersion)
                return "attachment.active.hasNextVersion.error"
        }
        isInternal nullable: true
    }

    static mapping = {
        filePath type: 'text'
        originalName type: 'text'
    }

    static hasMany = [
            tags: Term
    ]

    String getName() {
        publicName ?: originalName
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
}
