package org.taack

import app.config.AttachmentContentType
import app.config.AttachmentContentTypeCategory
import app.config.AttachmentType
import app.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import groovy.transform.AutoClone
import taack.ast.annotation.TaackFieldEnum

enum AttachmentStatus {
    proposal,
    valid,
    toFix,
    obsolete
}

@AutoClone
@TaackFieldEnum
@GrailsCompileStatic
class Attachment {

    User userCreated
    Date dateCreated
    User userUpdated
    Date lastUpdated
    Date dateImported = new Date()

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
        dateImported nullable: true
        fileOrigin nullable: true
        declaredLanguage nullable: true
        nextVersion nullable: true, unique: true//, validator: { Attachment val, Attachment obj ->
//            if (val) {
//                if (val.contentShaOne == obj.contentShaOne)
//                    return "attachment.nextVersion.isIdentical.error"
//                if (val.contentShaOne in obj.getOldVersions()*.contentShaOne)
//                    return "attachment.nextVersion.isOlder.error"
//            }
//        }
        grantedRoles nullable: true
        grantedUsers nullable: true
        grantedRolesRead nullable: true
        grantedUsersRead nullable: true
        active validator: { boolean val, Attachment obj ->
            if (val && obj.nextVersion)
                return "attachment.active.hasNextVersion.error"
        }
        isInternal nullable: true
    }

    static mapping = {
        filePath type: 'text'
        originalName type: 'text'
        grantedRolesRead joinTable: [name: 'roles_read_attachment']
        grantedUsersRead joinTable: [name: 'users_read_attachment']
    }

    static hasMany = [grantedRoles        : Role,
                      grantedUsers        : User,
                      grantedRolesRead    : Role,
                      grantedUsersRead    : User,
                      tags                : Term,
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

    int getVersionNumber() {
        return getOldVersions().size() + 1
    }

    List<Attachment> getNewVersions() {
        List<Attachment> newVersions = []
        Attachment tmp = this
        while (tmp = tmp.nextVersion) {
            newVersions << tmp
        }
        return newVersions.reverse()
    }

    List<Attachment> getOldVersions() {
        List<Attachment> oldVersions = []
        Attachment tmp = this
        while (tmp = Attachment.findByNextVersion(tmp)) {
            oldVersions << tmp
        }
        return oldVersions
    }

    List<Attachment> getAllVersions() {
        List<Attachment> versions = []
        Attachment tmp = this
        Attachment first
        while (first = Attachment.findByNextVersion(tmp)) {
            tmp = first
        }
        versions << tmp
        while (tmp = tmp.nextVersion) {
            versions << tmp
        }
        return versions.reverse()
    }

    boolean isImage() {
        return contentType?.toString()?.startsWith("image/")
    }

    @Override
    String toString() {
        return getName() ?: "[$id]"
    }
}
