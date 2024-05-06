package crew


import app.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import grails.web.api.WebAttributes
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.taack.Attachment
import org.taack.Role
import org.taack.User
import org.taack.UserRole
import taack.domain.TaackFilter
import taack.domain.TaackFilterService
import taack.domain.TaackMetaModelService
import taack.domain.TaackSaveService
import taack.render.TaackUiService
import taack.ui.base.*
import taack.ui.base.block.BlockSpec
import taack.ui.base.common.ActionIcon
import taack.ui.base.common.IconStyle
import taack.ui.base.filter.expression.FilterExpression
import taack.ui.base.filter.expression.Operator
import taack.ui.base.form.FormSpec

@GrailsCompileStatic
@Secured(['isAuthenticated()'])
class CrewController implements WebAttributes {
    TaackUiService taackUiService
    TaackFilterService taackFilterService
    TaackSaveService taackSaveService
    SpringSecurityService springSecurityService
    CrewUiService crewUiService
    CrewSearchService crewSearchService
    CrewSecurityService crewSecurityService
    CrewPdfService crewPdfService

    private UiMenuSpecifier buildMenu(String q = null) {
        new UiMenuSpecifier().ui {
            menu CrewController.&index as MC
            menu CrewController.&listRoles as MC
            menu CrewController.&hierarchy as MC
            menuIcon ActionIcon.CONFIG_USER, this.&editUser as MC
            menuIcon ActionIcon.EXPORT_PDF, this.&downloadBinPdf as MC
            menuSearch this.&search as MethodClosure, q
            menuOptions(SupportedLanguage.fromContext())
        }
    }

    private UiTableSpecifier buildUserTableHierarchy(final User u) {

        def groups = taackFilterService.getBuilder(User).build().listGroup()

        boolean hasActions = crewSecurityService.admin

        new UiTableSpecifier().ui {
            header {
                column {
                    fieldHeader u.username_
                    groupFieldHeader u.businessUnit_
                }
                column {
                    groupFieldHeader u.subsidiary_
                    fieldHeader u.manager_
                }
                column {
                    fieldHeader u.lastName_
                    fieldHeader u.firstName_
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
                                if (hasActions) rowAction ActionIcon.EDIT * IconStyle.SCALE_DOWN, this.&editUser as MC, mu.id
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
                    rowGroupHeader g as String
                    rec(taackFilterService.getBuilder(User).build().listInGroup(g, new UiFilterSpecifier().ui(User, {
                        filterFieldExpressionBool new FilterExpression(true, Operator.EQ, filterUser.enabled_)
                    })).aValue, 0)
                    rowGroupFooter "Count: ${count - oldCount}"
                }
            } else {
                rec(User.findAllByManagerIsNullAndEnabled(true), 0)
            }
        }
    }

    def search(String q) {
        taackUiService.show(crewSearchService.buildSearchBlock(q), buildMenu(q))
    }

    def hierarchy() {
        UiTableSpecifier t = buildUserTableHierarchy(new User(enabled: true))
        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            table "Users Hierarchy", t, BlockSpec.Width.MAX
        }

        taackUiService.show(b, buildMenu())
    }

    def index() {
        User cu = authenticatedUser as User

        UiFilterSpecifier f = CrewUiService.buildUserTableFilter cu
        UiTableSpecifier t = crewUiService.buildUserTable f

        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            tableFilter 'Filter', f, 'Users', t, BlockSpec.Width.MAX, {
                action ActionIcon.CREATE, CrewController.&editUser as MC
            }
        }
        taackUiService.show(b, buildMenu())
    }

    def selectRoleM2O() {
        UiFilterSpecifier f = CrewUiService.buildRoleTableFilter()
        UiTableSpecifier t = crewUiService.buildRoleTable f, true

        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            modal !params.boolean("refresh"), {
                tableFilter "Filter", f, "Roles", t, BlockSpec.Width.MAX
            }
        }

        taackUiService.show(b)
    }

    def selectUserM2O() {
        User cu = springSecurityService.currentUser as User

        UiFilterSpecifier f = CrewUiService.buildUserTableFilter cu
        UiTableSpecifier t = crewUiService.buildUserTable f, true

        taackUiService.show new UiBlockSpecifier().ui {
            modal {
                tableFilter "Filter", f, "Users", t, BlockSpec.Width.MAX
            }
        }
    }

    def showUser(User u) {
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                show u.username, crewUiService.buildUserShow(u), BlockSpec.Width.MAX
            }
        })

    }

    def showUserFromSearch() {
        User u = User.read(params.long('id'))
        taackUiService.show(new UiBlockSpecifier().ui {
            show u.username, crewUiService.buildUserShow(u), BlockSpec.Width.MAX
        }, buildMenu())
    }

    def editUser(User user) {
        user ?= new User(params)

        UiFormSpecifier f = new UiFormSpecifier()
        f.ui user, {
            section "User", FormSpec.Width.ONE_THIRD, {
                field user.username_
                field user.firstName_
                field user.lastName_
                ajaxField user.manager_, this.&selectUserM2O as MC
                ajaxField user.mainPicture_, this.&selectUserMainPicture as MC
                field user.password_
            }
            section "Coords", FormSpec.Width.ONE_THIRD, {
                field user.businessUnit_
                field user.mail_
                field user.subsidiary_
            }
            section "Status", FormSpec.Width.ONE_THIRD, {
                field user.enabled_
                field user.accountExpired_
                field user.accountLocked_
                field user.passwordExpired_
            }
            formAction this.&saveUser as MC, user.id
        }

        taackUiService.show new UiBlockSpecifier().ui {
            modal {
                form f, BlockSpec.Width.MAX
            }
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def saveUser() {
        params.password = springSecurityService.encodePassword(params.password as String)
        taackSaveService.saveThenReloadOrRenderErrors(User)
    }

    @Secured("ROLE_ADMIN")
    def editUserRoles(User user) {
        Role role = new Role()

        UiTableSpecifier t = new UiTableSpecifier().ui {
            header {
                column {
                    sortableFieldHeader role.authority_
                }
                column {
                    fieldHeader "Action"
                }
            }
            iterate(taackFilterService.getBuilder(Role)
                    .setMaxNumberOfLine(20)
                    .setSortOrder(TaackFilter.Order.DESC, role.authority_).build()) { Role r ->
                row {
                    rowColumn {
                        rowField r.authority
                    }
                    rowColumn {
                        if (!UserRole.exists(user.id, r.id)) {
                            rowAction ActionIcon.ADD, this.&addRoleToUser as MC, [userId: user.id, roleId: r.id]
                        } else {
                            rowAction ActionIcon.DELETE, this.&removeRoleToUser as MC, [userId: user.id, roleId: r.id]
                        }
                    }
                }
            }
        }

        taackUiService.show(new UiBlockSpecifier().ui {
            modal !params.boolean("refresh"), {
                table "Edit User Role for ${user.username}", t, BlockSpec.Width.MAX
            }
        }, buildMenu())
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

    @Transactional
    def selectUserMainPicture() {
        def ad = crewSecurityService.mainPictureAttachmentDescriptor
        def a = new Attachment()
        a.attachmentDescriptor = ad

        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form(
                        new UiFormSpecifier().ui(a, {
                            hiddenField a.attachmentDescriptor_
                            field a.filePath_
                            formAction(this.&selectUserMainPictureCloseModal as MC)
                        })
                )
            }
        })
    }

    @Transactional
    def selectUserMainPictureCloseModal() {
        def a = taackSaveService.save(Attachment)
        taackSaveService.displayBlockOrRenderErrors(a, new UiBlockSpecifier().ui {
            closeModal(a.id, a.toString())
        })
    }

    def listRoles() {
        boolean hasActions = crewSecurityService.admin

        UiTableSpecifier t = new UiTableSpecifier()
        t.ui {
            header {
                column {
                    sortableFieldHeader new Role().authority_
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

            iterate(taackFilterService.getBuilder(Role)
                    .setMaxNumberOfLine(20)
                    .setSortOrder(TaackFilter.Order.DESC, new Role().authority_)
                    .build()) { Role r ->
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
                            rowAction ActionIcon.EDIT * IconStyle.SCALE_DOWN, this.&roleForm as MC, r.id
                        }
                    }
                }
            }
        }
        UiBlockSpecifier b = new UiBlockSpecifier().ui {
            table "Roles", t, BlockSpec.Width.MAX, {
                if (hasActions) action ActionIcon.CREATE, CrewController.&roleForm as MC
            }
        }
        taackUiService.show(b, buildMenu())
    }

    def roleForm() {
        Role role = Role.read(params.long("id")) ?: new Role(params)

        UiFormSpecifier f = new UiFormSpecifier()
        f.ui role, {
            field role.authority_
            formAction this.&saveRole as MC, role.id
        }
        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            modal {
                form f, BlockSpec.Width.MAX
            }
        }
        taackUiService.show(b, buildMenu())
    }

    @Secured(["ROLE_ADMIN", "ROLE_SWITCH_USER"])
    def switchUser(User user) {
        render """
   <html>
   <form action='/login/impersonate' method='POST'>
      Switch to user: <input type='text' name='username' value="${user.username}"/> <br/>
      <input type='submit' value='Switch'/>
   </form>
   </html>
        """
    }

    @Secured(["ROLE_ADMIN", "ROLE_SWITCH_USER"])
    def replaceUser(User user) {
        render """
   <html>
   <form action='doReplaceUser' method='POST'>
      Replace user: <input type='text' name='userFrom' value="${user.username}"/> <br/>
      By user: <input type='text' name='userTo' value=""/> <br/>
      <input type='submit' value='Replace'/>
   </form>
   </html>
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
        } catch (e) {
            log.error(e.message)
            render("Error: ${e.message}")
        }
        render 'Done'
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def saveRole() {
        taackSaveService.saveThenRedirectOrRenderErrors(Role, this.&listRoles as MC)
    }

    def downloadBinPdf(Boolean isHtml) {
        taackUiService.downloadPdf(crewPdfService.buildPdfHierarchy(), 'UserHierarchy', isHtml)
    }
}
