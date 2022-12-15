package org.taack

interface IAttachmentConverter {
    Map<String, List<String>> getSupportedExtensionConversions()
    File convertTo(Attachment attachment, String extensionTo)
}