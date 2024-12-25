package crew

import attachement.AttachmentSearchService
import attachement.AttachmentUiService
import attachment.*
import attachment.config.TermGroupConfig
import crew.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.springframework.beans.factory.annotation.Value
import taack.domain.*
import taack.render.TaackUiService
import taack.ui.dsl.*
import taack.ui.dsl.block.BlockSpec
import taack.ui.dsl.common.ActionIcon
import taack.ui.dsl.common.IconStyle
import taack.ui.dsl.common.Style
import taack.wysiwyg.Markdown

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
        response.setHeader('Content-disposition', "filename=" + "${URLEncoder.encode((attachment?.getName() ?: 'noPreview.webp'), 'UTF-8')}")
        if (!attachment?.getName()) response.setHeader('Cache-Control', 'max-age=604800')
        response.outputStream << (taackAttachmentService.attachmentPreview(attachment, f)).bytes
        return false
    }

    def previewFull(Attachment attachment) {
        response.setContentType('image/webp')
        response.setHeader('Content-disposition', "filename=" + "${URLEncoder.encode((attachment?.getName() ?: 'noPreview.webp'), 'UTF-8')}")
        if (!attachment?.getName()) response.setHeader('Cache-Control', 'max-age=604800')
        response.outputStream << (taackAttachmentService.attachmentPreview(attachment, TaackAttachmentService.PreviewFormat.PREVIEW_LARGE)).bytes
        return false
    }

    def showAttachment(Attachment attachment) {
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                show this.attachmentUiService.buildShowAttachment(attachment), {
                    menu AttachmentController.&showLinkedData as MC, attachment.id
                    menuIcon ActionIcon.EDIT, AttachmentController.&updateAttachment as MC, attachment.id
                    menuIcon ActionIcon.DOWNLOAD, AttachmentController.&downloadBinAttachment as MC, attachment.id
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


    def uploadAttachment() {
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form AttachmentUiService.buildAttachmentForm(new Attachment())
            }
        })
    }

    def editAttachmentDescriptor(DocumentAccess attachmentDescriptor) {
        attachmentDescriptor ?= new DocumentAccess()
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form AttachmentUiService.buildDocumentAccessForm(attachmentDescriptor)
            }
        })
    }

    @Transactional
    def saveDocAccess() {
        params['id'] = null
        DocumentAccess ad = taackSaveService.save(DocumentAccess, null, false)
        ad = DocumentAccess.findOrCreateWhere(
                isInternal: ad.isInternal,
                isRestrictedToMyBusinessUnit: ad.isRestrictedToMyBusinessUnit,
                isRestrictedToMySubsidiary: ad.isRestrictedToMySubsidiary,
                isRestrictedToMyManagers: ad.isRestrictedToMyManagers,
                isRestrictedToEmbeddingObjects: ad.isRestrictedToEmbeddingObjects,
        )
        if (!ad.id)
            ad.save(flush: true)
        taackSaveService.displayBlockOrRenderErrors(ad,
                new UiBlockSpecifier().ui {
                    closeModal(ad.id, ad.toString())
                }
        )
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


    def updateAttachment(Attachment attachment) {
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form AttachmentUiService.buildAttachmentForm(attachment)
            }
        })
    }

    @Transactional
    @Secured(['ROLE_ADMIN', 'ROLE_ATT_USER'])
    def saveAttachment() {
        taackSaveService.saveThenReloadOrRenderErrors(Attachment)
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
                            rowAction ActionIcon.GRAPH, this.&model as MethodClosure, [modelName: classNameField.aValue]
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
                    menuIcon ActionIcon.GRAPH, this.&model as MethodClosure, [modelName: Attachment.name]
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

    def extensionForAttachment(Attachment attachment) {
        String ext = (params['extension'] as String)?.toLowerCase()
        def f = TaackAttachmentService.convertExtension(attachment, ext)
        if (f?.exists()) {
            response.setContentType("application/${ext}")
            response.setHeader('Content-disposition', "filename=" + "${URLEncoder.encode('${attachment.originalNameWithoutExtension}.${ext}', 'UTF-8')}\"")
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
                        rowField term.name
                        rowAction ActionIcon.SHOW * IconStyle.SCALE_DOWN, this.&showTermAttachments as MC, term.id
                    }
                    if (termHasChildren) {
                        for (def tc : children) rec(tc)
                    }
                }
            }

            if (parentTerms) {
                for (Term t in parentTerms) {
                    if (t) {
                        row {
                            rowField "${t}"
                        }
                        rec(t)
                    }
                }
            }
        }
        taackUiService.show new UiBlockSpecifier().ui {
            row {
                col BlockSpec.Width.QUARTER, {
                    table ts
                }
                col BlockSpec.Width.THREE_QUARTER, {
                    ajaxBlock('taggedFiles') {
                        show new UiShowSpecifier().ui {
                            field Markdown.getContentHtml('# Click on a tag ..')
                        }
                    }
                }
            }
        }, buildMenu()
    }

    def showTermAttachments(Term term) {
        Attachment a = new Attachment()
        User u = new User()
        def attachments = Attachment.executeQuery('from Attachment a where a.active = true and ?0 in elements(a.documentCategory.tags)', term) as List<Attachment>
        def ts = new UiTableSpecifier().ui {
            header {
                column {
                    label 'Preview'
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
                    label 'Actions'
                }
            }

            iterate(taackFilterService.getBuilder(Attachment)
                    .setSortOrder(TaackFilter.Order.DESC, a.dateCreated_)
                    .setMaxNumberOfLine(20)
                    .addRestrictedIds(attachments*.id as Long[])
                    .build()) { Attachment aIt ->

                rowColumn {
                    rowField this.attachmentUiService.preview(aIt.id)
                }
                rowColumn {
                    rowField aIt.originalName
                    rowField aIt.dateCreated_
                }
                rowColumn {
                    rowField aIt.fileSize_
                    rowField aIt.contentType
                }
                rowColumn {
                    rowField aIt.userCreated.username
                    rowField aIt.userCreated.subsidiary.toString()
                }
                rowColumn {
                    rowAction ActionIcon.DOWNLOAD, AttachmentController.&downloadBinAttachment as MC, aIt.id
                    rowAction ActionIcon.SHOW, AttachmentController.&showAttachment as MC, aIt.id
                }
            }
        }
        taackUiService.show(new UiBlockSpecifier().ui {
            ajaxBlock('taggedFiles') {
                table ts
            }
        })
    }

    def selectDocumentAccess() {
        TaackDocument td = taackUiService.ajaxBind(TaackDocument)
        DocumentAccess documentAccess = td.documentAccess ?: new DocumentAccess()
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock 'selectDocumentAccessTable', {
                    table new UiTableSpecifier().ui({
                        header {
                            label documentAccess.isInternal_
                            label documentAccess.isRestrictedToMyManagers_
                            label documentAccess.isRestrictedToMyBusinessUnit_
                            label documentAccess.isRestrictedToMySubsidiary_
                            label documentAccess.isRestrictedToEmbeddingObjects_
                        }
                        for (DocumentAccess da in DocumentAccess.list()) {
                            row {
                                rowColumn {
                                    rowAction ActionIcon.SELECT * IconStyle.SCALE_DOWN, this.&selectDocumentAccessCloseModal as MC, da.id
                                    rowField da.isInternal_
                                }
                                rowField da.isRestrictedToMyManagers_
                                rowField da.isRestrictedToMyBusinessUnit_
                                rowField da.isRestrictedToMySubsidiary_
                                rowField da.isRestrictedToEmbeddingObjects_
                            }
                        }
                    })
                }
                ajaxBlock 'selectDocumentAccessForm', {
                    form AttachmentUiService.buildDocumentAccessForm(documentAccess)
                }
            }
        })
    }

    def selectDocumentAccessCloseModal(DocumentAccess documentAccess) {
        taackUiService.closeModal(documentAccess.id, documentAccess.toString())
    }

    def selectDocumentCategory() {
        DocumentCategory documentCategory = taackUiService.ajaxBind(DocumentCategory)
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form AttachmentUiService.buildDocumentDescriptorForm(documentCategory)
            }
        })
    }

    def selectTagsM2M() {
        List<Term> parentTerms = Term.findAllByActiveAndParentIsNull(true)
        Term t = new Term()

        UiTableSpecifier ts = new UiTableSpecifier().ui {
            header {
                label t.name_
                label t.termGroupConfig_
                label 'Action'
            }
            Closure rec

            rec = { Term term ->
                rowIndent {
                    def children = Term.findAllByParentAndActive term, true
                    boolean termHasChildren = !children.isEmpty()
                    rowTree termHasChildren, {
                        rowField term.name
                        rowField term.termGroupConfig?.toString()
                        rowAction ActionIcon.SELECT * IconStyle.SCALE_DOWN, this.&selectTagsM2MCloseModal as MethodClosure, term.id
                    }
                    if (termHasChildren) {
                        for (def tc : children) rec(tc)
                    }
                }
            }

            if (parentTerms) {
                for (Term pt in parentTerms) {
                    if (pt) {
                        rec(pt)
                    }
                }
            }
        }
        taackUiService.show new UiBlockSpecifier().ui {
            modal {
                table ts
            }
        }
    }

    def selectTagsM2MCloseModal(Term term) {
        taackUiService.closeModal(term.id, term.toString())
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

    @Secured(['ROLE_ADMIN', 'ROLE_TERM_ADMIN'])
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

    @Secured(['ROLE_ADMIN', 'ROLE_TERM_ADMIN'])
    def selectTermM2OCloseModal(Term term) {
        UiBlockSpecifier block = new UiBlockSpecifier()
        block.ui {
            closeModal term.id, "${term}"
        }
        taackUiService.show(block)
    }

}
