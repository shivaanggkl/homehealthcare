package com.homehealthcare.workforce.application;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailabilityRepository;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailabilityType;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityApprovalStatus;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityReasonType;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityRepository;
import com.homehealthcare.caregivercredential.domain.CaregiverCredential;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialRepository;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialStatus;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialVerificationStatus;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreference;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreferenceRepository;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreferenceType;
import com.homehealthcare.caregiverlanguage.domain.CaregiverLanguageProfile;
import com.homehealthcare.caregiverlanguage.domain.CaregiverLanguageProfileRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.caregivershift.domain.CaregiverShiftPreference;
import com.homehealthcare.caregivershift.domain.CaregiverShiftPreferenceRepository;
import com.homehealthcare.caregivershift.domain.ShiftPreferenceStrength;
import com.homehealthcare.caregiverskill.domain.CaregiverSkill;
import com.homehealthcare.caregiverskill.domain.CaregiverSkillRepository;
import com.homehealthcare.caregiverskillprofile.domain.CaregiverSkillProfile;
import com.homehealthcare.caregiverskillprofile.domain.CaregiverSkillProfileRepository;
import com.homehealthcare.certification.domain.CaregiverCertification;
import com.homehealthcare.certification.domain.CaregiverCertificationRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.workforce.foundation.Epic4WorkforceTargetType;
import com.homehealthcare.workforce.foundation.WorkforceLifecycleStatus;
import com.homehealthcare.workforce.foundation.WorkforceAuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class CaregiverWorkforceService {

    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchRepository branchRepository;
    private final CaregiverCertificationRepository caregiverCertificationRepository;
    private final CaregiverSkillRepository caregiverSkillRepository;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CaregiverCredentialRepository caregiverCredentialRepository;
    private final CaregiverLanguageProfileRepository caregiverLanguageProfileRepository;
    private final CaregiverSkillProfileRepository caregiverSkillProfileRepository;
    private final CaregiverGeographyPreferenceRepository caregiverGeographyPreferenceRepository;
    private final CaregiverShiftPreferenceRepository caregiverShiftPreferenceRepository;
    private final CaregiverAvailabilityRepository caregiverAvailabilityRepository;
    private final CaregiverUnavailabilityRepository caregiverUnavailabilityRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final WorkforceAuditService workforceAuditService;

    @Transactional
    public CaregiverProfile createProfile(@NotNull AgencyMembership actorMembership, @Valid ManageCaregiverProfileCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        AgencyMembership subjectMembership = agencyMembershipRepository.findById(command.agencyMembershipId())
                .orElseThrow(() -> new WorkforceEntityNotFoundException("AgencyMembership", command.agencyMembershipId()));
        assertSameAgency(actorMembership.getAgencyId(), subjectMembership.getAgencyId(), "AgencyMembership", command.agencyMembershipId());
        if (!subjectMembership.isActive()) {
            throw new WorkforceConflictException("Caregiver profile requires an active agency membership.");
        }
        if (caregiverProfileRepository.existsByAgency_IdAndAgencyMembership_Id(actorMembership.getAgencyId(), subjectMembership.getId())) {
            throw new WorkforceConflictException("An active caregiver profile already exists for this membership.");
        }

        CaregiverProfile saved = caregiverProfileRepository.saveAndFlush(CaregiverProfile.create(
                subjectMembership,
                findBranch(actorMembership.getAgencyId(), command.primaryBranchId()),
                command.caregiverCode(),
                command.displayName(),
                command.employmentType(),
                command.startDate(),
                command.endDate(),
                command.notes()));
        workforceAuditService.recordCreated(actorMembership, Epic4WorkforceTargetType.CAREGIVER_PROFILE, saved.getId(), saved.getPrimaryBranchId(), metadata(saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public CaregiverCredential createCredential(@NotNull AgencyMembership actorMembership, @NotNull UUID caregiverProfileId, @Valid ManageCaregiverCredentialCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREGIVER_CREDENTIALS);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverProfileId);
        String normalizedLicense = normalizeNullable(command.licenseNumber());
        if (normalizedLicense != null
                && caregiverCredentialRepository.existsByCaregiverProfile_IdAndCredentialTypeIgnoreCaseAndLicenseNumber(
                        profile.getId(),
                        command.credentialType(),
                        normalizedLicense)) {
            throw new WorkforceConflictException("A caregiver credential with the same type and license number already exists.");
        }
        CaregiverCredential saved = caregiverCredentialRepository.saveAndFlush(CaregiverCredential.create(
                profile,
                findCertification(actorMembership.getAgencyId(), command.certificationId()),
                command.credentialType(),
                command.licenseNumber(),
                command.issuingAuthority(),
                command.issuedOn(),
                command.expiresOn(),
                command.status(),
                command.verificationStatus(),
                command.notes()));
        workforceAuditService.recordCreated(actorMembership, Epic4WorkforceTargetType.CAREGIVER_CREDENTIAL, saved.getId(), profile.getPrimaryBranchId(), metadata(saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public CaregiverLanguageProfile createLanguage(@NotNull AgencyMembership actorMembership, @NotNull UUID caregiverProfileId, @Valid ManageCaregiverLanguageCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverProfileId);
        if (caregiverLanguageProfileRepository.existsByCaregiverProfile_IdAndLanguageCode(profile.getId(), command.languageCode().trim())) {
            throw new WorkforceConflictException("That caregiver language is already assigned.");
        }
        CaregiverLanguageProfile saved = caregiverLanguageProfileRepository.saveAndFlush(CaregiverLanguageProfile.create(
                profile,
                command.languageCode(),
                command.proficiencyLevel(),
                command.primaryLanguage()));
        workforceAuditService.recordCreated(actorMembership, Epic4WorkforceTargetType.CAREGIVER_LANGUAGE, saved.getId(), profile.getPrimaryBranchId(), metadata(saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public CaregiverSkillProfile createSkill(@NotNull AgencyMembership actorMembership, @NotNull UUID caregiverProfileId, @Valid ManageCaregiverSkillCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverProfileId);
        if (caregiverSkillProfileRepository.existsByCaregiverProfile_IdAndSkill_Id(profile.getId(), command.skillId())) {
            throw new WorkforceConflictException("That caregiver skill is already assigned.");
        }
        CaregiverSkill skill = caregiverSkillRepository.findById(command.skillId())
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverSkill", command.skillId()));
        assertSameAgency(actorMembership.getAgencyId(), skill.getAgencyId(), "CaregiverSkill", command.skillId());
        CaregiverSkillProfile saved = caregiverSkillProfileRepository.saveAndFlush(CaregiverSkillProfile.create(
                profile,
                skill,
                command.proficiencyLevel(),
                command.verified(),
                command.notes()));
        workforceAuditService.recordCreated(actorMembership, Epic4WorkforceTargetType.CAREGIVER_SKILL, saved.getId(), profile.getPrimaryBranchId(), metadata(saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public CaregiverGeographyPreference createGeographyPreference(@NotNull AgencyMembership actorMembership, @NotNull UUID caregiverProfileId, @Valid ManageCaregiverGeographyPreferenceCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverProfileId);
        CaregiverGeographyPreference saved = caregiverGeographyPreferenceRepository.saveAndFlush(CaregiverGeographyPreference.create(
                profile,
                findBranch(actorMembership.getAgencyId(), command.branchId()),
                command.preferenceType(),
                command.postalCode(),
                command.city(),
                command.state(),
                command.anchorLatitude(),
                command.anchorLongitude(),
                command.radiusMiles(),
                command.priorityRank(),
                command.notes()));
        workforceAuditService.recordCreated(actorMembership, Epic4WorkforceTargetType.CAREGIVER_GEOGRAPHY_PREFERENCE, saved.getId(), profile.getPrimaryBranchId(), metadata(saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public CaregiverShiftPreference createShiftPreference(@NotNull AgencyMembership actorMembership, @NotNull UUID caregiverProfileId, @Valid ManageCaregiverShiftPreferenceCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverProfileId);
        CaregiverShiftPreference saved = caregiverShiftPreferenceRepository.saveAndFlush(CaregiverShiftPreference.create(
                profile,
                command.dayOfWeek(),
                command.preferredStartTime(),
                command.preferredEndTime(),
                command.preferredShiftLengthMinutes(),
                command.preferredVisitTypes(),
                command.preferenceStrength(),
                command.notes()));
        workforceAuditService.recordCreated(actorMembership, Epic4WorkforceTargetType.CAREGIVER_SHIFT_PREFERENCE, saved.getId(), profile.getPrimaryBranchId(), metadata(saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public CaregiverAvailability createAvailability(@NotNull AgencyMembership actorMembership, @NotNull UUID caregiverProfileId, @Valid ManageCaregiverAvailabilityCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREGIVER_AVAILABILITY);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverProfileId);
        CaregiverAvailability availability = CaregiverAvailability.create(
                profile,
                findBranch(actorMembership.getAgencyId(), command.branchId()),
                command.availabilityType(),
                command.startsAt(),
                command.endsAt(),
                command.dayOfWeek(),
                command.startTime(),
                command.endTime(),
                command.effectiveFrom(),
                command.effectiveTo(),
                command.notes());
        List<CaregiverAvailability> existing = caregiverAvailabilityRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(profile.getId());
        if (existing.stream().filter(item -> item.getStatus() == WorkforceLifecycleStatus.ACTIVE).anyMatch(item -> item.overlaps(availability))) {
            throw new WorkforceConflictException("Availability overlaps an existing active availability window.");
        }
        CaregiverAvailability saved = caregiverAvailabilityRepository.saveAndFlush(availability);
        workforceAuditService.recordCreated(actorMembership, Epic4WorkforceTargetType.CAREGIVER_AVAILABILITY, saved.getId(), profile.getPrimaryBranchId(), metadata(saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public CaregiverUnavailability createUnavailability(@NotNull AgencyMembership actorMembership, @NotNull UUID caregiverProfileId, @Valid ManageCaregiverUnavailabilityCommand command) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREGIVER_UNAVAILABILITY);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverProfileId);
        CaregiverUnavailability unavailability = CaregiverUnavailability.create(
                profile,
                command.reasonType(),
                command.startsAt(),
                command.endsAt(),
                command.allDay(),
                command.approvalStatus(),
                command.notes());
        List<CaregiverUnavailability> existing = caregiverUnavailabilityRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(profile.getId());
        if (existing.stream().filter(item -> item.getStatus() == WorkforceLifecycleStatus.ACTIVE).anyMatch(item -> item.overlaps(unavailability))) {
            throw new WorkforceConflictException("Unavailability overlaps an existing active unavailability window.");
        }
        CaregiverUnavailability saved = caregiverUnavailabilityRepository.saveAndFlush(unavailability);
        workforceAuditService.recordCreated(actorMembership, Epic4WorkforceTargetType.CAREGIVER_UNAVAILABILITY, saved.getId(), profile.getPrimaryBranchId(), metadata(saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public CaregiverProfile deactivateProfile(@NotNull AgencyMembership actorMembership, @NotNull UUID caregiverProfileId) {
        requirePermission(actorMembership, AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverProfileId);
        profile.deactivate();
        CaregiverProfile saved = caregiverProfileRepository.saveAndFlush(profile);
        workforceAuditService.recordDeactivated(actorMembership, Epic4WorkforceTargetType.CAREGIVER_PROFILE, saved.getId(), saved.getPrimaryBranchId(), metadata(saved.getStatus().name()));
        return saved;
    }

    private void requirePermission(AgencyMembership actorMembership, AgencyPermission permission) {
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, UnauthorizedWorkforceActorException::new);
    }

    private CaregiverProfile findProfile(UUID agencyId, UUID caregiverProfileId) {
        return caregiverProfileRepository.findByIdAndAgency_Id(caregiverProfileId, agencyId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverProfile", caregiverProfileId));
    }

    private Branch findBranch(UUID agencyId, UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findByIdAndAgency_Id(branchId, agencyId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("Branch", branchId));
    }

    private CaregiverCertification findCertification(UUID agencyId, UUID certificationId) {
        if (certificationId == null) {
            return null;
        }
        CaregiverCertification certification = caregiverCertificationRepository.findById(certificationId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverCertification", certificationId));
        assertSameAgency(agencyId, certification.getAgencyId(), "CaregiverCertification", certificationId);
        return certification;
    }

    private static void assertSameAgency(UUID expectedAgencyId, UUID actualAgencyId, String entityType, UUID entityId) {
        if (!expectedAgencyId.equals(actualAgencyId)) {
            throw new WorkforceEntityNotFoundException(entityType, entityId);
        }
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String metadata(String status) {
        return "{\"status\":\"" + status + "\"}";
    }

    public record ManageCaregiverProfileCommand(
            @NotNull UUID agencyMembershipId,
            UUID primaryBranchId,
            String caregiverCode,
            String displayName,
            String employmentType,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
    }

    public record ManageCaregiverCredentialCommand(
            UUID certificationId,
            @NotBlank String credentialType,
            String licenseNumber,
            String issuingAuthority,
            LocalDate issuedOn,
            LocalDate expiresOn,
            @NotNull CaregiverCredentialStatus status,
            CaregiverCredentialVerificationStatus verificationStatus,
            String notes) {
    }

    public record ManageCaregiverLanguageCommand(
            @NotBlank String languageCode,
            String proficiencyLevel,
            boolean primaryLanguage) {
    }

    public record ManageCaregiverSkillCommand(
            @NotNull UUID skillId,
            String proficiencyLevel,
            boolean verified,
            String notes) {
    }

    public record ManageCaregiverGeographyPreferenceCommand(
            UUID branchId,
            @NotNull CaregiverGeographyPreferenceType preferenceType,
            String postalCode,
            String city,
            String state,
            BigDecimal anchorLatitude,
            BigDecimal anchorLongitude,
            BigDecimal radiusMiles,
            Integer priorityRank,
            String notes) {
    }

    public record ManageCaregiverShiftPreferenceCommand(
            DayOfWeek dayOfWeek,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            Integer preferredShiftLengthMinutes,
            String preferredVisitTypes,
            ShiftPreferenceStrength preferenceStrength,
            String notes) {
    }

    public record ManageCaregiverAvailabilityCommand(
            UUID branchId,
            @NotNull CaregiverAvailabilityType availabilityType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String notes) {
    }

    public record ManageCaregiverUnavailabilityCommand(
            @NotNull CaregiverUnavailabilityReasonType reasonType,
            @NotNull OffsetDateTime startsAt,
            @NotNull OffsetDateTime endsAt,
            boolean allDay,
            CaregiverUnavailabilityApprovalStatus approvalStatus,
            String notes) {
    }
}
