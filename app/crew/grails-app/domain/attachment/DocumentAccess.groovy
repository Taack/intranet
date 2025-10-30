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

    @Override
    String toString() {
        String internal = isInternal ? 'intern ' : ''
        String restrictedToMyBusinessUnit = isRestrictedToMyBusinessUnit ? 'BU Only ' : ''
        String restrictedToMySubsidiary = isRestrictedToMySubsidiary ? 'SUB Only ' : ''
        String restrictedToMyManager = isRestrictedToMyManagers ? 'Managers Only ' : ''
        return "Access: ${internal}${restrictedToMyBusinessUnit}${restrictedToMySubsidiary}${restrictedToMyManager}"
    }
}
