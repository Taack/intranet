package org.taack

import app.config.AttachmentType
import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum

@TaackFieldEnum
@GrailsCompileStatic
class AttachmentDescriptor {
    AttachmentType type = AttachmentType.noTypeSpecified

    Boolean isInternal = false
    Boolean isRestrictedToMyBusinessUnit = false
    Boolean isRestrictedToMySubsidiary = false
    Boolean isRestrictedToMyManagers = false
    Boolean isRestrictedToEmbeddingObjects = false

    static constraints = {
        type(unique: ['isInternal', 'isRestrictedToMyBusinessUnit', 'isRestrictedToMySubsidiary', 'isRestrictedToMyManagers', 'isRestrictedToEmbeddingObjects'])
    }

}
