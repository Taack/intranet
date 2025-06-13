package crew

import attachment.Attachment
import attachment.DocumentCategory
import attachment.config.DocumentCategoryEnum
import crew.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import grails.web.api.WebAttributes
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.MethodClosure as MC
import taack.domain.TaackFilter
import taack.domain.TaackFilterService
import taack.domain.TaackMetaModelService
import taack.domain.TaackSaveService
import taack.render.TaackUiPdfService
import taack.render.TaackUiService
import taack.ui.dsl.*
import taack.ui.dsl.common.ActionIcon
import taack.ui.dsl.common.IconStyle

@GrailsCompileStatic
@Secured(['isAuthenticated()'])
class CrewController implements WebAttributes {
    TaackUiService taackUiService
    TaackUiPdfService taackUiPdfService
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
            menuIcon ActionIcon.EXPORT_CSV, this.&downloadBinPdf as MC
            menuIcon ActionIcon.EXPORT_PDF, this.&downloadBinPdf2 as MC
            menuSearch this.&search as MethodClosure, q
            menuOptions(SupportedLanguage.fromContext())
        }
    }

    private UiTableSpecifier buildUserTableHierarchy(final User u) {
        boolean hasActions = crewSecurityService.admin

        new UiTableSpecifier().ui {
            header {
                column {
                    label u.username_
                    sortableFieldHeader u.businessUnit_
                }
                column {
                    sortableFieldHeader u.subsidiary_
                    label u.manager_
                }
                column {
                    label u.lastName_
                    label u.firstName_
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

            rec(User.findAllByManagerIsNullAndEnabled(true), 0)
        }
    }

    def search(String q) {
        taackUiService.show(crewSearchService.buildSearchBlock(q), buildMenu(q))
    }

    def hierarchy() {
        UiTableSpecifier t = buildUserTableHierarchy(new User(enabled: true))
        UiBlockSpecifier b = new UiBlockSpecifier().ui {
            table t
        }
        taackUiService.show(b, buildMenu())
    }

    def index() {
        User cu = authenticatedUser as User

        UiFilterSpecifier f = CrewUiService.buildUserTableFilter cu
        UiTableSpecifier t = crewUiService.buildUserTable f

        UiBlockSpecifier b = new UiBlockSpecifier().ui {
            tableFilter f, t, {
                menuIcon ActionIcon.CREATE, CrewController.&editUser as MC
            }
        }
        taackUiService.show(b, buildMenu())
    }

    def selectRoleM2O() {
        UiFilterSpecifier f = CrewUiService.buildRoleTableFilter()
        UiTableSpecifier t = crewUiService.buildRoleTable f, true

        taackUiService.show new UiBlockSpecifier().ui {
            modal {
                tableFilter f, t
            }
        }
    }

    def selectUserM2O() {
        User cu = springSecurityService.currentUser as User

        UiFilterSpecifier f = CrewUiService.buildUserTableFilter cu
        UiTableSpecifier t = crewUiService.buildUserTable f, true

        taackUiService.show new UiBlockSpecifier().ui {
            modal {
                tableFilter f, t
            }
        }
    }

    def showUser(User u) {
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                show crewUiService.buildUserShow(u)
            }
        })
    }

    def showUserFromSearch() {
        User u = User.read(params.long('id'))
        taackUiService.show(new UiBlockSpecifier().ui {
            show crewUiService.buildUserShow(u)
        }, buildMenu())
    }

    def editUser(User user) {
        user ?= new User(enabled: true)

        UiFormSpecifier f = new UiFormSpecifier().ui user, {
            row {
                col {
                    section "User", {
                        field user.username_
                        field user.firstName_
                        field user.lastName_
                        ajaxField user.manager_, this.&selectUserM2O as MC
                        ajaxField user.mainPicture_, this.&selectUserMainPicture as MC
                        field user.password_
                    }
                }
                col {
                    section "Coords", {
                        field user.businessUnit_
                        field user.mail_
                        field user.subsidiary_
                    }
                }
                col {
                    section "Status", {
                        field user.enabled_
                        field user.accountExpired_
                        field user.accountLocked_
                        field user.passwordExpired_
                    }
                }
            }
            formAction this.&saveUser as MC, user?.id
        }

        taackUiService.show new UiBlockSpecifier().ui {
            modal {
                form f
            }
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def saveUser() {
        User u = User.get(params.long('id'))
        if ((u && u.password != params.password) || !u) {
            params.password = springSecurityService.encodePassword(params.password as String)
        }
        taackSaveService.saveThenReloadOrRenderErrors(User)
    }

    @Secured("ROLE_ADMIN")
    def editUserRoles(User user) {
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock 'editUserRolesClick', {
                    table crewUiService.buildRoleTable(user)
                }
            }
        })
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def addRoleToUser() {
        def ur = UserRole.create(User.read(params.long("userId")), Role.read(params.long("roleId")))
        if (ur.hasErrors()) log.error "${ur.errors}"
        chain(action: "editUserRoles", id: params.long("userId"), params: [isAjax: true, refresh: true])
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    def removeRoleToUser() {
        UserRole.remove(User.read(params.long("userId")), Role.read(params.long("roleId")))
        chain(action: "editUserRoles", id: params.long("userId"), params: [isAjax: true, refresh: true])
    }

    @Transactional
    def selectUserMainPicture() {
        def ad = crewSecurityService.mainPictureDocumentAccess
        def dc = DocumentCategory.findOrSaveByCategory(DocumentCategoryEnum.OTHER)
        if (!dc.id) dc.save(flush: true)
        def a = new Attachment(documentCategory: dc)
        a.documentAccess = ad
        a.documentCategory = dc

        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form(
                        new UiFormSpecifier().ui(a, {
                            hiddenField a.documentAccess_
                            hiddenField a.documentCategory_
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
                    label "Users"
                }
                if (hasActions) {
                    column {
                        label "Edit"
                    }
                }
            }

            iterate(taackFilterService.getBuilder(Role)
                    .setMaxNumberOfLine(20)
                    .setSortOrder(TaackFilter.Order.DESC, new Role().authority_)
                    .build()) { Role r ->
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
        UiBlockSpecifier b = new UiBlockSpecifier().ui {
            table t, {
                if (hasActions) menuIcon ActionIcon.CREATE, CrewController.&roleForm as MC
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
                form f
            }
        }
        taackUiService.show(b)
    }

    @Secured(["ROLE_ADMIN", "ROLE_SWITCH_USER"])
    def switchUser(User user) {
        render """\
   <!doctype html>
   <html>
   <form action='/login/impersonate' method='POST'>
      Switch to user: <input type='text' name='username' value="${user.username}"/> <br/>
      <input type='submit' value='Switch'/>
   </form>
   </html>
   """.stripIndent()
    }

    @Secured(["ROLE_ADMIN", "ROLE_SWITCH_USER"])
    def replaceUser(User user) {
        render """\
   <!doctype html>
   <html>
   <form action='doReplaceUser' method='POST'>
      Replace user: <input type='text' name='userFrom' value="${user.username}"/> <br/>
      By user: <input type='text' name='userTo' value=""/> <br/>
      <input type='submit' value='Replace'/>
   </form>
   </html>
   """.stripIndent()
    }

    TaackMetaModelService taackMetaModelService

    @Secured(["ROLE_ADMIN", "ROLE_SWITCH_USER"])
    @Transactional
    def doReplaceUser() {
        User userFrom = User.findByUsername(params.userFrom as String)
        User userTo = User.findByUsername(params.userTo as String)
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

    def downloadBinPdf() {
        taackUiPdfService.downloadPdf(crewPdfService.buildPdfHierarchy(), 'UserHierarchy', true)
    }

    def downloadBinPdf2() {
        taackUiPdfService.downloadPdf(crewPdfService.buildPdfHierarchy(), 'UserHierarchy', false)
    }
}
