package attachement

import attachment.Attachment
import attachment.DocumentAccess
import attachment.DocumentCategory
import attachment.Term
import crew.AttachmentController
import crew.User
import crew.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import grails.util.Triple
import grails.web.api.WebAttributes
import jakarta.annotation.PostConstruct
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.grails.datastore.gorm.GormEntity
import org.grails.plugins.web.taglib.ApplicationTagLib
import org.springframework.beans.factory.annotation.Autowired
import taack.domain.TaackAttachmentService
import taack.domain.TaackFilter
import taack.domain.TaackFilterService
import taack.ui.dsl.UiFilterSpecifier
import taack.ui.dsl.UiFormSpecifier
import taack.ui.dsl.UiShowSpecifier
import taack.ui.dsl.UiTableSpecifier
import taack.ui.dsl.block.BlockSpec
import taack.ui.dsl.common.ActionIcon
import taack.ui.dsl.common.IconStyle
import taack.ui.dsl.common.Style
import taack.ui.dsl.filter.expression.FilterExpression
import taack.ui.dsl.filter.expression.Operator
import taack.ui.dsl.helper.Utils
import taack.ui.dsl.table.TableOption

import static taack.render.TaackUiService.tr

@GrailsCompileStatic
final class AttachmentUiService implements WebAttributes {

    TaackAttachmentService taackAttachmentService
    TaackFilterService taackFilterService

    static lazyInit = false

    static AttachmentUiService INSTANCE = null

    @Autowired
    ApplicationTagLib applicationTagLib

    @PostConstruct
    void init() {
        INSTANCE = this
    }


    String preview(final Long id) {
        if (!id) return '<span/>'
        if (params.boolean('isPdf')) """<img style="max-height: 64px; max-width: 64px;" src="file://${taackAttachmentService.attachmentPreview(Attachment.read(id)).path}">"""
        else """<div style="text-align: center;"><img class="preview-img" style="max-height: 64px; max-width: 64px;" src="${applicationTagLib.createLink(controller: 'attachment', action: 'preview', id: id)}"></div>"""
    }

    String previewInline(Long id, boolean isInline) {
        Attachment a = Attachment.read(id)
        if (a && isInline) {
            if (a.contentType.contains('svg')) {
                """<img class='preview-img' style="max-height: 64px; max-width: 64px;" src="data:image/webp;base64, ${Base64.getEncoder().encodeToString(taackAttachmentService.attachmentPreview(a).bytes)}">"""
            } else {
                """<img class='preview-img' style="max-height: 64px; max-width: 64px;" src="data:${a.contentType};base64, ${Base64.getEncoder().encodeToString(taackAttachmentService.attachmentPreview(a).bytes)}">"""
            }
        } else if (a && !isInline)
            """<img class='preview-img' style="max-height: 64px; max-width: 64px;" src="/attachment/preview/${id}"/> """
        else
            ''
    }

    String preview(final Long id, TaackAttachmentService.PreviewFormat format) {
        if (!id) return '<span/>'
        if (format.isPdf) """<div style="text-align: center;"><img style="max-height: 64px; max-width: 64px;" src="file://${taackAttachmentService.attachmentPreview(Attachment.get(id), format).path}"></div>"""
        else """<div style="text-align: center;"><img style="max-height: ${format.previewPixelHeight}px; max-width: ${format.previewPixelWidth}px;" src="${applicationTagLib.createLink(controller: 'attachment', action: 'preview', id: id, params: [format: format.toString()])}"></div>"""
    }

    String previewFull(Long id, String p = null) {
        if (!id) return '<span/>'
        """<div style="text-align: center;"><img style="max-height: 420px" src="${applicationTagLib.createLink(controller: 'attachment', action: 'previewFull', id: id)}${p ? "?$p" : ""}"></div>"""
    }

    UiTableSpecifier buildAttachmentsTable(final UiFilterSpecifier f, final MC selectMC = null, final Long objectId = null, Long... ids) {
        Attachment a = new Attachment(active: true, userCreated: new User())
        UiTableSpecifier t = new UiTableSpecifier()
        t.ui(new TableOption.TableOptionBuilder().onDropAction(AttachmentController.&onDrop as MC).build()) {
            header {
                column {
                    label tr('default.preview.label')
                }
                column {
                    sortableFieldHeader a.originalName_
                    sortableFieldHeader a.dateCreated_
                }
                column {
                    sortableFieldHeader a.contentTypeEnum_
                }
                column {
                    sortableFieldHeader tr('default.userCreated.label'), a.userCreated_, a.userCreated.username_
                    sortableFieldHeader a.userCreated_, a.userCreated.subsidiary_
                }
            }

            iterate(taackFilterService.getBuilder(Attachment)
                    .setMaxNumberOfLine(8)
                    .addFilter(f)
                    //.addRestrictedIds(ids)
                    .setSortOrder(TaackFilter.Order.DESC, a.dateCreated_)
                    .build()) { Attachment att ->
                rowColumn {
                    rowFieldRaw this.preview(att.id)
                }
                rowColumn {
                    if (selectMC) {
                        rowAction ActionIcon.SELECT * IconStyle.SCALE_DOWN, selectMC as MC, att.id, [objectId: objectId]
                    } else {
                        rowAction ActionIcon.DOWNLOAD * IconStyle.SCALE_DOWN, AttachmentController.&downloadBinAttachment as MC, att.id
                    }
                    rowAction tr('default.preview.label'), ActionIcon.SHOW * IconStyle.SCALE_DOWN, AttachmentController.&showAttachmentIFrame as MC, att.id
                    rowAction(att.originalName, AttachmentController.&showAttachment as MC, att.id)
                    rowField att.dateCreated_
                }
                rowColumn {
                    rowField att.contentTypeEnum_
                }
                rowColumn {
                    rowField att.userCreated_
                    rowField att.userCreated.subsidiary_
                }
            }
        }
    }

    Closure<BlockSpec> buildAttachmentsBlock(final MC selectMC = null, final Long objectId = null) {
        Attachment a = new Attachment(active: true, userCreated: new User(), documentCategory: new DocumentCategory(category: null))
        UiFilterSpecifier f = new UiFilterSpecifier()
        f.ui Attachment, [objectId: objectId], {
            section tr('file.metadata.label'), {
                filterField a.originalName_
                filterField a.contentTypeEnum_
                filterField a.contentTypeCategoryEnum_
                filterField tr('default.userCreated.label'), a.userCreated_, a.userCreated.username_
                filterField a.userCreated_, a.userCreated.subsidiary_
                filterFieldExpressionBool tr('default.active.label'), new FilterExpression(true, Operator.EQ, a.active_)
            }
            section tr('default.documentCategory.label'), {
                filterField a.documentCategory_, a.documentCategory.category_
                filterField a.documentCategory_, a.documentCategory.tags_, new Term().termGroupConfig_
            }
        }

        UiTableSpecifier t = buildAttachmentsTable(f, selectMC, objectId)

        BlockSpec.buildBlockSpec {
            if (selectMC) {
                ajaxBlock 'selectingAttachmentBlock', { // Avoid opening a new modal: The current LIST page will be covered by EDIT page
                    tableFilter f, t, {
                        String selectUrl = "/${Utils.getControllerName(selectMC)}/${selectMC.method}?objectId=${objectId}"
                        menuIcon ActionIcon.CREATE, AttachmentController.&editAttachment as MC, [selectActionUrl: selectUrl]
                    }
                }
            } else {
                tableFilter f, t, {
                    menuIcon ActionIcon.CREATE, AttachmentController.&editAttachment as MC
                }
            }
        }
    }

    UiShowSpecifier buildShowAttachment(final Attachment attachment, boolean hasPreview = true) {
        new UiShowSpecifier().ui {
            if (hasPreview)
                section 'Preview', {
                    field this.previewFull(attachment.id)
                }
            section 'File Meta', {
                fieldLabeled attachment.originalName_
                fieldLabeled attachment.dateCreated_
                fieldLabeled attachment.userCreated_
                fieldLabeled attachment.fileSize_
                fieldLabeled attachment.contentTypeEnum_
            }
            section 'Attachment Meta', {
                fieldLabeled attachment.documentCategory_, attachment.documentCategory?.category_
                fieldLabeled attachment.documentCategory_, attachment.documentCategory?.tags_
                fieldLabeled attachment.documentAccess_
            }
            showAction tr('default.relatedData.label'), AttachmentController.&showLinkedData as MC, attachment.id
        }
    }

    UiFormSpecifier buildDocumentAccessForm(DocumentAccess docAccess) {
        new UiFormSpecifier().ui new DocumentAccess(), {
            section tr('default.documentAccess.label'), {
                field docAccess.isInternal_
                field docAccess.isRestrictedToMyBusinessUnit_
                field docAccess.isRestrictedToMyManagers_
                field docAccess.isRestrictedToEmbeddingObjects_
                field docAccess.isRestrictedToMySubsidiary_
            }
            formAction AttachmentController.&saveDocAccess as MC
        }
    }

    UiFormSpecifier buildDocumentCategoryForm(DocumentCategory docCat) {
        new UiFormSpecifier().ui docCat, {
            section tr('documentCategory.category.label'), {
                field docCat.category_
                ajaxField docCat.tags_, AttachmentController.&selectTermM2O as MC
            }
            formAction AttachmentController.&saveDocDesc as MC, docCat.id
        }
    }

    UiFormSpecifier buildAttachmentForm(Attachment attachment, String selectActionUrl = null) {
        new UiFormSpecifier().ui attachment, {
            section tr('file.metadata.label'), {
                if (attachment.originalName)
                    field attachment.originalName_
                field attachment.filePath_
                field attachment.writeAccess_
                ajaxField attachment.documentCategory_, AttachmentController.&selectDocumentCategory as MC, attachment.documentCategory_
                ajaxField attachment.documentAccess_, AttachmentController.&selectDocumentAccess as MC, attachment.documentAccess_
            }
            formAction AttachmentController.&saveAttachment as MC, attachment.id, [selectActionUrl: selectActionUrl]
        }
    }

    UiFormSpecifier buildTermForm(Term term) {
        new UiFormSpecifier().ui term, {
            section 'Term', {
                field term.name_
                field term.termGroupConfig_
                ajaxField term.parent_, AttachmentController.&selectTermM2O as MC
                tabs BlockSpec.Width.MAX, {
                    for (SupportedLanguage language : SupportedLanguage.values()) {
                        tabLabel "Translation ${language.name()}", {
                            fieldFromMap "Translation ${language.toString().toLowerCase()}", term.translations_, language.toString().toLowerCase()
                        }
                    }
                }
                field term.display_
                field term.active_
            }
            formAction AttachmentController.&saveTerm as MC, term.id
        }
    }

    UiFilterSpecifier buildTermFilter() {
        Term t = new Term()
        new UiFilterSpecifier().ui Term, {
            section 'Term', {
                filterField t.name_
                filterField t.termGroupConfig_
                filterFieldExpressionBool 'Display', new FilterExpression(true, Operator.EQ, t.display_)
                filterFieldExpressionBool 'Active', new FilterExpression(true, Operator.EQ, t.active_)
                filterFieldExpressionBool 'Hierarchy Showing Mode', new FilterExpression(null, Operator.EQ, t.parent_)
            }
        }
    }

    UiTableSpecifier buildTermTable(final UiFilterSpecifier f, boolean selectMode = false) {
        Term ti = new Term(parent: new Term())
        new UiTableSpecifier().ui {
            header {
                sortableFieldHeader ti.name_
                sortableFieldHeader ti.termGroupConfig_
                sortableFieldHeader ti.display_
                sortableFieldHeader ti.active_
            }
            Closure rec
            rec = { List<Term> termList ->
                for (Term term in termList) {
                    rowIndent {
                        List<Term> children = Term.findAllByActiveAndParent(true, term)
                        boolean hasChildren = children.size() > 0 && this.params.get('_filterExpression_parent_EQ') != '0'
                        rowTree hasChildren, {
                            rowColumn {
                                if (selectMode)
                                    rowAction ActionIcon.SELECT * IconStyle.SCALE_DOWN, term.id, term.toString()
                                else {
                                    if (term.active)
                                        rowAction ActionIcon.DELETE * IconStyle.SCALE_DOWN, AttachmentController.&deleteTerm as MC, term.id
                                    rowAction ActionIcon.EDIT * IconStyle.SCALE_DOWN, AttachmentController.&editTerm as MC, term.id
                                }
                                rowField term.name
                            }
                            rowField term.termGroupConfig_, null, Style.TAG
                            rowField term.display_
                            rowField term.active_
                        }
                        if (hasChildren) {
                            rec(children)
                        }
                    }
                }
            }
            List<Term> termList = []
            iterate(taackFilterService.getBuilder(Term)
                    .setMaxNumberOfLine(30)
                    .addFilter(f)
                    .setSortOrder(TaackFilter.Order.ASC, ti.name_)
                    .build()) { Term t ->
                termList.add(t)
            }
            rec(termList)
        }
    }

    UiTableSpecifier buildObjectAttachmentsTable(final GormEntity linkedObject, final Collection<Attachment> attachments, MethodClosure disassociateMC = null) {
        buildObjectAttachmentsTable([(linkedObject): new Triple(attachments*.id, null, disassociateMC)])
    }

    UiTableSpecifier buildObjectAttachmentsTable(Map<GormEntity, Triple<List<Long>, MethodClosure, MethodClosure>> objectAttachmentsMap) { // [linkedObject1: (attachmentIds, addFileMC, disassociateMC), linkedObject2: (...), ...]
        Attachment a = new Attachment(userCreated: new User())
        new UiTableSpecifier().ui {
            header {
                column {
                    label tr('default.preview.label')
                }
                column {
                    sortableFieldHeader a.originalName_
                    sortableFieldHeader a.dateCreated_
                }
                column {
                    sortableFieldHeader a.contentTypeEnum_
                }
                column {
                    sortableFieldHeader tr('default.userCreated.label'), a.userCreated_, a.userCreated.username_
                    sortableFieldHeader a.userCreated_, a.userCreated.subsidiary_
                }
                column {
                    label tr('default.actions.label')
                }
            }

            for (GormEntity linkedObject in objectAttachmentsMap.keySet()) {
                Triple<List<Long>, MethodClosure, MethodClosure> objectAttachmentsInfo = objectAttachmentsMap[linkedObject]
                List<Attachment> attachments = Attachment.getAll(objectAttachmentsInfo.aValue as Long[]).findAll { it.active }
                MethodClosure addFileMC = objectAttachmentsInfo.bValue
                MethodClosure disassociateMC = objectAttachmentsInfo.cValue
                if (objectAttachmentsMap.size() > 1) {
                    row {
                        rowColumn 5, {
                            if (addFileMC) {
                                rowAction ActionIcon.CREATE * IconStyle.SCALE_DOWN, addFileMC as MethodClosure, [objectId: linkedObject.ident()]
                            }
                            rowField linkedObject.toString() + ' :', Style.BOLD + Style.BLUE
                        }
                    }
                    if (attachments.size() == 0) {
                        row {
                            rowColumn {
                                rowField tr('attachment.no.label'), Style.TAG + Style.GREY_TAG
                            }
                            rowColumn {}
                            rowColumn {}
                            rowColumn {}
                            rowColumn {}
                        }
                    }
                }
                for (Attachment att in attachments) {
                    row {
                        rowColumn {
                            rowFieldRaw this.preview(att.id)
                        }
                        rowColumn {
                            rowAction ActionIcon.SHOW * IconStyle.SCALE_DOWN, AttachmentController.&showAttachment as MethodClosure, att.id
                            rowField att.originalName_, null, Style.BLUE
                            rowField att.dateCreated_
                        }
                        rowColumn {
                            rowField att.contentTypeEnum_
                        }
                        rowColumn {
                            rowField att.userCreated_
                            rowField att.userCreated.subsidiary_
                        }
                        rowColumn {
                            if (disassociateMC) {
                                rowAction ActionIcon.DELETE * IconStyle.SCALE_DOWN, disassociateMC, att.id, [objectId: linkedObject.ident()]
                            }
                            rowAction ActionIcon.DOWNLOAD * IconStyle.SCALE_DOWN, AttachmentController.&downloadBinAttachment as MethodClosure, att.id
                        }
                    }
                }
            }
        }
    }
}

