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
import grails.web.api.ServletAttributes
import grails.web.api.WebAttributes
import grails.web.databinding.DataBinder
import jakarta.annotation.PostConstruct
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.ocr.TesseractOCRConfig
import org.apache.tika.sax.BodyContentHandler
import org.grails.datastore.gorm.GormEntity
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.taack.*
import taack.render.TaackSaveService
import taack.ui.TaackUi
import taack.ui.TaackUiConfiguration
import taack.ui.dsl.UiMenuSpecifier

import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.FileImageInputStream
import javax.imageio.stream.ImageInputStream
import java.nio.file.Files
import java.security.MessageDigest

@GrailsCompileStatic
class TaackAttachmentService implements WebAttributes, DataBinder, ServletAttributes {
    SpringSecurityService springSecurityService

    final Object imageConverter = new Object()

    static Map<String, File> filePaths = [:]

    TaackUiConfiguration taackUiConfiguration

    final String intranetRoot = TaackUiConfiguration.root

    String getStorePath() {
        intranetRoot + '/attachment/store'
    }

    String getAttachmentTmpPath() {
        intranetRoot + '/attachment/tmp'
    }

    String getAttachmentTxtPath() {
        intranetRoot + '/attachment/txt'
    }

    String getAttachmentStorePath() {
        intranetRoot + '/attachment/store'
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
        new File(storePath).mkdirs()
        new File(attachmentTmpPath).mkdirs()
        new File(attachmentTxtPath).mkdirs()
        for (PreviewFormat f : PreviewFormat.values()) {
            new File(previewPath(f)).mkdirs()
        }

        TaackSaveService.registerFieldCustomSavingClosure('filePath', { GormEntity gormEntity, Map params ->
            if (gormEntity.hasProperty('filePath')) {
                final List<MultipartFile> mfl = (request as MultipartHttpServletRequest).getFiles('filePath')
                final mf = mfl.first()
                if (mf.size > 0) {
                    final String sha1ContentSum = MessageDigest.getInstance('SHA1').digest(mf.bytes).encodeHex().toString()
                    final String p = sha1ContentSum + '.' + (mf.originalFilename.substring(mf.originalFilename.lastIndexOf('.') + 1) ?: 'NONE')
                    final String d = (filePaths.get(controllerName) ?: attachmentStorePath)
                    File target = new File(d + '/' + p)
                    mf.transferTo(target)

                    gormEntity['filePath'] = p
                    if (gormEntity.hasProperty('contentType')) {
                        gormEntity['contentType'] = mf.contentType
                        if (gormEntity.hasProperty('contentTypeEnum')) {
                            AttachmentContentType attachmentContentType = AttachmentContentType.fromMimeType(mf.contentType)
                            gormEntity['contentTypeEnum'] = attachmentContentType
                            if (gormEntity.hasProperty('contentTypeCategoryEnum'))
                                gormEntity['contentTypeCategoryEnum'] = attachmentContentType.category
                        }
                    }
                    if (gormEntity.hasProperty('originalName')) {
                        gormEntity['originalName'] = mf.originalFilename
                    }
                    if (gormEntity.hasProperty('md5sum')) {
                        gormEntity['md5sum'] = MessageDigest.getInstance('MD5').digest(mf.bytes).encodeHex().toString()
                    }
                    if (gormEntity.hasProperty('contentShaOne')) {
                        gormEntity['contentShaOne'] = sha1ContentSum
                    }
                    if (gormEntity.hasProperty('fileSize')) {
                        gormEntity['fileSize'] = mf.size
                    }
                    if (gormEntity.hasProperty('width')) {
                        final String suffix = mf.name.substring(mf.name.lastIndexOf('.') + 1)
                        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix)
                        while (iter.hasNext()) {
                            ImageReader reader = iter.next()
                            try {
                                ImageInputStream stream = new FileImageInputStream(target)
                                reader.setInput(stream)
                                int width = reader.getWidth(reader.getMinIndex())
                                int height = reader.getHeight(reader.getMinIndex())
                                gormEntity['width'] = width
                                if (gormEntity.hasProperty('height')) gormEntity['height'] = height
                                break
                            } catch (IOException e) {
                                log.warn 'Error reading: ' + mf.name, e
                            } finally {
                                reader.dispose()
                            }
                        }
                    }
                }
            }
        })
    }

    String imageHtmlAttributes(final Attachment attachment, PreviewFormat previewFormat = PreviewFormat.DEFAULT) {
        if (!attachment) return new File("${taackUiConfiguration.resources}/noPreview.${previewFormat.previewExtension}")
        final File preview = new File(attachmentPreviewPath(previewFormat, attachment))
        if (preview.exists() && preview.length() > 8 * 4) {
            // https://developers.google.com/speed/webp/docs/riff_container?hl=fr#webp_file_header
            byte[] s = new byte[8 * 4]
            new FileInputStream(preview).read(s, 0, 8 * 4)
            String header = new String(s)
            if (header.contains('VP8X')) {
                int w = s[6 * 4 + 0] & 0xFF + (s[6 * 4 + 1] & 0xFF) * 256
                int h = s[6 * 4 + 3] & 0xFF + (s[6 * 4 + 4] & 0xFF) * 256

                w++
                h++
                if (w == h && w > 64) {
                    h = 64
                    w = 64
                } else if (w > h && w > 64) {
                    h = h * 64 / w as int
                    w = 64
                } else if (h > w && h > 64) {
                    w = w * 64 / h as int
                    h = 64
                }
                return """ width="${w}" height="${h}" """
            }
        }
        null
    }

    File attachmentPreview(final Attachment attachment, PreviewFormat previewFormat = PreviewFormat.DEFAULT) {
        if (!attachment) return new File("${taackUiConfiguration.resources}/noPreview.${previewFormat.previewExtension}")
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
                    log.info "AUO TaackSimpleAttachmentService executing unoconv -f pdf -e PageRange=1-1 --stdout ${attachmentPath(attachment)}'.execute() | 'convert -resize ${previewFormat.pixelWidth + 'x' + previewFormat.pixelHeight} - ${preview.path}"
                    synchronized (imageConverter) {
                        def p = "unoconv -f pdf -e PageRange=1-1 --stdout ${attachmentPath(attachment)}".execute() | "convert -resize ${previewFormat.pixelWidth + 'x' + previewFormat.pixelHeight} - ${preview.path}".execute()
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
        return new File("${taackUiConfiguration.resources}/noPreview.${previewFormat.previewExtension}")
    }

    File restrictedAccessPreview() {
        return new File("${taackUiConfiguration.resources}/restricted-icon.webp")
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
    Attachment createAttachment(String path, byte[] contentBytes, boolean save = false) {
        final String sha1ContentSum = MessageDigest.getInstance('SHA1').digest(contentBytes).encodeHex().toString()
        Attachment attachment = Attachment.findByContentShaOneAndOriginalName(sha1ContentSum, path)
        if (attachment)
            return attachment
        attachment = new Attachment()
        final String p = sha1ContentSum + '.' + (path.substring(path.lastIndexOf('.') + 1) ?: 'NONE')
        File target = new File(storePath + '/' + p)
        target.bytes = contentBytes

        attachment.filePath = p
        attachment.contentType = Files.probeContentType(target.toPath())
        attachment.contentTypeEnum = AttachmentContentType.fromMimeType(attachment.contentType)
        attachment.contentTypeCategoryEnum = AttachmentContentTypeCategory.DOCUMENT
        attachment.originalName = path
        attachment.contentShaOne = sha1ContentSum
        attachment.fileSize = contentBytes.length
        User currentUser = User.read(springSecurityService.currentUserId as Long)
        attachment.userCreated = currentUser
        attachment.userUpdated = currentUser
        attachment.documentCategory = DocumentCategory.findOrCreateByCategory(DocumentCategoryEnum.OTHER)
        attachment.documentAccess = DocumentAccess.findOrCreateByIsInternalAndIsRestrictedToMyBusinessUnitAndIsRestrictedToMySubsidiaryAndIsRestrictedToMyManagersAndIsRestrictedToEmbeddingObjects(false, false, false, false, true)
        if (save) {
            attachment.save(flush: true, failOnError: true)
            if (attachment.hasErrors()) log.error("${attachment.errors}")
        }
        return attachment
    }

    @Transactional
    Attachment createAttachment(File f, boolean save = false) {
        createAttachment(f.name, f.bytes, save)
    }

    @Transactional
    Attachment createAttachment(MultipartFile f) {
        if (!f || f.empty) {
            return null
        }
        createAttachment(f.originalFilename, f.bytes, true)
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