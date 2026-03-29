package com.homehealthcare.patientattachment.storage;

public interface PatientAttachmentStorage {

    StoredAttachment store(StoreAttachmentRequest request);

    LoadedAttachment load(String storageKey);

    record StoreAttachmentRequest(String fileName, String contentType, byte[] content) {
    }

    record StoredAttachment(String storageKey, long sizeBytes) {
    }

    record LoadedAttachment(String fileName, String contentType, byte[] content) {
    }
}
