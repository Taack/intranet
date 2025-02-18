package attachment

import crew.User
import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileStatic
import taack.ast.annotation.TaackFieldEnum

@CompileStatic
enum WriteAccess {
    OWNERS, // User Created hierarchy
    READ_ONLY, // Cannot be updated unless OWNERS change that
    READERS // All those who can read the file
}

@TaackFieldEnum
@GrailsCompileStatic
class TaackDocument {

    User userCreated
    Date dateCreated
    User userUpdated
    Date lastUpdated

    DocumentAccess documentAccess
    DocumentCategory documentCategory

    WriteAccess writeAccess = WriteAccess.OWNERS

    static constraints = {
        userUpdated nullable: true
        writeAccess nullable: true
    }

    static mapping = {
        tablePerSubclass true
    }

}
