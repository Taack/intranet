package attachment


import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum

@TaackFieldEnum
@GrailsCompileStatic
class DocumentAccess {
    Boolean isRestrictedToMyBusinessUnit = false
    Boolean isRestrictedToMySubsidiary = false
    Boolean isRestrictedToMyManagers = false

    static constraints = {
    }

    @Override
    String toString() {
        String restrictedToMyBusinessUnit = isRestrictedToMyBusinessUnit ? 'BU Only ' : ''
        String restrictedToMySubsidiary = isRestrictedToMySubsidiary ? 'SUB Only ' : ''
        String restrictedToMyManager = isRestrictedToMyManagers ? 'Managers Only ' : ''
        return "Access: ${restrictedToMyBusinessUnit}${restrictedToMySubsidiary}${restrictedToMyManager}"
    }
}
