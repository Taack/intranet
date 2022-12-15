package app.config


import groovy.transform.CompileStatic

/*
TODO register type from enums in gradle modules

mainPicture	5914 OK
noTypeSpecified	5181 => NULL
certificate	1277 OK
testData	1033 => OK
testReport	870 => OK
electricalSchemas	810
mechanicalSchemas	773
studyDescription	604
other	578
commercialDrawing	496
technicalSheet	485
installationSheet	478
test	420
documentation	274
technicalFile	272
model3d	259 => We can guess
technicalData	259
secondaryPicture	251
replacementDeliveryOrder	180
mechanicalDrawing1	165
technicalDescription	143
internalNews	129
plan	129
technicalAnalysis	123
alternativeMainPicture	115
customerDeliveryOrder	111
installationInstruction	110
replacementOrderAOR	90
marking	75
flyersCitel	73
validationDocument	64
rangeDescription	52
technicalFolder	52
mechanicalDrawing2	51
priceList	48
competitorDatasheet	46
preliminaryDatasheet	45
screenShot	38
flyersObsta	33
baseProduct	32
null	31
competitorProductPicture	28
shape	28
variousSchemas	20
externalTestReport	19
attachments	18
customerTechnicalAnalysis	17
qualitySystemProcedure	15
generalCatalogObsta	13
lightIntensityDiagram	13
slideshow	13
certificationsTestReports	12
releaseNote	12
generalCatalogCitel	10
printing	10
employeeChart	9
visitingReport	9
norm	7
markingRequest	6
completionStatement	2
graphicCharterCitel	2
logoCitel	2
graphicCharterObsta	1
logoObsta	1
padLayout	1

 */

@CompileStatic
interface IAttachmentType {
    boolean isObsolete()

    BusinessUnit getSpecificToBusinessUnit()
}

@CompileStatic
enum AttachmentType implements IAttachmentType {
    mainPicture,//	5914 OK
    noTypeSpecified(true),//	5181 => NULL
    certificate,//	1277 OK
    testData,//	1033 => OK
    testReport,//	870 => OK
    electricalSchemas,//	810
    mechanicalSchemas,//	773
    studyDescription,//	604
    other,//	578
    commercialDrawing,//	496
    technicalSheet,//	485
    installationSheet,//	478
    test,//	420
    documentation,//	274
    technicalFile,//	272
    model3d,//	259 => We can guess
    technicalData,//	259
    secondaryPicture,//	251
    replacementDeliveryOrder,//	180
    mechanicalDrawing1,//	165
    technicalDescription,//	143
    internalNews,//	129
    plan,//	129
    technicalAnalysis,//	123
    alternativeMainPicture,//	115
    customerDeliveryOrder,//	111
    installationInstruction,//	110
    replacementOrderAOR,//	90
    marking,//	75
    flyersCitel,//	73
    validationDocument,//	64
    rangeDescription,//	52
    technicalFolder,//	52
    mechanicalDrawing2,//	51
    priceList,//	48
    competitorDatasheet,//	46
    preliminaryDatasheet,//	45
    screenShot,//	38
    flyersObsta,//33
    baseProduct,//	32
    competitorProductPicture,//	28
    shape,//	28
    variousSchemas,//	20
    externalTestReport,//	19
    attachments,//	18
    customerTechnicalAnalysis,//	17
    qualitySystemProcedure,//	15
    generalCatalogObsta,//	13
    lightIntensityDiagram,//	13
    slideshow,//	13
    certificationsTestReports,//	12
    releaseNote,//	12
    generalCatalogCitel(true),//	10
    printing,//	10
    employeeChart,//	9
    visitingReport,//	9
    norm,//	7
    markingRequest,//	6
    completionStatement,//	2
    graphicCharterCitel(true),//	2
    logoCitel(true),//	2
    graphicCharterObsta(true),//	1
    logoObsta(true),//	1
    interventionPlan,
    gerber,
    padLayout,//	1
    pickAndPlace,
    svgTemplateForSticker,
    model2d,
    source,
    productInformation

    AttachmentType(boolean obsolete = false, BusinessUnit specificToBusinessUnit = null) {
        this.obsolete = obsolete
        this.specificToBusinessUnit = specificToBusinessUnit
    }

    final boolean obsolete
    final BusinessUnit specificToBusinessUnit
}

/*
application/pdf	9457
image/png	5637
image/jpeg	3903
application/octet-stream	1021
application/vnd.openxmlformats-officedocument.spreadsheetml.sheet	747
application/vnd.oasis.opendocument.spreadsheet	572
application/step	431
application/vnd.openxmlformats-officedocument.wordprocessingml.document	300
application/vnd.ms-excel	263
application/vnd.openxmlformats-officedocument.presentationml.presentation	227
application/vnd.ms-excel.sheet.macroEnabled.12	126
application/msword	100
application/vnd.oasis.opendocument.text	90
application/x-zip-compressed	57
video/mp4	42
application/vnd.oasis.opendocument.presentation	37
application/zip	26
image/gif	26
message/rfc822	17
application/postscript	16
application/vnd.ms-pki.stl	15
image/svg+xml	15
text/plain	13
image/tiff	12
image/vnd.dxf	12
text/csv	8
application/vnd.rar	7
text/xml	7
video/3gpp	7
application/vnd.ms-powerpoint	5
application/x-msdownload	5
image/bmp	4
application/x-download	3
image/webp	3
application/force-download	2
text/html	2
text/x-python	2
video/quicktime	2
application/vnd.oasis.opendocument.graphics	1
application/vnd.oasis.opendocument.presentation-template	1
application/vnd.oasis.opendocument.spreadsheet-template	1
application/vnd.openxmlformats-officedocument.presentationml.slideshow	1
image/vnd.adobe.photoshop	1
 */

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