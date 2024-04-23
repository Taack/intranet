package crew

import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import org.codehaus.groovy.runtime.MethodClosure
import org.taack.User
import taack.base.*

import javax.annotation.PostConstruct

@GrailsCompileStatic
class CrewSecurityService  {

    static lazyInit = false

    SpringSecurityService springSecurityService
    TaackUiSimpleService taackUiSimpleService
    TaackSimpleSaveService taackSimpleSaveService
    TaackSimpleAttachmentService taackSimpleAttachmentService
    TaackSimpleFilterService taackSimpleFilterService

    private securityClosure(Long id, Map p) {
        if (!id && !p) return true
        def priceList = ItemPriceList.read(id)
        if (!id) return true
        if (crm2SecurityService.isManagerOfUser(priceList.userCreated)) true
        else false
    }

    @PostConstruct
    void init() {
        TaackUiEnablerService.securityClosure(
                this.&securityClosure,
                Crm2PriceListController.&uploadCsv as MethodClosure,
                Crm2PriceListController.&deletePriceList as MethodClosure,
                Crm2PriceListController.&editPriceList as MethodClosure,
                Crm2PriceListController.&importPriceList as MethodClosure)
    }

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
