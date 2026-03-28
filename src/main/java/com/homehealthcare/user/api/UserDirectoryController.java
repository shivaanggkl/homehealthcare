package com.homehealthcare.user.api;

import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.user.application.UserDirectoryService;
import com.homehealthcare.user.domain.UserStatus;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserDirectoryController {

    private final UserDirectoryService userDirectoryService;

    UserDirectoryController(UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }

    @GetMapping
    UserDirectoryPageResponse directory(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) UserStatus status,
            @RequestParam(name = "role", required = false) AgencyRole role,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDirectoryService.UserDirectoryEntry> result = userDirectoryService.viewDirectory(
                new UserDirectoryService.UserDirectoryFilter(search, status, role, branchId),
                pageable);

        return new UserDirectoryPageResponse(
                result.getContent().stream().map(entry -> new UserDirectoryEntryResponse(
                        entry.userId(),
                        entry.membershipId(),
                        entry.firstName(),
                        entry.lastName(),
                        entry.email(),
                        entry.phone(),
                        entry.userStatus(),
                        entry.role(),
                        entry.lastLoginAt(),
                        entry.mfaEnabled(),
                        entry.branchNames()))
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    record UserDirectoryPageResponse(
            List<UserDirectoryEntryResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    record UserDirectoryEntryResponse(
            UUID userId,
            UUID membershipId,
            String firstName,
            String lastName,
            String email,
            String phone,
            UserStatus userStatus,
            AgencyRole role,
            java.time.OffsetDateTime lastLoginAt,
            boolean mfaEnabled,
            List<String> branchNames) {
    }
}
