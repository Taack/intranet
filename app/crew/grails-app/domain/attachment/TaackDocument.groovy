package attachment

import crew.User
import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum

@TaackFieldEnum
@GrailsCompileStatic
class TaackDocument {

    User userCreated
    Date dateCreated
    User userUpdated
    Date lastUpdated

    DocumentAccess documentAccess
    DocumentCategory documentCategory

    static mapping = {
        tablePerSubclass true
    }

}
