package org.taack

import attachment.Attachment
import taack.domain.TaackAttachmentService.PreviewFormat

interface IAttachmentPreviewConverter {
    List<String> getPreviewManagedExtensions()
    void createWebpPreview(Attachment attachment, String previewPath, PreviewFormat format)
}