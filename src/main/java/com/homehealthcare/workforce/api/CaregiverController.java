package com.homehealthcare.workforce.api;

import com.homehealthcare.branch.domain.Branch;
import com.homehealthcare.branch.domain.BranchRepository;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailabilityRepository;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailabilityType;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityApprovalStatus;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityReasonType;
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
import com.homehealthcare.caregiverskillprofile.domain.CaregiverSkillProfile;
import com.homehealthcare.caregiverskillprofile.domain.CaregiverSkillProfileRepository;
import com.homehealthcare.configuration.api.ConfigurationActorResolver;
import com.homehealthcare.configuration.api.PagedResponse;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.user.domain.User;
import com.homehealthcare.user.domain.UserRepository;
import com.homehealthcare.workforce.application.CaregiverWorkforceService;
import com.homehealthcare.workforce.application.UnauthorizedWorkforceActorException;
import com.homehealthcare.workforce.application.WorkforceEntityNotFoundException;
import com.homehealthcare.workforce.foundation.WorkforceAuditService;
import com.homehealthcare.workforce.foundation.WorkforceLifecycleStatus;
import com.homehealthcare.workforce.foundation.WorkforcePerformanceIndicatorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/caregivers")
class CaregiverController {

    private final ConfigurationActorResolver configurationActorResolver;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final CaregiverWorkforceService caregiverWorkforceService;
    private final CaregiverProfileRepository caregiverProfileRepository;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final CaregiverCredentialRepository caregiverCredentialRepository;
    private final CaregiverLanguageProfileRepository caregiverLanguageProfileRepository;
    private final CaregiverSkillProfileRepository caregiverSkillProfileRepository;
    private final CaregiverGeographyPreferenceRepository caregiverGeographyPreferenceRepository;
    private final CaregiverShiftPreferenceRepository caregiverShiftPreferenceRepository;
    private final CaregiverAvailabilityRepository caregiverAvailabilityRepository;
    private final com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityRepository caregiverUnavailabilityRepository;
    private final WorkforceAuditService workforceAuditService;

    CaregiverController(
            ConfigurationActorResolver configurationActorResolver,
            AgencyAuthorizationGuard agencyAuthorizationGuard,
            CaregiverWorkforceService caregiverWorkforceService,
            CaregiverProfileRepository caregiverProfileRepository,
            AgencyMembershipRepository agencyMembershipRepository,
            UserRepository userRepository,
            BranchRepository branchRepository,
            CaregiverCredentialRepository caregiverCredentialRepository,
            CaregiverLanguageProfileRepository caregiverLanguageProfileRepository,
            CaregiverSkillProfileRepository caregiverSkillProfileRepository,
            CaregiverGeographyPreferenceRepository caregiverGeographyPreferenceRepository,
            CaregiverShiftPreferenceRepository caregiverShiftPreferenceRepository,
            CaregiverAvailabilityRepository caregiverAvailabilityRepository,
            com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityRepository caregiverUnavailabilityRepository,
            WorkforceAuditService workforceAuditService) {
        this.configurationActorResolver = configurationActorResolver;
        this.agencyAuthorizationGuard = agencyAuthorizationGuard;
        this.caregiverWorkforceService = caregiverWorkforceService;
        this.caregiverProfileRepository = caregiverProfileRepository;
        this.agencyMembershipRepository = agencyMembershipRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.caregiverCredentialRepository = caregiverCredentialRepository;
        this.caregiverLanguageProfileRepository = caregiverLanguageProfileRepository;
        this.caregiverSkillProfileRepository = caregiverSkillProfileRepository;
        this.caregiverGeographyPreferenceRepository = caregiverGeographyPreferenceRepository;
        this.caregiverShiftPreferenceRepository = caregiverShiftPreferenceRepository;
        this.caregiverAvailabilityRepository = caregiverAvailabilityRepository;
        this.caregiverUnavailabilityRepository = caregiverUnavailabilityRepository;
        this.workforceAuditService = workforceAuditService;
    }

    @GetMapping
    PagedResponse<CaregiverSummaryResponse> list(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) WorkforceLifecycleStatus status,
            @RequestParam(name = "branchId", required = false) UUID branchId,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) int size) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.VIEW_WORKFORCE_DIRECTORY);
        List<CaregiverProfile> profiles = caregiverProfileRepository.findAllByAgency_IdOrderByCreatedAtAsc(actorMembership.getAgencyId());
        Map<UUID, AgencyMembership> membershipsById = agencyMembershipRepository.findAllById(
                        profiles.stream().map(CaregiverProfile::getAgencyMembershipId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(AgencyMembership::getId, Function.identity()));
        Map<UUID, User> usersById = userRepository.findAllById(
                        membershipsById.values().stream().map(AgencyMembership::getUserId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<UUID, Branch> branchesById = branchRepository.findAllById(
                        profiles.stream()
                                .map(CaregiverProfile::getPrimaryBranchId)
                                .filter(id -> id != null)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Branch::getId, Function.identity()));

        List<CaregiverSummaryResponse> filtered = profiles.stream()
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> branchId == null || branchId.equals(item.getPrimaryBranchId()))
                .map(item -> toSummaryResponse(item, membershipsById.get(item.getAgencyMembershipId()), usersById, branchesById))
                .filter(item -> matchesSearch(item, search))
                .sorted(Comparator.comparing(CaregiverSummaryResponse::displayName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(CaregiverSummaryResponse::caregiverCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        return page(filtered, page, size);
    }

    @GetMapping("/{caregiverId}")
    CaregiverProfileResponse get(@PathVariable UUID caregiverId) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.VIEW_WORKFORCE_DIRECTORY);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverId);
        return toProfileResponse(profile);
    }

    @PostMapping
    CaregiverProfileResponse create(@Valid @RequestBody ManageCaregiverProfileRequest request) {
        CaregiverProfile saved = caregiverWorkforceService.createProfile(
                configurationActorResolver.requireActorMembership(),
                toProfileCommand(request));
        return toProfileResponse(saved);
    }

    @PutMapping("/{caregiverId}")
    CaregiverProfileResponse update(@PathVariable UUID caregiverId, @Valid @RequestBody ManageCaregiverProfileRequest request) {
        CaregiverProfile saved = caregiverWorkforceService.updateProfile(
                configurationActorResolver.requireActorMembership(),
                caregiverId,
                toProfileCommand(request));
        return toProfileResponse(saved);
    }

    @DeleteMapping("/{caregiverId}")
    CaregiverProfileResponse deactivate(@PathVariable UUID caregiverId) {
        CaregiverProfile saved = caregiverWorkforceService.deactivateProfile(configurationActorResolver.requireActorMembership(), caregiverId);
        return toProfileResponse(saved);
    }

    @GetMapping("/{caregiverId}/credentials")
    List<CaregiverCredentialResponse> credentials(@PathVariable UUID caregiverId) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.MANAGE_CAREGIVER_CREDENTIALS);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverId);
        return caregiverCredentialRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(profile.getId()).stream()
                .map(CaregiverController::toCredentialResponse)
                .toList();
    }

    @PostMapping("/{caregiverId}/credentials")
    CaregiverCredentialResponse createCredential(@PathVariable UUID caregiverId, @Valid @RequestBody ManageCaregiverCredentialRequest request) {
        CaregiverCredential saved = caregiverWorkforceService.createCredential(
                configurationActorResolver.requireActorMembership(),
                caregiverId,
                toCredentialCommand(request));
        return toCredentialResponse(saved);
    }

    @PutMapping("/{caregiverId}/credentials/{credentialId}")
    CaregiverCredentialResponse updateCredential(
            @PathVariable UUID caregiverId,
            @PathVariable UUID credentialId,
            @Valid @RequestBody ManageCaregiverCredentialRequest request) {
        assertCredentialParent(caregiverId, credentialId);
        CaregiverCredential saved = caregiverWorkforceService.updateCredential(
                configurationActorResolver.requireActorMembership(),
                credentialId,
                toCredentialCommand(request));
        return toCredentialResponse(saved);
    }

    @DeleteMapping("/{caregiverId}/credentials/{credentialId}")
    CaregiverCredentialResponse deactivateCredential(@PathVariable UUID caregiverId, @PathVariable UUID credentialId) {
        assertCredentialParent(caregiverId, credentialId);
        CaregiverCredential saved = caregiverWorkforceService.deactivateCredential(configurationActorResolver.requireActorMembership(), credentialId);
        return toCredentialResponse(saved);
    }

    @GetMapping("/{caregiverId}/languages")
    List<CaregiverLanguageResponse> languages(@PathVariable UUID caregiverId) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverId);
        return caregiverLanguageProfileRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(profile.getId()).stream()
                .map(CaregiverController::toLanguageResponse)
                .toList();
    }

    @PostMapping("/{caregiverId}/languages")
    CaregiverLanguageResponse createLanguage(@PathVariable UUID caregiverId, @Valid @RequestBody ManageCaregiverLanguageRequest request) {
        CaregiverLanguageProfile saved = caregiverWorkforceService.createLanguage(
                configurationActorResolver.requireActorMembership(),
                caregiverId,
                new CaregiverWorkforceService.ManageCaregiverLanguageCommand(
                        request.languageCode(),
                        request.proficiencyLevel(),
                        request.primaryLanguage()));
        return toLanguageResponse(saved);
    }

    @PutMapping("/{caregiverId}/languages/{languageId}")
    CaregiverLanguageResponse updateLanguage(
            @PathVariable UUID caregiverId,
            @PathVariable UUID languageId,
            @Valid @RequestBody ManageCaregiverLanguageRequest request) {
        assertLanguageParent(caregiverId, languageId);
        CaregiverLanguageProfile saved = caregiverWorkforceService.updateLanguage(
                configurationActorResolver.requireActorMembership(),
                languageId,
                new CaregiverWorkforceService.ManageCaregiverLanguageCommand(
                        request.languageCode(),
                        request.proficiencyLevel(),
                        request.primaryLanguage()));
        return toLanguageResponse(saved);
    }

    @DeleteMapping("/{caregiverId}/languages/{languageId}")
    CaregiverLanguageResponse deactivateLanguage(@PathVariable UUID caregiverId, @PathVariable UUID languageId) {
        assertLanguageParent(caregiverId, languageId);
        CaregiverLanguageProfile saved = caregiverWorkforceService.deactivateLanguage(configurationActorResolver.requireActorMembership(), languageId);
        return toLanguageResponse(saved);
    }

    @GetMapping("/{caregiverId}/skills")
    List<CaregiverSkillResponse> skills(@PathVariable UUID caregiverId) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverId);
        return caregiverSkillProfileRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(profile.getId()).stream()
                .map(CaregiverController::toCaregiverSkillResponse)
                .toList();
    }

    @PostMapping("/{caregiverId}/skills")
    CaregiverSkillResponse createSkill(@PathVariable UUID caregiverId, @Valid @RequestBody ManageCaregiverSkillRequest request) {
        CaregiverSkillProfile saved = caregiverWorkforceService.createSkill(
                configurationActorResolver.requireActorMembership(),
                caregiverId,
                new CaregiverWorkforceService.ManageCaregiverSkillCommand(
                        request.skillId(),
                        request.proficiencyLevel(),
                        request.verified(),
                        request.notes()));
        return toCaregiverSkillResponse(saved);
    }

    @PutMapping("/{caregiverId}/skills/{skillProfileId}")
    CaregiverSkillResponse updateSkill(
            @PathVariable UUID caregiverId,
            @PathVariable UUID skillProfileId,
            @Valid @RequestBody ManageCaregiverSkillRequest request) {
        assertSkillParent(caregiverId, skillProfileId);
        CaregiverSkillProfile saved = caregiverWorkforceService.updateSkill(
                configurationActorResolver.requireActorMembership(),
                skillProfileId,
                new CaregiverWorkforceService.ManageCaregiverSkillCommand(
                        request.skillId(),
                        request.proficiencyLevel(),
                        request.verified(),
                        request.notes()));
        return toCaregiverSkillResponse(saved);
    }

    @DeleteMapping("/{caregiverId}/skills/{skillProfileId}")
    CaregiverSkillResponse deactivateSkill(@PathVariable UUID caregiverId, @PathVariable UUID skillProfileId) {
        assertSkillParent(caregiverId, skillProfileId);
        CaregiverSkillProfile saved = caregiverWorkforceService.deactivateSkill(configurationActorResolver.requireActorMembership(), skillProfileId);
        return toCaregiverSkillResponse(saved);
    }

    @GetMapping("/{caregiverId}/geography-preferences")
    List<CaregiverGeographyPreferenceResponse> geographyPreferences(@PathVariable UUID caregiverId) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverId);
        return caregiverGeographyPreferenceRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(profile.getId()).stream()
                .map(CaregiverController::toGeographyResponse)
                .toList();
    }

    @PostMapping("/{caregiverId}/geography-preferences")
    CaregiverGeographyPreferenceResponse createGeographyPreference(
            @PathVariable UUID caregiverId,
            @Valid @RequestBody ManageCaregiverGeographyPreferenceRequest request) {
        CaregiverGeographyPreference saved = caregiverWorkforceService.createGeographyPreference(
                configurationActorResolver.requireActorMembership(),
                caregiverId,
                toGeographyCommand(request));
        return toGeographyResponse(saved);
    }

    @PutMapping("/{caregiverId}/geography-preferences/{preferenceId}")
    CaregiverGeographyPreferenceResponse updateGeographyPreference(
            @PathVariable UUID caregiverId,
            @PathVariable UUID preferenceId,
            @Valid @RequestBody ManageCaregiverGeographyPreferenceRequest request) {
        assertGeographyParent(caregiverId, preferenceId);
        CaregiverGeographyPreference saved = caregiverWorkforceService.updateGeographyPreference(
                configurationActorResolver.requireActorMembership(),
                preferenceId,
                toGeographyCommand(request));
        return toGeographyResponse(saved);
    }

    @DeleteMapping("/{caregiverId}/geography-preferences/{preferenceId}")
    CaregiverGeographyPreferenceResponse deactivateGeographyPreference(@PathVariable UUID caregiverId, @PathVariable UUID preferenceId) {
        assertGeographyParent(caregiverId, preferenceId);
        CaregiverGeographyPreference saved = caregiverWorkforceService.deactivateGeographyPreference(
                configurationActorResolver.requireActorMembership(),
                preferenceId);
        return toGeographyResponse(saved);
    }

    @GetMapping("/{caregiverId}/shift-preferences")
    List<CaregiverShiftPreferenceResponse> shiftPreferences(@PathVariable UUID caregiverId) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.MANAGE_CAREGIVER_PROFILES);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverId);
        return caregiverShiftPreferenceRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(profile.getId()).stream()
                .map(CaregiverController::toShiftPreferenceResponse)
                .toList();
    }

    @PostMapping("/{caregiverId}/shift-preferences")
    CaregiverShiftPreferenceResponse createShiftPreference(
            @PathVariable UUID caregiverId,
            @Valid @RequestBody ManageCaregiverShiftPreferenceRequest request) {
        CaregiverShiftPreference saved = caregiverWorkforceService.createShiftPreference(
                configurationActorResolver.requireActorMembership(),
                caregiverId,
                toShiftCommand(request));
        return toShiftPreferenceResponse(saved);
    }

    @PutMapping("/{caregiverId}/shift-preferences/{shiftPreferenceId}")
    CaregiverShiftPreferenceResponse updateShiftPreference(
            @PathVariable UUID caregiverId,
            @PathVariable UUID shiftPreferenceId,
            @Valid @RequestBody ManageCaregiverShiftPreferenceRequest request) {
        assertShiftParent(caregiverId, shiftPreferenceId);
        CaregiverShiftPreference saved = caregiverWorkforceService.updateShiftPreference(
                configurationActorResolver.requireActorMembership(),
                shiftPreferenceId,
                toShiftCommand(request));
        return toShiftPreferenceResponse(saved);
    }

    @DeleteMapping("/{caregiverId}/shift-preferences/{shiftPreferenceId}")
    CaregiverShiftPreferenceResponse deactivateShiftPreference(@PathVariable UUID caregiverId, @PathVariable UUID shiftPreferenceId) {
        assertShiftParent(caregiverId, shiftPreferenceId);
        CaregiverShiftPreference saved = caregiverWorkforceService.deactivateShiftPreference(
                configurationActorResolver.requireActorMembership(),
                shiftPreferenceId);
        return toShiftPreferenceResponse(saved);
    }

    @GetMapping("/{caregiverId}/availabilities")
    List<CaregiverAvailabilityResponse> availabilities(@PathVariable UUID caregiverId) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.MANAGE_CAREGIVER_AVAILABILITY);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverId);
        return caregiverAvailabilityRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(profile.getId()).stream()
                .map(CaregiverController::toAvailabilityResponse)
                .toList();
    }

    @PostMapping("/{caregiverId}/availabilities")
    CaregiverAvailabilityResponse createAvailability(
            @PathVariable UUID caregiverId,
            @Valid @RequestBody ManageCaregiverAvailabilityRequest request) {
        CaregiverAvailability saved = caregiverWorkforceService.createAvailability(
                configurationActorResolver.requireActorMembership(),
                caregiverId,
                toAvailabilityCommand(request));
        return toAvailabilityResponse(saved);
    }

    @PutMapping("/{caregiverId}/availabilities/{availabilityId}")
    CaregiverAvailabilityResponse updateAvailability(
            @PathVariable UUID caregiverId,
            @PathVariable UUID availabilityId,
            @Valid @RequestBody ManageCaregiverAvailabilityRequest request) {
        assertAvailabilityParent(caregiverId, availabilityId);
        CaregiverAvailability saved = caregiverWorkforceService.updateAvailability(
                configurationActorResolver.requireActorMembership(),
                availabilityId,
                toAvailabilityCommand(request));
        return toAvailabilityResponse(saved);
    }

    @DeleteMapping("/{caregiverId}/availabilities/{availabilityId}")
    CaregiverAvailabilityResponse deactivateAvailability(@PathVariable UUID caregiverId, @PathVariable UUID availabilityId) {
        assertAvailabilityParent(caregiverId, availabilityId);
        CaregiverAvailability saved = caregiverWorkforceService.deactivateAvailability(configurationActorResolver.requireActorMembership(), availabilityId);
        return toAvailabilityResponse(saved);
    }

    @GetMapping("/{caregiverId}/unavailabilities")
    List<CaregiverUnavailabilityResponse> unavailabilities(@PathVariable UUID caregiverId) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.MANAGE_CAREGIVER_UNAVAILABILITY);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverId);
        return caregiverUnavailabilityRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(profile.getId()).stream()
                .map(CaregiverController::toUnavailabilityResponse)
                .toList();
    }

    @PostMapping("/{caregiverId}/unavailabilities")
    CaregiverUnavailabilityResponse createUnavailability(
            @PathVariable UUID caregiverId,
            @Valid @RequestBody ManageCaregiverUnavailabilityRequest request) {
        CaregiverUnavailability saved = caregiverWorkforceService.createUnavailability(
                configurationActorResolver.requireActorMembership(),
                caregiverId,
                toUnavailabilityCommand(request));
        return toUnavailabilityResponse(saved);
    }

    @PutMapping("/{caregiverId}/unavailabilities/{unavailabilityId}")
    CaregiverUnavailabilityResponse updateUnavailability(
            @PathVariable UUID caregiverId,
            @PathVariable UUID unavailabilityId,
            @Valid @RequestBody ManageCaregiverUnavailabilityRequest request) {
        assertUnavailabilityParent(caregiverId, unavailabilityId);
        CaregiverUnavailability saved = caregiverWorkforceService.updateUnavailability(
                configurationActorResolver.requireActorMembership(),
                unavailabilityId,
                toUnavailabilityCommand(request));
        return toUnavailabilityResponse(saved);
    }

    @DeleteMapping("/{caregiverId}/unavailabilities/{unavailabilityId}")
    CaregiverUnavailabilityResponse deactivateUnavailability(@PathVariable UUID caregiverId, @PathVariable UUID unavailabilityId) {
        assertUnavailabilityParent(caregiverId, unavailabilityId);
        CaregiverUnavailability saved = caregiverWorkforceService.deactivateUnavailability(
                configurationActorResolver.requireActorMembership(),
                unavailabilityId);
        return toUnavailabilityResponse(saved);
    }

    @GetMapping("/{caregiverId}/performance-summary")
    CaregiverPerformanceSummaryResponse performanceSummary(
            @PathVariable UUID caregiverId,
            @RequestParam(name = "windowStart", required = false) OffsetDateTime windowStart,
            @RequestParam(name = "windowEnd", required = false) OffsetDateTime windowEnd) {
        AgencyMembership actorMembership = requirePermission(AgencyPermission.VIEW_CAREGIVER_PERFORMANCE);
        CaregiverProfile profile = findProfile(actorMembership.getAgencyId(), caregiverId);
        OffsetDateTime effectiveWindowStart = windowStart == null ? OffsetDateTime.now().minusDays(30) : windowStart;
        OffsetDateTime effectiveWindowEnd = windowEnd == null ? OffsetDateTime.now() : windowEnd;

        List<CaregiverCredential> credentials = caregiverCredentialRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverId);
        List<CaregiverLanguageProfile> languages = caregiverLanguageProfileRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverId);
        List<CaregiverSkillProfile> skills = caregiverSkillProfileRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverId);
        List<CaregiverAvailability> availabilities = caregiverAvailabilityRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverId);
        List<CaregiverUnavailability> unavailabilities = caregiverUnavailabilityRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverId);

        long activeCredentialCount = credentials.stream().filter(item -> item.getStatus() == CaregiverCredentialStatus.ACTIVE).count();
        long expiringCredentialCount = credentials.stream()
                .filter(item -> item.getStatus() == CaregiverCredentialStatus.ACTIVE)
                .filter(item -> item.getExpiresOn() != null)
                .filter(item -> !item.getExpiresOn().isBefore(effectiveWindowStart.toLocalDate()))
                .filter(item -> !item.getExpiresOn().isAfter(effectiveWindowEnd.toLocalDate().plusDays(30)))
                .count();
        long activeLanguageCount = languages.stream().filter(item -> item.getStatus() == WorkforceLifecycleStatus.ACTIVE).count();
        long activeSkillCount = skills.stream().filter(item -> item.getStatus() == WorkforceLifecycleStatus.ACTIVE).count();
        long recurringAvailabilityCount = availabilities.stream()
                .filter(item -> item.getStatus() == WorkforceLifecycleStatus.ACTIVE && item.getAvailabilityType() == CaregiverAvailabilityType.RECURRING)
                .count();
        long dateSpecificAvailabilityCount = availabilities.stream()
                .filter(item -> item.getStatus() == WorkforceLifecycleStatus.ACTIVE && item.getAvailabilityType() == CaregiverAvailabilityType.DATE_SPECIFIC)
                .count();
        long activeUnavailabilityCount = unavailabilities.stream()
                .filter(item -> item.getStatus() == WorkforceLifecycleStatus.ACTIVE)
                .filter(item -> item.getStartsAt().isBefore(effectiveWindowEnd) && effectiveWindowStart.isBefore(item.getEndsAt()))
                .count();
        boolean currentlySchedulable = profile.getStatus() == WorkforceLifecycleStatus.ACTIVE
                && activeUnavailabilityCount == 0;

        workforceAuditService.recordPerformanceRefreshed(
                actorMembership,
                profile.getId(),
                profile.getPrimaryBranchId(),
                "{\"windowStart\":\"" + effectiveWindowStart + "\",\"windowEnd\":\"" + effectiveWindowEnd + "\"}");

        return new CaregiverPerformanceSummaryResponse(
                profile.getId(),
                effectiveWindowStart,
                effectiveWindowEnd,
                profile.getStatus(),
                currentlySchedulable,
                activeCredentialCount,
                expiringCredentialCount,
                activeLanguageCount,
                activeSkillCount,
                recurringAvailabilityCount,
                dateSpecificAvailabilityCount,
                activeUnavailabilityCount,
                List.of(
                        WorkforcePerformanceIndicatorType.COMPLETED_VISITS_COUNT,
                        WorkforcePerformanceIndicatorType.MISSED_VISITS_COUNT,
                        WorkforcePerformanceIndicatorType.ON_TIME_PERCENTAGE,
                        WorkforcePerformanceIndicatorType.DOCUMENTATION_COMPLETION_PERCENTAGE,
                        WorkforcePerformanceIndicatorType.EXCEPTION_COUNT));
    }

    private AgencyMembership requirePermission(AgencyPermission permission) {
        AgencyMembership actorMembership = configurationActorResolver.requireActorMembership();
        agencyAuthorizationGuard.requirePermission(actorMembership, permission, UnauthorizedWorkforceActorException::new);
        return actorMembership;
    }

    private CaregiverProfile findProfile(UUID agencyId, UUID caregiverId) {
        return caregiverProfileRepository.findByIdAndAgency_Id(caregiverId, agencyId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverProfile", caregiverId));
    }

    private void assertCredentialParent(UUID caregiverId, UUID credentialId) {
        CaregiverCredential credential = caregiverCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverCredential", credentialId));
        if (!caregiverId.equals(credential.getCaregiverProfileId())) {
            throw new WorkforceEntityNotFoundException("CaregiverCredential", credentialId);
        }
    }

    private void assertLanguageParent(UUID caregiverId, UUID languageId) {
        CaregiverLanguageProfile language = caregiverLanguageProfileRepository.findById(languageId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverLanguage", languageId));
        if (!caregiverId.equals(language.getCaregiverProfileId())) {
            throw new WorkforceEntityNotFoundException("CaregiverLanguage", languageId);
        }
    }

    private void assertSkillParent(UUID caregiverId, UUID skillProfileId) {
        CaregiverSkillProfile caregiverSkillProfile = caregiverSkillProfileRepository.findById(skillProfileId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverSkillProfile", skillProfileId));
        if (!caregiverId.equals(caregiverSkillProfile.getCaregiverProfileId())) {
            throw new WorkforceEntityNotFoundException("CaregiverSkillProfile", skillProfileId);
        }
    }

    private void assertGeographyParent(UUID caregiverId, UUID preferenceId) {
        CaregiverGeographyPreference geographyPreference = caregiverGeographyPreferenceRepository.findById(preferenceId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverGeographyPreference", preferenceId));
        if (!caregiverId.equals(geographyPreference.getCaregiverProfileId())) {
            throw new WorkforceEntityNotFoundException("CaregiverGeographyPreference", preferenceId);
        }
    }

    private void assertShiftParent(UUID caregiverId, UUID shiftPreferenceId) {
        CaregiverShiftPreference shiftPreference = caregiverShiftPreferenceRepository.findById(shiftPreferenceId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverShiftPreference", shiftPreferenceId));
        if (!caregiverId.equals(shiftPreference.getCaregiverProfileId())) {
            throw new WorkforceEntityNotFoundException("CaregiverShiftPreference", shiftPreferenceId);
        }
    }

    private void assertAvailabilityParent(UUID caregiverId, UUID availabilityId) {
        CaregiverAvailability availability = caregiverAvailabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverAvailability", availabilityId));
        if (!caregiverId.equals(availability.getCaregiverProfileId())) {
            throw new WorkforceEntityNotFoundException("CaregiverAvailability", availabilityId);
        }
    }

    private void assertUnavailabilityParent(UUID caregiverId, UUID unavailabilityId) {
        CaregiverUnavailability unavailability = caregiverUnavailabilityRepository.findById(unavailabilityId)
                .orElseThrow(() -> new WorkforceEntityNotFoundException("CaregiverUnavailability", unavailabilityId));
        if (!caregiverId.equals(unavailability.getCaregiverProfileId())) {
            throw new WorkforceEntityNotFoundException("CaregiverUnavailability", unavailabilityId);
        }
    }

    private static boolean matchesSearch(CaregiverSummaryResponse item, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String normalized = search.trim().toLowerCase(Locale.ROOT);
        return contains(item.displayName(), normalized)
                || contains(item.caregiverCode(), normalized)
                || contains(item.userEmail(), normalized)
                || contains(item.userPhone(), normalized)
                || contains(item.userFullName(), normalized)
                || contains(item.branchName(), normalized);
    }

    private static boolean contains(String source, String search) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(search);
    }

    private static <T> PagedResponse<T> page(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) items.size() / size);
        return new PagedResponse<>(items.subList(fromIndex, toIndex), page, size, items.size(), totalPages);
    }

    private static CaregiverWorkforceService.ManageCaregiverProfileCommand toProfileCommand(ManageCaregiverProfileRequest request) {
        return new CaregiverWorkforceService.ManageCaregiverProfileCommand(
                request.agencyMembershipId(),
                request.primaryBranchId(),
                request.caregiverCode(),
                request.displayName(),
                request.employmentType(),
                request.startDate(),
                request.endDate(),
                request.notes());
    }

    private static CaregiverWorkforceService.ManageCaregiverCredentialCommand toCredentialCommand(ManageCaregiverCredentialRequest request) {
        return new CaregiverWorkforceService.ManageCaregiverCredentialCommand(
                request.certificationId(),
                request.credentialType(),
                request.licenseNumber(),
                request.issuingAuthority(),
                request.issuedOn(),
                request.expiresOn(),
                request.status(),
                request.verificationStatus(),
                request.notes());
    }

    private static CaregiverWorkforceService.ManageCaregiverGeographyPreferenceCommand toGeographyCommand(
            ManageCaregiverGeographyPreferenceRequest request) {
        return new CaregiverWorkforceService.ManageCaregiverGeographyPreferenceCommand(
                request.branchId(),
                request.preferenceType(),
                request.postalCode(),
                request.city(),
                request.state(),
                request.anchorLatitude(),
                request.anchorLongitude(),
                request.radiusMiles(),
                request.priorityRank(),
                request.notes());
    }

    private static CaregiverWorkforceService.ManageCaregiverShiftPreferenceCommand toShiftCommand(ManageCaregiverShiftPreferenceRequest request) {
        return new CaregiverWorkforceService.ManageCaregiverShiftPreferenceCommand(
                request.dayOfWeek(),
                request.preferredStartTime(),
                request.preferredEndTime(),
                request.preferredShiftLengthMinutes(),
                request.preferredVisitTypes(),
                request.preferenceStrength(),
                request.notes());
    }

    private static CaregiverWorkforceService.ManageCaregiverAvailabilityCommand toAvailabilityCommand(ManageCaregiverAvailabilityRequest request) {
        return new CaregiverWorkforceService.ManageCaregiverAvailabilityCommand(
                request.branchId(),
                request.availabilityType(),
                request.startsAt(),
                request.endsAt(),
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.effectiveFrom(),
                request.effectiveTo(),
                request.notes());
    }

    private static CaregiverWorkforceService.ManageCaregiverUnavailabilityCommand toUnavailabilityCommand(ManageCaregiverUnavailabilityRequest request) {
        return new CaregiverWorkforceService.ManageCaregiverUnavailabilityCommand(
                request.reasonType(),
                request.startsAt(),
                request.endsAt(),
                request.allDay(),
                request.approvalStatus(),
                request.notes());
    }

    private CaregiverProfileResponse toProfileResponse(CaregiverProfile profile) {
        AgencyMembership membership = agencyMembershipRepository.findById(profile.getAgencyMembershipId())
                .orElseThrow(() -> new WorkforceEntityNotFoundException("AgencyMembership", profile.getAgencyMembershipId()));
        User user = userRepository.findById(membership.getUserId())
                .orElseThrow(() -> new WorkforceEntityNotFoundException("User", membership.getUserId()));
        Branch branch = profile.getPrimaryBranchId() == null ? null : branchRepository.findById(profile.getPrimaryBranchId()).orElse(null);
        return new CaregiverProfileResponse(
                profile.getId(),
                profile.getAgencyId(),
                profile.getAgencyMembershipId(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                membership.getRole().name(),
                profile.getStatus(),
                profile.getCaregiverCode(),
                profile.getDisplayName(),
                branch == null ? null : branch.getId(),
                branch == null ? null : branch.getName(),
                profile.getEmploymentType(),
                profile.getStartDate(),
                profile.getEndDate(),
                profile.getNotes());
    }

    private static CaregiverSummaryResponse toSummaryResponse(
            CaregiverProfile profile,
            AgencyMembership membership,
            Map<UUID, User> usersById,
            Map<UUID, Branch> branchesById) {
        User user = membership == null ? null : usersById.get(membership.getUserId());
        Branch branch = profile.getPrimaryBranchId() == null ? null : branchesById.get(profile.getPrimaryBranchId());
        String userFullName = user == null ? null : user.getFirstName() + " " + user.getLastName();
        return new CaregiverSummaryResponse(
                profile.getId(),
                profile.getStatus(),
                profile.getCaregiverCode(),
                profile.getDisplayName(),
                membership == null ? null : membership.getId(),
                user == null ? null : user.getId(),
                userFullName,
                user == null ? null : user.getEmail(),
                user == null ? null : user.getPhone(),
                branch == null ? null : branch.getId(),
                branch == null ? null : branch.getName());
    }

    private static CaregiverCredentialResponse toCredentialResponse(CaregiverCredential item) {
        return new CaregiverCredentialResponse(
                item.getId(),
                item.getCaregiverProfileId(),
                item.getCertificationId(),
                item.getCredentialType(),
                item.getLicenseNumber(),
                item.getIssuingAuthority(),
                item.getIssuedOn(),
                item.getExpiresOn(),
                item.getStatus(),
                item.getVerificationStatus(),
                item.getNotes());
    }

    private static CaregiverLanguageResponse toLanguageResponse(CaregiverLanguageProfile item) {
        return new CaregiverLanguageResponse(
                item.getId(),
                item.getCaregiverProfileId(),
                item.getLanguageCode(),
                item.getProficiencyLevel(),
                item.isPrimaryLanguage(),
                item.getStatus());
    }

    private static CaregiverSkillResponse toCaregiverSkillResponse(CaregiverSkillProfile item) {
        return new CaregiverSkillResponse(
                item.getId(),
                item.getCaregiverProfileId(),
                item.getSkillId(),
                item.getSkill().getName(),
                item.getSkill().getCode(),
                item.getProficiencyLevel(),
                item.isVerified(),
                item.getStatus(),
                item.getNotes());
    }

    private static CaregiverGeographyPreferenceResponse toGeographyResponse(CaregiverGeographyPreference item) {
        return new CaregiverGeographyPreferenceResponse(
                item.getId(),
                item.getCaregiverProfileId(),
                item.getBranchId(),
                item.getPreferenceType(),
                item.getPostalCode(),
                item.getCity(),
                item.getState(),
                item.getAnchorLatitude(),
                item.getAnchorLongitude(),
                item.getRadiusMiles(),
                item.getPriorityRank(),
                item.getStatus(),
                item.getNotes());
    }

    private static CaregiverShiftPreferenceResponse toShiftPreferenceResponse(CaregiverShiftPreference item) {
        return new CaregiverShiftPreferenceResponse(
                item.getId(),
                item.getCaregiverProfileId(),
                item.getDayOfWeek(),
                item.getPreferredStartTime(),
                item.getPreferredEndTime(),
                item.getPreferredShiftLengthMinutes(),
                item.getPreferredVisitTypes(),
                item.getPreferenceStrength(),
                item.getStatus(),
                item.getNotes());
    }

    private static CaregiverAvailabilityResponse toAvailabilityResponse(CaregiverAvailability item) {
        return new CaregiverAvailabilityResponse(
                item.getId(),
                item.getCaregiverProfileId(),
                item.getBranchId(),
                item.getAvailabilityType(),
                item.getStartsAt(),
                item.getEndsAt(),
                item.getDayOfWeek(),
                item.getStartTime(),
                item.getEndTime(),
                item.getEffectiveFrom(),
                item.getEffectiveTo(),
                item.getStatus(),
                item.getNotes());
    }

    private static CaregiverUnavailabilityResponse toUnavailabilityResponse(CaregiverUnavailability item) {
        return new CaregiverUnavailabilityResponse(
                item.getId(),
                item.getCaregiverProfileId(),
                item.getReasonType(),
                item.getStartsAt(),
                item.getEndsAt(),
                item.isAllDay(),
                item.getApprovalStatus(),
                item.getStatus(),
                item.getNotes());
    }

    record ManageCaregiverProfileRequest(
            @NotNull UUID agencyMembershipId,
            UUID primaryBranchId,
            String caregiverCode,
            String displayName,
            String employmentType,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
    }

    record ManageCaregiverCredentialRequest(
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

    record ManageCaregiverLanguageRequest(
            @NotBlank String languageCode,
            String proficiencyLevel,
            boolean primaryLanguage) {
    }

    record ManageCaregiverSkillRequest(
            @NotNull UUID skillId,
            String proficiencyLevel,
            boolean verified,
            String notes) {
    }

    record ManageCaregiverGeographyPreferenceRequest(
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

    record ManageCaregiverShiftPreferenceRequest(
            DayOfWeek dayOfWeek,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            Integer preferredShiftLengthMinutes,
            String preferredVisitTypes,
            ShiftPreferenceStrength preferenceStrength,
            String notes) {
    }

    record ManageCaregiverAvailabilityRequest(
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

    record ManageCaregiverUnavailabilityRequest(
            @NotNull CaregiverUnavailabilityReasonType reasonType,
            @NotNull OffsetDateTime startsAt,
            @NotNull OffsetDateTime endsAt,
            boolean allDay,
            CaregiverUnavailabilityApprovalStatus approvalStatus,
            String notes) {
    }

    record CaregiverSummaryResponse(
            UUID id,
            WorkforceLifecycleStatus status,
            String caregiverCode,
            String displayName,
            UUID agencyMembershipId,
            UUID userId,
            String userFullName,
            String userEmail,
            String userPhone,
            UUID branchId,
            String branchName) {
    }

    record CaregiverProfileResponse(
            UUID id,
            UUID agencyId,
            UUID agencyMembershipId,
            UUID userId,
            String userFirstName,
            String userLastName,
            String userEmail,
            String userPhone,
            String membershipRole,
            WorkforceLifecycleStatus status,
            String caregiverCode,
            String displayName,
            UUID primaryBranchId,
            String primaryBranchName,
            String employmentType,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {
    }

    record CaregiverCredentialResponse(
            UUID id,
            UUID caregiverProfileId,
            UUID certificationId,
            String credentialType,
            String licenseNumber,
            String issuingAuthority,
            LocalDate issuedOn,
            LocalDate expiresOn,
            CaregiverCredentialStatus status,
            CaregiverCredentialVerificationStatus verificationStatus,
            String notes) {
    }

    record CaregiverLanguageResponse(
            UUID id,
            UUID caregiverProfileId,
            String languageCode,
            String proficiencyLevel,
            boolean primaryLanguage,
            WorkforceLifecycleStatus status) {
    }

    record CaregiverSkillResponse(
            UUID id,
            UUID caregiverProfileId,
            UUID skillId,
            String skillName,
            String skillCode,
            String proficiencyLevel,
            boolean verified,
            WorkforceLifecycleStatus status,
            String notes) {
    }

    record CaregiverGeographyPreferenceResponse(
            UUID id,
            UUID caregiverProfileId,
            UUID branchId,
            CaregiverGeographyPreferenceType preferenceType,
            String postalCode,
            String city,
            String state,
            BigDecimal anchorLatitude,
            BigDecimal anchorLongitude,
            BigDecimal radiusMiles,
            Integer priorityRank,
            WorkforceLifecycleStatus status,
            String notes) {
    }

    record CaregiverShiftPreferenceResponse(
            UUID id,
            UUID caregiverProfileId,
            DayOfWeek dayOfWeek,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            Integer preferredShiftLengthMinutes,
            String preferredVisitTypes,
            ShiftPreferenceStrength preferenceStrength,
            WorkforceLifecycleStatus status,
            String notes) {
    }

    record CaregiverAvailabilityResponse(
            UUID id,
            UUID caregiverProfileId,
            UUID branchId,
            CaregiverAvailabilityType availabilityType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            WorkforceLifecycleStatus status,
            String notes) {
    }

    record CaregiverUnavailabilityResponse(
            UUID id,
            UUID caregiverProfileId,
            CaregiverUnavailabilityReasonType reasonType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            boolean allDay,
            CaregiverUnavailabilityApprovalStatus approvalStatus,
            WorkforceLifecycleStatus status,
            String notes) {
    }

    record CaregiverPerformanceSummaryResponse(
            UUID caregiverId,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            WorkforceLifecycleStatus profileStatus,
            boolean currentlySchedulable,
            long activeCredentialCount,
            long expiringCredentialCount,
            long activeLanguageCount,
            long activeSkillCount,
            long recurringAvailabilityCount,
            long dateSpecificAvailabilityCount,
            long activeUnavailabilityCount,
            List<WorkforcePerformanceIndicatorType> unsupportedMetrics) {
    }
}
