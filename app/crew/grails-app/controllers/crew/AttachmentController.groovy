package crew

import app.config.SupportedLanguage
import app.config.TermGroupConfig
import attachement.AttachmentSearchService
import attachement.AttachmentUiService
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.springframework.beans.factory.annotation.Value
import org.taack.Attachment
import org.taack.AttachmentDescriptor
import org.taack.Term
import org.taack.User
import taack.domain.*
import taack.render.TaackUiService
import taack.ui.base.*
import taack.ui.base.block.BlockSpec
import taack.ui.base.common.ActionIcon
import taack.ui.base.common.IconStyle
import taack.ui.base.common.Style
import taack.ui.utils.Markdown

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
        UiMenuSpecifier m = new UiMenuSpecifier()

        m.ui {
            menu AttachmentController.&index as MC
            menu 'Tagged', {
                for (def tagGroup : TermGroupConfig.values().findAll { it.active }) {
                    menu tagGroup.toString(), AttachmentController.&showTermGroup as MC, [group: tagGroup.toString()]
                }
            }
            menu AttachmentController.&listTerm as MC
            menuSearch this.&search as MC, q
            menuOptions(SupportedLanguage.fromContext())
        }
        m
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
        if (params.boolean('isAjax'))
            taackUiService.show(new UiBlockSpecifier().ui {
                modal {
                    ajaxBlock 'showAttachment', {
                        inline attachmentUiService.buildShowAttachmentBlock(attachment)
                    }
                }
            })
        else {
            taackUiService.show(new UiBlockSpecifier().ui {
                ajaxBlock 'showAttachment', {
                    inline attachmentUiService.buildShowAttachmentBlock(attachment)
                }
            }, buildMenu())
        }
    }

    def downloadAttachment(Attachment attachment) {
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
                form AttachmentUiService.buildAttachmentForm(new Attachment()), BlockSpec.Width.MAX
            }
        })
    }

    def editAttachmentDescriptor(AttachmentDescriptor attachmentDescriptor) {
        attachmentDescriptor ?= new AttachmentDescriptor()
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form AttachmentUiService.buildAttachmentDescriptorForm(attachmentDescriptor), BlockSpec.Width.MAX
            }
        })
    }

    @Transactional
    def saveAttachmentDescriptor() {
        AttachmentDescriptor ad = taackSaveService.save(AttachmentDescriptor, null, false)
        ad = AttachmentDescriptor.findOrSaveWhere(
                type: ad.type,
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

    def updateAttachment(Attachment attachment) {
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                form AttachmentUiService.buildAttachmentForm(attachment), BlockSpec.Width.MAX
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
                table "Object Referencing ${attachment.originalName}", new UiTableSpecifier().ui({
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
                }), BlockSpec.Width.MAX, {
                    action ActionIcon.GRAPH, this.&model as MethodClosure, [modelName: Attachment.name]
                }
            }
        })
    }

    def model(String modelName) {
        String graph = taackMetaModelService.modelGraph(modelName ? Class.forName(modelName) : null)
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock 'model$modelName', {
                    custom "Relation for domain: ${modelName}", taackMetaModelService.svg(graph)
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
        Attachment a = new Attachment()
        User u = new User()

        List<Term> parentTerms = Term.findAllByActiveAndTermGroupConfigAndParentIsNull(true, termGroup)
        UiTableSpecifier ts = new UiTableSpecifier()

        ts.ui {
            header {
                fieldHeader 'Name'
                fieldHeader 'Action'
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
                        rowGroupHeader "${t}"
                        rec(t)
                    }
                }
            }
        }
        taackUiService.show new UiBlockSpecifier().ui {
            table 'Tags', ts, BlockSpec.Width.THIRD
            show 'Files', new UiShowSpecifier().ui(new Object(), {
                field Markdown.getContentHtml('# Click on a tag ..')
            }), BlockSpec.Width.TWO_THIRD
        }, buildMenu()
    }

    def showTermAttachments(Term term) {
        Attachment a = new Attachment()
        AttachmentDescriptor ad = new AttachmentDescriptor()
        User u = new User()
        def attachments = Attachment.executeQuery('from Attachment a where a.active = true and ?0 in elements(a.tags)', term) as List<Attachment>
        def ts = new UiTableSpecifier().ui {
            header {
                column {
                    fieldHeader 'Preview'
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
                    fieldHeader 'Actions'
                }
            }

            iterate(taackFilterService.getBuilder(Attachment)
                    .setSortOrder(TaackFilter.Order.DESC, a.dateCreated_)
                    .setMaxNumberOfLine(20)
                    .addRestrictedIds(attachments*.id as Long[])
                    .build()) { Attachment aIt, Long counter ->

                row {
                    rowColumn {
                        rowField attachmentUiService.preview(aIt.id)
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
                        rowAction ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MC, aIt.id
                        rowAction ActionIcon.SHOW, AttachmentController.&showAttachment as MC, aIt.id
                    }
                }
            }
        }
        taackUiService.show new UiBlockSpecifier().ui {
            table "Files for ${term.name}", ts, BlockSpec.Width.TWO_THIRD
        }
    }

    def selectTagsM2M() {
        UiTableSpecifier ts = new UiTableSpecifier()
        List<Term> parentTerms = Term.findAllByActiveAndParentIsNull(true)
        Term t = new Term()

        ts.ui {
            header {
                fieldHeader t.name_
                fieldHeader t.termGroupConfig_
                fieldHeader 'Action'
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
                table 'Tags', ts, BlockSpec.Width.MAX
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
            tableFilter tr('default.filter.label'), f, tr('default.term.label'), t, BlockSpec.Width.MAX, {
                action ActionIcon.CREATE, AttachmentController.&editTerm as MC
            }
        }
        taackUiService.show(b, buildMenu())
    }

    @Secured(['ROLE_ADMIN', 'ROLE_TERM_ADMIN'])
    def editTerm(Term term) {
        term = term ?: new Term()
        UiBlockSpecifier b = new UiBlockSpecifier().ui {
            modal {
                form attachmentUiService.buildTermForm(term), BlockSpec.Width.MAX
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
                tableFilter tr('default.filter.label'), f, tr('default.term.label'), t, BlockSpec.Width.MAX
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
