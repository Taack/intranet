package org.taack

import attachment.Attachment

interface IAttachmentEditorIFrame {
    List<String> getEditIFrameManagedExtensions()
    String createEditIFrame(Attachment attachment)
}