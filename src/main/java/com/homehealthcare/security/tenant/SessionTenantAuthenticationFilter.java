package com.homehealthcare.security.tenant;

import com.homehealthcare.auth.application.CurrentAuthSessionNotFoundException;
import com.homehealthcare.auth.application.CurrentAuthSessionResolver;
import com.homehealthcare.auth.domain.AuthSession;
import com.homehealthcare.branchassignment.domain.BranchAssignment;
import com.homehealthcare.branchassignment.domain.BranchAssignmentRepository;
import com.homehealthcare.branchassignment.domain.BranchAssignmentStatus;
import com.homehealthcare.membership.domain.AgencyMembership;
import com.homehealthcare.membership.domain.AgencyMembershipRepository;
import com.homehealthcare.membership.domain.AgencyMembershipStatus;
import com.homehealthcare.security.branch.AgencyRole;
import com.homehealthcare.security.branch.BranchAccessPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionTenantAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_COOKIE = "hhc_access_token";
    private static final String SESSION_ID_COOKIE = "hhc_session_id";

    private final CurrentAuthSessionResolver currentAuthSessionResolver;
    private final AgencyMembershipRepository agencyMembershipRepository;
    private final BranchAssignmentRepository branchAssignmentRepository;

    public SessionTenantAuthenticationFilter(
            CurrentAuthSessionResolver currentAuthSessionResolver,
            AgencyMembershipRepository agencyMembershipRepository,
            BranchAssignmentRepository branchAssignmentRepository) {
        this.currentAuthSessionResolver = currentAuthSessionResolver;
        this.agencyMembershipRepository = agencyMembershipRepository;
        this.branchAssignmentRepository = branchAssignmentRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication existingAuthentication = SecurityContextHolder.getContext().getAuthentication();

        if (existingAuthentication == null
                || !existingAuthentication.isAuthenticated()
                || existingAuthentication instanceof AnonymousAuthenticationToken) {
            authenticateFromActiveSession(request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateFromActiveSession(HttpServletRequest request) {
        String accessToken = bearerToken(
                request.getHeader(HttpHeaders.AUTHORIZATION),
                cookieValue(request, ACCESS_TOKEN_COOKIE));
        String sessionId = firstNonBlank(
                request.getHeader("X-Session-Id"),
                cookieValue(request, SESSION_ID_COOKIE));

        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            AuthSession authSession = currentAuthSessionResolver.requireActive(accessToken, sessionId);
            Set<TenantMembership> memberships = agencyMembershipRepository
                    .findAllByUser_IdAndStatus(authSession.getUser().getId(), AgencyMembershipStatus.ACTIVE)
                    .stream()
                    .map(membership -> new TenantMembership(
                            membership.getId(),
                            membership.getAgencyId(),
                            membership.getStatus()))
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

            if (memberships.isEmpty()) {
                return;
            }

            AgencyMembership currentMembership = resolveCurrentMembership(memberships);
            if (currentMembership == null) {
                return;
            }

            Set<UUID> assignedBranchIds = branchAssignmentRepository
                    .findAllByAgencyMembership_IdAndStatusOrderByBranch_NameAsc(
                            currentMembership.getId(),
                            BranchAssignmentStatus.ACTIVE)
                    .stream()
                    .map(BranchAssignment::getBranchId)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

            SessionTenantPrincipal principal = new SessionTenantPrincipal(
                    currentMembership.getUserId(),
                    currentMembership.getAgencyId(),
                    memberships,
                    currentMembership.getRole(),
                    Set.copyOf(assignedBranchIds));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_USER"));
            authentication.setDetails(principal);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (CurrentAuthSessionNotFoundException ignored) {
            // Leave the request anonymous so the target endpoint can return its normal unauthenticated response.
        }
    }

    private AgencyMembership resolveCurrentMembership(Set<TenantMembership> memberships) {
        if (memberships.size() == 1) {
            UUID membershipId = memberships.iterator().next().membershipId();
            return agencyMembershipRepository.findById(membershipId)
                    .filter(AgencyMembership::isActive)
                    .orElse(null);
        }

        return null;
    }

    private static String bearerToken(String authorizationHeader, String fallbackCookie) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return fallbackCookie;
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private static String cookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    record SessionTenantPrincipal(
            UUID userId,
            UUID currentAgencyId,
            Set<TenantMembership> memberships,
            AgencyRole agencyRole,
            Set<UUID> assignedBranchIds) implements BranchAccessPrincipal {
    }
}
