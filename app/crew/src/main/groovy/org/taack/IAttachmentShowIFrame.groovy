package org.taack

import attachment.Attachment

interface IAttachmentShowIFrame {
    List<String> getShowIFrameManagedExtensions()
    String createShowIFrame(Attachment attachment)
}