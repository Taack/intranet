package taack.website

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import taack.ui.TaackUiConfiguration

@CompileStatic
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false")
        TaackUiConfiguration.root = TaackUiConfiguration.home + '/intranetFilesDev'
        GrailsApp.run(Application, args)
    }
}