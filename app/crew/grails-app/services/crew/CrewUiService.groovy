package crew

import app.config.AttachmentType
import attachement.AttachmentUiService
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import grails.web.api.WebAttributes
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.taack.Attachment
import org.taack.Role
import org.taack.User
import org.taack.UserRole
import taack.base.TaackSimpleFilterService
import taack.ui.base.UiBlockSpecifier
import taack.ui.base.UiFilterSpecifier
import taack.ui.base.UiShowSpecifier
import taack.ui.base.UiTableSpecifier
import taack.ui.base.common.ActionIcon
import taack.ui.base.common.IconStyle
import taack.ui.base.filter.expression.FilterExpression
import taack.ui.base.filter.expression.Operator
import taack.ui.base.table.ColumnHeaderFieldSpec

import static taack.base.TaackUiSimpleService.tr

@GrailsCompileStatic
class CrewUiService implements WebAttributes {
    TaackSimpleFilterService taackSimpleFilterService
    SpringSecurityService springSecurityService
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
                filterFieldExpressionBool tr('user.myTeam.label'), new FilterExpression(cu.allManagedUsers*.id, Operator.IN, u.selfObject_), user ? false : true
            }
            section tr('default.role.label'), {
                UserRole ur = new UserRole(role: new Role())
                filterFieldInverse tr('default.role.label'), UserRole, ur.user_, ur.role_, ur.role.authority_
            }
        }
    }

    UiTableSpecifier buildRoleTable(final UiFilterSpecifier f, final boolean hasSelect = false) {
        Role u = new Role()

        ColumnHeaderFieldSpec.SortableDirection defaultDirection
        new UiTableSpecifier().ui Role, {
            header {
                column {
                    defaultDirection = sortableFieldHeader ColumnHeaderFieldSpec.DefaultSortingDirection.ASC, u.authority_
                }
            }

            def roles = taackSimpleFilterService.list(Role, 10, f, null, defaultDirection)

            for (Role r : roles.aValue) {
                row {
                    rowColumn {
                        rowField r.authority
                        if (hasSelect) rowLink tr('default.role.label'), ActionIcon.SELECT * IconStyle.SCALE_DOWN, r.id, r.toString(), true
                    }
                }
            }
            paginate(10, params.long("offset"), roles.bValue)
        }
    }

    UiTableSpecifier buildUserTable(final UiFilterSpecifier f, final boolean hasSelect = false) {
        User u = new User(manager: new User(), enabled: true)

        ColumnHeaderFieldSpec.SortableDirection defaultDirection

        new UiTableSpecifier().ui User, {
            header {
                if (!hasSelect) {
                    column {
                        fieldHeader tr('picture.header.label')
                    }
                }
                column {
                    sortableFieldHeader u.username_
                    defaultDirection = sortableFieldHeader ColumnHeaderFieldSpec.DefaultSortingDirection.DESC, u.dateCreated_
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

            def users = taackSimpleFilterService.list(User, 10, f, null, defaultDirection)

            boolean canSwitchUser = crewSecurityService.canSwitchUser()
            for (User ru : users.aValue) {
                boolean hasActions = crewSecurityService.canEdit(ru)
                row {
                    if (!hasSelect) {
                        Attachment picture = ru.attachments.find { it.type == AttachmentType.mainPicture }
                        rowColumn {
                            rowField attachmentUiService.preview(picture?.id)
                        }
                    }
                    rowColumn {
                        rowLink tr('show.user.label'), ActionIcon.SHOW * IconStyle.SCALE_DOWN, CrewController.&showUser as MC, ru.id, true
                        if (hasSelect) rowLink "Select User", ActionIcon.SELECT * IconStyle.SCALE_DOWN, ru.id, ru.toString(), true
                        else if (hasActions) {
                            rowLink "Edit User", ActionIcon.EDIT * IconStyle.SCALE_DOWN, CrewController.&editUser as MC, ru.id
                            if (canSwitchUser && ru.enabled) rowLink "Switch User", ActionIcon.SHOW * IconStyle.SCALE_DOWN, CrewController.&switchUser as MC, [id: ru.id], false
                            else if (canSwitchUser && !ru.enabled) {
                                rowLink "Replace By User", ActionIcon.MERGE * IconStyle.SCALE_DOWN, CrewController.&replaceUser as MC, ru.id, false
                                rowLink "Remove User", ActionIcon.DELETE * IconStyle.SCALE_DOWN, CrewController.&deleteUser as MC, ru.id, false
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
                        if (hasActions && !hasSelect) rowLink "Edit Roles", ActionIcon.EDIT * IconStyle.SCALE_DOWN, CrewController.&editUserRoles as MC, ru.id, true
                        rowField ru.authorities*.authority.join(', ')
                    }
                }
            }
            paginate(10, params.long("offset"), users.bValue)
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
            field "Picture", attachmentUiService.previewFull(u.mainPictureId, update ? "${System.currentTimeMillis()}": null)
            showAction "Change Picture", CrewController.&updateUserMainPicture as MC, u.id, true
            field "User Name", u.username
            field "First Name", u.firstName
            field "Last Name", u.lastName
            field "BU", u.businessUnit?.toString()
            field "Main Subsidiary", u.subsidiary?.toString()
            field "Mail", u.mail
            field "Manager", u.manager?.toString()
        })
    }

}
