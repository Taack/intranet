package support

import attachement.AttachmentUiService
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure
import org.taack.Attachment
import taack.base.TaackSimpleSaveService
import taack.render.TaackUiSimpleService
import taack.ui.base.UiBlockSpecifier
import taack.ui.base.block.BlockSpec
import taack.ui.utils.Markdown

@GrailsCompileStatic
@Secured(['isAuthenticated()'])
class MarkdownController {
    TaackUiSimpleService taackUiSimpleService
    AttachmentUiService attachmentUiService
    TaackSimpleSaveService taackSimpleSaveService

    def showPreview(String body) {
        render Markdown.getContentHtml(body)
    }

    def selectAttachment() {
        if (params['directUpload'] == "true") {
            redirect action: "uploadAttachment", params: [directUpload: true, isAjax: true]
            return
        }
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
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
        taackUiSimpleService.show(block)
    }

    def uploadAttachment() {
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            modal {
                ajaxBlock "uploadAttachment", {
                    form "Upload a File", AttachmentUiService.buildAttachmentForm(new Attachment(), MarkdownController.&saveAttachment as MethodClosure, [directUpload: params['directUpload'] == "true"]), BlockSpec.Width.MAX
                }
            }
        })
    }

    @Transactional
    def saveAttachment() {
        if (taackUiSimpleService.isProcessingForm()) {
            Attachment a = attachmentUiService.saveAttachment()
            a.save(flush: true, failOnError: true)
            if (params['directUpload'] == "true") {
                selectAttachmentCloseModal(a)
            } else {
                taackUiSimpleService.cleanForm()
                taackSimpleSaveService.displayBlockOrRenderErrors(a, new UiBlockSpecifier().ui {
                    closeModalAndUpdateBlock attachmentUiService.buildAttachmentsBlock(MarkdownController.&selectAttachmentCloseModal as MethodClosure, null, MarkdownController.&uploadAttachment as MethodClosure)
                })
            }
        } else {
            taackUiSimpleService.show(new UiBlockSpecifier().ui {
                inline(attachmentUiService.buildAttachmentsBlock(MarkdownController.&selectAttachmentCloseModal as MethodClosure, null, MarkdownController.&uploadAttachment as MethodClosure))
            })
        }
    }
}
