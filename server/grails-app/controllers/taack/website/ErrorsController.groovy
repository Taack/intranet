package taack.website

import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.annotation.Secured

@GrailsCompileStatic
@Secured('permitAll')
class ErrorsController {

   def error403() {
       render view: 'error403'
   }

   def error404() {
      render view: 'notFound'
   }

   def error405() {
      render view: 'error403'
   }

   def error500() {
      render view: 'error'
   }
}