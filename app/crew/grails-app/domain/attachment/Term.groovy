package attachment

import attachment.config.TermGroupConfig
import grails.compiler.GrailsCompileStatic
import org.springframework.context.i18n.LocaleContextHolder
import taack.ast.annotation.TaackFieldEnum

@GrailsCompileStatic
@TaackFieldEnum
class Term {
    String name
    //   TermGroup termGroup
    TermGroupConfig termGroupConfig
    Term parent
    Boolean display = true
    Boolean active = true

    List<Term> getParents() {
        List<Term> parents = []
        Term p = parent
        while (p) {
            parents << p
            p = p.parent
            if (parents.count(p) > 1) break
        }

        return parents
    }

    Term getTopParent() {
        if (!parent)
            return this
        return parent.getTopParent()
    }

    Map translations

    static constraints = {
        name nullable: false, unique: 'termGroupConfig', validator: { String val, Term obj ->
            !val.matches('.*\\s+.*')
        }
        translations nullable: true
        parent nullable: true, validator: { Term val, Term obj ->
            if (val) {
                val.termGroupConfig == obj.termGroupConfig && val?.id != obj.id && !(val.parents && obj.id in val.parents*.id)
            } else true
        }
        termGroupConfig nullable: true, validator: { TermGroupConfig val, Term obj ->
            if (obj.parent) val == obj.parent?.termGroupConfig
            else true
        }
    }

    @Override
    String toString() {
        return getOptionDisplay()
    }

    String getOptionDisplay(String dataLang = null) {
        getTranslatedValue(dataLang)
    }

    String getTranslatedValue(String dataLang) {
        dataLang = dataLang ?: LocaleContextHolder.getLocale().language
        return translations[dataLang] ?: translations['en'] ?: translations.values()?.getAt(0) ?: name
    }

    String getTermTagHexColor() {
        Integer hashCode = name.hashCode()
        return (Integer.toHexString(((hashCode>>16)&0xFF))+
                Integer.toHexString(((hashCode>>8)&0xFF))+
                Integer.toHexString((hashCode&0xFF))+
                '00').take(6)
    }
}
