package crew.ssh

import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileStatic
import taack.ast.annotation.TaackFieldEnum
import taack.ssh.CommandRegister
import taack.ssh.SshEventRegistry
import taack.ssh.command.CommandContextToken
import taack.ssh.command.CommandTree
import taack.ui.base.UiTableSpecifier
import taack.ui.vt100.render.RenderingTable

import javax.annotation.PostConstruct

enum TestEnum {
    ENUM1, ENUM2, ENUM3
}

@CompileStatic
@TaackFieldEnum
class TableElement {
    Integer id
    Date createdDate

    String name
    TestEnum testEnum

    TableElement(Integer id, Date createdDate, String name, TestEnum testEnum) {
        this.id = id
        this.createdDate = createdDate
        this.name = name
        this.testEnum = testEnum
    }
}

@GrailsCompileStatic
class CrewShellService implements SshEventRegistry.CommandEvent {

    static lazyInit = false

    @PostConstruct
    void init() {
        SshEventRegistry.Command.initCommandEventProvider("table", this)
    }

    @Override
    CommandRegister initCommandAppEvent(String username) {
        final tree = new CommandTree("table", [], "testTable")
        return new CommandRegister(tree)
    }

    @Override
    String processCommandEvent(Iterator<CommandContextToken> tokens, InputStream inputStream, OutputStream outputStream) {
        while (tokens.hasNext()) {
            def t = tokens.next()
            def c = t.command.substring(t.startPos, t.endPos)
            if (c == "table") {
                def table = buildTable()
                println "Got a visit !"
                table.visitTable(new RenderingTable(inputStream, outputStream))
            }
        }
        return "done."
    }

    @Override
    void closeCommandConnection(String username) {

    }

    final static List<TableElement> tableElements = [
            new TableElement(0, new Date(), "t1", TestEnum.ENUM1),
            new TableElement(1, new Date(), "t2", TestEnum.ENUM2),
            new TableElement(12, new Date(), "t2", TestEnum.ENUM2),
            new TableElement(13, new Date(), "t21", TestEnum.ENUM2),
            new TableElement(14, new Date(), "t2", TestEnum.ENUM2),
            new TableElement(15, new Date(), "t2", TestEnum.ENUM2),
            new TableElement(16, new Date(), "auo", TestEnum.ENUM2),
            new TableElement(17, new Date(), "t2", TestEnum.ENUM2),
            new TableElement(2, new Date(), "t3", TestEnum.ENUM3),
            new TableElement(21, new Date(), "t3", TestEnum.ENUM3),
            new TableElement(22, new Date(), "t3", TestEnum.ENUM3),
            new TableElement(23, new Date(), "toto", TestEnum.ENUM3),
            new TableElement(24, new Date(), "t3", TestEnum.ENUM3),
            new TableElement(25, new Date(), "t3", TestEnum.ENUM3),
            new TableElement(26, new Date(), "t3", TestEnum.ENUM3),
            new TableElement(27, new Date(), "t3", TestEnum.ENUM3),
            new TableElement(28, new Date(), "t3", TestEnum.ENUM3),
            new TableElement(29, new Date(), "t3", TestEnum.ENUM3),
            new TableElement(3, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(30, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(31, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(32, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(33, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(34, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(35, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(36, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(37, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(38, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(39, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(4, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(40, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(41, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(42, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(43, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(44, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(45, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(46, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(47, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(48, new Date(), "fg1", TestEnum.ENUM2),
            new TableElement(49, new Date(), "fg1", TestEnum.ENUM2),
    ]

    static UiTableSpecifier buildTable() {
        new UiTableSpecifier().ui TableElement, {
            TableElement th = new TableElement(null, null, null, null)
            header {
                column {
                    fieldHeader "id"
                }
                column {
                    fieldHeader "createdDate"
                }
                column {
                    sortableFieldHeader "name", th.name_
                }
                column {
                    fieldHeader "testEnum"
                }
                column {
                    fieldHeader "name"
                }
                column {
                    fieldHeader "createdDate"
                }
                column {
                    fieldHeader "testEnum"
                }
                column {
                    fieldHeader "id"
                }
            }

            for (def te : tableElements) {
                row {
                    rowColumn {
                        rowField te.id
                    }
                    rowColumn {
                        rowField te.createdDate
                    }
                    rowColumn {
                        rowField te.name
                    }
                    rowColumn {
                        rowField te.testEnum.toString()
                    }
                    rowColumn {
                        rowField te.name
                    }
                    rowColumn {
                        rowField te.createdDate
                    }
                    rowColumn {
                        rowField te.testEnum.toString()
                    }
                    rowColumn {
                        rowField te.id
                    }
                }
            }
        }
    }
}
