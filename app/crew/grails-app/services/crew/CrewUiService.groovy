package crew

import app.config.AttachmentType
import attachement.AttachmentUiService
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import grails.web.api.WebAttributes
import org.codehaus.groovy.runtime.MethodClosure
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
import taack.ui.base.common.ActionIconStyleModifier
import taack.ui.base.filter.expression.FilterExpression
import taack.ui.base.filter.expression.Operator
import taack.ui.base.table.ColumnHeaderFieldSpec

@GrailsCompileStatic
class CrewUiService implements WebAttributes {
    TaackSimpleFilterService taackSimpleFilterService
    SpringSecurityService springSecurityService
    AttachmentUiService attachmentUiService

    static UiFilterSpecifier buildRoleTableFilter(final Role role = null) {
        Role r = role ?: new Role()
        UiFilterSpecifier f = new UiFilterSpecifier()

        f.ui Role, {
            section "Role", {
                filterField r.authority_
            }
        }
        f
    }

    static UiFilterSpecifier buildUserTableFilter(final User cu, final User user = null) {
        User u = user ?: new User(manager: new User(), enabled: true)
        UiFilterSpecifier f = new UiFilterSpecifier()

        f.ui User, {
            section "User", {
                filterField u.username_
                filterField u.lastName_
                filterField u.firstName_
                filterField u.manager_, u.manager.username_
                filterField u.subsidiary_
                filterField u.businessUnit_
                filterField u.enabled_
                filterFieldExpressionBool "My Team", new FilterExpression(cu.allManagedUsers*.id, Operator.IN, u.selfObject_), user ? false : true
            }
            section "Credentials", {
                UserRole ur = new UserRole(role: new Role())
                filterFieldInverse "Role", UserRole, ur.user_, ur.role_, ur.role.authority_
            }
        }
        f
    }

    UiTableSpecifier buildRoleTable(final UiFilterSpecifier f, final boolean hasSelect = false) {
        Role u = new Role()
        UiTableSpecifier t = new UiTableSpecifier()
        ColumnHeaderFieldSpec.SortableDirection defaultDirection
        t.ui Role, {
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
                        if (hasSelect) rowLink "Select Role", ActionIcon.SELECT * ActionIconStyleModifier.SCALE_DOWN, r.id, r.toString(), true
                    }
                }
            }
            paginate(10, params.long("offset"), roles.bValue)
        }
        t
    }

    UiTableSpecifier buildUserTable(final UiFilterSpecifier f, final boolean hasSelect = false) {
        User u = new User(manager: new User(), enabled: true)
        UiTableSpecifier t = new UiTableSpecifier()
        boolean canSwitchUser = (springSecurityService.currentUser as User).authorities*.authority.contains("ROLE_SWITCH_USER")
        boolean hasActions = (springSecurityService.currentUser as User).authorities*.authority.contains("ROLE_ADMIN")
        ColumnHeaderFieldSpec.SortableDirection defaultDirection
        t.ui User, {
            header {
                if (!hasSelect) {
                    column {
                        fieldHeader "Picture"
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
                    fieldHeader "Roles"
                }
            }

            def users = taackSimpleFilterService.list(User, 10, f, null, defaultDirection)

            for (User ru : users.aValue) {
                row {
                    if (!hasSelect) {
                        Attachment picture = ru.attachments.find { it.type == AttachmentType.mainPicture }
                        rowColumn {
                            rowField attachmentUiService.preview(picture?.id)
                        }
                    }
                    rowColumn {
                        rowLink "Show User", ActionIcon.SHOW * ActionIconStyleModifier.SCALE_DOWN, CrewController.&showUser as MethodClosure, ru.id, true
                        if (hasSelect) rowLink "Select User", ActionIcon.SELECT * ActionIconStyleModifier.SCALE_DOWN, ru.id, ru.toString(), true
                        else if (hasActions) {
                            rowLink "Edit User", ActionIcon.EDIT * ActionIconStyleModifier.SCALE_DOWN, CrewController.&editUser as MethodClosure, ru.id
                            if (canSwitchUser && ru.enabled) rowLink "Switch User", ActionIcon.SHOW * ActionIconStyleModifier.SCALE_DOWN, CrewController.&switchUser as MethodClosure, [id: ru.id], false
                            else if (canSwitchUser && !ru.enabled) {
                                rowLink "Replace By User", ActionIcon.MERGE * ActionIconStyleModifier.SCALE_DOWN, CrewController.&replaceUser as MethodClosure, ru.id, false
                                rowLink "Remove User", ActionIcon.DELETE * ActionIconStyleModifier.SCALE_DOWN, CrewController.&deleteUser as MethodClosure, ru.id, false
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
                        if (hasActions && !hasSelect) rowLink "Edit Roles", ActionIcon.EDIT * ActionIconStyleModifier.SCALE_DOWN, CrewController.&editUserRoles as MethodClosure, ru.id, true
                        rowField ru.authorities*.authority.join(', ')
                    }
                }
            }
            paginate(10, params.long("offset"), users.bValue)
        }
        t
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
            showAction "Change Picture", CrewController.&updateUserMainPicture as MethodClosure, u.id, true
            field "User Name", u.username
            field "First Name", u.firstName
            field "Last Name", u.lastName
            field "BU", u.businessUnit?.toString()
            field "Main Subsidiary", u.subsidiary?.toString()
            field "Mail", u.mail
            field "Manager", u.manager?.toString()
        })
    }

    boolean canManage(User other) {
        def u = springSecurityService.currentUser as User
        if (u.id == other?.id) return true
        return u.authorities*.authority.contains("ROLE_ADMIN") || u.managedUsers*.id.contains(other.id)
    }

}
