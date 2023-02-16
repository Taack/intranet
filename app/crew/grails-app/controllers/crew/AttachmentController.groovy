package crew

import app.config.TermGroupConfig
import attachement.AttachmentSearchService
import attachement.AttachmentSecurityService
import attachement.AttachmentUiService
import grails.artefact.Controller
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.springframework.beans.factory.annotation.Value
import org.taack.Attachment
import org.taack.Term
import org.taack.User
import taack.base.TaackMetaModelService
import taack.base.TaackSimpleAttachmentService
import taack.base.TaackSimpleFilterService
import taack.base.TaackSimpleSaveService
import taack.base.TaackUiSimpleService
import taack.ui.base.UiBlockSpecifier
import taack.ui.base.UiFilterSpecifier
import taack.ui.base.UiMenuSpecifier
import taack.ui.base.UiShowSpecifier
import taack.ui.base.UiTableSpecifier
import taack.ui.base.block.BlockSpec
import taack.ui.base.common.ActionIcon
import taack.ui.base.common.ActionIconStyleModifier
import taack.ui.base.common.Style
import taack.ui.base.table.ColumnHeaderFieldSpec
import taack.ui.utils.Markdown

@GrailsCompileStatic
@Secured(["IS_AUTHENTICATED_REMEMBERED"])
class AttachmentController implements Controller {
    TaackUiSimpleService taackUiSimpleService
    TaackSimpleAttachmentService taackSimpleAttachmentService
    TaackMetaModelService taackMetaModelService
    AttachmentUiService attachmentUiService
    AttachmentSearchService attachmentSearchService
    AttachmentSecurityService attachmentSecurityService
    TaackSimpleSaveService taackSimpleSaveService
    SpringSecurityService springSecurityService
    TaackSimpleFilterService taackSimpleFilterService

    @Value('${intranet.root}')
    String rootPath

    static private UiMenuSpecifier buildMenu(String q = null) {
        UiMenuSpecifier m = new UiMenuSpecifier()

        m.ui {
            menu "List Files", AttachmentController.&index as MC
            menu "Tagged", {
                for (def tagGroup : TermGroupConfig.values().findAll { it.active }) {
                    menu tagGroup.toString(), AttachmentController.&showTermGroup as MC, [group: tagGroup.toString()]
                }
            }
            menu "Terms", AttachmentController.&listTerm as MC
            menuSearch this.&search as MC, q
        }
        m
    }

    def index() {
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            inline(attachmentUiService.buildAttachmentsBlock())
        }, buildMenu())
    }

    def search(String q) {
        taackUiSimpleService.show(attachmentSearchService.buildSearchBlock(q), buildMenu(q))
    }

    def preview(Attachment attachment, String format) {
        TaackSimpleAttachmentService.PreviewFormat f = format as TaackSimpleAttachmentService.PreviewFormat ?: TaackSimpleAttachmentService.PreviewFormat.DEFAULT
        response.setContentType("image/webp")
        response.setHeader("Content-disposition", "filename=\"${URLEncoder.encode(attachment?.getName() ?: 'noPreview.webp', 'UTF-8')}\"")
        if (!attachment?.getName()) response.setHeader("Cache-Control", "max-age=604800")
        response.outputStream << (taackSimpleAttachmentService.attachmentPreview(attachment, f)).bytes
        return false
    }

    def previewFull(Attachment attachment) {
        response.setContentType("image/webp")
        response.setHeader("Content-disposition", "filename=\"${URLEncoder.encode(attachment?.getName() ?: "noPreview.webp", "UTF-8")}\"")
        if (!attachment?.getName()) response.setHeader("Cache-Control", "max-age=604800")
        response.outputStream << (taackSimpleAttachmentService.attachmentPreview(attachment, TaackSimpleAttachmentService.PreviewFormat.PREVIEW_LARGE)).bytes
        return false
    }

    def showAttachment(Attachment attachment) {
        if (params.boolean("isAjax"))
            taackUiSimpleService.show(new UiBlockSpecifier().ui {
                modal {
                    ajaxBlock "showAttachment", {
                        inline attachmentUiService.buildShowAttachmentBlock(attachment)
                    }
                }
            })
        else {
            taackUiSimpleService.show(new UiBlockSpecifier().ui {
                ajaxBlock "showAttachment", {
                    inline attachmentUiService.buildShowAttachmentBlock(attachment)
                }
            }, buildMenu())
        }
    }

    def downloadAttachment(Attachment attachment) {
        // TODO: Add Simple Security Layer here..
        if (!attachmentSecurityService.canDownloadFile(attachment, springSecurityService.currentUser as User)) {
            taackUiSimpleService.show(CrewUiService.messageBlock("<p>Not Allowed ...</p>"))

        }
        taackSimpleAttachmentService.downloadAttachment(attachment)
    }

    def renderAttachment(Attachment attachment) {
        if (attachment) {
            TaackSimpleAttachmentService.showIFrame(attachment)
        }
    }


    def uploadAttachment() {
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock "uploadAttachment", {
                    form "Upload a File", AttachmentUiService.buildAttachmentForm(new Attachment(fileOrigin: controllerName)), BlockSpec.Width.MAX
                }
            }
        })
    }

    def updateAttachment(Attachment attachment) {
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock "updateAttachment", {
                    form "Update a File", AttachmentUiService.buildAttachmentForm(attachment), BlockSpec.Width.MAX
                }
            }
        })
    }

    @Transactional
    @Secured(['ROLE_ADMIN', 'ROLE_ATT_USER'])
    def saveAttachment() {
        if (taackUiSimpleService.isProcessingForm()) {
            def a = attachmentUiService.saveAttachment()
            a.save(flush: true)
            taackUiSimpleService.cleanForm()
            taackSimpleSaveService.displayBlockOrRenderErrors(a, new UiBlockSpecifier().ui {
                closeModalAndUpdateBlock attachmentUiService.buildAttachmentsBlock()
            })
        } else {
            taackUiSimpleService.show(new UiBlockSpecifier().ui {
                inline(attachmentUiService.buildAttachmentsBlock())
            })
        }
    }

    def showLinkedData(Attachment attachment) {
        def objs = taackMetaModelService.listObjectsPointingTo(attachment)
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock "showLinkedData", {
                    table "Object Referencing ${attachment.originalName}", new UiTableSpecifier().ui(Attachment, {
                        for (def classNameField : objs.keySet()) {
                            row {
                                rowColumn 3, {
                                    rowField "${classNameField.aValue}: ${classNameField.bValue}", Style.EMPHASIS + Style.BLUE
                                }
                                rowLink "Graph", ActionIcon.GRAPH, this.&model as MethodClosure, [modelName: classNameField.aValue], true
                            }
                            for (def obj : objs[classNameField]) {
                                row {
                                    rowField obj.toString()
                                    rowField((obj.hasProperty("userCreated") ? obj["userCreated"] : "") as String)
                                    rowField((obj.hasProperty("dateCreated") ? obj["dateCreated"] : null) as Date)
                                    rowField((obj.hasProperty("version") ? obj["version"] : "??") as String)
                                }
                            }
                        }
                    }), BlockSpec.Width.MAX, {
                        action "Graph", ActionIcon.GRAPH, this.&model as MethodClosure, [modelName: Attachment.name], true
                    }
                }
            }
        })
    }

    def model(String modelName) {
        String graph = taackMetaModelService.modelGraph(modelName ? Class.forName(modelName) : null)
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock "model$modelName", {
                    custom "Relation for domain: ${modelName}", taackMetaModelService.svg(graph)
                }
            }
        })
    }

    def extensionForAttachment(Attachment attachment) {
        if (!attachmentSecurityService.canDownloadFile(attachment, springSecurityService.currentUser as User)) {
            taackUiSimpleService.show(CrewUiService.messageBlock("<p>Not Allowed ...</p>"))
            return
        }
        String ext = (params["extension"] as String)?.toLowerCase()
        def f = TaackSimpleAttachmentService.convertExtension(attachment, ext)
        if (f?.exists()) {
            response.setContentType("application/${ext}")
            response.setHeader("Content-disposition", "filename=\"${URLEncoder.encode("${attachment.originalNameWithoutExtension}.${ext}", "UTF-8")}\"")
            response.outputStream << f.bytes
        } else return null
    }

    def showTermGroup(String group) {
        def termGroup = group as TermGroupConfig
        Attachment a = new Attachment()
        User u = new User()

        List<Term> parentTerms = Term.findAllByActiveAndTermGroupConfigAndParentIsNull(true, termGroup)
        UiTableSpecifier ts = new UiTableSpecifier()

        ts.ui Term, {
            header {
                fieldHeader "Name"
                fieldHeader "Action"
            }
            Closure rec

            rec = { Term term ->
                rowIndent {
                    def children = Term.findAllByParentAndActive term, true
                    boolean termHasChildren = !children.isEmpty()
                    rowTree termHasChildren, {
                        rowField term.name
                        rowLink "See Attachments", ActionIcon.SHOW * ActionIconStyleModifier.SCALE_DOWN, this.&showTermAttachments as MethodClosure, term.id, true
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
        taackUiSimpleService.show new UiBlockSpecifier().ui {
            ajaxBlock "tableTerm", {
                table "Tags", ts, BlockSpec.Width.THIRD
            }
            ajaxBlock "tableAttachments", {
                show "Files", new UiShowSpecifier().ui(new Object(), {
                    field Markdown.getContentHtml("# Click on a tag ..")
                }), BlockSpec.Width.TWO_THIRD
            }
        }, buildMenu()
    }

    def showTermAttachments(Term term) {
        Attachment a = new Attachment()
        User u = new User()
        def attachments = Attachment.executeQuery('from Attachment a where a.active = true and ?0 in elements(a.tags)', term) as List<Attachment>
        def ts = new UiTableSpecifier()
        ts.ui Attachment, {
            ColumnHeaderFieldSpec.SortableDirection defaultSort
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
            def objects = taackSimpleFilterService.list(Attachment, 20, null, null, defaultSort, attachments*.id)
            paginate(20, params.int('offset'), objects.bValue)
            for (def att : objects.aValue) {
                row att, {
                    rowColumn {
                        rowField attachmentUiService.preview(att.id)
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
                        rowField att.userCreated.subsidiary.toString()
                    }
                    rowColumn {
                        if (attachmentSecurityService.canDownloadFile(att)) rowLink "Download", ActionIcon.DOWNLOAD, AttachmentController.&downloadAttachment as MethodClosure, att.id, false
                        rowLink "Show", ActionIcon.SHOW, AttachmentController.&showAttachment as MethodClosure, att.id
                    }
                }
            }
        }
        taackUiSimpleService.show new UiBlockSpecifier().ui {
            ajaxBlock "tableAttachments", {
                table "Files for ${term.name}", ts, BlockSpec.Width.TWO_THIRD
            }
        }
    }

    def selectTagsM2M() {
        UiTableSpecifier ts = new UiTableSpecifier()
        List<Term> parentTerms = Term.findAllByActiveAndParentIsNull(true)

        ts.ui Term, {
            header {
                fieldHeader "Name"
                fieldHeader "Group"
                fieldHeader "Action"
            }
            Closure rec

            rec = { Term term ->
                rowIndent {
                    def children = Term.findAllByParentAndActive term, true
                    boolean termHasChildren = !children.isEmpty()
                    rowTree termHasChildren, {
                        rowField term.name
                        rowField term.termGroupConfig?.toString()
                        rowLink "Select Tag", ActionIcon.SELECT * ActionIconStyleModifier.SCALE_DOWN, this.&selectTagsM2MCloseModal as MethodClosure, term.id, true
                    }
                    if (termHasChildren) {
                        for (def tc : children) rec(tc)
                    }
                }
            }

            if (parentTerms) {
                for (Term t in parentTerms) {
                    if (t) {
                        rec(t)
                    }
                }
            }
        }
        taackUiSimpleService.show new UiBlockSpecifier().ui {
            modal {
                ajaxBlock "tableTermSelect", {
                    table "Tags", ts, BlockSpec.Width.MAX
                }
            }
        }
    }

    def selectTagsM2MCloseModal(Term term) {
        taackUiSimpleService.closeModal(term.id, term.toString())
    }

    def listTerm() {
        UiBlockSpecifier b = new UiBlockSpecifier()
        UiFilterSpecifier f = attachmentUiService.buildTermFilter()
        UiTableSpecifier t = attachmentUiService.buildTermTable f
        b.ui {
            ajaxBlock "listTerm", {
                tableFilter "Filter", f, "Terms", t, BlockSpec.Width.MAX, {
                    action "Create term", ActionIcon.CREATE, AttachmentController.&editTerm as MethodClosure, true
                }
            }
        }
        taackUiSimpleService.show(b, buildMenu())
    }

    @Secured(['ROLE_ADMIN', 'ROLE_TERM_ADMIN'])
    def editTerm(Term term) {
        String title = term ? "Edit term" : "New term"
        term = term ?: new Term()
        UiBlockSpecifier b = new UiBlockSpecifier().ui {
            modal {
                ajaxBlock "editTerm", {
                    form title, attachmentUiService.buildTermForm(term), BlockSpec.Width.MAX
                }
            }
        }
        taackUiSimpleService.show(b)
    }

    @Transactional
    @Secured(['ROLE_ADMIN', 'ROLE_TERM_ADMIN'])
    def saveTerm() {
        taackSimpleSaveService.saveThenReloadOrRenderErrors(Term, null)
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
            modal !params.boolean("refresh"), {
                ajaxBlock "termListSelect", {
                    tableFilter "Filter", f, "Terms", t, BlockSpec.Width.MAX
                }
            }
        }
        taackUiSimpleService.show(b)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_TERM_ADMIN'])
    def selectTermM2OCloseModal(Term term) {
        UiBlockSpecifier block = new UiBlockSpecifier()
        block.ui {
            closeModal term.id, "${term}"
        }
        taackUiSimpleService.show(block)
    }
}
