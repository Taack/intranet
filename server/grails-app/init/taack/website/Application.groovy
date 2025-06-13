package taack.website

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration

import groovy.transform.CompileStatic
import org.springframework.boot.context.properties.EnableConfigurationProperties
import taack.ui.TaackUiConfiguration

@CompileStatic
@EnableConfigurationProperties(TaackUiConfiguration)
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }
}