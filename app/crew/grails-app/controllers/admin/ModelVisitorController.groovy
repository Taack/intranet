package admin

import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.annotation.Secured
import taack.base.TaackMetaModelService

@GrailsCompileStatic
@Secured(["ROLE_ADMIN"])
class ModelVisitorController {
    TaackMetaModelService taackMetaModelService

    def model() {
        render taackMetaModelService.modelGraph()
    }
}
