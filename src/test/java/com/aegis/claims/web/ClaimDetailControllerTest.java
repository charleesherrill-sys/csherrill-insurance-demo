package com.aegis.claims.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.ClaimService;
import com.aegis.common.audit.AuditService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the CWE-639 (IDOR) fix on {@code GET /claims/{id}}: a
 * member may only view their own claims, while ADJUSTER/ADMIN may view any.
 * Runs without a database; collaborators are mocked.
 */
public class ClaimDetailControllerTest {

    // Seeded scenario from README.md.
    private static final long OWNER_USER_ID = 5583;   // amorgan owns claim 90233
    private static final long OTHER_USER_ID = 4471;   // bhopkins
    private static final long CLAIM_ID = 90233;

    private ClaimService claimService;
    private AuditService auditService;
    private ClaimDetailController controller;

    @Before
    public void setUp() {
        claimService = mock(ClaimService.class);
        auditService = mock(AuditService.class);
        controller = new ClaimDetailController(claimService, auditService);
    }

    private Claim ownedClaim() {
        Claim claim = new Claim();
        claim.setId(CLAIM_ID);
        claim.setMemberUserId(OWNER_USER_ID);
        when(claimService.getClaim(CLAIM_ID)).thenReturn(claim);
        return claim;
    }

    private HttpServletRequest requestFor(UserSession user) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(CurrentUser.ATTRIBUTE)).thenReturn(user);
        return request;
    }

    @Test
    public void ownerCanViewOwnClaim() {
        Claim claim = ownedClaim();
        UserSession user = new UserSession("s1", OWNER_USER_ID, "amorgan", "MEMBER");
        HttpServletResponse response = mock(HttpServletResponse.class);
        Model model = new ExtendedModelMap();

        String view = controller.getClaim(CLAIM_ID, requestFor(user), response, model);

        assertEquals("claims/detail", view);
        assertSame(claim, model.getAttribute("claim"));
        verify(auditService, times(1))
                .record(eq(OWNER_USER_ID), eq("CLAIM_VIEW"), eq("claim"), eq(String.valueOf(CLAIM_ID)), anyString());
    }

    @Test
    public void otherMemberIsDenied() {
        ownedClaim();
        UserSession user = new UserSession("s2", OTHER_USER_ID, "bhopkins", "MEMBER");
        HttpServletResponse response = mock(HttpServletResponse.class);
        Model model = new ExtendedModelMap();

        String view = controller.getClaim(CLAIM_ID, requestFor(user), response, model);

        assertEquals("error/forbidden", view);
        assertFalse(model.containsAttribute("claim"));
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(auditService, times(1))
                .record(eq(OTHER_USER_ID), eq("CLAIM_VIEW"), eq("claim"), eq(String.valueOf(CLAIM_ID)), anyString());
    }

    @Test
    public void adjusterCanViewAnotherMembersClaim() {
        Claim claim = ownedClaim();
        UserSession adjuster = new UserSession("s3", 2, "jadjuster", "ADJUSTER");
        HttpServletResponse response = mock(HttpServletResponse.class);
        Model model = new ExtendedModelMap();

        String view = controller.getClaim(CLAIM_ID, requestFor(adjuster), response, model);

        assertEquals("claims/detail", view);
        assertSame(claim, model.getAttribute("claim"));
        verify(auditService, times(1))
                .record(eq(2L), eq("CLAIM_VIEW"), eq("claim"), eq(String.valueOf(CLAIM_ID)), anyString());
    }

    @Test
    public void adminCanViewAnotherMembersClaim() {
        Claim claim = ownedClaim();
        UserSession admin = new UserSession("s4", 1, "admin", "ADMIN");
        HttpServletResponse response = mock(HttpServletResponse.class);
        Model model = new ExtendedModelMap();

        String view = controller.getClaim(CLAIM_ID, requestFor(admin), response, model);

        assertEquals("claims/detail", view);
        assertTrue(model.containsAttribute("claim"));
        assertSame(claim, model.getAttribute("claim"));
        verify(auditService, times(1))
                .record(eq(1L), eq("CLAIM_VIEW"), eq("claim"), eq(String.valueOf(CLAIM_ID)), anyString());
    }
}
