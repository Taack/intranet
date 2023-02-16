package attachement

import app.config.AttachmentType
import app.config.SupportedLanguage
import crew.AttachmentController
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import grails.web.api.WebAttributes
import org.codehaus.groovy.runtime.MethodClosure
import org.grails.plugins.web.taglib.ApplicationTagLib
import org.springframework.beans.factory.annotation.Autowired
import org.taack.Attachment
import org.taack.Term
import org.taack.User
import taack.ast.type.FieldInfo
import taack.base.TaackSimpleAttachmentService
import taack.base.TaackSimpleFilterService
import taack.base.TaackSimpleSaveService
import taack.ui.TaackPluginService
import taack.ui.base.UiFilterSpecifier
import taack.ui.base.UiFormSpecifier
import taack.ui.base.UiShowSpecifier
import taack.ui.base.UiTableSpecifier
import taack.ui.base.block.BlockSpec
import taack.ui.base.common.ActionIcon
import taack.ui.base.common.ActionIconStyleModifier
import taack.ui.base.filter.expression.FilterExpression
import taack.ui.base.filter.expression.Operator
import taack.ui.base.form.FormSpec
import taack.ui.base.table.ColumnHeaderFieldSpec

@GrailsCompileStatic
final class AttachmentUiService implements WebAttributes {
    TaackSimpleAttachmentService taackSimpleAttachmentService
    TaackSimpleFilterService taackSimpleFilterService
    TaackSimpleSaveService taackSimpleSaveService
    AttachmentSecurityService attachmentSecurityService
    SpringSecurityService springSecurityService
    TaackPluginService taackPluginService

    @Autowired
    ApplicationTagLib applicationTagLib

    String preview(final Long id) {
        if (!id) return "<span/>"
        if (params.boolean("isPdf")) """<img style="max-height: 64px; max-width: 64px;" src="file://${taackSimpleAttachmentService.attachmentPreview(Attachment.get(id)).path}">"""
        else """<div style="text-align: center;"><img style="max-height: 64px; max-width: 64px;" src="${applicationTagLib.createLink(controller: 'attachment', action: 'preview', id: id)}"></div>"""
    }

    String preview(final Long id, TaackSimpleAttachmentService.PreviewFormat format) {
        if (!id) return "<span/>"
        if (params.boolean("isPdf")) """<img style="max-height: 64px; max-width: 64px;" src="file://${taackSimpleAttachmentService.attachmentPreview(Attachment.get(id), format).path}">"""
        else """<div style="text-align: center;"><img style="max-height: ${format.pixelHeight}px; max-width: ${format.pixelWidth}px;" src="${applicationTagLib.createLink(controller: 'attachment', action: 'preview', id: id, params: [format: format.toString()])}"></div>"""
    }

    String previewFull(Long id, String p = null) {
        if (!id) return "<span/>"
        """<div style="text-align: center;"><img src="${applicationTagLib.createLink(controller: 'attachment', action: 'previewFull', id: id)}${p ? "?$p" : ""}"></div>"""
    }

    Closure<BlockSpec> buildAttachmentsBlock(final MethodClosure selectMC = null, final Map selectParams = null, final MethodClosure uploadAttachment = AttachmentController.&uploadAttachment as MethodClosure, String fileOrigin = null) {
        Attachment a = new Attachment(fileOrigin: fileOrigin)
        Term term = new Term()
        User u = new User()

        UiFilterSpecifier f = new UiFilterSpecifier()
        f.ui Attachment, selectParams, {
            section "File Metadata Filter", {
                filterField a.originalName_
                filterField a.publicName_
                filterField a.contentTypeCategoryEnum_
                filterField a.contentTypeEnum_
                filterField a.type_
                filterField a.tags_, term.termGroupConfig_
                filterFieldExpressionBool "Active", new FilterExpression(a.active_, Operator.EQ, true), true
            }
            section "File Access Related Filter", {
                filterField a.userCreated_, u.username_
                filterField a.userCreated_, u.firstName_
                filterField a.userCreated_, u.lastName_
                filterField a.userCreated_, u.subsidiary_
                filterField a.fileOrigin_, taackPluginService.enumOptions
            }
        }

        UiTableSpecifier t = new UiTableSpecifier()
        ColumnHeaderFieldSpec.SortableDirection defaultSort
        t.ui Attachment, {
            header {
                column {
                    fieldHeader "Preview"
                }
                column {
                    sortableFieldHeader a.originalName_
                    sortableFieldHeader a.publicName_
                    defaultSort = sortableFieldHeader ColumnHeaderFieldSpec.DefaultSortingDirection.DESC, a.dateCreated_
                }
                column {
                    sortableFieldHeader a.fileSize_
                    sortableFieldHeader a.contentType_
                    sortableFieldHeader a.fileOrigin_
                }
                column {
                    sortableFieldHeader a.userCreated_, u.username_
                    sortableFieldHeader a.userCreated_, u.subsidiary_
                }
                column {
                    fieldHeader "Actions"
                }
            }
            def attachments = taackSimpleFilterService.list(Attachment, 8, f, a, defaultSort)
            for (def att : attachments.aValue as List<Attachment>) {
                row att, {
                    rowColumn {
                        rowField preview(att.id)
                    }
                    rowColumn {
                        rowField att.originalName
                        rowField att.publicName
                        rowField att.dateCreated
                    }
                    rowColumn {
                        rowField att.fileSize
                        rowField att.contentType
                        rowField att.fileOrigin
                    }
                    rowColumn {
                        rowField att.userCreated.username
                        rowField att.userCreated.subsidiary?.toString()
                    }
                    rowColumn {
                        if (selectMC) rowLink "Select", ActionIcon.SELECT, selectMC, att.id, selectParams
                        else if (attachmentSecurityService.canDownloadFile(att)) rowLink "Download", ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MethodClosure, att.id, false
                        rowLink "Show", ActionIcon.SHOW, AttachmentController.&showAttachment as MethodClosure, att.id
                    }
                }
            }
            paginate(8, params.long("offset"), attachments.bValue)
        }
        BlockSpec.buildBlockSpec {
            ajaxBlock "attachmentList", {
                tableFilter "Filter", f, "Attachment", t, BlockSpec.Width.MAX, {
                    if (uploadAttachment)
                        action "Upload New File", ActionIcon.CREATE, uploadAttachment, selectParams, true
                }
            }
        }
    }

    Closure<BlockSpec> buildShowAttachmentBlock(final Attachment attachment, final String fieldName = "") {
        String iFrame = TaackSimpleAttachmentService.showIFrame(attachment)
        def converterExtensions = TaackSimpleAttachmentService.converterExtensions(attachment)
        BlockSpec.buildBlockSpec {
            if (iFrame) {
                ajaxBlock "showAttachment${fieldName}IFrame", {
                    custom iFrame
                }
            }
            ajaxBlock "showAttachment${fieldName}", {
                show "${attachment.originalName}", buildShowAttachment(attachment, iFrame == null), BlockSpec.Width.MAX, {
                    action "Update", ActionIcon.EDIT, AttachmentController.&updateAttachment as MethodClosure, attachment.id, true
                    if (attachmentSecurityService.canDownloadFile(attachment)) action "Download", ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MethodClosure, attachment.id, false
                    if (attachmentSecurityService.canDownloadFile(attachment) && converterExtensions) {
                        for (def ext in converterExtensions) {
                            action "Download ${ext}", ext == 'pdf' ? ActionIcon.EXPORT_PDF : ActionIcon.EXPORT, AttachmentController.&extensionForAttachment as MethodClosure, [extension: ext, id: attachment.id], false
                        }
                    }
                }
            }
        }
    }

    Closure<BlockSpec> buildShowAttachmentBlock(FieldInfo<Attachment> fieldInfo) {
        final String fieldName = fieldInfo.fieldConstraint.field.declaringClass.simpleName + fieldInfo.fieldName
        buildShowAttachmentBlock(fieldInfo.value, fieldName)
    }

    UiShowSpecifier buildShowAttachment(final Attachment attachment, boolean hasPreview = true) {
        new UiShowSpecifier().ui attachment, {
            if (hasPreview)
                section "Preview", {
                    field previewFull(attachment.id)
                }
            section "File Meta", {
                field "Name", attachment.originalName_
                field "Size", attachment.fileSize_
                field "Date Created", attachment.dateCreated_
                field "Content Type", attachment.contentType_

            }
            section "Attachment Meta", {
                field "Type", attachment.type_
                field "Public Name", attachment.publicName_
                field "Is Internal", attachment.isInternal_
                field "Language", attachment.declaredLanguage_
            }
            showAction "Display Linked Data", AttachmentController.&showLinkedData as MethodClosure, attachment.id
        }
    }

    UiTableSpecifier buildAttachmentsTable(final Collection<Attachment> attachments, final String fieldName = null, final boolean hasUpload = false) {
        new UiTableSpecifier().ui Attachment, {
            for (Attachment a : attachments.sort { a1, a2 -> a2.dateCreated <=> a1.dateCreated }) {
                row {
                    rowField preview(a.id)
                    rowColumn {
                        rowField a.userCreated.username
                        rowField a.dateCreated
                    }
                    rowColumn {
                        rowField a.getName()
                        rowField a.fileSize
                    }
                    if (attachmentSecurityService.canDownloadFile(a)) rowLink "Download File", ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MethodClosure, a.id, false
                }
            }
        }
    }

    UiTableSpecifier buildAttachmentsTableWithGroups(final Collection<Attachment> attachments, boolean hasActions = false, MethodClosure disassociate = null, Long objectId = null) {
        new UiTableSpecifier().ui Attachment, {
            header {
                column {
                    fieldHeader "Preview"
                }
                column {
                    fieldHeader "UserCreated"
                    fieldHeader "DateCreated"
                }
                column {
                    fieldHeader "UserUpdated"
                    fieldHeader "DateUpdated"
                }
                column {
                    fieldHeader "Name"
                    fieldHeader "Version"
                }
                column {
                    fieldHeader "ContentTypeCat."
                    fieldHeader "ContentType"
                }
                if (hasActions) {
                    fieldHeader "Actions"
                }
            }
            for (AttachmentType typeGroup : [AttachmentType.values(), null].flatten() as List<AttachmentType>) {
                Collection<Attachment> attachmentsInGroup = attachments.findAll { it.type == typeGroup && it.active }
                if (attachmentsInGroup && !attachmentsInGroup.empty) {
                    rowGroupHeader "Type: ${typeGroup ?: '---'}"
                    for (Attachment a : attachmentsInGroup.sort { a1, a2 -> a2.dateCreated <=> a1.dateCreated }) {
                        row {
                            rowColumn {
                                rowField preview(a.id)
                            }
                            rowColumn {
                                rowField a.userCreated.username
                                rowField a.dateCreated
                            }
                            rowColumn {
                                rowField a.userUpdated?.username
                                rowField a.dateCreated
                            }
                            rowColumn {
                                rowField a.getName()
                                rowField a.version
                            }
                            rowColumn {
                                rowField a.contentTypeCategoryEnum?.toString()
                                rowField a.contentType
                            }
                            if (hasActions) {
                                rowColumn {
                                    if (attachmentSecurityService.canDownloadFile(a)) rowLink "Download", ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MethodClosure, a.id, false
                                    rowLink "Show", ActionIcon.SHOW, AttachmentController.&showAttachment as MethodClosure, a.id
                                    if (disassociate) rowLink "Disassociate", ActionIcon.DELETE, disassociate, a.id, [objectId: objectId]
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    UiTableSpecifier buildAttachmentTable(final Attachment attachment, final String fieldName, final boolean hasUpload = false) {
        buildAttachmentsTable([attachment], fieldName, hasUpload)
    }

    Closure<BlockSpec> buildAttachmentsAjaxBlock(final FieldInfo<Set<Attachment>> fieldInfo, final boolean hasUpload = false, final String origin = "ATT") {
        final String fieldName = fieldInfo.fieldConstraint.field.declaringClass.simpleName + fieldInfo.fieldName
        BlockSpec.buildBlockSpec {
            ajaxBlock "attachments${fieldName}", {
                table "Attachments", buildAttachmentsTable(fieldInfo.value, fieldName, hasUpload), BlockSpec.Width.MAX
            }
        }
    }

    Closure<BlockSpec> buildAttachmentAjaxBlock(final FieldInfo<Attachment> fieldInfo, final boolean hasUpload = false, final String origin = "ATT") {
        final String fieldName = fieldInfo.fieldConstraint.field.declaringClass.simpleName + fieldInfo.fieldName
        BlockSpec.buildBlockSpec {
            ajaxBlock "attachment${fieldName}", {
                table "Attachment", buildAttachmentTable(fieldInfo.value, fieldName, hasUpload), BlockSpec.Width.MAX
            }
        }
    }

    static UiFormSpecifier buildAttachmentForm(Attachment attachment, MethodClosure returnMethod = AttachmentController.&saveAttachment as MethodClosure, long objectId) {
        buildAttachmentForm(attachment, returnMethod, [objectId: objectId])
    }

    static UiFormSpecifier buildAttachmentForm(Attachment attachment, MethodClosure returnMethod = AttachmentController.&saveAttachment as MethodClosure, Map other = null) {
        new UiFormSpecifier().ui attachment, {
            hiddenField attachment.fileOrigin_
            section "File Info", FormSpec.Width.DOUBLE_WIDTH, {
                field attachment.type_
                field attachment.status_
                field attachment.declaredLanguage_
                field attachment.filePath_
                field attachment.active_
                ajaxField attachment.tags_, AttachmentController.&selectTagsM2M as MethodClosure
            }
            section "Security", FormSpec.Width.DOUBLE_WIDTH, {
                field attachment.isInternal_
                field attachment.isRestrictedToMyBusinessUnit_
                field attachment.isRestrictedToMyManagers_
                field attachment.isRestrictedToEmbeddingObjects_
            }
            formAction "Save", returnMethod, attachment.id, other, true
        }
    }

    static UiFormSpecifier buildAttachmentFormSimpleMode(Attachment attachment, MethodClosure returnMethod = AttachmentController.&saveAttachment as MethodClosure, Map other = null) {
        new UiFormSpecifier().ui attachment, {
            hiddenField attachment.fileOrigin_
            section "File Info", FormSpec.Width.DOUBLE_WIDTH, {
                hiddenField attachment.type_
                hiddenField attachment.status_
                hiddenField attachment.declaredLanguage_
                field attachment.filePath_
                hiddenField attachment.active_
                hiddenField attachment.tags_
            }
            formAction "Save", returnMethod, attachment.id, other, true
        }
    }

    Attachment saveAttachment() {
        Long aId = params.long('id')
        if (aId) {
            def previous = taackSimpleAttachmentService.copyAttachment(Attachment.get(aId), new Attachment(), true)
            def a = taackSimpleSaveService.prepareSave(Attachment)
            taackSimpleAttachmentService.postPrepareSave(a)
            if (!a.originalName && !a.fileSize) {
                a.originalName = previous.originalName
                a.fileSize = previous.fileSize
                a.contentShaOne = previous.contentShaOne
            }
            a.save(flush: true)
            previous.nextVersion = a
            previous.save()
            return a
        } else {
            def a = taackSimpleSaveService.prepareSave(Attachment)
            taackSimpleAttachmentService.postPrepareSave(a)
            a.save()
            return a
        }
    }

    UiFormSpecifier buildTermForm(Term term) {
        UiFormSpecifier f = new UiFormSpecifier()
        f.ui term, {
            field term.name_
            field term.termGroupConfig_
            ajaxField term.parent_, AttachmentController.&selectTermM2O as MethodClosure
            sectionTabs FormSpec.Width.FULL_WIDTH, {
                for (SupportedLanguage language : SupportedLanguage.values()) {
                    sectionTab "Translation ${language.label}", {
                        fieldFromMap "Translation ${language.toString().toLowerCase()}", term.translations_, language.toString().toLowerCase()
                    }
                }
            }
            field term.display_
            field term.active_
            formAction "Save", AttachmentController.&saveTerm as MethodClosure, term.id, true
        }
    }

    UiFilterSpecifier buildTermFilter() {
        Term t = new Term(parent: new Term())
        UiFilterSpecifier f = new UiFilterSpecifier()
        f.ui Term, {
            section "Term", {
                filterField t.name_
                filterField t.termGroupConfig_
                section "Parent", {
                    filterField t.parent_, t.parent.name_
                }
                filterFieldExpressionBool "Display", new FilterExpression(t.display_, Operator.EQ, true), true
                filterFieldExpressionBool "Active", new FilterExpression(t.active_, Operator.EQ, true), true
            }
        }
    }

    UiTableSpecifier buildTermTable(final UiFilterSpecifier f, boolean selectMode = false) {
        UiTableSpecifier t = new UiTableSpecifier()
        Term ti = new Term(parent: new Term())
        ColumnHeaderFieldSpec.SortableDirection defaultDirection
        t.ui Term, {
            header {
                defaultDirection = sortableFieldHeader ColumnHeaderFieldSpec.DefaultSortingDirection.ASC, ti.name_
                sortableFieldHeader ti.termGroupConfig_
                sortableFieldHeader ti.parent_, ti.parent.name_
                sortableFieldHeader ti.display_
                sortableFieldHeader ti.active_
                fieldHeader "Actions"

                def objects = taackSimpleFilterService.list(Term, 30, f, null, defaultDirection)
                paginate(30, params.long("offset"), objects.bValue)

                for (Term term : objects.aValue) {
                    row {
                        rowField term.name
                        rowField term.termGroupConfig?.toString()
                        rowField term.parent?.name
                        rowField term.display.toString()
                        rowField term.active.toString()
                        rowColumn {
                            if (selectMode)
                                rowLink "Select", ActionIcon.SELECT * ActionIconStyleModifier.SCALE_DOWN, AttachmentController.&selectTermM2OCloseModal as MethodClosure, term.id
                            else {
                                if (term.active)
                                    rowLink "Delete term", ActionIcon.DELETE * ActionIconStyleModifier.SCALE_DOWN, AttachmentController.&deleteTerm as MethodClosure, term.id, false
                                rowLink "Edit term", ActionIcon.EDIT * ActionIconStyleModifier.SCALE_DOWN, AttachmentController.&editTerm as MethodClosure, term.id, true
                            }
                        }
                    }
                }
            }
            t
        }
    }
}