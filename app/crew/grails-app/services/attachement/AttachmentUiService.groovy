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
import taack.domain.TaackAttachmentService
import taack.domain.TaackFilter
import taack.domain.TaackFilterService
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

import static taack.render.TaackUiService.tr

@GrailsCompileStatic
final class AttachmentUiService implements WebAttributes {
    TaackAttachmentService taackAttachmentService
    TaackFilterService taackFilterService
    AttachmentSecurityService attachmentSecurityService

    @Autowired
    ApplicationTagLib applicationTagLib

    String preview(final Long id) {
        if (!id) return "<span/>"
        if (params.boolean("isPdf")) """<img style="max-height: 64px; max-width: 64px;" src="file://${taackAttachmentService.attachmentPreview(Attachment.get(id)).path}">"""
        else """<div style="text-align: center;"><img style="max-height: 64px; max-width: 64px;" src="${applicationTagLib.createLink(controller: 'attachment', action: 'preview', id: id)}"></div>"""
    }

    String preview(final Long id, TaackAttachmentService.PreviewFormat format) {
        if (!id) return "<span/>"
        if (format.isPdf) """<img style="max-height: 64px; max-width: 64px;" src="file://${taackAttachmentService.attachmentPreview(Attachment.get(id), format).path}">"""
        else """<div style="text-align: center;"><img style="max-height: ${format.pixelHeight}px; max-width: ${format.pixelWidth}px;" src="${applicationTagLib.createLink(controller: 'attachment', action: 'preview', id: id, params: [format: format.toString()])}"></div>"""
    }

    String previewFull(Long id, String p = null) {
        if (!id) return "<span/>"
        """<div style="text-align: center;"><img style="max-height: 420px" src="${applicationTagLib.createLink(controller: 'attachment', action: 'previewFull', id: id)}${p ? "?$p" : ""}"></div>"""
    }

    Closure<BlockSpec> buildAttachmentsBlock(final MC selectMC = null, final Map selectParams = null, final MC uploadAttachment = AttachmentController.&uploadAttachment as MC) {
        Attachment a = new Attachment()
        AttachmentDescriptor ad = new AttachmentDescriptor(type: null)
        Term term = new Term()
        User u = new User()

        UiFilterSpecifier f = new UiFilterSpecifier()
        f.ui Attachment, selectParams, {
            section tr('file.metadata.label'), {
                filterField a.originalName_
                filterField a.contentTypeCategoryEnum_
                filterField a.contentTypeEnum_
                filterField a.attachmentDescriptor_, ad.type_
                filterField a.tags_, term.termGroupConfig_
                filterFieldExpressionBool "Active", new FilterExpression(true, Operator.EQ, a.active_)
            }
            section tr('file.access.label'), {
                filterField a.userCreated_, u.username_
                filterField a.userCreated_, u.firstName_
                filterField a.userCreated_, u.lastName_
                filterField a.userCreated_, u.subsidiary_
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
                    sortableFieldHeader a.dateCreated_
                }
                column {
                    sortableFieldHeader a.fileSize_
                    sortableFieldHeader a.contentType_
                }
                column {
                    sortableFieldHeader a.userCreated_, u.username_
                    sortableFieldHeader a.userCreated_, u.subsidiary_
                }
                column {
                    fieldHeader "Actions"
                }
            }
            iterate(taackFilterService.getBuilder(Attachment)
                    .setMaxNumberOfLine(8)
                    .setSortOrder(TaackFilter.Order.DESC, a.dateCreated_)
                    .build()) { Attachment att ->
                String aPreview = this.preview(att.id)
                rowColumn {
                    rowField aPreview
                }
                rowColumn {
                    rowField att.originalName
                    rowField att.dateCreated_
                }
                rowColumn {
                    rowField att.fileSize_
                    rowField att.contentType
                }
                rowColumn {
                    rowField att.userCreated.username
                    rowField att.userCreated.subsidiary?.toString()
                }
                rowColumn {
                    if (selectMC)
                        rowAction ActionIcon.SELECT * IconStyle.SCALE_DOWN, selectMC as MC, att.id, selectParams
                    rowAction ActionIcon.DOWNLOAD * IconStyle.SCALE_DOWN, AttachmentController.&downloadAttachment as MC, att.id
                    rowAction ActionIcon.SHOW * IconStyle.SCALE_DOWN, AttachmentController.&showAttachment as MC, att.id
                }
            }
        }
        BlockSpec.buildBlockSpec {
            tableFilter tr('default.filter.label'), f, tr('default.attachment.label'), t, BlockSpec.Width.MAX, {
                if (uploadAttachment)
                    action ActionIcon.CREATE, uploadAttachment, selectParams
            }
        }
    }

    Closure<BlockSpec> buildShowAttachmentBlock(final Attachment attachment, final String fieldName = "") {
        String iFrame = TaackAttachmentService.showIFrame(attachment)
        def converterExtensions = TaackAttachmentService.converterExtensions(attachment)
        BlockSpec.buildBlockSpec {
            if (iFrame) {
                custom iFrame
            }
            show "${attachment.originalName}", buildShowAttachment(attachment, iFrame == null), BlockSpec.Width.MAX, {
                action ActionIcon.EDIT, AttachmentController.&updateAttachment as MC, attachment.id
                action ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MC, attachment.id
                if (attachmentSecurityService.canDownloadFile(attachment) && converterExtensions) {
                    for (def ext in converterExtensions) {
                        action ext == 'pdf' ? ActionIcon.EXPORT_PDF : ActionIcon.EXPORT, AttachmentController.&extensionForAttachment as MC, [extension: ext, id: attachment.id]
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
        AttachmentDescriptor ad = new AttachmentDescriptor()
        new UiShowSpecifier().ui attachment, {
            if (hasPreview)
                section "Preview", {
                    field this.previewFull(attachment.id)
                }
            section "File Meta", {
                fieldLabeled attachment.originalName_
                fieldLabeled attachment.fileSize_
                fieldLabeled attachment.dateCreated_
                fieldLabeled attachment.contentType_

            }
            section "Attachment Meta", {
                fieldLabeled attachment.attachmentDescriptor_, ad.type_
                fieldLabeled attachment.attachmentDescriptor_, ad.isInternal_
            }
            showAction AttachmentController.&showLinkedData as MC, attachment.id
        }
    }

    UiTableSpecifier buildAttachmentsTable(final Collection<Attachment> attachments, final String fieldName = null, final boolean hasUpload = false) {
        new UiTableSpecifier().ui {
            for (Attachment a : attachments.sort { a1, a2 -> a2.dateCreated <=> a1.dateCreated }) {
                row {
                    rowField this.preview(a.id)
                    rowColumn {
                        rowField a.userCreated.username
                        rowField a.dateCreated_
                    }
                    rowColumn {
                        rowField a.getName()
                        rowField a.fileSize_
                    }
                    if (attachmentSecurityService.canDownloadFile(a))
                        rowAction ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MC, a.id
                }
            }
        }
    }

    static UiFormSpecifier buildAttachmentDescriptorForm(AttachmentDescriptor attachment, MC returnMethod = AttachmentController.&saveAttachmentDescriptor as MC, Map other = null) {
        new UiFormSpecifier().ui attachment, {
            section "File Info", FormSpec.Width.DOUBLE_WIDTH, {
                field attachment.type_
            }
            section "Security", FormSpec.Width.DOUBLE_WIDTH, {
                field attachment.isInternal_
                field attachment.isRestrictedToMyBusinessUnit_
                field attachment.isRestrictedToMyManagers_
                field attachment.isRestrictedToEmbeddingObjects_
            }
            formAction returnMethod, attachment.id, other
        }
    }

    static UiFormSpecifier buildAttachmentForm(Attachment attachment, MC returnMethod = AttachmentController.&saveAttachment as MC, Map other = null) {
        new UiFormSpecifier().ui attachment, {
            section "File Info", FormSpec.Width.DOUBLE_WIDTH, {
                field attachment.filePath_
                ajaxField attachment.attachmentDescriptor_, AttachmentController.&editAttachmentDescriptor as MC, attachment.attachmentDescriptor?.id
                ajaxField attachment.tags_, AttachmentController.&selectTagsM2M as MC
            }
            formAction returnMethod, attachment.id, other
        }
    }

    UiFormSpecifier buildTermForm(Term term) {
        new UiFormSpecifier().ui term, {
            field term.name_
            field term.termGroupConfig_
            ajaxField term.parent_, AttachmentController.&selectTermM2O as MC
            tabs FormSpec.Width.FULL_WIDTH, {
                for (SupportedLanguage language : SupportedLanguage.values()) {
                    tab "Translation ${language.label}", {
                        fieldFromMap "Translation ${language.toString().toLowerCase()}", term.translations_, language.toString().toLowerCase()
                    }
                }
            }
            field term.display_
            field term.active_
            formAction AttachmentController.&saveTerm as MC, term.id
        }
    }

    UiFilterSpecifier buildTermFilter() {
        Term t = new Term(parent: new Term())
        new UiFilterSpecifier().ui Term, {
            section "Term", {
                filterField t.name_
                filterField t.termGroupConfig_
                filterField t.parent_, t.parent.name_
                filterFieldExpressionBool "Display", new FilterExpression(true, Operator.EQ, t.display_)
                filterFieldExpressionBool "Active", new FilterExpression(true, Operator.EQ, t.active_)
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
            }

            iterate(taackFilterService.getBuilder(Term)
                    .setMaxNumberOfLine(30)
                    .addFilter(f)
                    .setSortOrder(TaackFilter.Order.ASC, ti.name_)
                    .build()) { Term term ->
                rowField term.name
                rowField term.termGroupConfig?.toString()
                rowField term.parent?.name
                rowField term.display.toString()
                rowField term.active.toString()
                rowColumn {
                    if (selectMode)
                        rowAction ActionIcon.SELECT * IconStyle.SCALE_DOWN, AttachmentController.&selectTermM2OCloseModal as MC, term.id
                    else {
                        if (term.active)
                            rowAction ActionIcon.DELETE * IconStyle.SCALE_DOWN, AttachmentController.&deleteTerm as MC, term.id
                        rowAction ActionIcon.EDIT * IconStyle.SCALE_DOWN, AttachmentController.&editTerm as MC, term.id
                    }
                }
            }
        }
    }
}

