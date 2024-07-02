package crew

import crew.config.BusinessUnit
import crew.config.Subsidiary
import crew.config.SupportedLanguage
import attachment.Attachment
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum

@GrailsCompileStatic
@TaackFieldEnum
@EqualsAndHashCode(includes = 'username')
@ToString(includes = 'username', includeNames = true, includePackage = false)
class User implements Serializable {

    private static final long serialVersionUID = 1
    User userCreated
    Date dateCreated

    String firstName
    String lastName
    String username
    String password
    BusinessUnit businessUnit
    Subsidiary subsidiary
    SupportedLanguage language
    String mail

    User manager

    Attachment mainPicture

    Boolean enabled = true
    boolean accountExpired
    boolean accountLocked
    boolean passwordExpired

    boolean rememberMe

    Set<Role> getAuthorities() {
        (UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
    }

    static transients = ['rememberMe']

    static constraints = {
        userCreated nullable: true
        dateCreated nullable: true
        firstName nullable: true
        lastName nullable: true
        language nullable: true
        subsidiary nullable: true
        manager nullable: true
        mainPicture nullable: true
        mail nullable: true, email: true
        password nullable: false, blank: false, password: true, widget: 'passwd'
        username nullable: false, blank: false, unique: true
    }

    static mapping = {
        table name: 'taack_users'
        password column: '`password`'
    }

    List<User> getManagedUsers() {
        User.findAllByManagerAndEnabled(this, true)
    }

    List<User> getAllManagedUsers() {
        getAllManagedUsersFromList(managedUsers)
    }

    private static List<User> getAllManagedUsersFromList(List<User> users) {
        List<User> ret = []
        users.each {
            ret.add it
            ret.addAll getAllManagedUsersFromList(it.getManagedUsers())
        }
        ret
    }

    List<User> getAllManagers() {
        final List<User> res = []
        User cursor = this
        User m
        while ((m = cursor.manager) && m) {
            res.add m
            cursor = m
        }
        res
    }

}
