package attachment


import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum

@TaackFieldEnum
@GrailsCompileStatic
class DocumentAccess {
    Boolean isInternal = false
    Boolean isRestrictedToMyBusinessUnit = false
    Boolean isRestrictedToMySubsidiary = false
    Boolean isRestrictedToMyManagers = false
    Boolean isRestrictedToEmbeddingObjects = false

    static constraints = {
        isInternal(unique: ['isRestrictedToMyBusinessUnit', 'isRestrictedToMySubsidiary', 'isRestrictedToMyManagers', 'isRestrictedToEmbeddingObjects'])
    }

}
