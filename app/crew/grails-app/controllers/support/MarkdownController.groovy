package support

import attachement.AttachmentUiService
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure
import attachment.Attachment
import taack.domain.TaackSaveService
import taack.render.TaackUiService
import taack.ui.dsl.UiBlockSpecifier
import taack.ui.dump.markdown.Markdown

@GrailsCompileStatic
@Secured(['isAuthenticated()'])
class MarkdownController {
    TaackUiService taackUiService
    AttachmentUiService attachmentUiService
    TaackSaveService taackSaveService

    def showPreview(String body) {
        render Markdown.getContentHtml(body)
    }

    def selectAttachment() {
        if (params['directUpload'] == "true") {
            redirect action: "uploadAttachment", params: [directUpload: true, isAjax: true]
            return
        }
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                inline(attachmentUiService.buildAttachmentsBlock(MarkdownController.&selectAttachmentCloseModal as MethodClosure, null, MarkdownController.&uploadAttachment as MethodClosure))
            }
        })
    }

    def selectAttachmentCloseModal(Attachment attachment) {
        UiBlockSpecifier block = new UiBlockSpecifier()
        block.ui {
            closeModal "/attachment/preview/${attachment.id}", attachment.toString()
        }
        taackUiService.show(block)
    }

    def uploadAttachment() {
        taackUiService.show(new UiBlockSpecifier().ui {
            modal {
                    form AttachmentUiService.buildAttachmentForm(new Attachment(), MarkdownController.&saveAttachment as MethodClosure, [directUpload: params['directUpload'] == "true"])
            }
        })
    }

    @Transactional
    def saveAttachment() {
        if (taackUiService.isProcessingForm()) {
            Attachment a = taackSaveService.save(Attachment)
            a.save(flush: true, failOnError: true)
            if (params['directUpload'] == "true") {
                selectAttachmentCloseModal(a)
            } else {
                taackUiService.cleanForm()
                taackSaveService.displayBlockOrRenderErrors(a, new UiBlockSpecifier().ui {
                    closeModalAndUpdateBlock attachmentUiService.buildAttachmentsBlock(MarkdownController.&selectAttachmentCloseModal as MethodClosure, null, MarkdownController.&uploadAttachment as MethodClosure)
                })
            }
        } else {
            taackUiService.show(new UiBlockSpecifier().ui {
                inline(attachmentUiService.buildAttachmentsBlock(MarkdownController.&selectAttachmentCloseModal as MethodClosure, null, MarkdownController.&uploadAttachment as MethodClosure))
            })
        }
    }
}
