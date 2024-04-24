package attachement


import app.config.SupportedLanguage
import crew.AttachmentController
import grails.compiler.GrailsCompileStatic
import grails.web.api.WebAttributes
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.grails.plugins.web.taglib.ApplicationTagLib
import org.springframework.beans.factory.annotation.Autowired
import org.taack.Attachment
import org.taack.AttachmentDescriptor
import org.taack.Term
import org.taack.User
import taack.ast.type.FieldInfo
import taack.base.TaackSimpleAttachmentService
import taack.base.TaackSimpleSaveService
import taack.domain.TaackFilter
import taack.domain.TaackFilterService
import taack.ui.TaackPluginService
import taack.ui.base.UiFilterSpecifier
import taack.ui.base.UiFormSpecifier
import taack.ui.base.UiShowSpecifier
import taack.ui.base.UiTableSpecifier
import taack.ui.base.block.BlockSpec
import taack.ui.base.common.ActionIcon
import taack.ui.base.common.IconStyle
import taack.ui.base.filter.expression.FilterExpression
import taack.ui.base.filter.expression.Operator
import taack.ui.base.form.FormSpec
import taack.ui.base.table.ColumnHeaderFieldSpec

@GrailsCompileStatic
final class AttachmentUiService implements WebAttributes {
    TaackSimpleAttachmentService taackSimpleAttachmentService
    TaackFilterService taackFilterService
    TaackSimpleSaveService taackSimpleSaveService
    AttachmentSecurityService attachmentSecurityService
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

    Closure<BlockSpec> buildAttachmentsBlock(final MC selectMC = null, final Map selectParams = null, final MC uploadAttachment = AttachmentController.&uploadAttachment as MC, String fileOrigin = null) {
        Attachment a = new Attachment(fileOrigin: fileOrigin)
        AttachmentDescriptor ad = new AttachmentDescriptor()
        Term term = new Term()
        User u = new User()

        UiFilterSpecifier f = new UiFilterSpecifier()
        f.ui Attachment, selectParams, {
            section "File Metadata Filter", {
                filterField a.originalName_
                filterField a.attachmentDescriptor_, ad.publicName_
                filterField a.attachmentDescriptor_, ad.contentTypeCategoryEnum_
                filterField a.attachmentDescriptor_, ad.contentTypeEnum_
                filterField a.attachmentDescriptor_, ad.type_
                filterField a.attachmentDescriptor_, ad.tags_, term.termGroupConfig_
                filterFieldExpressionBool "Active", new FilterExpression(true, Operator.EQ, a.active_), true
            }
            section "File Access Related Filter", {
                filterField a.userCreated_, u.username_
                filterField a.userCreated_, u.firstName_
                filterField a.userCreated_, u.lastName_
                filterField a.userCreated_, u.subsidiary_
                filterField taackPluginService.enumOptions, a.attachmentDescriptor_, ad.fileOrigin_
            }
        }

        UiTableSpecifier t = new UiTableSpecifier()
        t.ui {
            header {
                column {
                    fieldHeader "Preview"
                }
                column {
                    sortableFieldHeader a.originalName_
                    sortableFieldHeader a.attachmentDescriptor_, ad.publicName_
                    sortableFieldHeader a.dateCreated_
                }
                column {
                    sortableFieldHeader a.fileSize_
                    sortableFieldHeader a.contentType_
                    sortableFieldHeader a.attachmentDescriptor_, ad.fileOrigin_
                }
                column {
                    sortableFieldHeader a.userCreated_, u.username_
                    sortableFieldHeader a.userCreated_, u.subsidiary_
                }
                column {
                    fieldHeader "Actions"
                }
            }
            taackFilterService.getBuilder(Attachment)
                    .setMaxNumberOfLine(8)
                    .addFilter(f)
                    .setSortOrder(TaackFilter.Order.DESC, a.dateCreated_)
                    .build()
                    .iterate { Attachment att ->
                        row att, {
                            rowColumn {
                                rowField preview(att.id)
                            }
                            rowColumn {
                                rowField att.originalName
                                rowField att.attachmentDescriptor.publicName
                                rowField att.dateCreated
                            }
                            rowColumn {
                                rowField att.fileSize
                                rowField att.contentType
                                rowField att.attachmentDescriptor.fileOrigin
                            }
                            rowColumn {
                                rowField att.userCreated.username
                                rowField att.userCreated.subsidiary?.toString()
                            }
                            rowColumn {
                                if (selectMC) rowLink "Select", ActionIcon.SELECT, selectMC, att.id, selectParams
                                else if (attachmentSecurityService.canDownloadFile(att)) rowLink "Download", ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MC, att.id, false
                                rowLink "Show", ActionIcon.SHOW, AttachmentController.&showAttachment as MC, att.id
                            }
                        }
                    }
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
                    action "Update", ActionIcon.EDIT, AttachmentController.&updateAttachment as MC, attachment.id, true
                    if (attachmentSecurityService.canDownloadFile(attachment)) action "Download", ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MC, attachment.id, false
                    if (attachmentSecurityService.canDownloadFile(attachment) && converterExtensions) {
                        for (def ext in converterExtensions) {
                            action "Download ${ext}", ext == 'pdf' ? ActionIcon.EXPORT_PDF : ActionIcon.EXPORT, AttachmentController.&extensionForAttachment as MC, [extension: ext, id: attachment.id], false
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
            showAction "Display Linked Data", AttachmentController.&showLinkedData as MC, attachment.id
        }
    }

    UiTableSpecifier buildAttachmentsTable(final Collection<Attachment> attachments, final String fieldName = null, final boolean hasUpload = false) {
        new UiTableSpecifier().ui {
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
                    if (attachmentSecurityService.canDownloadFile(a))
                        rowLink "Download File", ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MC, a.id, false
                }
            }
        }
    }

    static UiFormSpecifier buildAttachmentDescriptorForm(AttachmentDescriptor attachment, MC returnMethod = AttachmentController.&saveAttachment as MC, Map other = null) {
        new UiFormSpecifier().ui attachment, {
            hiddenField attachment.fileOrigin_
            section "File Info", FormSpec.Width.DOUBLE_WIDTH, {
                field attachment.type_
                field attachment.status_
                field attachment.declaredLanguage_
                ajaxField attachment.tags_, AttachmentController.&selectTagsM2M as MC
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

    static UiFormSpecifier buildAttachmentForm(Attachment attachment, MC returnMethod = AttachmentController.&saveAttachment as MC, Map other = null) {
        new UiFormSpecifier().ui attachment, {
            section "File Info", FormSpec.Width.DOUBLE_WIDTH, {
                field attachment.filePath_
                ajaxField attachment.attachmentDescriptor_,
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
        new UiFormSpecifier().ui term, {
            field term.name_
            field term.termGroupConfig_
            ajaxField term.parent_, AttachmentController.&selectTermM2O as MC
            sectionTabs FormSpec.Width.FULL_WIDTH, {
                for (SupportedLanguage language : SupportedLanguage.values()) {
                    sectionTab "Translation ${language.label}", {
                        fieldFromMap "Translation ${language.toString().toLowerCase()}", term.translations_, language.toString().toLowerCase()
                    }
                }
            }
            field term.display_
            field term.active_
            formAction "Save", AttachmentController.&saveTerm as MC, term.id, true
        }
    }

    UiFilterSpecifier buildTermFilter() {
        Term t = new Term(parent: new Term())
        new UiFilterSpecifier().ui Term, {
            section "Term", {
                filterField t.name_
                filterField t.termGroupConfig_
                section "Parent", {
                    filterField t.parent_, t.parent.name_
                }
                filterFieldExpressionBool "Display", new FilterExpression(true, Operator.EQ, t.display_), true
                filterFieldExpressionBool "Active", new FilterExpression(true, Operator.EQ, t.active_), true
            }
        }
    }

    UiTableSpecifier buildTermTable(final UiFilterSpecifier f, boolean selectMode = false) {
        Term ti = new Term(parent: new Term())
        new UiTableSpecifier().ui {
            header {
                sortableFieldHeader ti.name_
                sortableFieldHeader ti.termGroupConfig_
                sortableFieldHeader ti.parent_, ti.parent.name_
                sortableFieldHeader ti.display_
                sortableFieldHeader ti.active_
                fieldHeader "Actions"

                taackFilterService.getBuilder(Term)
                        .setMaxNumberOfLine(30)
                        .addFilter(f)
                        .setSortOrder(TaackFilter.Order.ASC, ti.name_)
                        .build()
                        .iterate { Term term ->
                            row {
                                rowField term.name
                                rowField term.termGroupConfig?.toString()
                                rowField term.parent?.name
                                rowField term.display.toString()
                                rowField term.active.toString()
                                rowColumn {
                                    if (selectMode)
                                        rowLink "Select", ActionIcon.SELECT * IconStyle.SCALE_DOWN, AttachmentController.&selectTermM2OCloseModal as MC, term.id
                                    else {
                                        if (term.active)
                                            rowLink "Delete term", ActionIcon.DELETE * IconStyle.SCALE_DOWN, AttachmentController.&deleteTerm as MC, term.id, false
                                        rowLink "Edit term", ActionIcon.EDIT * IconStyle.SCALE_DOWN, AttachmentController.&editTerm as MC, term.id, true
                                    }
                                }
                            }
                        }
            }
        }
    }
}