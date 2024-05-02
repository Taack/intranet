package crew


import app.config.Subsidiary
import grails.compiler.GrailsCompileStatic
import org.taack.User
import taack.ui.base.UiPrintableSpecifier
import taack.ui.base.UiShowSpecifier
import taack.ui.base.UiTableSpecifier
import taack.ui.base.block.BlockSpec

@GrailsCompileStatic
class CrewPdfService {

    private static UiTableSpecifier buildUserPdfTableHierarchy(Subsidiary subsidiary) {

        new UiTableSpecifier().ui {

            header {
                User u = new User()
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
                        row {
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

    static UiPrintableSpecifier buildPdfHierarchy() {
        new UiPrintableSpecifier().ui {
            for (Subsidiary s in Subsidiary.values()) {
                show(new UiShowSpecifier().ui {
                    field("""
                        <h1>$s</h1>
                    """)
                }, BlockSpec.Width.MAX)
                table(buildUserPdfTableHierarchy(s), BlockSpec.Width.MAX)
            }
        }
    }

}
