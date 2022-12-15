package admin

import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.annotation.Secured
import taack.base.TaackUiProgressBarService
import taack.base.TaackUiSimpleService
import taack.ui.base.UiBlockSpecifier

@GrailsCompileStatic
@Secured("isAuthenticated()")
class ProgressController {

    TaackUiProgressBarService taackUiProgressBarService
    TaackUiSimpleService taackUiSimpleService

    def drawProgress(String id) {
        if (id && taackUiProgressBarService.progressMax(id)) {
            int max = taackUiProgressBarService.progressMax(id)
            int value = taackUiProgressBarService.progress(id)
            boolean ended = taackUiProgressBarService.progressHasEnded(id)
            if (!ended) {
                //response.outputStream << taackUiSimpleService.visit(TaackUiProgressBarService.buildProgressBlock(id, max, value), true)
                response.outputStream << taackUiSimpleService.visit(new UiBlockSpecifier().ui {
                    closeModalAndUpdateBlock(TaackUiProgressBarService.buildProgressBlock(id, max, value).closure)
                }, true)
                response.outputStream.flush()
                response.outputStream.close()
            } else {
                taackUiSimpleService.show(new UiBlockSpecifier().ui {
                    closeModalAndUpdateBlock(taackUiProgressBarService.progressEnds(id).closure)
                })
            }
        } else {
            return true
        }
    }
}
