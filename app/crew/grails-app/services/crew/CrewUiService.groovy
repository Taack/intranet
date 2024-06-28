package crew

import attachement.AttachmentUiService
import grails.compiler.GrailsCompileStatic
import grails.web.api.WebAttributes
import org.codehaus.groovy.runtime.MethodClosure as MC
import taack.domain.TaackFilter
import taack.domain.TaackFilterService
import taack.ui.dsl.UiBlockSpecifier
import taack.ui.dsl.UiFilterSpecifier
import taack.ui.dsl.UiShowSpecifier
import taack.ui.dsl.UiTableSpecifier
import taack.ui.dsl.common.ActionIcon
import taack.ui.dsl.common.IconStyle
import taack.ui.dsl.filter.expression.FilterExpression
import taack.ui.dsl.filter.expression.Operator

import static taack.render.TaackUiService.tr

@GrailsCompileStatic
class CrewUiService implements WebAttributes {

    TaackFilterService taackFilterService
    AttachmentUiService attachmentUiService
    CrewSecurityService crewSecurityService

    static UiFilterSpecifier buildRoleTableFilter(Role role = null) {
        role ?= new Role()
        new UiFilterSpecifier().ui Role, {
            section tr('default.role.label'), {
                filterField role.authority_
            }
        }
    }

    static UiFilterSpecifier buildUserTableFilter(final User cu, User user = null) {
        User u = user ?: new User(manager: new User(), enabled: true)

        new UiFilterSpecifier().ui User, {
            section tr('default.user.label'), {
                filterField u.username_
                filterField u.lastName_
                filterField u.firstName_
                filterField u.manager_, u.manager.username_
                filterField u.subsidiary_
                filterField u.businessUnit_
                filterField u.enabled_
                filterFieldExpressionBool tr('user.myTeam.label'), user ? false : true, new FilterExpression(cu.allManagedUsers*.id, Operator.IN, u.selfObject_)
            }
            section tr('default.role.label'), {
                UserRole ur = new UserRole(role: new Role())
                filterFieldInverse tr('default.role.label'), UserRole, ur.user_, ur.role_, ur.role.authority_
            }
        }
    }

    UiTableSpecifier buildRoleTable(final UiFilterSpecifier f, final boolean hasSelect = false) {
        Role u = new Role()

        new UiTableSpecifier().ui {
            header {
                column {
                    sortableFieldHeader u.authority_
                }
            }

            iterate(taackFilterService.getBuilder(Role)
                    .setMaxNumberOfLine(20)
                    .setSortOrder(TaackFilter.Order.DESC, u.authority_)
                    .build()) { Role r, Long counter ->
                rowColumn {
                    rowField r.authority
                    if (hasSelect)
                        rowAction tr('default.role.label'), ActionIcon.SELECT * IconStyle.SCALE_DOWN, r.id, r.toString()
                }
            }
        }
    }



    UiTableSpecifier buildRoleTable(User user) {
        Role role = new Role()
        new UiTableSpecifier().ui {
            header {
                column {
                    sortableFieldHeader role.authority_
                }
                column {
                    label "Action"
                }
            }
            iterate(taackFilterService.getBuilder(Role)
                    .setMaxNumberOfLine(20)
                    .setSortOrder(TaackFilter.Order.DESC, role.authority_).build()) { Role r ->
                rowColumn {
                    rowField r.authority
                }
                rowColumn {
                    if (!UserRole.exists(user.id, r.id)) {
                        rowAction ActionIcon.ADD, CrewController.&addRoleToUser as MC, [userId: user.id, roleId: r.id]
                    } else {
                        rowAction ActionIcon.DELETE, CrewController.&removeRoleToUser as MC, [userId: user.id, roleId: r.id]
                    }
                }
            }
        }

    }

        UiTableSpecifier buildUserTable(final UiFilterSpecifier f, final boolean hasSelect = false) {

        new UiTableSpecifier().ui {
            User u = new User(manager: new User(), enabled: true)
            header {
                if (!hasSelect) {
                    column {
                        label tr('picture.header.label')
                    }
                }
                column {
                    sortableFieldHeader u.username_
                    sortableFieldHeader u.dateCreated_
                }
                column {
                    sortableFieldHeader u.subsidiary_
                    sortableFieldHeader u.manager_, u.manager.username_
                }
                column {
                    sortableFieldHeader u.lastName_
                    sortableFieldHeader u.firstName_
                }
                column {
                    label tr('default.roles.label')
                }
            }
            boolean canSwitchUser = crewSecurityService.canSwitchUser()

            TaackFilter tf = taackFilterService.getBuilder(User).setSortOrder(TaackFilter.Order.DESC, u.dateCreated_)
                    .setMaxNumberOfLine(10).addFilter(f).build()

            iterate tf, { User ru ->
                boolean hasActions = this.crewSecurityService.canEdit(ru)
                if (!hasSelect) {
                    rowColumn {
                        rowField this.attachmentUiService.preview(ru.mainPicture?.id)
                    }
                }
                rowColumn {
                    if (hasSelect)
                        rowAction "User", ActionIcon.SELECT * IconStyle.SCALE_DOWN, ru.id, ru.toString()
                    else {
                        rowAction ActionIcon.SHOW * IconStyle.SCALE_DOWN, CrewController.&showUser as MC, ru.id
                        if (hasActions) {
                            rowAction ActionIcon.EDIT * IconStyle.SCALE_DOWN, CrewController.&editUser as MC, ru.id
                            if (canSwitchUser && ru.enabled)
                                rowAction ActionIcon.SHOW * IconStyle.SCALE_DOWN, CrewController.&switchUser as MC, ru.id
                            else if (canSwitchUser && !ru.enabled) {
                                rowAction ActionIcon.MERGE * IconStyle.SCALE_DOWN, CrewController.&replaceUser as MC, ru.id
                                rowAction ActionIcon.DELETE * IconStyle.SCALE_DOWN, CrewController.&deleteUser as MC, ru.id
                            }
                        }
                    }

                    rowField ru.username_
                    rowField ru.dateCreated_
                }
                rowColumn {
                    rowField ru.subsidiary_
                    rowField ru.manager?.username
                }
                rowColumn {
                    rowField ru.lastName_
                    rowField ru.firstName_
                }
                rowColumn {
                    if (hasActions && !hasSelect)
                        rowAction ActionIcon.EDIT * IconStyle.SCALE_DOWN, CrewController.&editUserRoles as MC, ru.id
                    rowField ru.authorities*.authority.join(', ')
                }
            }
        }
    }

    static UiBlockSpecifier messageBlock(String message) {
        new UiBlockSpecifier().ui {
            modal {
                custom message
            }
        }
    }

    UiShowSpecifier buildUserShow(User u, boolean update = false) {
        new UiShowSpecifier().ui(u, {
            field "Picture", this.attachmentUiService.previewFull(u.mainPicture?.id, update ? "${System.currentTimeMillis()}" : null)
            fieldLabeled u.username_
            fieldLabeled u.firstName_
            fieldLabeled u.lastName_
            fieldLabeled u.businessUnit_
            fieldLabeled u.subsidiary_
            fieldLabeled u.mail_
            fieldLabeled u.manager_
        })
    }

}
