package taack.website

import grails.artefact.Interceptor
import groovy.transform.CompileStatic
import taack.render.TaackUiEnablerService

@CompileStatic
class ActionSecurityInterceptor implements Interceptor {

    int order = HIGHEST_PRECEDENCE
    TaackUiEnablerService taackUiEnablerService

    ActionSecurityInterceptor() {
        matchAll().excludes(controller:"login")
    }

    boolean before() {
        boolean continueOrNot = taackUiEnablerService.hasAccess(controllerName, actionName, params.id as Long, params)
        if (!continueOrNot) log.warn("someone is trying to access a forbidden action $params")
        return continueOrNot
    }

    boolean after() { true }

}
