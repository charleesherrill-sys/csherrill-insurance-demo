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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Authorization regression tests for {@code POST /claims/{id}/adjudicate} (CWE-862/639):
 * adjudication moves money, so only the claim owner or staff may trigger it, and only
 * while the claim is still awaiting adjudication.
 */
public class ClaimIntakeControllerAdjudicateAuthTest {

    private static final long CLAIM_ID = 90233L;
    private static final long OWNER_ID = 5583L;
    private static final long ATTACKER_ID = 4471L;

    private ClaimIntakeService intakeService;
    private AdjudicationService adjudicationService;
    private ClaimService claimService;
    private ClaimIntakeController controller;

    @Before
    public void setUp() {
        intakeService = mock(ClaimIntakeService.class);
        adjudicationService = mock(AdjudicationService.class);
        claimService = mock(ClaimService.class);
        controller = new ClaimIntakeController(intakeService, adjudicationService,
                mock(PolicyService.class), claimService);
    }

    private MockHttpServletRequest requestFor(long userId, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUser.ATTRIBUTE,
                new UserSession("session-" + userId, userId, "user" + userId, role));
        return request;
    }

    private void seedClaim(String status) {
        Claim claim = new Claim();
        claim.setId(CLAIM_ID);
        claim.setMemberUserId(OWNER_ID);
        claim.setStatus(status);
        when(claimService.getClaim(CLAIM_ID)).thenReturn(claim);
    }

    private ResponseStatusException expectRejection(MockHttpServletRequest request) {
        try {
            controller.adjudicate(CLAIM_ID, request);
            fail("expected the adjudicate request to be rejected");
            return null;
        } catch (ResponseStatusException e) {
            verifyNoInteractions(intakeService);
            verifyNoInteractions(adjudicationService);
            return e;
        }
    }

    @Test
    public void foreignMemberCannotAdjudicateAnotherMembersClaim() {
        seedClaim("SUBMITTED");
        ResponseStatusException e = expectRejection(requestFor(ATTACKER_ID, "MEMBER"));
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    public void unauthenticatedRequestIsRejected() {
        seedClaim("SUBMITTED");
        ResponseStatusException e = expectRejection(new MockHttpServletRequest());
        assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
    }

    @Test
    public void alreadyPaidClaimCannotBeReAdjudicated() {
        seedClaim("PAID");
        ResponseStatusException e = expectRejection(requestFor(OWNER_ID, "MEMBER"));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }

    @Test
    public void missingClaimIsNotFound() {
        when(claimService.getClaim(CLAIM_ID)).thenReturn(null);
        ResponseStatusException e = expectRejection(requestFor(OWNER_ID, "MEMBER"));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }

    @Test
    public void ownerCanAdjudicateOwnSubmittedClaim() {
        seedClaim("SUBMITTED");
        assertEquals("redirect:/claims/" + CLAIM_ID,
                controller.adjudicate(CLAIM_ID, requestFor(OWNER_ID, "MEMBER")));
        verify(intakeService).validate(CLAIM_ID);
        verify(adjudicationService).adjudicate(CLAIM_ID);
    }

    @Test
    public void adjusterCanAdjudicateAnotherMembersClaim() {
        seedClaim("VALIDATED");
        assertEquals("redirect:/claims/" + CLAIM_ID,
                controller.adjudicate(CLAIM_ID, requestFor(ATTACKER_ID, "ADJUSTER")));
        verify(intakeService).validate(CLAIM_ID);
        verify(adjudicationService).adjudicate(CLAIM_ID);
    }
}
