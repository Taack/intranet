package taack.website

import crew.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.annotation.Secured
import org.codehaus.groovy.runtime.MethodClosure as MC
import taack.app.TaackApp
import taack.app.TaackAppRegisterService
import taack.render.TaackUiService
import taack.ui.dsl.UiBlockSpecifier
import taack.ui.dsl.UiMenuSpecifier
import taack.ui.dsl.UiShowSpecifier
import taack.ui.dsl.block.BlockSpec
import taack.ui.dump.Parameter

/*
TODO: Add an infrastructure to list new stuffs from a user and a timestamp
 */

@GrailsCompileStatic
@Secured(["isAuthenticated()"])
class RootController {
    TaackUiService taackUiService
    RootSearchService rootSearchService

    private static UiMenuSpecifier buildMenu(String q = null) {
        UiMenuSpecifier m = new UiMenuSpecifier()
        m.ui {
            menu this.&index as MC
            menuSearch RootController.&search as MC, q
            menuOptions(SupportedLanguage.fromContext())
        }
        m
    }

    def index() {
        UiBlockSpecifier b = new UiBlockSpecifier().ui {
            row {
                TaackAppRegisterService.apps.eachWithIndex { TaackApp a, int i ->
                    col BlockSpec.Width.FLEX, {
                        show(new UiShowSpecifier().ui {
                            inlineHtml(
                                    """\
                                    <a href="${new Parameter().urlMapped(a.entryPoint)}">
                                        <div class="taack-app" style="width: 220px; padding: 35px; text-align: center;display: inline-grid;">${a.svg}<div><b>${a.label}</b><br>${a.desc}</div></div>
                                    </a>
                                    """.stripIndent())
                        })
                    }
                }
            }
        }
        taackUiService.show(b, buildMenu())
    }

    def search(String q) {
        taackUiService.show(rootSearchService.buildSearchBlock(q), buildMenu(q))
    }

    def updates() {
        taackUiService.show(new UiBlockSpecifier().ui {
            ajaxBlock "updates", {
                show(new UiShowSpecifier().ui {
                    inlineHtml("WiP", "")
                })
            }
        }, buildMenu())
    }

    def todo() {
        taackUiService.show(new UiBlockSpecifier().ui {
            ajaxBlock "todo", {
                show(new UiShowSpecifier().ui {
                    inlineHtml("WiP", "")
                })
            }
        }, buildMenu())
    }

    @Secured(["ROLE_ADMIN"])
    def solrIndexAll() {
        rootSearchService.taackSearchService.indexAll()
        render 'Done !'
    }
}
