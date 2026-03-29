package com.homehealthcare.scheduling.application;

import com.homehealthcare.branchpolicy.domain.BranchPolicyRepository;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailabilityRepository;
import com.homehealthcare.caregiveravailability.domain.CaregiverAvailabilityType;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailability;
import com.homehealthcare.caregiveravailability.domain.CaregiverUnavailabilityRepository;
import com.homehealthcare.caregivercredential.domain.CaregiverCredential;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialRepository;
import com.homehealthcare.caregivercredential.domain.CaregiverCredentialStatus;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreference;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreferenceRepository;
import com.homehealthcare.caregivergeography.domain.CaregiverGeographyPreferenceType;
import com.homehealthcare.caregiverlanguage.domain.CaregiverLanguageProfile;
import com.homehealthcare.caregiverlanguage.domain.CaregiverLanguageProfileRepository;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfile;
import com.homehealthcare.caregiverprofile.domain.CaregiverProfileRepository;
import com.homehealthcare.caregiverskillprofile.domain.CaregiverSkillProfile;
import com.homehealthcare.caregiverskillprofile.domain.CaregiverSkillProfileRepository;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.patientaddress.domain.PatientAddress;
import com.homehealthcare.patientaddress.domain.PatientAddressRepository;
import com.homehealthcare.scheduling.foundation.SchedulingAuditService;
import com.homehealthcare.scheduling.foundation.SchedulingConflictOutcome;
import com.homehealthcare.scheduling.foundation.SchedulingTravelAwareness;
import com.homehealthcare.scheduling.foundation.TravelAwarenessLevel;
import com.homehealthcare.schedulingassignment.domain.CaregiverAssignmentStatus;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignment;
import com.homehealthcare.schedulingassignment.domain.CaregiverVisitAssignmentRepository;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrence;
import com.homehealthcare.schedulingvisit.domain.VisitOccurrenceRepository;
import com.homehealthcare.security.authorization.AgencyAuthorizationGuard;
import com.homehealthcare.security.authorization.AgencyPermission;
import com.homehealthcare.workforce.foundation.WorkforceLifecycleStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class SchedulingRulesService {

    private static final String OVERTIME_POLICY_KEY = "SCHEDULING_OVERTIME_RULES";
    private static final String INTEGER_POLICY_PATTERN_TEMPLATE = "\"%s\"\\s*:\\s*(\\d+)";

    private final CaregiverProfileRepository caregiverProfileRepository;
    private final CaregiverVisitAssignmentRepository caregiverVisitAssignmentRepository;
    private final CaregiverAvailabilityRepository caregiverAvailabilityRepository;
    private final CaregiverUnavailabilityRepository caregiverUnavailabilityRepository;
    private final CaregiverCredentialRepository caregiverCredentialRepository;
    private final CaregiverLanguageProfileRepository caregiverLanguageProfileRepository;
    private final CaregiverSkillProfileRepository caregiverSkillProfileRepository;
    private final CaregiverGeographyPreferenceRepository caregiverGeographyPreferenceRepository;
    private final PatientAddressRepository patientAddressRepository;
    private final VisitOccurrenceRepository visitOccurrenceRepository;
    private final BranchPolicyRepository branchPolicyRepository;
    private final AgencyAuthorizationGuard agencyAuthorizationGuard;
    private final SchedulingAuditService schedulingAuditService;

    public List<CaregiverMatchResult> matchCaregivers(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            @Valid MatchCaregiversCommand command) {
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.ASSIGN_CAREGIVERS, UnauthorizedSchedulingActorException::new);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        return caregiverProfileRepository.findAllByAgency_IdOrderByCreatedAtAsc(actorMembership.getAgencyId()).stream()
                .map(caregiverProfile -> toMatchResult(visit, caregiverProfile, command, false))
                .sorted(Comparator.comparingInt(CaregiverMatchResult::score).reversed()
                        .thenComparing(CaregiverMatchResult::caregiverDisplayName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    public SchedulingConflictEvaluation previewAssignment(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            @NotNull UUID caregiverProfileId,
            @Valid ConflictCheckCommand command) {
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.VIEW_SCHEDULE_CONFLICTS, UnauthorizedSchedulingActorException::new);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        CaregiverProfile caregiverProfile = resolveCaregiverProfile(actorMembership.getAgencyId(), caregiverProfileId);
        SchedulingConflictEvaluation evaluation = evaluateAssignment(visit, caregiverProfile, command);
        if (evaluation.outcome() != SchedulingConflictOutcome.CLEAR) {
            schedulingAuditService.recordConflictFlagged(actorMembership, visit.getId(), visit.getBranchId(), conflictMetadata(evaluation));
        }
        return evaluation;
    }

    public SchedulingTravelAwareness evaluateTravelAwareness(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID visitOccurrenceId,
            @NotNull UUID caregiverProfileId) {
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.VIEW_SCHEDULE_CONFLICTS, UnauthorizedSchedulingActorException::new);
        VisitOccurrence visit = resolveVisit(actorMembership.getAgencyId(), visitOccurrenceId);
        CaregiverProfile caregiverProfile = resolveCaregiverProfile(actorMembership.getAgencyId(), caregiverProfileId);
        SchedulingTravelAwareness travelAwareness = evaluateTravel(visit, caregiverProfile);
        schedulingAuditService.recordTravelEvaluated(actorMembership, visit.getId(), visit.getBranchId(), travelMetadata(travelAwareness));
        return travelAwareness;
    }

    public OvertimeEvaluation evaluateOvertime(
            @NotNull AgencyMembership actorMembership,
            @NotNull UUID caregiverProfileId,
            @NotNull OffsetDateTime proposedStartAt,
            @NotNull OffsetDateTime proposedEndAt,
            UUID excludeVisitOccurrenceId) {
        agencyAuthorizationGuard.requirePermission(actorMembership, AgencyPermission.VIEW_SCHEDULE_CONFLICTS, UnauthorizedSchedulingActorException::new);
        CaregiverProfile caregiverProfile = resolveCaregiverProfile(actorMembership.getAgencyId(), caregiverProfileId);
        return evaluateOvertimeInternal(caregiverProfile, proposedStartAt, proposedEndAt, excludeVisitOccurrenceId, null);
    }

    public void assertAssignmentAllowed(VisitOccurrence visit, CaregiverProfile caregiverProfile) {
        SchedulingConflictEvaluation evaluation = evaluateAssignment(visit, caregiverProfile, ConflictCheckCommand.defaultForMutations());
        if (evaluation.outcome() == SchedulingConflictOutcome.BLOCKING) {
            String message = evaluation.items().stream()
                    .filter(item -> item.outcome() == SchedulingConflictOutcome.BLOCKING)
                    .map(ConflictItem::message)
                    .findFirst()
                    .orElse("Scheduling conflict blocks this action.");
            throw new SchedulingConflictException(message);
        }
    }

    private CaregiverMatchResult toMatchResult(
            VisitOccurrence visit,
            CaregiverProfile caregiverProfile,
            MatchCaregiversCommand command,
            boolean audit) {
        ConflictCheckCommand conflictCheckCommand = new ConflictCheckCommand(
                command.requiredSkillIds(),
                command.requiredCredentialTypes(),
                command.preferredLanguage() == null ? visit.getPatient().getLanguage() : command.preferredLanguage(),
                command.enforcePatientOverlapCheck(),
                command.requireAvailabilityFit(),
                command.dailyOvertimeThresholdMinutes(),
                command.blockingOvertimeThresholdMinutes());
        SchedulingConflictEvaluation evaluation = evaluateAssignment(visit, caregiverProfile, conflictCheckCommand);
        int score = 100;
        for (ConflictItem item : evaluation.items()) {
            if (item.outcome() == SchedulingConflictOutcome.BLOCKING) {
                score -= 100;
            } else if (item.outcome() == SchedulingConflictOutcome.WARNING) {
                score -= 15;
            }
        }
        if (evaluation.travelAwareness().level() == TravelAwarenessLevel.FEASIBLE) {
            score += 10;
        }
        if (evaluation.overtimeEvaluation().outcome() == SchedulingConflictOutcome.CLEAR) {
            score += 5;
        }
        if (audit && evaluation.outcome() != SchedulingConflictOutcome.CLEAR) {
            // Reserved for future use if match-list audit becomes required.
        }
        return new CaregiverMatchResult(
                caregiverProfile.getId(),
                caregiverProfile.getDisplayName(),
                Math.max(score, 0),
                evaluation.outcome(),
                evaluation.items(),
                evaluation.travelAwareness(),
                evaluation.overtimeEvaluation());
    }

    private SchedulingConflictEvaluation evaluateAssignment(
            VisitOccurrence visit,
            CaregiverProfile caregiverProfile,
            ConflictCheckCommand command) {
        List<ConflictItem> items = new ArrayList<>();

        if (caregiverProfile.getStatus() != WorkforceLifecycleStatus.ACTIVE) {
            items.add(blocking("INACTIVE_CAREGIVER", "Only active caregivers can receive active assignments."));
        }
        if (visit.getBranchId() != null && caregiverProfile.getPrimaryBranchId() != null
                && !Objects.equals(visit.getBranchId(), caregiverProfile.getPrimaryBranchId())) {
            items.add(blocking("BRANCH_MISMATCH", "Caregiver primary branch does not match the visit branch."));
        }
        if (hasCaregiverOverlap(caregiverProfile.getId(), visit.getPlannedStartAt(), visit.getPlannedEndAt(), visit.getId())) {
            items.add(blocking("CAREGIVER_OVERLAP", "Caregiver already has an overlapping active visit assignment."));
        }
        if (hasUnavailabilityConflict(caregiverProfile.getId(), visit.getPlannedStartAt(), visit.getPlannedEndAt())) {
            items.add(blocking("CAREGIVER_UNAVAILABLE", "Caregiver has an overlapping unavailability window."));
        }
        if (command.enforcePatientOverlapCheck() && hasPatientOverlap(visit)) {
            items.add(blocking("PATIENT_OVERLAP", "Patient already has an overlapping scheduled visit."));
        }

        AvailabilityCheck availabilityCheck = evaluateAvailability(caregiverProfile.getId(), visit.getPlannedStartAt(), visit.getPlannedEndAt());
        if (command.requireAvailabilityFit()) {
            if (availabilityCheck.hasProfiles() && !availabilityCheck.fits()) {
                items.add(blocking("NO_AVAILABILITY_FIT", "Caregiver does not have active availability covering this visit window."));
            } else if (!availabilityCheck.hasProfiles()) {
                items.add(warning("NO_AVAILABILITY_PROFILE", "Caregiver has no active availability profile on file."));
            }
        }

        Set<UUID> missingSkillIds = missingRequiredSkills(caregiverProfile.getId(), command.requiredSkillIds());
        if (!missingSkillIds.isEmpty()) {
            items.add(blocking("MISSING_REQUIRED_SKILL", "Caregiver is missing one or more required skills."));
        } else if (activeSkillProfiles(caregiverProfile.getId()).isEmpty()) {
            items.add(warning("NO_ACTIVE_SKILL_PROFILE", "Caregiver has no active skill profile on file."));
        }

        Set<String> missingCredentialTypes = missingRequiredCredentialTypes(caregiverProfile.getId(), command.requiredCredentialTypes(), visit.getPlannedStartAt().toLocalDate());
        if (!missingCredentialTypes.isEmpty()) {
            items.add(blocking("MISSING_REQUIRED_CREDENTIAL", "Caregiver is missing one or more required active credentials."));
        } else if (activeCredentials(caregiverProfile.getId(), visit.getPlannedStartAt().toLocalDate()).isEmpty()) {
            items.add(warning("NO_ACTIVE_CREDENTIAL", "Caregiver has no active credential covering the visit date."));
        }

        String preferredLanguage = normalizeOptional(command.preferredLanguage());
        if (preferredLanguage != null && !hasLanguageAlignment(caregiverProfile.getId(), preferredLanguage)) {
            items.add(warning("LANGUAGE_MISMATCH", "Caregiver does not have a matching active language profile."));
        }

        SchedulingTravelAwareness travelAwareness = evaluateTravel(visit, caregiverProfile);
        if (travelAwareness.level() == TravelAwarenessLevel.INFEASIBLE) {
            items.add(blocking("TRAVEL_INFEASIBLE", "Travel fit is infeasible for this caregiver and visit."));
        } else if (travelAwareness.level() == TravelAwarenessLevel.TIGHT_CONNECTION) {
            items.add(warning("TRAVEL_TIGHT", "Travel fit is tight for this caregiver and visit."));
        }

        OvertimeEvaluation overtimeEvaluation = evaluateOvertimeInternal(
                caregiverProfile,
                visit.getPlannedStartAt(),
                visit.getPlannedEndAt(),
                visit.getId(),
                visit.getBranchId());
        if (overtimeEvaluation.outcome() == SchedulingConflictOutcome.BLOCKING) {
            items.add(blocking("OVERTIME_BLOCK", overtimeEvaluation.message()));
        } else if (overtimeEvaluation.outcome() == SchedulingConflictOutcome.WARNING) {
            items.add(warning("OVERTIME_WARNING", overtimeEvaluation.message()));
        }

        SchedulingConflictOutcome outcome = items.stream().anyMatch(item -> item.outcome() == SchedulingConflictOutcome.BLOCKING)
                ? SchedulingConflictOutcome.BLOCKING
                : items.stream().anyMatch(item -> item.outcome() == SchedulingConflictOutcome.WARNING)
                        ? SchedulingConflictOutcome.WARNING
                        : SchedulingConflictOutcome.CLEAR;
        return new SchedulingConflictEvaluation(visit.getId(), caregiverProfile.getId(), outcome, List.copyOf(items), travelAwareness, overtimeEvaluation);
    }

    private AvailabilityCheck evaluateAvailability(UUID caregiverProfileId, OffsetDateTime plannedStartAt, OffsetDateTime plannedEndAt) {
        List<CaregiverAvailability> availabilities = caregiverAvailabilityRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverProfileId).stream()
                .filter(availability -> availability.getStatus() == WorkforceLifecycleStatus.ACTIVE)
                .toList();
        if (availabilities.isEmpty()) {
            return new AvailabilityCheck(false, false);
        }
        boolean fits = availabilities.stream().anyMatch(availability -> availabilityCovers(availability, plannedStartAt, plannedEndAt));
        return new AvailabilityCheck(true, fits);
    }

    private boolean availabilityCovers(CaregiverAvailability availability, OffsetDateTime plannedStartAt, OffsetDateTime plannedEndAt) {
        if (availability.getAvailabilityType() == CaregiverAvailabilityType.DATE_SPECIFIC) {
            return !plannedStartAt.isBefore(availability.getStartsAt()) && !plannedEndAt.isAfter(availability.getEndsAt());
        }
        LocalDate proposedDate = plannedStartAt.toLocalDate();
        return availability.getDayOfWeek() == plannedStartAt.getDayOfWeek()
                && !plannedStartAt.toLocalTime().isBefore(availability.getStartTime())
                && !plannedEndAt.toLocalTime().isAfter(availability.getEndTime())
                && (availability.getEffectiveFrom() == null || !proposedDate.isBefore(availability.getEffectiveFrom()))
                && (availability.getEffectiveTo() == null || !proposedDate.isAfter(availability.getEffectiveTo()));
    }

    private boolean hasUnavailabilityConflict(UUID caregiverProfileId, OffsetDateTime plannedStartAt, OffsetDateTime plannedEndAt) {
        return caregiverUnavailabilityRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverProfileId).stream()
                .filter(unavailability -> unavailability.getStatus() == WorkforceLifecycleStatus.ACTIVE)
                .anyMatch(unavailability -> plannedStartAt.isBefore(unavailability.getEndsAt()) && unavailability.getStartsAt().isBefore(plannedEndAt));
    }

    private boolean hasCaregiverOverlap(UUID caregiverProfileId, OffsetDateTime plannedStartAt, OffsetDateTime plannedEndAt, UUID excludeVisitOccurrenceId) {
        return caregiverVisitAssignmentRepository.existsActiveOverlap(
                caregiverProfileId,
                plannedStartAt,
                plannedEndAt,
                excludeVisitOccurrenceId,
                CaregiverAssignmentStatus.ACTIVE);
    }

    private boolean hasPatientOverlap(VisitOccurrence visit) {
        return visitOccurrenceRepository.findAllByAgency_IdOrderByPlannedStartAtAsc(visit.getAgencyId()).stream()
                .filter(candidate -> !candidate.getId().equals(visit.getId()))
                .filter(candidate -> candidate.getStatus() != com.homehealthcare.scheduling.foundation.SchedulingVisitStatus.CANCELLED)
                .filter(candidate -> candidate.getPatient().getId().equals(visit.getPatient().getId()))
                .anyMatch(candidate -> candidate.getPlannedStartAt().isBefore(visit.getPlannedEndAt())
                        && visit.getPlannedStartAt().isBefore(candidate.getPlannedEndAt()));
    }

    private Set<UUID> missingRequiredSkills(UUID caregiverProfileId, Set<UUID> requiredSkillIds) {
        Set<UUID> normalized = requiredSkillIds == null ? Set.of() : requiredSkillIds;
        if (normalized.isEmpty()) {
            return Set.of();
        }
        Set<UUID> caregiverSkillIds = new HashSet<>();
        for (CaregiverSkillProfile profile : activeSkillProfiles(caregiverProfileId)) {
            caregiverSkillIds.add(profile.getSkillId());
        }
        Set<UUID> missing = new HashSet<>(normalized);
        missing.removeAll(caregiverSkillIds);
        return missing;
    }

    private List<CaregiverSkillProfile> activeSkillProfiles(UUID caregiverProfileId) {
        return caregiverSkillProfileRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverProfileId).stream()
                .filter(profile -> profile.getStatus() == WorkforceLifecycleStatus.ACTIVE)
                .toList();
    }

    private Set<String> missingRequiredCredentialTypes(UUID caregiverProfileId, Set<String> requiredCredentialTypes, LocalDate visitDate) {
        Set<String> normalized = requiredCredentialTypes == null
                ? Set.of()
                : requiredCredentialTypes.stream()
                        .filter(Objects::nonNull)
                        .map(value -> value.trim().toUpperCase(Locale.ROOT))
                        .filter(value -> !value.isBlank())
                        .collect(java.util.stream.Collectors.toSet());
        if (normalized.isEmpty()) {
            return Set.of();
        }
        Set<String> caregiverCredentialTypes = activeCredentials(caregiverProfileId, visitDate).stream()
                .map(CaregiverCredential::getCredentialType)
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> missing = new HashSet<>(normalized);
        missing.removeAll(caregiverCredentialTypes);
        return missing;
    }

    private List<CaregiverCredential> activeCredentials(UUID caregiverProfileId, LocalDate visitDate) {
        return caregiverCredentialRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverProfileId).stream()
                .filter(credential -> credential.getStatus() == CaregiverCredentialStatus.ACTIVE)
                .filter(credential -> credential.getExpiresOn() == null || !credential.getExpiresOn().isBefore(visitDate))
                .toList();
    }

    private boolean hasLanguageAlignment(UUID caregiverProfileId, String preferredLanguage) {
        return caregiverLanguageProfileRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverProfileId).stream()
                .filter(language -> language.getStatus() == WorkforceLifecycleStatus.ACTIVE)
                .map(CaregiverLanguageProfile::getLanguageCode)
                .anyMatch(languageCode -> languageCode.equalsIgnoreCase(preferredLanguage));
    }

    private SchedulingTravelAwareness evaluateTravel(VisitOccurrence visit, CaregiverProfile caregiverProfile) {
        PatientAddress patientAddress = patientAddressRepository.findByPatient_Id(visit.getPatient().getId()).orElse(null);
        List<CaregiverGeographyPreference> preferences = caregiverGeographyPreferenceRepository.findAllByCaregiverProfile_IdOrderByCreatedAtAsc(caregiverProfile.getId()).stream()
                .filter(preference -> preference.getStatus() == WorkforceLifecycleStatus.ACTIVE)
                .toList();
        int estimatedTravelMinutes = estimateTravelMinutes(visit, patientAddress, preferences);
        int gapMinutes = estimateGapMinutes(caregiverProfile.getId(), visit);
        if (estimatedTravelMinutes < 0) {
            return SchedulingTravelAwareness.unknown();
        }
        if (gapMinutes > 0 && estimatedTravelMinutes > gapMinutes) {
            return SchedulingTravelAwareness.infeasible(estimatedTravelMinutes, gapMinutes);
        }
        if (gapMinutes > 0 && estimatedTravelMinutes >= Math.max(15, Math.round(gapMinutes * 0.8f))) {
            return SchedulingTravelAwareness.tightConnection(estimatedTravelMinutes, gapMinutes);
        }
        return SchedulingTravelAwareness.feasible(estimatedTravelMinutes, gapMinutes);
    }

    private int estimateTravelMinutes(VisitOccurrence visit, PatientAddress patientAddress, List<CaregiverGeographyPreference> preferences) {
        if (visit.getBranchId() != null && preferences.stream().anyMatch(preference ->
                preference.getPreferenceType() == CaregiverGeographyPreferenceType.BRANCH
                        && Objects.equals(preference.getBranchId(), visit.getBranchId()))) {
            return 20;
        }
        if (patientAddress == null || preferences.isEmpty()) {
            return -1;
        }
        Integer bestMinutes = null;
        for (CaregiverGeographyPreference preference : preferences) {
            Integer minutes = travelMinutesForPreference(preference, patientAddress);
            if (minutes != null && (bestMinutes == null || minutes < bestMinutes)) {
                bestMinutes = minutes;
            }
        }
        return bestMinutes == null ? -1 : bestMinutes;
    }

    private Integer travelMinutesForPreference(CaregiverGeographyPreference preference, PatientAddress patientAddress) {
        return switch (preference.getPreferenceType()) {
            case BRANCH -> null;
            case POSTAL_CODE -> patientAddress.getPostalCode().equalsIgnoreCase(preference.getPostalCode()) ? 15 : 45;
            case CITY_STATE -> Objects.equals(normalizeOptional(patientAddress.getCity()), normalizeOptional(preference.getCity()))
                    && Objects.equals(normalizeOptional(patientAddress.getState()), normalizeOptional(preference.getState()))
                            ? 20
                            : 50;
            case RADIUS -> estimateRadiusTravelMinutes(preference, patientAddress);
        };
    }

    private Integer estimateRadiusTravelMinutes(CaregiverGeographyPreference preference, PatientAddress patientAddress) {
        if (preference.getAnchorLatitude() == null || preference.getAnchorLongitude() == null
                || preference.getRadiusMiles() == null || patientAddress.getLatitude() == null || patientAddress.getLongitude() == null) {
            return null;
        }
        double miles = haversineMiles(
                preference.getAnchorLatitude().doubleValue(),
                preference.getAnchorLongitude().doubleValue(),
                patientAddress.getLatitude().doubleValue(),
                patientAddress.getLongitude().doubleValue());
        double radiusMiles = preference.getRadiusMiles().setScale(2, RoundingMode.HALF_UP).doubleValue();
        if (miles <= radiusMiles) {
            return (int) Math.round(Math.max(10, miles * 2));
        }
        if (miles <= radiusMiles * 1.25d) {
            return (int) Math.round(Math.max(20, miles * 2.5d));
        }
        return 999;
    }

    private int estimateGapMinutes(UUID caregiverProfileId, VisitOccurrence visit) {
        List<CaregiverVisitAssignment> assignments = caregiverVisitAssignmentRepository.findAllByCaregiverProfile_IdOrderByAssignedAtAsc(caregiverProfileId).stream()
                .filter(assignment -> assignment.getAssignmentStatus() == CaregiverAssignmentStatus.ACTIVE)
                .filter(assignment -> !assignment.getVisitOccurrenceId().equals(visit.getId()))
                .toList();
        int bestGap = 240;
        for (CaregiverVisitAssignment assignment : assignments) {
            VisitOccurrence scheduled = assignment.getVisitOccurrence();
            if (!scheduled.getPlannedEndAt().isAfter(visit.getPlannedStartAt()) && scheduled.getPlannedEndAt().toLocalDate().equals(visit.getPlannedStartAt().toLocalDate())) {
                int gap = (int) ChronoUnit.MINUTES.between(scheduled.getPlannedEndAt(), visit.getPlannedStartAt());
                bestGap = Math.min(bestGap, Math.max(gap, 0));
            }
            if (!visit.getPlannedEndAt().isAfter(scheduled.getPlannedStartAt()) && scheduled.getPlannedStartAt().toLocalDate().equals(visit.getPlannedStartAt().toLocalDate())) {
                int gap = (int) ChronoUnit.MINUTES.between(visit.getPlannedEndAt(), scheduled.getPlannedStartAt());
                bestGap = Math.min(bestGap, Math.max(gap, 0));
            }
        }
        return bestGap;
    }

    private OvertimeEvaluation evaluateOvertimeInternal(
            CaregiverProfile caregiverProfile,
            OffsetDateTime proposedStartAt,
            OffsetDateTime proposedEndAt,
            UUID excludeVisitOccurrenceId,
            UUID branchId) {
        OvertimeThreshold threshold = resolveOvertimeThreshold(branchId);
        LocalDate day = proposedStartAt.toLocalDate();
        int proposedMinutes = (int) ChronoUnit.MINUTES.between(proposedStartAt, proposedEndAt);
        int scheduledMinutesForDay = caregiverVisitAssignmentRepository.findAllByCaregiverProfile_IdOrderByAssignedAtAsc(caregiverProfile.getId()).stream()
                .filter(assignment -> assignment.getAssignmentStatus() == CaregiverAssignmentStatus.ACTIVE)
                .map(CaregiverVisitAssignment::getVisitOccurrence)
                .filter(visit -> !Objects.equals(visit.getId(), excludeVisitOccurrenceId))
                .filter(visit -> visit.getStatus() != com.homehealthcare.scheduling.foundation.SchedulingVisitStatus.CANCELLED)
                .filter(visit -> visit.getPlannedStartAt().toLocalDate().equals(day))
                .mapToInt(visit -> (int) ChronoUnit.MINUTES.between(visit.getPlannedStartAt(), visit.getPlannedEndAt()))
                .sum();
        int projectedMinutes = scheduledMinutesForDay + proposedMinutes;
        if (projectedMinutes > threshold.blockingMinutesThreshold()) {
            return new OvertimeEvaluation(projectedMinutes, proposedMinutes, threshold.warningMinutesThreshold(), threshold.blockingMinutesThreshold(),
                    SchedulingConflictOutcome.BLOCKING,
                    "Projected scheduled minutes exceed the blocking overtime threshold.");
        }
        if (projectedMinutes > threshold.warningMinutesThreshold()) {
            return new OvertimeEvaluation(projectedMinutes, proposedMinutes, threshold.warningMinutesThreshold(), threshold.blockingMinutesThreshold(),
                    SchedulingConflictOutcome.WARNING,
                    "Projected scheduled minutes exceed the overtime warning threshold.");
        }
        return new OvertimeEvaluation(projectedMinutes, proposedMinutes, threshold.warningMinutesThreshold(), threshold.blockingMinutesThreshold(),
                SchedulingConflictOutcome.CLEAR,
                "Projected scheduled minutes are within the overtime threshold.");
    }

    private OvertimeThreshold resolveOvertimeThreshold(UUID branchId) {
        int warningThreshold = 480;
        int blockingThreshold = 600;
        if (branchId == null) {
            return new OvertimeThreshold(warningThreshold, blockingThreshold);
        }
        return branchPolicyRepository.findEffectivePolicy(branchId, OVERTIME_POLICY_KEY, OffsetDateTime.now())
                .flatMap(policy -> parseOvertimeThreshold(policy.effectiveSettingsPayloadJson()))
                .orElse(new OvertimeThreshold(warningThreshold, blockingThreshold));
    }

    private java.util.Optional<OvertimeThreshold> parseOvertimeThreshold(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return java.util.Optional.empty();
        }
        int warningMinutes = extractIntegerPolicy(payloadJson, "dailyMinutesThreshold", 480);
        int blockingMinutes = extractIntegerPolicy(payloadJson, "blockingMinutesThreshold", Math.max(warningMinutes, 600));
        return java.util.Optional.of(new OvertimeThreshold(warningMinutes, Math.max(blockingMinutes, warningMinutes)));
    }

    private static int extractIntegerPolicy(String payloadJson, String fieldName, int defaultValue) {
        Pattern pattern = Pattern.compile(String.format(INTEGER_POLICY_PATTERN_TEMPLATE, Pattern.quote(fieldName)));
        Matcher matcher = pattern.matcher(payloadJson);
        if (!matcher.find()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private VisitOccurrence resolveVisit(UUID agencyId, UUID visitOccurrenceId) {
        return visitOccurrenceRepository.findByIdAndAgency_Id(visitOccurrenceId, agencyId)
                .orElseThrow(() -> new SchedulingEntityNotFoundException("VisitOccurrence", visitOccurrenceId));
    }

    private CaregiverProfile resolveCaregiverProfile(UUID agencyId, UUID caregiverProfileId) {
        return caregiverProfileRepository.findByIdAndAgency_Id(caregiverProfileId, agencyId)
                .orElseThrow(() -> new SchedulingEntityNotFoundException("CaregiverProfile", caregiverProfileId));
    }

    private static ConflictItem blocking(String code, String message) {
        return new ConflictItem(code, SchedulingConflictOutcome.BLOCKING, message);
    }

    private static ConflictItem warning(String code, String message) {
        return new ConflictItem(code, SchedulingConflictOutcome.WARNING, message);
    }

    private static String conflictMetadata(SchedulingConflictEvaluation evaluation) {
        return "{\"visitOccurrenceId\":\"" + evaluation.visitOccurrenceId() + "\",\"outcome\":\"" + evaluation.outcome().name() + "\"}";
    }

    private static String travelMetadata(SchedulingTravelAwareness travelAwareness) {
        return "{\"level\":\"" + travelAwareness.level().name() + "\",\"estimatedTravelMinutes\":" + travelAwareness.estimatedTravelMinutes() + "}";
    }

    private static String normalizeOptional(String value) {
        String normalized = value == null ? null : value.trim();
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private static double haversineMiles(double latOne, double lonOne, double latTwo, double lonTwo) {
        double earthRadiusMiles = 3958.756d;
        double latDistance = Math.toRadians(latTwo - latOne);
        double lonDistance = Math.toRadians(lonTwo - lonOne);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(latOne)) * Math.cos(Math.toRadians(latTwo))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMiles * c;
    }

    private record AvailabilityCheck(boolean hasProfiles, boolean fits) {
    }

    private record OvertimeThreshold(int warningMinutesThreshold, int blockingMinutesThreshold) {
    }

    public record MatchCaregiversCommand(
            Set<UUID> requiredSkillIds,
            Set<String> requiredCredentialTypes,
            String preferredLanguage,
            boolean enforcePatientOverlapCheck,
            boolean requireAvailabilityFit,
            Integer dailyOvertimeThresholdMinutes,
            Integer blockingOvertimeThresholdMinutes) {
    }

    public record ConflictCheckCommand(
            Set<UUID> requiredSkillIds,
            Set<String> requiredCredentialTypes,
            String preferredLanguage,
            boolean enforcePatientOverlapCheck,
            boolean requireAvailabilityFit,
            Integer dailyOvertimeThresholdMinutes,
            Integer blockingOvertimeThresholdMinutes) {

        static ConflictCheckCommand defaultForMutations() {
            return new ConflictCheckCommand(Set.of(), Set.of(), null, false, true, null, null);
        }
    }

    public record ConflictItem(String code, SchedulingConflictOutcome outcome, String message) {
    }

    public record SchedulingConflictEvaluation(
            UUID visitOccurrenceId,
            UUID caregiverProfileId,
            SchedulingConflictOutcome outcome,
            List<ConflictItem> items,
            SchedulingTravelAwareness travelAwareness,
            OvertimeEvaluation overtimeEvaluation) {
    }

    public record OvertimeEvaluation(
            int projectedScheduledMinutes,
            int proposedMinutes,
            int warningThresholdMinutes,
            int blockingThresholdMinutes,
            SchedulingConflictOutcome outcome,
            String message) {
    }

    public record CaregiverMatchResult(
            UUID caregiverProfileId,
            String caregiverDisplayName,
            int score,
            SchedulingConflictOutcome outcome,
            List<ConflictItem> factors,
            SchedulingTravelAwareness travelAwareness,
            OvertimeEvaluation overtimeEvaluation) {
    }
}
