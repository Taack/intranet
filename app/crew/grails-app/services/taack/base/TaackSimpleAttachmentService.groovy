package taack.base

import app.config.AttachmentContentType
import app.config.AttachmentContentTypeCategory
import app.config.AttachmentType
import app.config.SupportedLanguage
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService
import grails.util.Pair
import grails.web.api.WebAttributes
import grails.web.databinding.DataBinder
import org.apache.commons.io.FileUtils
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.parser.ocr.TesseractOCRConfig
import org.apache.tika.sax.BodyContentHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import org.taack.Attachment
import org.taack.IAttachmentConverter
import org.taack.IAttachmentPreviewConverter
import org.taack.IAttachmentShowIFrame
import org.taack.User
import taack.ui.TaackUiConfiguration

import javax.annotation.PostConstruct
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.text.SimpleDateFormat

@GrailsCompileStatic
class TaackSimpleAttachmentService implements WebAttributes, DataBinder {
    final Object imageConverter = new Object()
    SpringSecurityService springSecurityService

    @Autowired
    TaackUiConfiguration taackUiConfiguration

    @Value('${intranet.root}')
    String intranetRoot

    String getStorePath() {
        intranetRoot + "/attachment/store"
    }

    String getAttachmentTmpPath() {
        intranetRoot + "/attachment/tmp"
    }

    String getAttachmentTxtPath() {
        intranetRoot + "/attachment/txt"
    }

    enum PreviewFormat {
        DEFAULT(false, 240, 150),
        DEFAULT_PDF(true, 480, 300),
        PREVIEW_MEDIUM(false, 480, 300),
        PREVIEW_LARGE(false, 1280, 800),
        PREVIEW_LARGE_PDF(true, 1280, 800)

        PreviewFormat(boolean isPdf, int pixelWidth, int pixelHeight) {
            this.isPdf = isPdf
            this.pixelHeight = pixelHeight
            this.pixelWidth = pixelWidth
        }

        String getPreviewExtension() {
            isPdf ? "png" : "webp"
        }

        String attachmentPreviewFileName(Attachment attachment) {
            attachment.contentShaOne.strip() + '.' + previewExtension
        }

        final boolean isPdf
        final int pixelHeight
        final int pixelWidth
    }

    String previewPath(final PreviewFormat format) {
        intranetRoot + "/attachment/preview/${format.toString()}"
    }

    String attachmentFileName(final Attachment attachment) {
        if (attachment.originalName.contains('.'))
            attachment.contentShaOne + attachment.originalName.substring(attachment.originalName.lastIndexOf('.'))
        else
            attachment.contentShaOne + ".NONE"
    }

    String attachmentPath(final Attachment attachment) {
        storePath + '/' + attachmentFileName(attachment)
    }

    String attachmentTxtPath(final Attachment attachment) {
        if (attachment.originalName.contains('.'))
            attachmentTxtPath + '/' + attachment.contentShaOne + attachment.originalName.substring(attachment.originalName.lastIndexOf('.')) + ".txt"
        else
            attachmentTxtPath + '/' + attachment.contentShaOne + ".NONE" + ".txt"
    }

    String attachmentPreviewPath(final PreviewFormat previewFormat, final Attachment attachment) {
        previewPath(previewFormat) + '/' + previewFormat.attachmentPreviewFileName(attachment)
    }

    enum ConvertMode {
        DIRECT_CONVERT,
        UNO_CONVERTER,
        LO_CONVERT_TEXT_DOCUMENT("writer_pdf_Export"),
        LO_CONVERT_SPREADSHEET("calc_pdf_Export"),
        LO_CONVERT_PRESENTATION("impress_pdf_Export")

        ConvertMode(final String pdfFilter = null) {
            this.pdfFilter = pdfFilter
        }

        final String pdfFilter
    }

    enum ConvertExtensions {
        ICO(".ico", "image.webp", ConvertMode.DIRECT_CONVERT),
        WEBP(".webp", "image.webp", ConvertMode.DIRECT_CONVERT),
        JPG(".jpg", "image.webp", ConvertMode.DIRECT_CONVERT),
        JPEG(".jpeg", "image.webp", ConvertMode.DIRECT_CONVERT),
        PNM(".pnm", "image.webp", ConvertMode.DIRECT_CONVERT),
        PNG(".png", "image.webp", ConvertMode.DIRECT_CONVERT),
        PIX(".pix", "image.webp", ConvertMode.DIRECT_CONVERT),
        PDF(".pdf", "image.webp", ConvertMode.DIRECT_CONVERT),
        TIF(".tif", "image.webp", ConvertMode.DIRECT_CONVERT),
        SVG(".svg", "image.webp", ConvertMode.DIRECT_CONVERT, false),
        ODT(".odt", "doc.webp", ConvertMode.UNO_CONVERTER),
        DOCX(".docx", "doc.webp", ConvertMode.UNO_CONVERTER),
        DOC(".doc", "doc.webp", ConvertMode.UNO_CONVERTER),
        XLS(".xls", "ods.webp", ConvertMode.UNO_CONVERTER),
        XLSX(".xlsx", "ods.webp", ConvertMode.UNO_CONVERTER),
        ODS(".ods", "ods.webp", ConvertMode.UNO_CONVERTER),
        PPT(".ppt", "odp.webp", ConvertMode.UNO_CONVERTER),
        PPTX(".pptx", "odp.webp", ConvertMode.UNO_CONVERTER),
        ODP(".odp", "odp.webp", ConvertMode.UNO_CONVERTER)

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

    @PostConstruct
    void init() {
        log.info "init"
        FileUtils.forceMkdir(new File(storePath))
        FileUtils.forceMkdir(new File(attachmentTmpPath))
        FileUtils.forceMkdir(new File(attachmentTxtPath))
        for (PreviewFormat f : PreviewFormat.values()) {
            FileUtils.forceMkdir(new File(previewPath(f)))
        }
    }

    private static final String makeContentShaOne(File savedFile) {
        return MessageDigest.getInstance("SHA1").digest(savedFile.bytes).encodeHex().toString()
    }

    private static final String makeContentShaOne(MultipartFile multipartFile) {
        return MessageDigest.getInstance("SHA1").digest(multipartFile.bytes).encodeHex().toString()
    }

    List<Attachment> getAttachments(List<MultipartFile> files, String fileOrigin = null, AttachmentType type = null, Boolean activeOnly = true) {
        if (!files || files.empty) {
            return null
        }

        List<Attachment> attachmentList = []
        files.each { f ->
            Attachment a = getAttachment(f, fileOrigin, type, activeOnly)
            if (a)
                attachmentList.add(a)
        }

        return attachmentList
    }

    Attachment getAttachment(String contentShaOne, String fileOrigin = null, AttachmentType type = null, Boolean activeOnly = true) {
        def results = Attachment.createCriteria().list() {
            if (activeOnly)
                eq "active", true
            if (fileOrigin)
                eq "fileOrigin", fileOrigin
            if (type)
                eq "type", type
            eq "contentShaOne", contentShaOne
        } as List<Attachment>

        Attachment match = (results && !results.empty) ? results.first() : null
        if (match) log.info "file exists on the server (id: ${match.id}, fileOrigin: ${match.fileOrigin}, type: ${match.type})"
        else if (activeOnly) log.info "file does not exist on the server or is not active"
        else log.info "file does not exist on the server"

        return match
    }

    Attachment getAttachment(File f, String fileOrigin = null, AttachmentType type = null, Boolean activeOnly = true) {
        getAttachment makeContentShaOne(f), fileOrigin, type, activeOnly
    }

    Attachment getAttachment(MultipartFile f, String fileOrigin = null, AttachmentType type = null, Boolean activeOnly = true) {
        getAttachment makeContentShaOne(f), fileOrigin, type, activeOnly
    }

    List<Attachment> createAttachments(List<MultipartFile> files, String fileOrigin, AttachmentType type = null, Boolean forceCreate = null) {
        if (!files || files.empty) {
            return null
        }

        List<Attachment> attachments = []
        String dateEnd = params?.dateEnd ?: null

        files.each { f ->
            if (!forceCreate && getAttachment(f, fileOrigin, type)) {
                log.warn "file already exists: $f.originalFilename"
                return
            }
            params?.dateEnd = dateEnd
            Attachment att = getOrCreateAttachment(f, fileOrigin, type)
            if (att) attachments << att
        }

        log.info("${attachments.size()} file(s) added")
        return attachments
    }

    Attachment getOrCreateAttachment(MultipartFile f, String fileOrigin, AttachmentType type = null) {
        if (!f || f.empty)
            return null
        println getAttachment(f, fileOrigin, type)
        return getAttachment(f, fileOrigin, type) ?: createAttachment(f, fileOrigin, type)
    }

    List<Attachment> getOrCreateAttachments(List<MultipartFile> files, String fileOrigin, AttachmentType type = null) {
        if (!files || files.empty) {
            return null
        }

        if (params?.choiceFile instanceof String) params.choiceFile = [params.choiceFile]

        List<Attachment> attachments = []
        files.each { f ->
            Attachment att = getOrCreateAttachment(f, fileOrigin, type)
            if (att) attachments << att
        }

        params?.choiceFile?.each {
            Attachment a = it ? Attachment.read(it as Long) : null

            if (a)
                attachments << a
        }
        log.info("${attachments.size()} file(s) added")
        return attachments
    }

    Attachment createAttachment(InputStream inputStream, String fileName, String fileOrigin) {
        fileName = fileName.replace("'", "_").replace(" ", "_")
        Path tmp = Files.createTempFile("Att", fileOrigin)
        Files.copy(inputStream, tmp)
        File tmpFile = tmp.toFile()
        makeAttachment(tmpFile, fileName, fileOrigin)
    }

    Attachment createAttachment(MultipartFile f, String fileOrigin, AttachmentType type = null) {
        if (!f || f.empty) {
            return null
        }
        Path tmp = Files.createTempFile("Att", fileOrigin)
        f.transferTo(tmp)
        File tmpFile = tmp.toFile()
        makeAttachment(tmpFile, f.originalFilename, fileOrigin, type, f.contentType)
    }

    Attachment createAttachment(File f, String originalFilename, String fileOrigin, AttachmentType type = null, boolean useRequest = true) {
        makeAttachment(f, originalFilename, fileOrigin, type, null, useRequest)
    }

    @Transactional
    private Attachment makeAttachment(File tmpFile, String fileName,
                                      String fileOrigin, AttachmentType type = null, String contentType = null, boolean useRequest = true) {
        String contentShaOne = makeContentShaOne(tmpFile)
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1)
        Attachment attachment = new Attachment()
        attachment.userCreated = springSecurityService.currentUser as User ?: User.findByUsername('golgoth42')
        attachment.fileSize = tmpFile.size()
        attachment.originalName = fileName
        attachment.filePath = contentShaOne + "." + extension
        attachment.fileOrigin = fileOrigin
        attachment.contentType = contentType ?: Files.probeContentType(tmpFile.toPath())
        attachment.contentShaOne = contentShaOne
        attachment.type = type

        Files.move(tmpFile.toPath(), Paths.get(storePath + '/' + attachment.filePath), StandardCopyOption.REPLACE_EXISTING)

        if (useRequest && params) {
            params.attachmentGroup = params["attachmentGroup.id"]
            params.dateEnd = params.dateEnd ? new SimpleDateFormat("yyyy-MM-dd").parse(params.dateEnd as String) : null
            params.declaredLanguage = params.declaredLanguage ? (params.declaredLanguage as String).toUpperCase() as SupportedLanguage : null
            bindData(attachment, params, [include: ['status', 'declaredLanguage', 'displayOrder', 'indexFile', 'dateEnd', 'active', "attachmentGroup", 'grantedRoles', 'grantedUsers', 'tags']])
        }
        postPrepareSave(attachment)
        attachment.save(flush: true, failOnError: true)
        if (attachment.hasErrors()) {
            log.error "${attachment.errors}"
            return null
        }
        return attachment
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
                    log.info "AUO TaackSimpleAttachmentService executing unoconv -f pdf -e PageRange=1-1 --stdout ${attachmentPath(attachment)}"
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
                        previewConverter.createWebpPreview(attachment, preview.path)
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


    @Transactional
    Attachment updateContent(Attachment attachment, File savedFile, String userFileName) {
        String contentShaOne = makeContentShaOne(savedFile)
        String extension = userFileName.substring(userFileName.lastIndexOf('.') + 1)
        attachment.userCreated = springSecurityService?.currentUser as User ?: attachment.userCreated
        attachment.fileSize = savedFile.size() as Integer
        attachment.originalName = userFileName
        attachment.filePath = contentShaOne + "." + extension
        attachment.contentType = Files.probeContentType(savedFile.toPath())
        attachment.contentShaOne = contentShaOne
        attachment.active = true

        if (attachment.validate()) {
            attachment.save(flush: true)
        } else {
            log.error("${attachment.errors}")
            throw new RuntimeException(attachment.errors.allErrors.first().getCode())
        }

        return attachment
    }

    void updateAttachment(File newContent, String fileName, Attachment attachmentToUpdate) {
        while (attachmentToUpdate.nextVersion) {
            attachmentToUpdate = attachmentToUpdate.nextVersion
        }
        if (!attachmentToUpdate.active || getAttachment(newContent, attachmentToUpdate.fileOrigin, attachmentToUpdate.type)) {
            log.error "attachmentToUpdate already exist or is not active .."
            return // See DocListController
        }
        try {
            Attachment cloned = cloneAttachment(attachmentToUpdate)
            attachmentToUpdate = updateContent(attachmentToUpdate, newContent, fileName)
            insertBefore(cloned, attachmentToUpdate)
        } catch (e) {
            log.error "updateAttachment error ${e.toString()}"
            e.printStackTrace()
        }
    }

    @Transactional
    Boolean insertBefore(Attachment newAtt, Attachment refAtt) {
        if (!newAtt || !refAtt) {
            throw new RuntimeException("attachment.isEmpty.error")
        }
        Attachment prevAtt = Attachment.findByNextVersion(refAtt)
        if (prevAtt) {
            prevAtt.nextVersion = newAtt
            if (prevAtt.validate()) {
                prevAtt.save(flush: true)
            } else {
                log.error "${prevAtt.errors}"
                throw new RuntimeException(prevAtt.errors.allErrors.first().getCode())
            }
        }
        newAtt.nextVersion = refAtt
        newAtt.active = false
        newAtt.dateImported = refAtt.dateImported
        refAtt.dateImported = new Date()
        if (newAtt.validate()) {
            newAtt.save(flush: true)
        } else {
            log.error "${newAtt.errors}"
            throw new RuntimeException(newAtt.errors.allErrors.first().getCode())
        }

        log.info("Attachment ${newAtt.id} inserted before attachment ${refAtt.id}!")
        return true
    }

    Attachment copyAttachment(Attachment src, Attachment dest, boolean forceInactive = false) {
        dest.userCreated = src?.userCreated
        dest.dateCreated = src.dateCreated
        dest.type = src.type
        dest.declaredLanguage = src.declaredLanguage
        dest.fileOrigin = src.fileOrigin
        dest.status = src.status
        dest.filePath = src.filePath
        dest.originalName = src.originalName
        dest.publicName = src.publicName
        dest.version = src.version
        dest.contentType = src.contentType
        dest.active = forceInactive ? false : src.active
        dest.fileSize = src.fileSize
        dest.contentShaOne = src.contentShaOne.strip()
        dest.contentTypeCategoryEnum = src.contentTypeCategoryEnum
        dest.contentTypeEnum = src.contentTypeEnum
        if (!dest.validate()) {
            log.error "${dest.errors}"
            throw new RuntimeException(dest.errors.allErrors.first().getCode())
        }
        return dest
    }

    @Transactional
    Attachment cloneAttachment(Attachment attachment) {
        if (!attachment) {
            throw new RuntimeException("attachment.isEmpty.error")
        }
        Attachment clone = new Attachment()
        return copyAttachment(attachment, clone)
    }

    static void registerPreviewConverter(IAttachmentPreviewConverter previewConverter) {
        for (String extension in previewConverter.previewManagedExtensions) {
            additionalPreviewConverter.put(extension, previewConverter)
        }
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

    static String showIFrame(Attachment attachment) {
        additionalShowIFrame(attachment)?.createShowIFrame(attachment)
    }

    void downloadAttachment(Attachment attachment) {
        if (!attachment) return
        def response = webRequest.currentResponse
        response.setContentType(attachment.contentType)
        response.setHeader("Content-disposition", "attachment;filename=\"${URLEncoder.encode(attachment.getName(), "UTF-8")}\"")
        response.outputStream << new File(attachmentPath(attachment)).bytes
    }

    void postPrepareSave(Attachment attachment) {
        attachment.contentTypeEnum = AttachmentContentType.fromMimeType(attachment.contentType)
        attachment.contentTypeCategoryEnum = attachment.contentTypeEnum?.category ?: AttachmentContentType.OTHER.category
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
}