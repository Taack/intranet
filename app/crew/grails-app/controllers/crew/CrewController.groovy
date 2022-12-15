package crew

import app.config.AttachmentType
import attachement.AttachmentUiService
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import grails.web.api.WebAttributes
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.springframework.security.access.AccessDeniedException
import org.springframework.transaction.TransactionStatus
import org.taack.Alert
import org.taack.Attachment
import org.taack.Role
import org.taack.User
import org.taack.UserRole
import taack.base.TaackMetaModelService
import taack.base.TaackSimpleFilterService
import taack.base.TaackSimpleSaveService
import taack.base.TaackUiSimpleService
import taack.ui.TaackPluginService
import taack.ui.base.*
import taack.ui.base.block.BlockSpec
import taack.ui.base.common.ActionIcon
import taack.ui.base.common.ActionIconStyleModifier
import taack.ui.base.filter.expression.FilterExpression
import taack.ui.base.filter.expression.Operator
import taack.ui.base.table.ColumnHeaderFieldSpec

@GrailsCompileStatic
@Secured(["IS_AUTHENTICATED_REMEMBERED"])
class CrewController implements WebAttributes {
    TaackUiSimpleService taackUiSimpleService
    TaackSimpleFilterService taackSimpleFilterService
    TaackSimpleSaveService taackSimpleSaveService
    SpringSecurityService springSecurityService
    CrewUiService crewUiService
    CrewSearchService crewSearchService
    AttachmentUiService attachmentUiService
    TaackPluginService taackPluginService

    private boolean isAdmin() {
        (authenticatedUser as User).authorities*.authority.contains("ROLE_ADMIN")
    }

    static private UiMenuSpecifier buildMenu(String q = null) {
        UiMenuSpecifier m = new UiMenuSpecifier()

        m.ui {
            menu "Users", CrewController.&index as MC
            menu "Roles", CrewController.&listRoles as MC
            menu "Alerts", CrewController.&listAlerts as MC
            menu "Hierachy", CrewController.&hierarchy as MC
            menuIcon "Config MySelf", ActionIcon.CONFIG_USER, this.&userSelfEdit as MC, null, true
            menuSearch this.&search as MethodClosure, q
        }
        m
    }

    private UiTableSpecifier buildUserTableHierarchy(final User u) {
        /*
        TODO: Improve grouping with subTable, sub total
        TODO: Choose Grouping layout: Per subsidiary, per Business Unit, (or both) flat ... why not adding filters ?
        TODO: Repeat user if grouping per subsidiary (Henri does not belongs to Citel Inc, but manage PC from CitelInc, so PC should appear 2 times)
        TODO: When grouping, capability to expend and focus on PC in Citel Inc from Citel Holding Henri hierarchy
         */
        UiTableSpecifier t = new UiTableSpecifier()

        def groups = taackSimpleFilterService.listGroup(User)

        boolean hasActions = isAdmin()

        t.ui User, {
            header {
                column {
                    fieldHeader "Username"
                    groupFieldHeader "Business Unit", u.businessUnit_
                }
                column {
                    groupFieldHeader "Subsidiary", u.subsidiary_
                    fieldHeader "Manager"
                }
                column {
                    fieldHeader "Last Name"
                    fieldHeader "First Name"
                }
            }

            int count = 0
            Closure rec
            rec = { List<User> mus, int level ->
                rowIndent({
                    level++
                    for (def mu : mus) {
                        count++
                        boolean muHasChildren = !mu.managedUsers.isEmpty()
                        rowTree muHasChildren, {
                            rowColumn {
                                if (hasActions) rowLink "Edit User", ActionIcon.EDIT * ActionIconStyleModifier.SCALE_DOWN, this.&userForm as MC, [id: mu.id, redirectAction: actionName]
                                rowField mu.username_
                                rowField mu.businessUnit_
                            }
                            rowColumn {
                                rowField mu.subsidiary_
                                rowField mu.manager?.username
                            }
                            rowColumn {
                                rowField mu.lastName_
                                rowField mu.firstName_
                            }
                        }
                        if (muHasChildren) {
                            rec(mu.managedUsers, level)
                        }
                    }
                })
            }

            if (groups) {
                User filterUser = new User(enabled: true)
                for (def g : groups) {
                    int oldCount = count
                    rowGroupHeader g
                    rec(taackSimpleFilterService.listInGroup(g, User, new UiFilterSpecifier().ui(User, {
                        filterFieldExpressionBool null, new FilterExpression(filterUser.enabled_, Operator.EQ, true), true
                    })).aValue, 0)
                    rowGroupFooter "Count: ${count - oldCount}"
                }
            } else {
                rec(User.findAllByManagerIsNullAndEnabled(true), 0)
            }
        }

        t
    }

    def search(String q) {
        taackUiSimpleService.show(crewSearchService.buildSearchBlock(q), buildMenu(q))
    }

    def hierarchy() {
        UiTableSpecifier t = buildUserTableHierarchy(new User(enabled: true))
        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            ajaxBlock "userList", {
                table "Users Hierarchy", t, BlockSpec.Width.MAX
            }
        }

        taackUiSimpleService.show(b, buildMenu())
    }

    def index() {
        User cu = authenticatedUser as User

        UiFilterSpecifier f = CrewUiService.buildUserTableFilter cu
        UiTableSpecifier t = crewUiService.buildUserTable f

        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            ajaxBlock 'userList', {
                tableFilter 'Filter', f, 'Users', t, BlockSpec.Width.MAX, {
                    if (isAdmin()) action 'Create User', ActionIcon.CREATE, CrewController.&userForm as MC, [redirectAction: actionName], true
                }
            }
        }

        taackUiSimpleService.show(b, buildMenu())
    }

    def selectRoleM2O() {
        UiFilterSpecifier f = CrewUiService.buildRoleTableFilter()
        UiTableSpecifier t = crewUiService.buildRoleTable f, true

        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            modal !params.boolean("refresh"), {
                ajaxBlock "userListSelect", {
                    tableFilter "Filter", f, "Roles", t, BlockSpec.Width.MAX
                }
            }
        }

        taackUiSimpleService.show(b)
    }

    def selectUserM2O() {
        User cu = springSecurityService.currentUser as User

        UiFilterSpecifier f = CrewUiService.buildUserTableFilter cu
        UiTableSpecifier t = crewUiService.buildUserTable f, true

        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            modal !params.boolean("refresh"), {
                ajaxBlock "userListSelect", {
                    tableFilter "Filter", f, "Users", t, BlockSpec.Width.MAX
                }
            }
        }

        taackUiSimpleService.show(b)
    }

    @Secured("isAuthenticated()")
    def selectRoleM2OCloseModal(Role role) {
        UiBlockSpecifier block = new UiBlockSpecifier()
        block.ui {
            closeModal role.id, "${role}"
        }
        taackUiSimpleService.show(block)
    }

    @Secured("isAuthenticated()")
    def selectUserM2OCloseModal(User user) {
        UiBlockSpecifier block = new UiBlockSpecifier()
        block.ui {
            closeModal user.id, "${user}"
        }
        taackUiSimpleService.show(block)
    }

    def showUser(User u) {
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock "showUser", {
                    show "${u.username}", crewUiService.buildUserShow(u), BlockSpec.Width.MAX
                }
            }
        })

    }

    def showUserFromSearch() {
        User u = User.read(params.long('id'))
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            ajaxBlock "showUserFromSearch", {
                show "${u.username}", crewUiService.buildUserShow(u), BlockSpec.Width.MAX
            }
        }, buildMenu())
    }

    def updateUserMainPicture(User u) {
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock "updateUserMainPicture", {
                    form "Update a File", AttachmentUiService.buildAttachmentForm(Attachment.read(u.mainPictureId) ?: new Attachment(type: AttachmentType.mainPicture), this.&saveUserMainPicture as MethodClosure, [userId: u.id]), BlockSpec.Width.MAX
                }
            }
        })
    }

    def saveUserMainPicture() {
        def u = User.get(params.long("userId"))
        def cu = springSecurityService.currentUser as User
        if (cu.id == u.id || cu.authorities*.authority.contains("ROLE_ADMIN")) {
            Attachment a = null
            Attachment.withTransaction { TransactionStatus status ->
                a = attachmentUiService.saveAttachment()
                u.addToAttachments(a)
                status.flush()
            }
            taackSimpleSaveService.displayBlockOrRenderErrors(a, new UiBlockSpecifier().ui {
                closeModalAndUpdateBlock {
                    ajaxBlock "showUser", {
                        show "${u.username} [Updated]", crewUiService.buildUserShow(u, true), BlockSpec.Width.MAX
                    }
                }
            })
        } else {
            render "Forbidden"
        }
    }

    def userForm() {
        User user = User.read(params.long("id")) ?: new User(params)

        UiFormSpecifier f = new UiFormSpecifier()
        f.ui user, {
            hiddenField user.subsidiary_
            section "User", {
                field user.username_
                field user.firstName_
                field user.lastName_
                ajaxField user.manager_, this.&selectUserM2O as MC
                field user.password_
            }
            section "Coords", {
                field user.businessUnit_
                field user.mail_
                field user.subsidiary_
            }
            section "Status", {
                field user.enabled_
                field user.accountExpired_
                field user.accountLocked_
                field user.passwordExpired_
            }
            formAction "Save", this.&saveUser as MC, user.id, [redirectAction: params.get("redirectAction")], true
        }

        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            modal {
                ajaxBlock "userForm", {
                    form "User Form", f, BlockSpec.Width.MAX
                }
            }
        }
        taackUiSimpleService.show(b)
    }

    def userSelfEdit() {
        def connectedUSer = authenticatedUser as User
        boolean completeEdit = isAdmin()

        if (completeEdit) {
            chain action: "userForm", id: connectedUSer.id, params: [isAjax: true]
        } else {
            UiShowSpecifier s = new UiShowSpecifier()

        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def saveUser(String redirectAction) {
        MethodClosure red = (redirectAction ? this.&"$redirectAction" : this.&index) as MethodClosure
        taackSimpleSaveService.saveThenRedirectOrRenderErrors(User, redirectAction == "null" ? null : red)
    }

    def listAlerts() {
        boolean hasActions = isAdmin()
        UiTableSpecifier t = crewUiService.buildAlertTable()
        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            ajaxBlock "blockList", {
                table "Alerts", t, BlockSpec.Width.MAX, {
                    if (hasActions) action "Create alert", ActionIcon.CREATE, CrewController.&alertForm as MC, true
                }
            }
        }

        taackUiSimpleService.show(b, buildMenu())
    }

    @Secured("ROLE_ADMIN")
    def alertForm() {
        final Alert alert = Alert.read(params.long("id")) ?: new Alert(params)

        UiFormSpecifier f = new UiFormSpecifier()
        f.ui alert, {
            section "Section 1", {
                field alert.name_
                field alert.description_
                field alert.origin_, taackPluginService.enumOptions
            }
            section "Section 2", {
                ajaxField alert.restrictedRoles_, CrewController.&selectRoleM2O as MethodClosure, alert.restrictedRoles_
                ajaxField alert.users_, CrewController.&selectUserM2O as MethodClosure, alert.users_
                field alert.isSubscriptionEnabled_
                field alert.active_
            }
            formAction "Save", this.&saveAlert as MC, alert.id, null, true
        }
        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            modal {
                ajaxBlock "blockForm", {
                    form "Alert Form", f, BlockSpec.Width.MAX
                }
            }
        }
        taackUiSimpleService.show(b, buildMenu())
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def saveAlert() {
        taackSimpleSaveService.saveThenRedirectOrRenderErrors(Alert, this.&listAlerts as MC)
    }

    @Secured("ROLE_ADMIN")
    def editUserRoles(User user) {
        Role role = new Role()

        UiTableSpecifier t = new UiTableSpecifier()

        ColumnHeaderFieldSpec.SortableDirection dir
        t.ui Role, {
            header {
                column {
                    dir = sortableFieldHeader ColumnHeaderFieldSpec.DefaultSortingDirection.ASC, role.authority_
                }
                column {
                    fieldHeader "Action"
                }
            }
            def roles = taackSimpleFilterService.list(Role, 20, null, null, dir)
            paginate(20, params.long('offset'), roles.bValue)
            for (def r : roles.aValue) {
                row {
                    rowColumn {
                        rowField r.authority
                    }
                    rowColumn {
                        if (!UserRole.exists(user.id, r.id)) {
                            rowLink "Add ROLE", ActionIcon.ADD, this.&addRoleToUser as MC, [userId: user.id, roleId: r.id]
                        } else {
                            rowLink "Remove ROLE", ActionIcon.DELETE, this.&removeRoleToUser as MC, [userId: user.id, roleId: r.id]
                        }
                    }
                }
            }
        }

        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            modal !params.boolean("refresh"), {
                ajaxBlock "userRoleBlock", {
                    table "Edit User Role for ${user.username}", t, BlockSpec.Width.MAX
                }
            }
        }
        taackUiSimpleService.show(b, buildMenu())
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def addRoleToUser() {
        def ur = UserRole.create(User.read(params.long("userId")), Role.read(params.long("roleId")))
        if (ur.hasErrors()) log.error "${ur.errors}"
        chain(action: "editUserRoles", id: params.long("userId"), params: [refresh: true, isAjax: true, recordState: params['recordState']])
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def removeRoleToUser() {
        UserRole.remove(User.read(params.long("userId")), Role.read(params.long("roleId")))
        chain(action: "editUserRoles", id: params.long("userId"), params: [refresh: true, isAjax: true, recordState: params['recordState']])
    }

    def listRoles() {
        boolean hasActions = isAdmin()

        UiTableSpecifier t = new UiTableSpecifier()
        ColumnHeaderFieldSpec.SortableDirection dir
        t.ui UserRole, {
            header {
                column {
                    dir = sortableFieldHeader "Role Name", new Role().authority_, ColumnHeaderFieldSpec.DefaultSortingDirection.ASC
                }
                column {
                    fieldHeader "Users"
                }
                if (hasActions) {
                    column {
                        fieldHeader "Edit"
                    }
                }
            }

            def roles = taackSimpleFilterService.list(Role, 20, null, null, dir)
            paginate(20, params.long('offset'), roles.bValue)
            for (def r : roles.aValue) {
                row {
                    rowColumn {
                        rowField r.authority_
                    }
                    rowColumn {
                        String userList = (UserRole.findAllByRole(r) as List<UserRole>)*.user.username.join(', ')
                        rowField userList
                    }
                    if (hasActions) {
                        rowColumn {
                            rowLink "Edit Role", ActionIcon.EDIT, this.&roleForm as MC, r.id
                        }
                    }
                }
            }
        }
        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            ajaxBlock "blockList", {
                table "Roles", t, BlockSpec.Width.MAX, {
                    if (hasActions) action "Create Role", ActionIcon.CREATE, CrewController.&roleForm as MC, true
                }
            }
        }

        taackUiSimpleService.show(b, buildMenu())
    }

    def roleForm() {
        Role role = Role.read(params.long("id")) ?: new Role(params)

        UiFormSpecifier f = new UiFormSpecifier()
        f.ui role, {
            section "Main", {
                field role.authority_
            }
            formAction "Save", this.&saveRole as MC, role.id, null, true
        }
        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            modal {
                ajaxBlock "blockForm", {
                    form "Role Form", f, BlockSpec.Width.MAX
                }
            }
        }
        taackUiSimpleService.show(b, buildMenu())
    }

    @Secured(["ROLE_ADMIN", "ROLE_SWITCH_USER"])
    def switchUser(User user) {
        render """
   <form action='/login/impersonate' method='POST'>
      Switch to user: <input type='text' name='username' value="${user.username}"/> <br/>
      <input type='submit' value='Switch'/>
   </form>
        """
    }

    @Secured(["ROLE_ADMIN", "ROLE_SWITCH_USER"])
    def replaceUser(User user) {
        render """
   <form action='doReplaceUser' method='POST'>
      Replace user: <input type='text' name='userFrom' value="${user.username}"/> <br/>
      By user: <input type='text' name='userTo' value=""/> <br/>
      <input type='submit' value='Replace'/>
   </form>
        """
    }

    TaackMetaModelService taackMetaModelService

    @Secured(["ROLE_ADMIN", "ROLE_SWITCH_USER"])
    @Transactional
    def doReplaceUser() {
        User userFrom = User.findByUsername(params['userFrom'])
        User userTo = User.findByUsername(params['userTo'])
        User.withNewTransaction {
            (UserRole.findAllByUser(userFrom) as List<UserRole>)*.delete()
        }
        taackMetaModelService.replaceEntity(userFrom, userTo)
        render 'Done'
    }

    @Secured(["ROLE_ADMIN", "ROLE_SWITCH_USER"])
    def deleteUser(User user) {
        try {
            User.withNewTransaction {
                user.delete()
            }
        } catch(e) {
            log.error(e.message)
            render("Error: ${e.message}")
        }
        render 'Done'
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def saveRole() {
        taackSimpleSaveService.saveThenRedirectOrRenderErrors(Role, this.&listRoles as MC)
    }

    @Transactional
    def subscribeToAlert(Alert alert) {
        def cu = springSecurityService.currentUser as User
        String controllerRedirect = params.get("controllerRedirect") ?: controllerName
        String actionRedirect = params.get("actionRedirect") ?: 'listAlerts'
        String subsidiaryRedirect = params.get("subsidiaryRedirect") ?: null
        if (alert.canSubscribe(cu)) {
            alert.addToUsers(cu)
            alert.save(failOnError: true)
            redirect controller: controllerRedirect, action: actionRedirect, params: [subsidiary: subsidiaryRedirect]
        } else {
            throw new AccessDeniedException("No access")
        }
    }

    @Transactional
    def unsubscribeFromAlert(Alert alert) {
        def cu = springSecurityService.currentUser as User
        String controllerRedirect = params.get("controllerRedirect") ?: controllerName
        String actionRedirect = params.get("actionRedirect") ?: 'listAlerts'
        String subsidiaryRedirect = params.get("subsidiaryRedirect") ?: null
        if (alert.canSubscribe(cu)) {
            alert.removeFromUsers(cu)
            alert.save(failOnError: true)
            redirect controller: controllerRedirect, action: actionRedirect, params: [subsidiary: subsidiaryRedirect]
        } else {
            throw new AccessDeniedException("No access")
        }
    }
}
