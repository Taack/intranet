package taack.website

import org.springframework.security.access.AccessDeniedException

class UrlMappings {

    static mappings = {
        "/"(controller: 'root')
//        "500"(view:'/error')
//        "404"(view:'/notFound')
//         "/login/$action"(controller: 'login')
//         "/admin"(controller: 'root', action: 'index')
//         "/"(controller: 'welcome')
//         "/$action"(controller: 'welcome')

        "/$controller/$action?/$id?(.$format)?"{
            constraints {
            }
        }

        "403"(controller: 'errors', action: 'error403')
        "404"(controller: 'errors', action: 'error404')
        "405"(controller: 'errors', action: 'error405')
        "500"(controller: 'errors', action: 'error500')
        "500"(controller: 'errors', action: 'error403',
                exception: AccessDeniedException)
    }
}
