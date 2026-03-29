package com.homehealthcare.patientattachment.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalFilesystemPatientAttachmentStorage implements PatientAttachmentStorage {

    private final Path baseDirectory;

    public LocalFilesystemPatientAttachmentStorage(PatientAttachmentStorageProperties properties) {
        this.baseDirectory = properties.getLocalStorageDirectory();
    }

    @Override
    public StoredAttachment store(StoreAttachmentRequest request) {
        try {
            Files.createDirectories(baseDirectory);
            String storageKey = UUID.randomUUID().toString();
            Path target = baseDirectory.resolve(storageKey);
            Files.write(target, request.content());
            return new StoredAttachment(storageKey, request.content().length);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store patient attachment content", exception);
        }
    }

    @Override
    public LoadedAttachment load(String storageKey) {
        try {
            Path target = baseDirectory.resolve(storageKey);
            if (!Files.exists(target)) {
                throw new IllegalStateException("Patient attachment content was not found for storage key: " + storageKey);
            }
            byte[] content = Files.readAllBytes(target);
            return new LoadedAttachment(null, null, content);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load patient attachment content", exception);
        }
    }
}
