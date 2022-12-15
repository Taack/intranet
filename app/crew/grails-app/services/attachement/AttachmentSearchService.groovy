package attachement

import app.config.AttachmentContentTypeCategory
import crew.AttachmentController
import grails.compiler.GrailsCompileStatic
import org.codehaus.groovy.runtime.MethodClosure
import org.grails.datastore.gorm.GormEntity
import org.taack.Attachment
import taack.base.TaackSearchService
import taack.base.TaackSimpleAttachmentService
import taack.solr.SolrFieldType
import taack.solr.SolrSpecifier
import taack.ui.base.UiBlockSpecifier

import javax.annotation.PostConstruct

@GrailsCompileStatic
class AttachmentSearchService implements TaackSearchService.IIndexService {

    static lazyInit = false

    TaackSearchService taackSearchService
    TaackSimpleAttachmentService taackSimpleAttachmentService

    @PostConstruct
    private void init() {
        taackSearchService.registerSolrSpecifier(this, new SolrSpecifier(Attachment, AttachmentController.&showAttachment as MethodClosure, this.&labeling as MethodClosure, { Attachment a ->
            a ?= new Attachment()
            String content = taackSimpleAttachmentService.attachmentContent(a)
            indexField "Original Name", SolrFieldType.TXT_GENERAL, a.originalName_
            if (content || !a.id)
                indexField "File Content", SolrFieldType.TXT_GENERAL, "fileContent", content
            indexField "File Origin", SolrFieldType.POINT_STRING, "fileOrigin", true, a.fileOrigin
            indexField "Content Type Cat.", SolrFieldType.POINT_STRING, "contentTypeCategoryEnum", true, a.contentTypeCategoryEnum?.toString()
            indexField "Date Created", SolrFieldType.DATE, 0.5f, true, a.dateCreated_
            indexField "User Created", SolrFieldType.POINT_STRING, "userCreated", 0.5f, true, a.userCreated?.username
        }))
    }

    String labeling(Long id) {
        def a = Attachment.read(id)
        "Attachment: ${a.originalName} ($id)"
    }

    @Override
    List<? extends GormEntity> indexThose(Class<? extends GormEntity> toIndex) {
        if (toIndex.isAssignableFrom(Attachment)) return Attachment.findAllByActiveAndContentTypeCategoryEnumInList(true, [AttachmentContentTypeCategory.DOCUMENT, AttachmentContentTypeCategory.PRESENTATION, AttachmentContentTypeCategory.SPREADSHEET])
        else null
    }

    UiBlockSpecifier buildSearchBlock(String q) {
        taackSearchService.search(q, AttachmentController.&search as MethodClosure, Attachment)
    }
}
