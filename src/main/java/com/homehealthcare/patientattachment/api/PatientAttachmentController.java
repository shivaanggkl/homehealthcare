package com.homehealthcare.patientattachment.api;

import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.patientattachment.application.PatientAttachmentService;
import com.homehealthcare.patientattachment.domain.PatientAttachment;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
class PatientAttachmentController {

    private final PatientAttachmentService patientAttachmentService;
    private final ConfigurationActorResolver configurationActorResolver;

    PatientAttachmentController(
            PatientAttachmentService patientAttachmentService,
            ConfigurationActorResolver configurationActorResolver) {
        this.patientAttachmentService = patientAttachmentService;
        this.configurationActorResolver = configurationActorResolver;
    }

    @PostMapping(path = "/patients/{patientId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    PatientAttachmentResponse upload(@PathVariable UUID patientId, @ModelAttribute UploadPatientAttachmentRequest request) throws Exception {
        PatientAttachment saved = patientAttachmentService.upload(
                configurationActorResolver.requireActorMembership(),
                patientId,
                new PatientAttachmentService.UploadPatientAttachmentCommand(
                        request.file().getOriginalFilename(),
                        request.file().getContentType(),
                        request.file().getBytes(),
                        request.category(),
                        request.description()));
        return toResponse(saved);
    }

    @GetMapping("/patients/{patientId}/attachments")
    List<PatientAttachmentResponse> list(@PathVariable UUID patientId) {
        return patientAttachmentService.list(configurationActorResolver.requireActorMembership(), patientId).stream()
                .map(PatientAttachmentController::toResponse)
                .toList();
    }

    @PutMapping("/patient-attachments/{attachmentId}")
    PatientAttachmentResponse updateMetadata(@PathVariable UUID attachmentId, @ModelAttribute UpdatePatientAttachmentMetadataRequest request) {
        PatientAttachment saved = patientAttachmentService.updateMetadata(
                configurationActorResolver.requireActorMembership(),
                attachmentId,
                new PatientAttachmentService.UpdatePatientAttachmentMetadataCommand(
                        request.category(),
                        request.description()));
        return toResponse(saved);
    }

    @GetMapping("/patient-attachments/{attachmentId}/download")
    ResponseEntity<ByteArrayResource> download(@PathVariable UUID attachmentId) {
        PatientAttachmentService.DownloadedPatientAttachment downloaded = patientAttachmentService.download(
                configurationActorResolver.requireActorMembership(),
                attachmentId);
        ByteArrayResource resource = new ByteArrayResource(downloaded.content());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloaded.contentType()))
                .contentLength(downloaded.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(downloaded.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resource);
    }

    private static PatientAttachmentResponse toResponse(PatientAttachment attachment) {
        return new PatientAttachmentResponse(
                attachment.getId(),
                attachment.getPatient().getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getCategory(),
                attachment.getUploaderMembership().getId(),
                attachment.getUploaderEmail(),
                attachment.getCreatedAt(),
                attachment.getStatus(),
                attachment.getDescription());
    }

    record UploadPatientAttachmentRequest(
            MultipartFile file,
            @NotBlank String category,
            String description) {
    }

    record UpdatePatientAttachmentMetadataRequest(
            @NotBlank String category,
            String description) {
    }

    record PatientAttachmentResponse(
            UUID id,
            UUID patientId,
            String fileName,
            String contentType,
            long sizeBytes,
            String category,
            UUID uploaderMembershipId,
            String uploaderEmail,
            java.time.Instant uploadedAt,
            com.homehealthcare.patientattachment.domain.PatientAttachmentStatus status,
            String description) {
    }
}
