package crew

import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import org.taack.User

@GrailsCompileStatic
class CrewSecurityService  {

    SpringSecurityService springSecurityService

    User authenticatedRolesUser() {
        springSecurityService.currentUser as User
    }

    boolean authenticatedRoles(String... roles) {
        User user = authenticatedRolesUser()
        for (String r : roles) {
            if (user.authorities*.authority.contains(r)) return true
        }
        return false
    }

    boolean canSwitchUser() {
        authenticatedRoles('ROLE_SWITCH_USER', 'ROLE_ADMIN')
    }

    boolean isAdmin() {
        authenticatedRoles('ROLE_ADMIN')
    }

    boolean isManagerOf(User target) {
        User u = authenticatedRolesUser()
        u.id == target.id || u.allManagers*.id.contains(target.id)
    }

    boolean canEdit(User target) {
        admin || canSwitchUser() || isManagerOf(target)
    }

}
