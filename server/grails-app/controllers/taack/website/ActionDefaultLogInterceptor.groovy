package taack.website

import grails.artefact.Interceptor
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import org.grails.web.util.WebUtils

@GrailsCompileStatic
class ActionDefaultLogInterceptor implements Interceptor {

    ActionDefaultLogInterceptor() {
        matchAll()
                .excludes(action: 'getPluginLogo')
                .excludes(action: 'preview')
    }

    SpringSecurityService springSecurityService

    boolean before() {
        final String c = params['controller']
        final String a = params['action']
        def request = WebUtils.retrieveGrailsWebRequest().getCurrentRequest()
        if (c && a) {
            try {
                def user = springSecurityService.currentUser
                log.info "AUOINT ${c} ${a} ${user} ${request.post ? 'post' : request.get ? 'get' : 'unknown'} ${request.remoteHost}|${request.getHeader('user-agent')} $params ${request.forwardURI}"
            } catch (ignored) {
                log.error "AUOEXP ${params.get('controller')} ${params.get('action')} ${ignored.message}"
            }
        } else {
            log.info "AUOOTR ${request.remoteHost}|${request.getHeader('user-agent')} $params"
        }
        true
    }

    boolean after() { true }

}
