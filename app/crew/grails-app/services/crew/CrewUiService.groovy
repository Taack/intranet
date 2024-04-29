package crew

import app.config.AttachmentType
import attachement.AttachmentUiService
import grails.compiler.GrailsCompileStatic
import grails.web.api.WebAttributes
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.taack.Attachment
import org.taack.Role
import org.taack.User
import org.taack.UserRole
import taack.domain.TaackFilter
import taack.domain.TaackFilterService
import taack.ui.base.UiBlockSpecifier
import taack.ui.base.UiFilterSpecifier
import taack.ui.base.UiShowSpecifier
import taack.ui.base.UiTableSpecifier
import taack.ui.base.common.ActionIcon
import taack.ui.base.common.IconStyle
import taack.ui.base.filter.expression.FilterExpression
import taack.ui.base.filter.expression.Operator

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
                row {
                    rowColumn {
                        rowField r.authority
                        if (hasSelect)
                            rowAction tr('default.role.label'), ActionIcon.SELECT * IconStyle.SCALE_DOWN, r.id, r.toString()
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
                        fieldHeader tr('picture.header.label')
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
                    fieldHeader tr('default.roles.label')
                }
            }
            boolean canSwitchUser = crewSecurityService.canSwitchUser()

            TaackFilter tf = taackFilterService.getBuilder(User).setSortOrder(TaackFilter.Order.DESC, u.dateCreated_)
                    .setMaxNumberOfLine(20).addFilter(f).build()

            iterate(tf) { User ru ->
                row {
                    boolean hasActions = crewSecurityService.canEdit(ru)
                    if (!hasSelect) {
                        Attachment picture = ru.attachments.find { it.attachmentDescriptor.type == AttachmentType.mainPicture }
                        rowColumn {
                            rowField attachmentUiService.preview(picture?.id)
                        }
                    }
                    rowColumn {
                        rowAction ActionIcon.SHOW * IconStyle.SCALE_DOWN, CrewController.&showUser as MC, ru.id
                        if (hasSelect)
                            rowAction "Select User", ActionIcon.SELECT * IconStyle.SCALE_DOWN, ru.id, ru.toString()
                        else if (hasActions) {
                            rowAction ActionIcon.EDIT * IconStyle.SCALE_DOWN, CrewController.&editUser as MC, ru.id
                            if (canSwitchUser && ru.enabled)
                                rowAction ActionIcon.SHOW * IconStyle.SCALE_DOWN, CrewController.&switchUser as MC, ru.id
                            else if (canSwitchUser && !ru.enabled) {
                                rowAction ActionIcon.MERGE * IconStyle.SCALE_DOWN, CrewController.&replaceUser as MC, ru.id
                                rowAction ActionIcon.DELETE * IconStyle.SCALE_DOWN, CrewController.&deleteUser as MC, ru.id
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
    }

    static UiBlockSpecifier messageBlock(String message) {
        new UiBlockSpecifier().ui {
            modal {
                custom "Message", message
            }
        }
    }

    UiShowSpecifier buildUserShow(User u, boolean update = false) {
        new UiShowSpecifier().ui(u, {
            field "Picture", attachmentUiService.previewFull(u.mainPictureId, update ? "${System.currentTimeMillis()}" : null)
            showAction CrewController.&updateUserMainPicture as MC, u.id
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
