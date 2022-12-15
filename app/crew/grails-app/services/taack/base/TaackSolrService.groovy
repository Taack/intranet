package taack.base

import grails.compiler.GrailsCompileStatic
import org.grails.datastore.gorm.GormEntity

import java.lang.reflect.Field

@GrailsCompileStatic
class TaackSolrService {

    enum SolrSuffix {
        STRING, NUMBER, TEXT
    }

    final static class SolrField {
        String fieldName
        Field field
        SolrSuffix suffix
        boolean file = false
        boolean many2manyy = false
        boolean faceted = false
        boolean displayed = true
    }

    final static class SolrSearchResult {
        Long id
        Map<SolrField, List<String>> matches
    }

    final private Map<Class, List<SolrField>> indexingSpecs = [:]

    final void registerClass() {

    }

    final <D> void indexObject(GormEntity<D> gormEntity) {

    }



    final Iterator<SolrSearchResult> search(Class aClass, String query, int offset, int max) {

    }
}
