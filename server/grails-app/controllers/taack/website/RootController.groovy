package taack.website

import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure as MC
import org.springframework.beans.factory.annotation.Autowired
import taack.render.TaackUiService
import taack.ui.TaackPluginConfiguration
import taack.ui.TaackPluginService
import taack.ui.TaackUiConfiguration
import taack.ui.base.UiBlockSpecifier
import taack.ui.base.UiMenuSpecifier
import taack.ui.base.UiShowSpecifier
import taack.ui.base.block.BlockSpec
/*
TODO: Add an infrastructure to list new stuffs from a user and a timestamp
 */

@GrailsCompileStatic
@Secured(["isAuthenticated()"])
class RootController {
    TaackPluginService taackPluginService
    TaackUiService taackUiService
    RootSearchService rootSearchService

    @Autowired
    TaackUiConfiguration taackUiPluginConfiguration

    final static Long randIcon = Math.round(Math.random() * Long.MAX_VALUE)

    final Map<String, byte[]> logos = [:]

    private static UiMenuSpecifier buildMenu(String q = null) {
        UiMenuSpecifier m = new UiMenuSpecifier()
        m.ui {
            menu 'Home', this.&index as MC
            menuSearch RootController.&search as MC, q
        }
        m
    }

    def index(Boolean taackReset) {
        render view: "root", model: [taackPluginService: taackPluginService,
                                     conf    : taackUiPluginConfiguration,
                                     menu: taackUiService.visitMenu(buildMenu(params["q"] as String))]
    }

    def getPluginLogo(String pluginControllerName) {
        if (pluginControllerName) {
            if (!logos[pluginControllerName]) {
                synchronized (logos) {
                    TaackPluginConfiguration conf = taackPluginService.getTaackPluginConfigurationFromControllerName(pluginControllerName)
                    if (conf) {
                        logos[pluginControllerName] = conf.class.getResourceAsStream(conf.imageResource).bytes
                    }
                }
            }
            response.setContentType 'image/svg+xml'
            response.setHeader("Cache-Control", "max-age=604800")
            try {
                response.outputStream << logos[pluginControllerName]
            } catch(Exception e) {
                log.warn "getPluginLogo: ${e.toString()}"
            }
        }
        return false
    }

    def search(String q) {
        taackUiService.show(rootSearchService.buildSearchBlock(q), buildMenu(q))
    }

    def updates() {
        taackUiService.show(new UiBlockSpecifier().ui {
            ajaxBlock "updates", {
                show(new UiShowSpecifier().ui {
                    inlineHtml("WiP", "")
                }, BlockSpec.Width.MAX)
            }
        }, buildMenu())
    }

    def todo() {
        taackUiService.show(new UiBlockSpecifier().ui {
            ajaxBlock "todo", {
                show(new UiShowSpecifier().ui {
                    inlineHtml("WiP", "")
                }, BlockSpec.Width.MAX)
            }
        }, buildMenu())
    }

    @Secured(["ROLE_ADMIN"])
    def solrIndexAll() {
        rootSearchService.taackSearchService.indexAll()
        render 'Done !'
    }
}
