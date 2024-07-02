package attachment

import attachment.config.DocumentCategoryEnum
import grails.compiler.GrailsCompileStatic
import taack.ast.annotation.TaackFieldEnum

@TaackFieldEnum
@GrailsCompileStatic
class DocumentCategory {

    DocumentCategoryEnum category = DocumentCategoryEnum.OTHER
    Set<Term> tags

    static constraints = {}

    static hasMany = [
            tags: Term
    ]

    @Override
    String toString() {
        return "DocCat[$id]: ${category}, tags: ${tags?.join(',')?:'None'}"
    }

}
