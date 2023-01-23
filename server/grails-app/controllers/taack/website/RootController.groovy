package taack.website

import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.i18n.LocaleContextHolder
import taack.base.TaackUiSimpleService
import taack.ui.TaackPluginConfiguration
import taack.ui.TaackPluginService
import taack.ui.TaackUiConfiguration
import taack.ui.base.UiBlockSpecifier
import taack.ui.base.UiMenuSpecifier
import taack.ui.base.UiShowSpecifier
import taack.ui.base.block.BlockSpec
import taack.ui.config.Language
/*
TODO: Add an infrastructure to list new stuffs from a user and a timestamp
 */

@GrailsCompileStatic
@Secured(["isAuthenticated()"])
class RootController {
    TaackPluginService taackPluginService
    TaackUiSimpleService taackUiSimpleService
    RootSearchService rootSearchService

    @Autowired
    TaackUiConfiguration taackUiPluginConfiguration

    final static Long randIcon = Math.round(Math.random() * Long.MAX_VALUE)

    final Map<String, byte[]> logos = [:]

    private static UiMenuSpecifier buildMenu(String q = null) {
        UiMenuSpecifier m = new UiMenuSpecifier()
        m.ui {
            menu "Home auo", this.&index as MethodClosure
            menuSearch RootController.&search as MethodClosure, q
        }
        m
    }

    def index(Boolean taackReset) {
        Language language = Language.EN
        try {
            language = LocaleContextHolder.locale.language.split("_")[0]?.toUpperCase()?.replace("ZH", "CN") as Language
        } catch (ignored) {
        }

        render view: "root", model: [taackPluginService: taackPluginService,
                                     language: language,
                                     conf    : taackUiPluginConfiguration,
                                     menu: taackUiSimpleService.visitMenu(buildMenu(params["q"] as String))]
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
        taackUiSimpleService.show(rootSearchService.buildSearchBlock(q), buildMenu(q))
    }

    def updates() {
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            ajaxBlock "updates", {
                show("WiP", new UiShowSpecifier().ui {
                    inlineHtml("WiP", "")
                }, BlockSpec.Width.MAX)
            }
        }, buildMenu())
    }

    def todo() {
        taackUiSimpleService.show(new UiBlockSpecifier().ui {
            ajaxBlock "todo", {
                show("WiP", new UiShowSpecifier().ui {
                    inlineHtml("WiP", "")
                }, BlockSpec.Width.MAX)
            }
        }, buildMenu())
    }
}
