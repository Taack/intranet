package org.taack

import attachment.Attachment

interface IAttachmentPreviewConverter {
    List<String> getPreviewManagedExtensions()
    void createWebpPreview(Attachment attachment, String previewPath)
}