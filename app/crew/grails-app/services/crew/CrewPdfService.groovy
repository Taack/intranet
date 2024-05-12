package crew


import app.config.Subsidiary
import attachement.AttachmentUiService
import grails.compiler.GrailsCompileStatic
import grails.plugin.springsecurity.SpringSecurityService
import grails.web.api.WebAttributes
import org.taack.User
import taack.domain.TaackAttachmentService
import taack.render.TaackUiService
import taack.ui.base.UiPrintableSpecifier
import taack.ui.base.UiShowSpecifier
import taack.ui.base.UiTableSpecifier
import taack.ui.base.block.BlockSpec
import taack.ui.base.common.Style

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
                    fieldHeader 'Photo'
                }
                column {
                    fieldHeader u.username_
                    fieldHeader u.businessUnit_
                }
                column {
                    fieldHeader u.lastName_
                    fieldHeader u.firstName_
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
                            rowColumn(1, 1, new Style("firstCellInGroup-${level}", cssStyle[level])) {
                                rowField attachmentUiService.preview(mu.mainPicture?.id, TaackAttachmentService.PreviewFormat.DEFAULT_PDF)
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
                            ${taackUiService.dumpAsset("logo-taack-web.svg")}
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
