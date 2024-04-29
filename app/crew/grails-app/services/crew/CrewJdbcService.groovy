package crew

import grails.compiler.GrailsCompileStatic
import org.taack.User
import taack.domain.TaackJdbcService

import javax.annotation.PostConstruct


@GrailsCompileStatic
class CrewJdbcService {

    static lazyInit = false

    @PostConstruct
    private static void init() {
        def u = new User()
        TaackJdbcService.Jdbc.registerClass(User, u.username_, u.mail_, u.subsidiary_, u.firstName_, u.lastName_, u.businessUnit_, u.enabled_)
    }
}
