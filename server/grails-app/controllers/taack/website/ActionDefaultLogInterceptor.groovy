package taack.website

import crew.User
import grails.artefact.Interceptor
import grails.compiler.GrailsCompileStatic
import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService
import org.grails.web.util.WebUtils
import org.springframework.boot.jdbc.DataSourceUnwrapper

import javax.sql.DataSource

@GrailsCompileStatic
class ActionDefaultLogInterceptor implements Interceptor {

    ActionDefaultLogInterceptor() {
        matchAll()
                .excludes(action: 'getPluginLogo')
                .excludes(action: 'preview')
                .excludes(action: 'mediaPreview')
                .excludes(action: 'doc')
    }

    SpringSecurityService springSecurityService

    boolean before() {
        final String c = params.get('controller')
        final String a = params.get('action')
        def request = WebUtils.retrieveGrailsWebRequest().getCurrentRequest()
        def sensitiveKeys = ['password', 'confirmPassword', 'token']
        def sanitizedParams = params.collectEntries { key, value ->
            sensitiveKeys.contains(key) ? [key, '[FILTERED]'] : [key, value?.toString()?.take(42)]
        }

        if (c && a) {
            try {
                User user = springSecurityService.currentUser as User
                log.info "AUOINT ${c} ${a} ${user.username} ${request.post ? 'post' : request.get ? 'get' : 'unknown'} ${request.remoteHost}|${request.getHeader('user-agent')} $sanitizedParams ${request.forwardURI}"
            } catch (ignored) {
                log.error "AUOEXP ${params.get('controller')} ${params.get('action')} ${ignored.message}"
            }
        } else {
            log.info "AUOOTR ${request.remoteHost}|${request.getHeader('user-agent')} $sanitizedParams"
        }
        true
    }

    boolean after() { true }

}
