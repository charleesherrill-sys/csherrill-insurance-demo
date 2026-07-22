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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the IDOR remediation (CWE-639) on {@code GET /claims/{id}}.
 *
 * <p>A member may only read their own claims; adjusters/admins may read any claim.
 * Cross-account reads by ordinary members must be rejected without returning the
 * claim data, while the cross-account attempt is still audited.
 */
public class ClaimDetailControllerTest {

    private static final long VIEWER_ID = 100L;
    private static final long OTHER_MEMBER_ID = 200L;
    private static final long CLAIM_ID = 555L;

    private ClaimService claimService;
    private AuditService auditService;
    private ClaimDetailController controller;

    @Before
    public void setUp() {
        claimService = mock(ClaimService.class);
        auditService = mock(AuditService.class);
        controller = new ClaimDetailController(claimService, auditService);
    }

    private Claim claimOwnedBy(long memberUserId) {
        Claim claim = new Claim();
        claim.setId(CLAIM_ID);
        claim.setMemberUserId(memberUserId);
        return claim;
    }

    private HttpServletRequest requestFor(UserSession session) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(CurrentUser.ATTRIBUTE)).thenReturn(session);
        return request;
    }

    @Test
    public void memberViewingOwnClaimSucceeds() {
        UserSession member = new UserSession("s1", VIEWER_ID, "member", "MEMBER");
        Claim own = claimOwnedBy(VIEWER_ID);
        when(claimService.getClaim(CLAIM_ID)).thenReturn(own);
        Model model = new ExtendedModelMap();

        String view = controller.getClaim(CLAIM_ID, requestFor(member), model);

        assertEquals("claims/detail", view);
        assertSame(own, model.getAttribute("claim"));
    }

    @Test
    public void memberViewingOtherMembersClaimIsRejected() {
        UserSession member = new UserSession("s1", VIEWER_ID, "member", "MEMBER");
        when(claimService.getClaim(CLAIM_ID)).thenReturn(claimOwnedBy(OTHER_MEMBER_ID));
        Model model = new ExtendedModelMap();

        String view = controller.getClaim(CLAIM_ID, requestFor(member), model);

        assertEquals("claims/forbidden", view);
        assertFalse("claim data must not be returned on a rejected cross-account read",
                model.containsAttribute("claim"));
        // The cross-account attempt is still audited.
        verify(auditService).record(eq(VIEWER_ID), eq("CLAIM_VIEW"), eq("claim"),
                eq(String.valueOf(CLAIM_ID)), contains("cross-account"));
    }

    @Test
    public void adjusterViewingOtherMembersClaimSucceeds() {
        UserSession adjuster = new UserSession("s2", VIEWER_ID, "adj", "ADJUSTER");
        Claim other = claimOwnedBy(OTHER_MEMBER_ID);
        when(claimService.getClaim(CLAIM_ID)).thenReturn(other);
        Model model = new ExtendedModelMap();

        String view = controller.getClaim(CLAIM_ID, requestFor(adjuster), model);

        assertEquals("claims/detail", view);
        assertSame(other, model.getAttribute("claim"));
    }

    @Test
    public void adminViewingOtherMembersClaimSucceeds() {
        UserSession admin = new UserSession("s3", VIEWER_ID, "admin", "ADMIN");
        Claim other = claimOwnedBy(OTHER_MEMBER_ID);
        when(claimService.getClaim(CLAIM_ID)).thenReturn(other);
        Model model = new ExtendedModelMap();

        String view = controller.getClaim(CLAIM_ID, requestFor(admin), model);

        assertEquals("claims/detail", view);
        assertSame(other, model.getAttribute("claim"));
    }

    @Test
    public void missingClaimReturnsNotFound() {
        UserSession member = new UserSession("s1", VIEWER_ID, "member", "MEMBER");
        when(claimService.getClaim(CLAIM_ID)).thenReturn(null);
        Model model = new ExtendedModelMap();

        String view = controller.getClaim(CLAIM_ID, requestFor(member), model);

        assertEquals("claims/not-found", view);
        assertFalse(model.containsAttribute("claim"));
    }
}
