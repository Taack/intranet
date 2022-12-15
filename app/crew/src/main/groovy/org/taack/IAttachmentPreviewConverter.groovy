package org.taack

interface IAttachmentPreviewConverter {
    List<String> getPreviewManagedExtensions()
    void createWebpPreview(Attachment attachment, String previewPath)
}