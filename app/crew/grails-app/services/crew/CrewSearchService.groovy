package crew


import grails.compiler.GrailsCompileStatic
import org.codehaus.groovy.runtime.MethodClosure
import org.grails.datastore.gorm.GormEntity
import org.taack.User
import taack.base.TaackSearchService
import taack.solr.SolrSpecifier
import taack.solr.SolrFieldType
import taack.ui.base.UiBlockSpecifier

import javax.annotation.PostConstruct

@GrailsCompileStatic
class CrewSearchService implements TaackSearchService.IIndexService {

    static lazyInit = false

    TaackSearchService taackSearchService

    @PostConstruct
    private void init() {
        taackSearchService.registerSolrSpecifier(this, new SolrSpecifier(User, CrewController.&showUserFromSearch as MethodClosure, this.&labeling as MethodClosure, { User u ->
            u ?= new User()
            indexField "User Name (without Accents)", SolrFieldType.TXT_NO_ACCENT, u.username_
            indexField "User Name", SolrFieldType.TXT_GENERAL, u.username_
            indexField "First Name", SolrFieldType.TXT_NO_ACCENT, u.firstName_
            indexField "Last Name", SolrFieldType.TXT_NO_ACCENT, u.lastName_
            indexField "Subsidiary", SolrFieldType.POINT_STRING, "mainSubsidiary", true, u.subsidiary?.toString()
            indexField "Business Unit", SolrFieldType.POINT_STRING, "businessUnit", true, u.businessUnit?.toString()
            indexField "Date Created", SolrFieldType.DATE, 0.5f, true, u.dateCreated_
            indexField "User Created", SolrFieldType.POINT_STRING, "userCreated", 0.5f, true, u.userCreated?.username
        }))
    }

    String labeling(Long id) {
        def u = User.read(id)
        "User: ${u.firstName} ${u.lastName} ($id)"
    }

    @Override
    List<? extends GormEntity> indexThose(Class<? extends GormEntity> toIndex) {
        if (toIndex.isAssignableFrom(User)) return User.findAllByEnabled(true)
        else null
    }

    UiBlockSpecifier buildSearchBlock(String q) {
        taackSearchService.search(q, CrewController.&search as MethodClosure, User)
    }
}
