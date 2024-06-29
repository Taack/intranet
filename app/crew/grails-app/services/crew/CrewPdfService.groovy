package crew

import attachement.AttachmentUiService
import crew.config.Subsidiary
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import grails.web.api.WebAttributes
import taack.domain.TaackAttachmentService
import taack.render.TaackUiService
import taack.ui.dsl.UiPrintableSpecifier
import taack.ui.dsl.UiShowSpecifier
import taack.ui.dsl.UiTableSpecifier
import taack.ui.dsl.block.BlockSpec
import taack.ui.dsl.common.Style

@GrailsCompileStatic
class CrewPdfService implements WebAttributes {

    AttachmentUiService attachmentUiService
    SpringSecurityService springSecurityService
    TaackUiService taackUiService

    private static final String[] cssStyle = [
            'padding-left: 1.25em',
            'padding-left: 2.5em',
            'padding-left: 3.75em',
            'padding-left: 5em',
            'padding-left: 6.25em',
            'padding-left: 7.5em'
    ]

    private UiTableSpecifier buildUserPdfTableHierarchy(Subsidiary subsidiary) {
        new UiTableSpecifier().ui {
            header {
                User u = new User()
                column {
                    label 'Photo'
                }
                column {
                    label u.username_
                    label u.businessUnit_
                }
                column {
                    label u.lastName_
                    label u.firstName_
                }
            }

            int count = 0
            Closure rec
            rec = { List<User> mus, int level ->
                rowIndent({
                    level++
                    for (def mu : mus) {
                        count++
                        boolean muHasChildren = !mu.managedUsers.isEmpty()
                        rowTree muHasChildren, {
                            rowColumn {
                                rowField this.attachmentUiService.preview(mu.mainPicture?.id, TaackAttachmentService.PreviewFormat.DEFAULT_PDF)
                            }
                            rowColumn {
                                rowField mu.username_
                                rowField mu.businessUnit_
                            }
                            rowColumn {
                                rowField mu.lastName_
                                rowField mu.firstName_
                            }
                        }
                        if (muHasChildren) {
                            rec(mu.managedUsers, level)
                        }
                    }
                })
            }
            rec(User.findAllByManagerIsNullAndEnabledAndSubsidiary(true, subsidiary), 0)
        }
    }

    UiPrintableSpecifier buildPdfHierarchy() {
        User cu = springSecurityService.currentUser as User

        new UiPrintableSpecifier().ui {
            printableHeaderLeft('4.5cm') {
                show new UiShowSpecifier().ui {
                    field null, "Printed for", Style.BOLD
                    field null, """${cu.firstName} ${cu.lastName}"""
                }, BlockSpec.Width.THIRD
                show new UiShowSpecifier().ui {
                    field """\
                        <div style="height: 2cm; text-align: center;align-content: center; width: 100%;margin-left: 1cm;">
                            ${this.taackUiService.dumpAsset("logo-taack-web.svg")}
                        </div>
                    """.stripIndent()
                }, BlockSpec.Width.THIRD
                show new UiShowSpecifier().ui {
                    field null, """${new Date()}""", Style.ALIGN_RIGHT
                }, BlockSpec.Width.THIRD

            }
            printableBody {
                for (Subsidiary s in Subsidiary.values()) {
                    show(new UiShowSpecifier().ui {
                        field("""
                        <h1>$s</h1>
                    """)
                    }, BlockSpec.Width.MAX)
                    table(buildUserPdfTableHierarchy(s), BlockSpec.Width.MAX)
                }
            }
            printableFooter {
                show new UiShowSpecifier().ui {
                    field "<b>Taackly</b> Powered"
                }, BlockSpec.Width.MAX
            }

        }
    }

}
