package attachment.config


import groovy.transform.CompileStatic

@CompileStatic
enum DocumentCategoryEnum {
    USER_GUIDE,
    HOW_TOS,
    BLOG,
    TECHNICAL,
    OTHER
}

@CompileStatic
enum AttachmentContentTypeCategory {
    IMAGE,
    DRAWING,
    OTHER,
    SPREADSHEET,
    DOCUMENT,
    PRESENTATION,
    VIDEO,
    SOUND,
    ARCHIVE,
    WEB
}

@CompileStatic
enum AttachmentContentType {
    PDF("application/pdf", AttachmentContentTypeCategory.DOCUMENT),
    PNG("image/png", AttachmentContentTypeCategory.IMAGE),
    JPEG("image/jpeg", AttachmentContentTypeCategory.IMAGE),
    OTHER("application/octet-stream"),
    SHEET_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", AttachmentContentTypeCategory.SPREADSHEET),
    SHEET_ODS("application/vnd.oasis.opendocument.spreadsheet", AttachmentContentTypeCategory.SPREADSHEET),
    STEP("application/step", AttachmentContentTypeCategory.DRAWING),
    DOC("application/vnd.openxmlformats-officedocument.wordprocessingml.document", AttachmentContentTypeCategory.DOCUMENT),
    SHEET_XLS("application/vnd.ms-excel", AttachmentContentTypeCategory.SPREADSHEET),
    PRESENTATION_PWP("application/vnd.openxmlformats-officedocument.presentationml.presentation", AttachmentContentTypeCategory.PRESENTATION),
    SHEET_XLSX_WITH_MACRO("application/vnd.ms-excel.sheet.macroEnabled.12", AttachmentContentTypeCategory.SPREADSHEET),
    MSWORD("application/msword", AttachmentContentTypeCategory.DOCUMENT),
    LO_TEXT("application/vnd.oasis.opendocument.text", AttachmentContentTypeCategory.DOCUMENT),
    ZIP("application/x-zip-compressed", AttachmentContentTypeCategory.ARCHIVE),
    MP4("video/mp4", AttachmentContentTypeCategory.VIDEO),
    LO_PRES("application/vnd.oasis.opendocument.presentation", AttachmentContentTypeCategory.PRESENTATION),
    ZIP2("application/zip", AttachmentContentTypeCategory.ARCHIVE),
    GIF("image/gif", AttachmentContentTypeCategory.IMAGE),
    MAIL("message/rfc822"),
    POSTSCRIPT("application/postscript", AttachmentContentTypeCategory.DOCUMENT),
    STL("application/vnd.ms-pki.stl", AttachmentContentTypeCategory.DRAWING),
    SVG("image/svg+xml", AttachmentContentTypeCategory.IMAGE),
    TEXT("text/plain", AttachmentContentTypeCategory.DOCUMENT),
    TIFF("image/tiff", AttachmentContentTypeCategory.IMAGE),
    DXF("image/vnd.dxf", AttachmentContentTypeCategory.DRAWING),
    CSV("text/csv", AttachmentContentTypeCategory.SPREADSHEET),
    RAR("application/vnd.rar", AttachmentContentTypeCategory.ARCHIVE),
    XML("text/xml", AttachmentContentTypeCategory.WEB),
    VIDEO_3GPP("video/3gpp", AttachmentContentTypeCategory.VIDEO),
    MSPWP("application/vnd.ms-powerpoint", AttachmentContentTypeCategory.PRESENTATION),
    DLL_OR_EXE("application/x-msdownload"),
    BMP("image/bmp", AttachmentContentTypeCategory.IMAGE),
    X_DL("application/x-download"),
    WEBP("image/webp", AttachmentContentTypeCategory.IMAGE),
    FORCE_DOWNLOAD("application/force-download"),
    HTML("text/html", AttachmentContentTypeCategory.WEB),
    PYTHON("text/x-python"),
    QUICKTIME("video/quicktime", AttachmentContentTypeCategory.VIDEO),
    LO_GRAPHICS("application/vnd.oasis.opendocument.graphics", AttachmentContentTypeCategory.DOCUMENT),
    LO_TEMPLATE("application/vnd.oasis.opendocument.presentation-template", AttachmentContentTypeCategory.DOCUMENT),
    LO_TEMPLATE_SPREAD("application/vnd.oasis.opendocument.spreadsheet-template", AttachmentContentTypeCategory.DOCUMENT),
    PWP_SLIDESHOW("application/vnd.openxmlformats-officedocument.presentationml.slideshow", AttachmentContentTypeCategory.PRESENTATION),
    PHOTOSHOP("image/vnd.adobe.photoshop")

    AttachmentContentType(String mimeType, AttachmentContentTypeCategory category = AttachmentContentTypeCategory.OTHER) {
        this.mimeType = mimeType
        this.category = category
    }

    static AttachmentContentType fromMimeType(final String mimeType) {
        values().find { it.mimeType == mimeType }
    }

    final AttachmentContentTypeCategory category
    final String mimeType
}