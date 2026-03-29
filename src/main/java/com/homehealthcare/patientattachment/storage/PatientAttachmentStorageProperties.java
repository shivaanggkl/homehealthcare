package com.homehealthcare.patientattachment.storage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.patient-attachments")
public class PatientAttachmentStorageProperties {

    private Path localStorageDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "homehealthcare", "patient-attachments");
    private long maxFileSizeBytes = 10 * 1024 * 1024;
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "text/plain"));

    public Path getLocalStorageDirectory() {
        return localStorageDirectory;
    }

    public void setLocalStorageDirectory(Path localStorageDirectory) {
        this.localStorageDirectory = localStorageDirectory;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = new ArrayList<>(allowedContentTypes);
    }
}
