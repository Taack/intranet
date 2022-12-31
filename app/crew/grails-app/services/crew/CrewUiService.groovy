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
import org.taack.Alert
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
                filterFieldExpressionBool "My Team", new FilterExpression(u.selfObject_, Operator.IN, cu.allManagedUsers_), user ? false : true
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
                        if (hasSelect) rowLink "Select Role", ActionIcon.SELECT * ActionIconStyleModifier.SCALE_DOWN, CrewController.&selectRoleM2OCloseModal as MethodClosure, r.id, true
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
                        if (hasSelect) rowLink "Select User", ActionIcon.SELECT * ActionIconStyleModifier.SCALE_DOWN, CrewController.&selectUserM2OCloseModal as MethodClosure, ru.id, true
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

    UiTableSpecifier buildAlertTable(String application = null, String controllerRedirect = null,
                                     String actionRedirect = null, String subsidiaryRedirect = null) {
        Alert ra = new Alert()
        boolean hasActions = !application
        User cu = springSecurityService.currentUser as User
        UiTableSpecifier t = new UiTableSpecifier()
        ColumnHeaderFieldSpec.SortableDirection dir
        t.ui Alert, {
            header {
                dir = sortableFieldHeader ColumnHeaderFieldSpec.DefaultSortingDirection.ASC, ra.name_
                fieldHeader "Description"
                fieldHeader "Origin"
                fieldHeader "Is Subscription Enabled"
                fieldHeader "Active"
                fieldHeader "Subscribe"
                if (hasActions) {
                    fieldHeader "Edit"
                }
            }

            UiFilterSpecifier alertFilter = new UiFilterSpecifier().ui Alert, {
                if (application) {
                    filterFieldExpressionBool(null, new FilterExpression(ra.origin_, Operator.EQ, application), true)
                }
            }

            def alerts = taackSimpleFilterService.list(Alert, 20, alertFilter, null, dir)
            paginate(20, params.long('offset'), alerts.bValue)
            for (def a : alerts.aValue) {
                row {
                    rowField a.name_
                    rowField a.description_
                    rowField a.origin_
                    rowField a.isSubscriptionEnabled_
                    rowField a.active_
                    if (a.canSubscribe(cu)) {
                        Map params = [id: a.id, controllerRedirect: controllerRedirect, actionRedirect: actionRedirect, subsidiaryRedirect: subsidiaryRedirect]
                        if (a.users.contains(cu)) {
                            rowLink "Unsubscribe", ActionIcon.DELETE, CrewController.&unsubscribeFromAlert as MethodClosure, params, false
                        } else {
                            rowLink "Subscribe", ActionIcon.ADD, CrewController.&subscribeToAlert as MethodClosure, params, false
                        }
                    } else {
                        rowField ""
                    }
                    if (hasActions) {
                        rowLink "Edit alert", ActionIcon.EDIT, CrewController.&alertForm as MethodClosure, a.id
                    }
                }
            }
        }
        t
    }
}
