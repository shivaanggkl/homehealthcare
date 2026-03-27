package com.homehealthcare.auth.application;

import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.membership.domain.AgencyMembershipStatus;
import com.homehealthcare.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MfaPolicyService {

    private final AgencyMembershipRepository agencyMembershipRepository;

    @Transactional(readOnly = true)
    public boolean requiresMfa(User user) {
        List<AgencyMembership> activeMemberships = agencyMembershipRepository.findAllByUser_IdAndStatus(
                user.getId(),
                AgencyMembershipStatus.ACTIVE);
        return activeMemberships.stream()
                .anyMatch(membership -> membership.getAgency().requiresMfaForRole(membership.getRole()));
    }
}
