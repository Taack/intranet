package crew

import attachement.AttachmentSearchService
import attachement.AttachmentUiService
import attachment.Attachment
import attachment.DocumentAccess
import attachment.DocumentCategory
import attachment.Term
import attachment.config.TermGroupConfig
import crew.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.springframework.beans.factory.annotation.Value
import org.taack.IAttachmentEditorIFrame
import org.taack.IAttachmentShowIFrame
import taack.domain.TaackAttachmentService
import taack.domain.TaackFilter
import taack.domain.TaackFilterService
import taack.domain.TaackMetaModelService
import taack.render.TaackSaveService
import taack.render.TaackUiService
import taack.ui.dsl.*
import taack.ui.dsl.block.BlockSpec
import taack.ui.dsl.common.ActionIcon
import taack.ui.dsl.common.IconStyle
import taack.ui.dsl.common.Style
import taack.ui.dsl.filter.expression.FilterExpression
import taack.ui.dsl.filter.expression.Operator
import taack.wysiwyg.Markdown

import static taack.render.TaackUiService.tr

@GrailsCompileStatic
@Secured(['IS_AUTHENTICATED_REMEMBERED'])
class AttachmentController {
    TaackUiService taackUiService
    TaackAttachmentService taackAttachmentService
    TaackMetaModelService taackMetaModelService
    AttachmentUiService attachmentUiService
    AttachmentSearchService attachmentSearchService
    TaackSaveService taackSaveService
    TaackFilterService taackFilterService

    @Value('${intranet.root}')
    String rootPath

    static private UiMenuSpecifier buildMenu(String q = null) {
        new UiMenuSpecifier().ui {
            menu AttachmentController.&index as MC
            label 'Tagged', {
                for (def tagGroup : TermGroupConfig.values().findAll { it.active }) {
                    subMenu tagGroup.toString(), AttachmentController.&showTermGroup as MC, [group: tagGroup.toString()]
                }
            }
            menu AttachmentController.&listTerm as MC
            menuSearch this.&search as MC, q
            menuOptions(SupportedLanguage.fromContext())
        }
    }

    def index() {
        taackUiService.show(new UiBlockSpecifier().ui {
            inline(attachmentUiService.buildAttachmentsBlock())
        }, buildMenu())
    }

    def search(String q) {
        taackUiService.show(attachmentSearchService.buildSearchBlock(q), buildMenu(q))
    }

    def preview(Attachment attachment, String format) {
        TaackAttachmentService.PreviewFormat f = format as TaackAttachmentService.PreviewFormat ?: TaackAttachmentService.PreviewFormat.DEFAULT
        response.setContentType('image/webp')
        response.setHeader('Content-disposition', 'filename=' + "${URLEncoder.encode((attachment?.getName() ?: 'noPreview.webp'), 'UTF-8')}")
        if (!attachment?.getName()) response.setHeader('Cache-Control', 'max-age=604800')
        response.outputStream << (taackAttachmentService.attachmentPreview(attachment, f)).bytes
        return false
    }

    def previewFull(Attachment attachment) {
        response.setContentType('image/webp')
        response.setHeader('Content-disposition', 'filename=' + "${URLEncoder.encode((attachment?.getName() ?: 'noPreview.webp'), 'UTF-8')}")
        if (!attachment?.getName()) response.setHeader('Cache-Control', 'max-age=604800')
        response.outputStream << (taackAttachmentService.attachmentPreview(attachment, TaackAttachmentService.PreviewFormat.PREVIEW_LARGE)).bytes
        return false
    }

    def showAttachment(Attachment attachment) {
        IAttachmentShowIFrame iFrame = TaackAttachmentService.additionalShowIFrame(attachment)
        IAttachmentEditorIFrame iEditor = TaackAttachmentService.additionalEditIFrame(attachment)
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                show this.attachmentUiService.buildShowAttachment(attachment), {
                    if (iEditor) {
                        menu 'EDIT', AttachmentController.&inlineEdition as MC, attachment.id
                    }
                    for (def ext in TaackAttachmentService.converterExtensions(attachment)) {
                        menu "Download ${ext}", AttachmentController.&downloadBinExtensionForAttachment as MC, [extension: ext, id: attachment.id]
                    }

                    if (iFrame)
                        menuIcon ActionIcon.SHOW, AttachmentController.&showAttachmentIFrame as MC, attachment.id
                    menuIcon ActionIcon.EDIT, AttachmentController.&editAttachment as MC, attachment.id
                    menuIcon ActionIcon.DOWNLOAD, AttachmentController.&downloadBinAttachment as MC, attachment.id
                    menuIcon ActionIcon.GRAPH, AttachmentController.&attachmentHistory as MC, attachment.id
                }
            }
        })
    }

    def attachmentHistory(Attachment attachment) {
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                List<Attachment> attachmentHistory = attachment.history
                table new UiTableSpecifier().ui({
                    header {
                        label tr('default.preview.label')
                        column {
                            label attachment.originalName_
                            label 'Version'
                        }
                        column {
                            label attachment.userCreated_
                            label attachment.dateCreated_
                        }
                    }

                    attachmentHistory.each { aIt ->
                        row {
                            rowFieldRaw this.attachmentUiService.preview(aIt.id)
                            rowColumn {
                                rowAction ActionIcon.DOWNLOAD * IconStyle.SCALE_DOWN, AttachmentController.&downloadBinAttachment as MC, aIt.id
                                rowField aIt.originalName
                                rowField "${aIt.version}"
                            }
                            rowColumn {
                                rowField aIt.userCreated_
                                rowField aIt.dateCreated_
                            }
                        }
                    }
                }), {
                    label tr('history.label')
                }
            }
        })
    }


    def inlineEdition(Attachment attachment) {
        String iFrame = TaackAttachmentService.editIFrame(attachment)
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                custom(iFrame)
            }
        })
    }


    def showAttachmentIFrame(Attachment attachment) {
        String iFrame = TaackAttachmentService.showIFrame(attachment)
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                if (iFrame) {
                    custom(iFrame)
                } else {
                    show this.attachmentUiService.buildShowAttachment(attachment), {
                        for (def ext in TaackAttachmentService.converterExtensions(attachment)) {
                            menu "Download ${ext}", AttachmentController.&downloadBinExtensionForAttachment as MC, [extension: ext, id: attachment.id]
                        }
                        menuIcon ActionIcon.EDIT, AttachmentController.&editAttachment as MC, attachment.id
                        menuIcon ActionIcon.DOWNLOAD, AttachmentController.&downloadBinAttachment as MC, attachment.id
                    }
                }
            }
        })
    }

    def downloadBinAttachment(Attachment attachment) {
        taackAttachmentService.downloadAttachment(attachment)
    }

    def renderAttachment(Attachment attachment) {
        if (attachment) {
            TaackAttachmentService.showIFrame(attachment)
        }
    }

    @Transactional
    def saveDocAccess(DocumentAccess ad) {
        DocumentAccess ad2 = DocumentAccess.findWhere(
                isInternal: ad.isInternal,
                isRestrictedToMyBusinessUnit: ad.isRestrictedToMyBusinessUnit,
                isRestrictedToMySubsidiary: ad.isRestrictedToMySubsidiary,
                isRestrictedToMyManagers: ad.isRestrictedToMyManagers,
                isRestrictedToEmbeddingObjects: ad.isRestrictedToEmbeddingObjects,
        )
        if (ad2) {
            ad = ad2
        } else {
            ad.save(flush: true)
        }
        taackSaveService.displayBlockOrRenderErrors(ad, new UiBlockSpecifier().ui {
            closeModal(ad.id, ad.toString())
        })
    }

    @Transactional
    def saveDocDesc() {
        DocumentCategory dc = taackSaveService.save(DocumentCategory, null, true)
        dc.save(flush: true, failOnError: true)
        if (dc.hasErrors()) {
            log.error "${dc.errors}"
        } else {
            log.info "DocumentCategory $dc"
        }
        taackSaveService.displayBlockOrRenderErrors(
                dc,
                new UiBlockSpecifier().ui {
                    closeModal(dc.id, dc.toString())
                }
        )
    }

    def editAttachment(Attachment attachment) {
        taackUiService.show(new UiBlockSpecifier().ui {
            String selectActionUrl = params.get('selectActionUrl')
            if (!selectActionUrl) {
                modal {
                    form attachmentUiService.buildAttachmentForm(attachment ?: new Attachment()), TaackAttachmentService.additionalCreate?.closure
                }
            } else {
                ajaxBlock 'selectingAttachmentBlock', {
                    form attachmentUiService.buildAttachmentForm(new Attachment(), selectActionUrl), TaackAttachmentService.additionalCreate?.closure
                }
            }

        })
    }

    @Transactional
    def saveAttachment() {
        Attachment a = taackSaveService.save(Attachment)
        if (a.validate()) {
            String selectActionUrl = params.get('selectActionUrl')
            if (selectActionUrl) {
                redirect url: selectActionUrl.replace('&#61;', '=') + "&id=${a.id}&isAjax=true"
            } else {
                taackUiService.ajaxReload()
            }
        } else {
            taackSaveService.redirectOrRenderErrors(a, null)
        }
    }

    def showLinkedData(Attachment attachment) {
        def objs = taackMetaModelService.listObjectsPointingTo(attachment)
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                table new UiTableSpecifier().ui({
                    for (def classNameField : objs.keySet()) {
                        row {
                            rowColumn 3, {
                                rowField "${classNameField.aValue}: ${classNameField.bValue}", Style.EMPHASIS + Style.BLUE
                            }
                            rowColumn {
                                rowAction ActionIcon.GRAPH, this.&model as MC, [modelName: classNameField.aValue]
                            }
                        }
                        for (def obj : objs[classNameField]) {
                            row {
                                rowField obj.toString()
                                rowField((obj.hasProperty('userCreated') ? obj['userCreated'] : '') as String)
                                rowField(((obj.hasProperty('dateCreated') ? obj['dateCreated'] : null) as Date)?.toString())
                                rowField((obj.hasProperty('version') ? obj['version'] : '??') as String)
                            }
                        }
                    }
                }), {
                    menuIcon ActionIcon.GRAPH, this.&model as MC, [modelName: Attachment.name]
                }
            }
        })
    }

    def model(String modelName) {
        String graph = taackMetaModelService.modelGraph(modelName ? Class.forName(modelName) : null)
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock 'model$modelName', {
                    custom taackMetaModelService.svg(graph)
                }
            }
        })
    }

    def downloadBinExtensionForAttachment(Attachment attachment) {
        String ext = (params.get('extension') as String)?.toLowerCase()
        def f = TaackAttachmentService.convertExtension(attachment, ext)
        if (f?.exists()) {
            response.setContentType("application/${ext}")
            response.setHeader('Content-disposition', 'filename=' + "${URLEncoder.encode("${attachment.originalName}.${ext}", 'UTF-8')}")
            response.outputStream << f.bytes
        } else return null
    }

    def showTermGroup(String group) {
        def termGroup = group as TermGroupConfig

        List<Term> parentTerms = Term.findAllByActiveAndTermGroupConfigAndParentIsNull(true, termGroup)
        UiTableSpecifier ts = new UiTableSpecifier()

        ts.ui {
            header {
                label 'Name'
            }

            Closure rec
            rec = { Term term ->
                rowIndent {
                    def children = Term.findAllByParentAndActive term, true
                    boolean termHasChildren = !children.isEmpty()
                    rowTree termHasChildren, {
                        rowColumn {
                            rowAction ActionIcon.SHOW * IconStyle.SCALE_DOWN, this.&showTermAttachments as MC, term.id
                            rowField term.name
                        }
                    }
                    if (termHasChildren) {
                        for (def tc : children) rec(tc)
                    }
                }
            }
            for (Term t in parentTerms) {
                rec(t)
            }
        }
        taackUiService.show new UiBlockSpecifier().ui {
            row {
                col BlockSpec.Width.QUARTER, {
                    table ts, {
                        label "Tags (${termGroup.toString()})"
                    }
                }
                col BlockSpec.Width.THREE_QUARTER, {
                    ajaxBlock('taggedFiles') {
                        show new UiShowSpecifier().ui({
                            field Markdown.getContentHtml('## Click on a tag ..')
                        }), {
                            label 'Files'
                        }
                    }
                }
            }
        }, buildMenu()
    }

    def showTermAttachments(Term term) {
        List<Attachment> attachments = Attachment.executeQuery('from Attachment a where a.active = true and ?0 in elements(a.documentCategory.tags)', term) as List<Attachment>
        taackUiService.show(new UiBlockSpecifier().ui {
            ajaxBlock('taggedFiles') {
                table attachmentUiService.buildObjectAttachmentsTable(term, attachments), {
                    label "Files (${term.name})"
                }
            }
        })
    }

    def selectAttachment() {
        Attachment a = new Attachment(documentCategory: new DocumentCategory(), userCreated: new User())
        taackUiService.createModal {
            tableFilter(new UiFilterSpecifier().ui(Attachment, {
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
            }), new UiTableSpecifier().ui {
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
                        .setSortOrder(TaackFilter.Order.DESC, a.dateCreated_)
                        .build()) { Attachment att ->
                    rowColumn {
                        rowFieldRaw this.attachmentUiService.preview(att.id)
                    }
                    rowColumn {
                        rowAction ActionIcon.SELECT * IconStyle.SCALE_DOWN, att.id, att.toString()
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
            })
        }
    }

    def selectDocumentAccess() {
        DocumentAccess documentAccess = taackUiService.ajaxBind(DocumentAccess)
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form attachmentUiService.buildDocumentAccessForm(documentAccess)
            }
        })
    }

    def selectDocumentCategory() {
        DocumentCategory documentCategory = taackUiService.ajaxBind(DocumentCategory)
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form attachmentUiService.buildDocumentCategoryForm(documentCategory)
            }
        })
    }

    def listTerm() {
        UiBlockSpecifier b = new UiBlockSpecifier()
        UiFilterSpecifier f = attachmentUiService.buildTermFilter()
        UiTableSpecifier t = attachmentUiService.buildTermTable f
        b.ui {
            tableFilter f, t, {
                menuIcon ActionIcon.CREATE, AttachmentController.&editTerm as MC
            }
        }
        taackUiService.show(b, buildMenu())
    }

    @Secured(['ROLE_ADMIN', 'ROLE_TERM_ADMIN'])
    def editTerm(Term term) {
        term = term ?: new Term()
        UiBlockSpecifier b = new UiBlockSpecifier().ui {
            modal {
                form attachmentUiService.buildTermForm(term)
            }
        }
        taackUiService.show(b)
    }

    @Transactional
    @Secured(['ROLE_ADMIN', 'ROLE_TERM_ADMIN'])
    def saveTerm() {
        taackSaveService.saveThenReloadOrRenderErrors(Term, null)
    }

    @Transactional
    @Secured(['ROLE_ADMIN', 'ROLE_TERM_ADMIN'])
    def deleteTerm(Term term) {
        term.active = false
        redirect action: 'listTerm'
    }

    def selectTermM2O() {
        UiFilterSpecifier f = attachmentUiService.buildTermFilter()
        UiTableSpecifier t = attachmentUiService.buildTermTable f, true
        UiBlockSpecifier b = new UiBlockSpecifier()
        b.ui {
            modal {
                tableFilter f, t
            }
        }
        taackUiService.show(b)
    }
}
