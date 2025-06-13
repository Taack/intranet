package taack.website

import crew.Role
import crew.User
import crew.UserRole
import crew.config.BusinessUnit
import grails.compiler.GrailsCompileStatic
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@GrailsCompileStatic
@Component
class BootStrap {

    @Value('${taack.admin.password}')
    String adminPassword

    def init = { servletContext ->
        createDefaultRoleAndUser()
    }
    def destroy = {
    }

    def createDefaultRoleAndUser() {
        log.info "Creating default user and role ... $adminPassword"
        User.withNewTransaction {
            def r = Role.findByAuthority("ROLE_ADMIN")
            if (!r) {
                r = new Role(authority: "ROLE_ADMIN")
                r.save(flush: true)
                if (r.hasErrors()) log.error "${r.errors}"
            }
            def u = User.findByUsername("admin")
            if (!u) {
                u = new User(username: "admin", password: "{noop}$adminPassword", businessUnit: BusinessUnit.IT)
                u.save(flush: true)
                if (u.hasErrors()) log.error "${u.errors}"
                def ur = UserRole.create(u, r, true)
                if (ur.hasErrors()) log.error "${ur.errors}"
            }
            u
        }
    }
}
