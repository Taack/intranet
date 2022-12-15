package org.taack

import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum

@GrailsCompileStatic
@TaackFieldEnum
class Alert {

    Date dateCreated
    String name
    String description
    String origin
    Boolean isSubscriptionEnabled = true
    Boolean active = true
    Set<Role> restrictedRoles
    Set<User> users

    static hasMany = [
            restrictedRoles: Role,
            users: User
    ]

    static constraints = {
        name unique: true
        description nullable: true, widget: 'textarea'
        origin nullable: true
        isSubscriptionEnabled nullable: true
        users validator: { Set<User> val, Alert obj ->
            if (val?.size() && obj.restrictedRoles?.size()) {
                if (val.grep { obj.hasPermission(it as User) }.size() != val.size())
                    return "alert.users.notAllowed.error"
            }
        }
    }

    static mapping = {
        description type: "text"
    }

    @Override
    String toString() {
        return name
    }

    Boolean hasPermission(User user) {
        if (user.authorities.any { it.authority == 'ROLE_ADMIN' })
            return true
        return user.authorities.intersect(restrictedRoles as Collection<Role>).size()
    }

    Boolean canSubscribe(User user) {
        if (!isSubscriptionEnabled || !user)
            return false
        if (!restrictedRoles)
            return true
        return hasPermission(user)
    }
}
