package com.aegis.claims.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.AdjudicationService;
import com.aegis.claims.service.ClaimIntakeService;
import com.aegis.claims.service.ClaimService;
import com.aegis.policy.service.PolicyService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the authorization gap on {@code POST /claims/{id}/adjudicate}:
 * adjudication (and the payment it triggers) is an ADJUSTER/ADMIN function, so a
 * MEMBER must never be able to run it on any claim (own or another member's).
 */
public class ClaimIntakeControllerTest {

    private ClaimIntakeService intakeService;
    private AdjudicationService adjudicationService;
    private PolicyService policyService;
    private ClaimService claimService;
    private ClaimIntakeController controller;

    private static final long CLAIM_ID = 5001L;
    private static final long ATTACKER_USER_ID = 42L;
    private static final long OWNER_USER_ID = 99L;

    @Before
    public void setUp() {
        intakeService = mock(ClaimIntakeService.class);
        adjudicationService = mock(AdjudicationService.class);
        policyService = mock(PolicyService.class);
        claimService = mock(ClaimService.class);
        controller = new ClaimIntakeController(
                intakeService, adjudicationService, policyService, claimService);
    }

    private HttpServletRequest requestFor(UserSession user) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(CurrentUser.ATTRIBUTE)).thenReturn(user);
        return request;
    }

    private Claim claimOwnedBy(long ownerUserId, String status) {
        Claim claim = new Claim();
        claim.setId(CLAIM_ID);
        claim.setMemberUserId(ownerUserId);
        claim.setStatus(status);
        return claim;
    }

    @Test
    public void memberCannotAdjudicateOwnClaim() {
        UserSession member = new UserSession("sess", ATTACKER_USER_ID, "member", "MEMBER");

        try {
            controller.adjudicate(CLAIM_ID, requestFor(member));
            fail("expected ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }

        // No state change and no payment must be triggered for a member.
        verifyNoInteractions(intakeService, adjudicationService, claimService);
    }

    @Test
    public void memberCannotAdjudicateAnotherMembersClaim() {
        UserSession member = new UserSession("sess", ATTACKER_USER_ID, "member", "MEMBER");

        try {
            controller.adjudicate(CLAIM_ID, requestFor(member));
            fail("expected ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }

        verifyNoInteractions(intakeService, adjudicationService, claimService);
    }

    @Test
    public void unauthenticatedCallerCannotAdjudicate() {
        try {
            controller.adjudicate(CLAIM_ID, requestFor(null));
            fail("expected ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }

        verifyNoInteractions(intakeService, adjudicationService, claimService);
    }

    @Test
    public void adjusterCanAdjudicateValidatedClaim() {
        UserSession adjuster = new UserSession("sess", 7L, "adj", "ADJUSTER");
        when(claimService.getClaim(CLAIM_ID))
                .thenReturn(claimOwnedBy(OWNER_USER_ID, "SUBMITTED"));

        String view = controller.adjudicate(CLAIM_ID, requestFor(adjuster));

        assertEquals("redirect:/claims/" + CLAIM_ID, view);
        verify(intakeService, times(1)).validate(CLAIM_ID);
        verify(adjudicationService, times(1)).adjudicate(CLAIM_ID);
    }

    @Test
    public void alreadyPaidClaimIsNotReadjudicated() {
        UserSession admin = new UserSession("sess", 1L, "admin", "ADMIN");
        when(claimService.getClaim(CLAIM_ID))
                .thenReturn(claimOwnedBy(OWNER_USER_ID, "PAID"));

        try {
            controller.adjudicate(CLAIM_ID, requestFor(admin));
            fail("expected ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.CONFLICT, e.getStatus());
        }

        // Idempotency: a decided/paid claim must not be re-run or re-paid.
        verify(intakeService, never()).validate(CLAIM_ID);
        verify(adjudicationService, never()).adjudicate(CLAIM_ID);
    }
}
