package org.taack

import attachment.Attachment

interface IAttachmentConverter {
    Map<String, List<String>> getSupportedExtensionConversions()
    File convertTo(Attachment attachment, String extensionTo)
}