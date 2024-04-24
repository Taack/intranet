package org.taack

import app.config.AttachmentContentType
import app.config.AttachmentContentTypeCategory
import app.config.AttachmentType
import app.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileStatic
import taack.ast.annotation.TaackFieldEnum
import taack.domain.IDomainHistory

@CompileStatic
enum AttachmentStatus {
    proposal,
    valid,
    toFix,
    obsolete
}

@TaackFieldEnum
@GrailsCompileStatic
class AttachmentDescriptor implements IDomainHistory<AttachmentDescriptor> {
    AttachmentType type
    String fileOrigin

    AttachmentStatus status = AttachmentStatus.proposal
    SupportedLanguage declaredLanguage

    String publicName
    AttachmentContentType contentTypeEnum
    AttachmentContentTypeCategory contentTypeCategoryEnum

    Boolean isInternal
    Boolean isRestrictedToMyBusinessUnit
    Boolean isRestrictedToMySubsidiary
    Boolean isRestrictedToMyManagers
    Boolean isRestrictedToEmbeddingObjects

    Set<Term> tags

    AttachmentDescriptor nextVersion

    static constraints = {
        isRestrictedToMyBusinessUnit nullable: true
        isRestrictedToMySubsidiary nullable: true
        isRestrictedToMyManagers nullable: true
        isRestrictedToEmbeddingObjects nullable: true
        contentTypeEnum nullable: true
        contentTypeCategoryEnum nullable: true
        publicName nullable: true
        type nullable: true
        status nullable: true
        fileOrigin nullable: true
        declaredLanguage nullable: true
        isInternal nullable: true
    }

    static hasMany = [
            tags: Term
    ]

    @Override
    AttachmentDescriptor cloneDirectObjectData() {
        if (this.id) {
            AttachmentDescriptor oldValue = new AttachmentDescriptor()
            log.info "AttachmentDescriptor::cloneDirectObjectData ${version} ${id}"
            oldValue.type = type
            oldValue.fileOrigin = fileOrigin
            oldValue.status = status
            oldValue.declaredLanguage = declaredLanguage
            oldValue.publicName = publicName
            oldValue.contentTypeEnum = contentTypeEnum
            oldValue.contentTypeCategoryEnum = contentTypeCategoryEnum
            oldValue.isInternal = isInternal
            oldValue.isRestrictedToMyBusinessUnit = isRestrictedToMyBusinessUnit
            oldValue.isRestrictedToMySubsidiary = isRestrictedToMySubsidiary
            oldValue.isInternal = isInternal
            oldValue.isRestrictedToMyManagers = isRestrictedToMyManagers
            oldValue.isRestrictedToEmbeddingObjects = isRestrictedToEmbeddingObjects
            oldValue.nextVersion = this
            return oldValue
        }
        return null
    }

    @Override
    List<AttachmentDescriptor> getHistory() {
        return AttachmentDescriptor.findAllByNextVersion(id).sort { it.id }
    }
}
