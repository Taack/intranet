package org.taack

import app.config.AttachmentType
import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum

@TaackFieldEnum
@GrailsCompileStatic
class AttachmentDescriptor {
    AttachmentType type

    Boolean isInternal
    Boolean isRestrictedToMyBusinessUnit
    Boolean isRestrictedToMySubsidiary
    Boolean isRestrictedToMyManagers
    Boolean isRestrictedToEmbeddingObjects

    static constraints = {
        type(unique: ['isInternal', 'isRestrictedToMyBusinessUnit', 'isRestrictedToMySubsidiary', 'isRestrictedToMyManagers', 'isRestrictedToEmbeddingObjects'])
        isRestrictedToMyBusinessUnit nullable: true
        isRestrictedToMySubsidiary nullable: true
        isRestrictedToMyManagers nullable: true
        isRestrictedToEmbeddingObjects nullable: true
        isInternal nullable: true
    }

}
