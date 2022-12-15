package org.taack

interface IAttachmentShowIFrame {
    List<String> getShowIFrameManagedExtensions()
    String createShowIFrame(Attachment attachment)
}