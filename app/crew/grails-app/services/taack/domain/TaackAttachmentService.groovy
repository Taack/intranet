package taack.domain

import attachment.Attachment
import attachment.DocumentAccess
import attachment.DocumentCategory
import attachment.config.AttachmentContentType
import attachment.config.AttachmentContentTypeCategory
import attachment.config.DocumentCategoryEnum
import crew.User
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import grails.util.Pair
import grails.web.api.WebAttributes
import grails.web.databinding.DataBinder
import jakarta.annotation.PostConstruct
import org.apache.commons.io.FileUtils
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.ocr.TesseractOCRConfig
import org.apache.tika.sax.BodyContentHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import org.taack.*
import taack.ui.TaackUi
import taack.ui.TaackUiConfiguration
import taack.ui.dsl.UiMenuSpecifier

import java.security.MessageDigest

@GrailsCompileStatic
class TaackAttachmentService implements WebAttributes, DataBinder {
    SpringSecurityService springSecurityService

    final Object imageConverter = new Object()

    @Value('${intranet.root}')
    String intranetRoot

    String getStorePath() {
        intranetRoot + '/attachment/store'
    }

    String getAttachmentTmpPath() {
        intranetRoot + '/attachment/tmp'
    }

    String getAttachmentTxtPath() {
        intranetRoot + '/attachment/txt'
    }

    enum PreviewFormat {
        DEFAULT(false, 240, 160, 240, 160),
        DEFAULT_PDF(true, 960, 640, 960, 640),
        PREVIEW_MEDIUM(false, 960, 640, 480, 320),
        PREVIEW_LARGE(false, 1920, 1080, 1920, 1080),
        PREVIEW_LARGE_PDF(true, 1920, 1080, 1920, 1080)

        PreviewFormat(boolean isPdf, int pixelWidth, int pixelHeight, int previewPixelHeight, int previewPixelWidth) {
            this.isPdf = isPdf
            this.pixelHeight = pixelHeight
            this.pixelWidth = pixelWidth
            this.previewPixelHeight = previewPixelHeight
            this.previewPixelWidth = previewPixelWidth
        }

        String getPreviewExtension() {
            isPdf ? 'png' : 'webp'
        }

        String attachmentPreviewFileName(Attachment attachment) {
            attachment.contentShaOne.strip() + '.' + previewExtension
        }

        final boolean isPdf
        final int pixelHeight
        final int pixelWidth
        final int previewPixelHeight
        final int previewPixelWidth
    }

    String previewPath(final PreviewFormat format) {
        intranetRoot + "/attachment/preview/${format.toString()}"
    }

    String attachmentFileName(final Attachment attachment) {
        if (attachment.originalName.contains('.'))
            attachment.contentShaOne + attachment.originalName.substring(attachment.originalName.lastIndexOf('.'))
        else
            attachment.contentShaOne + '.NONE'
    }

    String attachmentPath(final Attachment attachment) {
        storePath + '/' + attachmentFileName(attachment)
    }

    File attachmentFile(final Attachment attachment) {
        new File(attachmentPath(attachment))
    }

    String attachmentTxtPath(final Attachment attachment) {
        if (attachment.originalName.contains('.'))
            attachmentTxtPath + '/' + attachment.contentShaOne + attachment.originalName.substring(attachment.originalName.lastIndexOf('.')) + '.txt'
        else
            attachmentTxtPath + '/' + attachment.contentShaOne + '.NONE' + '.txt'
    }

    String attachmentPreviewPath(final PreviewFormat previewFormat, final Attachment attachment) {
        previewPath(previewFormat) + '/' + previewFormat.attachmentPreviewFileName(attachment)
    }

    enum ConvertMode {
        DIRECT_CONVERT,
        UNO_CONVERTER,
        LO_CONVERT_TEXT_DOCUMENT('writer_pdf_Export'),
        LO_CONVERT_SPREADSHEET('calc_pdf_Export'),
        LO_CONVERT_PRESENTATION('impress_pdf_Export')

        ConvertMode(final String pdfFilter = null) {
            this.pdfFilter = pdfFilter
        }

        final String pdfFilter
    }

    enum ConvertExtensions {
        ICO('.ico', 'image.webp', ConvertMode.DIRECT_CONVERT),
        WEBP('.webp', 'image.webp', ConvertMode.DIRECT_CONVERT),
        JPG('.jpg', 'image.webp', ConvertMode.DIRECT_CONVERT),
        JPEG('.jpeg', 'image.webp', ConvertMode.DIRECT_CONVERT),
        PNM('.pnm', 'image.webp', ConvertMode.DIRECT_CONVERT),
        PNG('.png', 'image.webp', ConvertMode.DIRECT_CONVERT),
        PIX('.pix', 'image.webp', ConvertMode.DIRECT_CONVERT),
        PDF('.pdf', 'image.webp', ConvertMode.DIRECT_CONVERT),
        TIF('.tif', 'image.webp', ConvertMode.DIRECT_CONVERT),
        SVG('.svg', 'image.webp', ConvertMode.DIRECT_CONVERT, false),
        ODT('.odt', 'doc.webp', ConvertMode.UNO_CONVERTER),
        DOCX('.docx', 'doc.webp', ConvertMode.UNO_CONVERTER),
        DOC('.doc', 'doc.webp', ConvertMode.UNO_CONVERTER),
        XLS('.xls', 'ods.webp', ConvertMode.UNO_CONVERTER),
        XLSM('.xlsm', 'ods.webp', ConvertMode.UNO_CONVERTER),
        XLSX('.xlsx', 'ods.webp', ConvertMode.UNO_CONVERTER),
        ODS('.ods', 'ods.webp', ConvertMode.UNO_CONVERTER),
        PPT('.ppt', 'odp.webp', ConvertMode.UNO_CONVERTER),
        PPTX('.pptx', 'odp.webp', ConvertMode.UNO_CONVERTER),
        ODP('.odp', 'odp.webp', ConvertMode.UNO_CONVERTER)

        ConvertExtensions(final String extension, final String icon,
                          final ConvertMode convertMode,
                          final boolean changeExtension = true) {
            this.extension = extension
            this.icon = icon
            this.convertMode = convertMode
            this.changeExtension = changeExtension
        }
        final String extension
        final String icon
        final ConvertMode convertMode
        final boolean changeExtension

        static ConvertExtensions fileConvertExtensions(Attachment a) {
            if (a.originalName.lastIndexOf('.') == -1) return null
            final String fileExtension = a.originalName.substring(a.originalName.lastIndexOf('.'))
            values().find { it.extension == fileExtension?.toLowerCase() }
        }
    }

    static Map<String, IAttachmentPreviewConverter> additionalPreviewConverter = [:]
    static Map<String, Pair<List<String>, IAttachmentConverter>> additionalConverter = [:]
    static Map<String, IAttachmentShowIFrame> additionalShow = [:]
    static Map<String, IAttachmentEditorIFrame> additionalEdit = [:]
    static UiMenuSpecifier additionalCreate = new UiMenuSpecifier()

    @PostConstruct
    void init() {
        log.info 'init'
        FileUtils.forceMkdir(new File(storePath))
        FileUtils.forceMkdir(new File(attachmentTmpPath))
        FileUtils.forceMkdir(new File(attachmentTxtPath))
        for (PreviewFormat f : PreviewFormat.values()) {
            FileUtils.forceMkdir(new File(previewPath(f)))
        }
    }

    File attachmentPreview(final Attachment attachment, PreviewFormat previewFormat = PreviewFormat.DEFAULT) {
        if (!attachment) return new File("${TaackUiConfiguration.resources}/noPreview.${previewFormat.previewExtension}")
        final File preview = new File(attachmentPreviewPath(previewFormat, attachment))
        if (preview.exists()) {
            return preview
        } else {
            final ConvertExtensions ce = ConvertExtensions.fileConvertExtensions(attachment)
            try {
                if (ce && ce.convertMode == ConvertMode.DIRECT_CONVERT) {
                    synchronized (imageConverter) {
                        String cmd = "convert ${attachmentPath(attachment)}[0] -resize ${previewFormat.pixelWidth + 'x' + previewFormat.pixelHeight} ${preview.path}"
                        log.info "AUO TaackSimpleAttachmentService executing $cmd"
                        Process p = cmd.execute()
                        p.consumeProcessOutput()
                        p.waitForOrKill(30 * 1000)
                    }
                    if (preview.exists()) {
                        return preview
                    }
                } else if (ce && ce.convertMode == ConvertMode.UNO_CONVERTER) {
                    log.info "AUO TaackSimpleAttachmentService executing unoconv -f pdf -e PageRange=1-1 --stdout ${attachmentPath(attachment)}"
                    synchronized (imageConverter) {
                        def p = "unoconv -f pdf -e PageRange=1-1 --stdout ${attachmentPath(attachment)}'.execute() | 'convert -resize ${previewFormat.pixelWidth + 'x' + previewFormat.pixelHeight} - ${preview.path}".execute()
                        p.waitForOrKill(30 * 1000)
                    }
                    if (preview.exists()) {
                        return preview
                    }
                } else if (!ce) {
                    final String fileExtension = attachment.originalName.substring(attachment.originalName.lastIndexOf('.') + 1)
                    IAttachmentPreviewConverter previewConverter = additionalPreviewConverter[fileExtension]
                    if (previewConverter) {
                        previewConverter.createWebpPreview(attachment, preview.path, previewFormat)
                        if (preview.exists()) {
                            return preview
                        }
                    }
                }
            } catch (IOException eio) {
                log.error "attachmentPreview killed before finishing for ${attachment.name} ${eio}"
            }
        }
        return new File("${TaackUiConfiguration.resources}/noPreview.${previewFormat.previewExtension}")
    }

    static void registerPreviewConverter(IAttachmentPreviewConverter previewConverter) {
        for (String extension in previewConverter.previewManagedExtensions) {
            additionalPreviewConverter.put(extension, previewConverter)
        }
    }

    static void registerEdit(IAttachmentEditorIFrame editor) {
        for (String extension in editor.editIFrameManagedExtensions) {
            additionalEdit.put(extension, editor)
        }
    }

    static void registerCreate(IAttachmentCreate create) {
        additionalCreate = TaackUi.mergeMenu create.editorCreate(), additionalCreate
    }

    static void registerConverter(IAttachmentConverter converter) {
        for (def extensionEntry in converter.supportedExtensionConversions) {
            additionalConverter.put(extensionEntry.key, new Pair(extensionEntry.value, converter))
        }
    }

    static List<String> converterExtensions(Attachment attachment) {
        additionalConverter.get(attachment.extension?.toLowerCase())?.aValue
    }

    static File convertExtension(Attachment attachment, String extension) {
        additionalConverter.get(attachment.extension?.toLowerCase())?.bValue?.convertTo(attachment, extension?.toLowerCase())
    }

    static void registerAdditionalShow(IAttachmentShowIFrame showIFrame) {
        for (String extension in showIFrame.showIFrameManagedExtensions) {
            additionalShow.put(extension, showIFrame)
        }
    }

    static IAttachmentShowIFrame additionalShowIFrame(Attachment attachment) {
        String name = attachment.originalName.substring(attachment.originalName.lastIndexOf('.') + 1)

        additionalShow[name]
    }

    static IAttachmentEditorIFrame additionalEditIFrame(Attachment attachment) {
        String name = attachment.originalName.substring(attachment.originalName.lastIndexOf('.') + 1)

        additionalEdit[name]
    }

    static String showIFrame(Attachment attachment) {
        additionalShowIFrame(attachment)?.createShowIFrame(attachment)
    }

    static String editIFrame(Attachment attachment) {
        additionalEditIFrame(attachment)?.createEditIFrame(attachment)
    }

    void downloadAttachment(Attachment attachment, boolean inline = false) {
        if (!attachment) return
        def response = webRequest.currentResponse
        response.setContentType(attachment.contentType)
        response.setHeader('Content-disposition', "${inline ? 'inline' : 'attachment'};filename=${URLEncoder.encode(attachment.getName(), 'UTF-8')}")
        response.outputStream << new File(attachmentPath(attachment)).bytes
    }

    String attachmentContent(Attachment attachment) {
        if (!attachment.originalName) return null
        File txt = new File(attachmentTxtPath(attachment))
        if (txt.exists()) return txt.text
        File a = new File(attachmentPath(attachment))
        if (a.exists()) {
            AutoDetectParser parser = new AutoDetectParser()
            BodyContentHandler handler = new BodyContentHandler(500_000)
            Metadata metadata = new Metadata()

            try (InputStream stream = new FileInputStream(a)) {
                if (attachment.contentTypeCategoryEnum == AttachmentContentTypeCategory.IMAGE) {
                    log.info "creating ${txt.path} with OCR"
                    parser.parse(stream, handler, metadata)
                    txt << handler.toString()
                    return txt.text
                } else {
                    log.info "creating ${txt.path} without OCR"
                    TesseractOCRConfig config = new TesseractOCRConfig()
                    config.setSkipOcr(true)
                    ParseContext context = new ParseContext()
                    context.set(TesseractOCRConfig.class, config)
                    parser.parse(stream, handler, metadata, context)
                    txt << handler.toString()
                    return txt.text
                }
            } catch (e) {
                log.error e.message
                txt << e.message
                return txt.text
            }
        }
        null
    }

    String fileContentToStringWithoutOcr(InputStream stream) {
        AutoDetectParser parser = new AutoDetectParser()
        BodyContentHandler handler = new BodyContentHandler(500_000)
        Metadata metadata = new Metadata()

        TesseractOCRConfig config = new TesseractOCRConfig()
        config.setSkipOcr(true)
        ParseContext context = new ParseContext()
        context.set(TesseractOCRConfig.class, config)
        parser.parse(stream, handler, metadata, context)
        handler.toString()
    }

    void postPrepareSave(Attachment attachment) {
        attachment.contentTypeEnum = AttachmentContentType.fromMimeType(attachment.contentType)
        attachment.contentTypeCategoryEnum = attachment.contentTypeEnum?.category ?: AttachmentContentTypeCategory.OTHER
    }

    @Transactional
    Attachment createAttachment(MultipartFile f) {
        if (!f || f.empty) {
            return null
        }
        final String sha1ContentSum = MessageDigest.getInstance('SHA1').digest(f.bytes).encodeHex().toString()
        final String p = sha1ContentSum + '.' + (f.originalFilename.substring(f.originalFilename.lastIndexOf('.') + 1) ?: 'NONE')
        final String d = (storePath)
        File target = new File(d + '/' + p)
        f.transferTo(target)

        Attachment attachment = new Attachment()
        attachment.filePath = p
        attachment.contentType = f.contentType
        attachment.contentTypeEnum = AttachmentContentType.fromMimeType(f.contentType)
        attachment.contentTypeCategoryEnum = AttachmentContentTypeCategory.DOCUMENT
        attachment.originalName = f.originalFilename
        attachment.contentShaOne = sha1ContentSum
        attachment.fileSize = f.size
        User currentUser = User.read(springSecurityService.currentUserId as Long)
        attachment.userCreated = currentUser
        attachment.userUpdated = currentUser
        attachment.documentCategory = DocumentCategory.findOrCreateByCategory(DocumentCategoryEnum.OTHER)
        attachment.documentAccess = DocumentAccess.findOrCreateByIsInternalAndIsRestrictedToMyBusinessUnitAndIsRestrictedToMySubsidiaryAndIsRestrictedToMyManagersAndIsRestrictedToEmbeddingObjects(false, false, false, false, true)
        attachment.save(flush: true, failOnError: true)
        return attachment
    }

    Attachment updateContentSameContentType(Attachment attachment, byte[] content) {
        final String sha1ContentSum = MessageDigest.getInstance('SHA1').digest(content).encodeHex().toString()
        final String p = sha1ContentSum + '.' + (attachment.originalName.substring(attachment.originalName.lastIndexOf('.') + 1) ?: 'NONE')
        final String d = (storePath)
        File target = new File(d + '/' + p)
        FileOutputStream fo = new FileOutputStream(target)
        fo.write(content)

        Attachment old = attachment.cloneDirectObjectData()
        old.save(flush: true, failOnError: true)

        if (old.errors)
            log.error("${old.errors}")

        attachment.fileSize = content.size()
        attachment.contentShaOne = sha1ContentSum
        attachment.filePath = p
        attachment
    }

    Attachment cloneToNewAttachment(Attachment attachment) {
        Attachment newAttachment = attachment.cloneDirectObjectData()
        newAttachment.userCreated = springSecurityService.currentUser as User
        newAttachment.nextVersion = null
        newAttachment.active = true
        newAttachment
    }
}