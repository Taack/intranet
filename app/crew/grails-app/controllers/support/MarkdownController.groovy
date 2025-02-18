package support

import attachement.AttachmentUiService
import attachment.Attachment
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure as MC
import taack.render.TaackUiService
import taack.ui.dsl.UiBlockSpecifier
import taack.wysiwyg.Markdown

@GrailsCompileStatic
@Secured(['isAuthenticated()'])
class MarkdownController {
    TaackUiService taackUiService
    AttachmentUiService attachmentUiService

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
                inline(attachmentUiService.buildAttachmentsBlock(MarkdownController.&selectAttachmentCloseModal as MC))
            }
        })
    }

    def selectAttachmentCloseModal(Attachment attachment) {
        UiBlockSpecifier block = new UiBlockSpecifier()
        block.ui {
            closeModal "/attachment/previewFull/${attachment.id}", attachment.toString()
        }
        taackUiService.show(block)
    }
}
