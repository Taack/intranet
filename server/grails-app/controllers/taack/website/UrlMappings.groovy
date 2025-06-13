package taack.website

import org.springframework.security.access.AccessDeniedException

class UrlMappings {

    static mappings = {
        "/"(controller: 'root', action: 'index')

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
            }
        }

        "/"(controller:"root")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
