package com.aegis.claims.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.ClaimService;
import com.aegis.common.audit.AuditService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for the IDOR fix (CWE-639) in {@link ClaimDetailController}.
 *
 * <p>A member must not be able to read a claim owned by a different member, while
 * the owning member and ADJUSTER/ADMIN roles still can.
 */
public class ClaimDetailControllerTest {

    private static final long OWNER_ID = 5583L;
    private static final long OTHER_MEMBER_ID = 4471L;
    private static final long CLAIM_ID = 90233L;

    private ClaimService claimService;
    private AuditService auditService;
    private ClaimDetailController controller;
    private Model model;

    @Before
    public void setUp() {
        claimService = mock(ClaimService.class);
        auditService = mock(AuditService.class);
        controller = new ClaimDetailController(claimService, auditService);
        model = mock(Model.class);

        Claim claim = new Claim();
        claim.setId(CLAIM_ID);
        claim.setMemberUserId(OWNER_ID);
        when(claimService.getClaim(CLAIM_ID)).thenReturn(claim);
    }

    private HttpServletRequest requestFor(long userId, String role) {
        UserSession session = new UserSession("sess-" + userId, userId, "user" + userId, role);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(CurrentUser.ATTRIBUTE)).thenReturn(session);
        return request;
    }

    @Test
    public void memberCannotReadAnotherMembersClaim() {
        HttpServletRequest request = requestFor(OTHER_MEMBER_ID, "MEMBER");

        String view = controller.getClaim(CLAIM_ID, request, model);

        assertEquals("claims/not-found", view);
        verify(model, never()).addAttribute(eq("claim"), any());
        // Cross-account access is still audited even though it is rejected.
        verify(auditService).record(eq(OTHER_MEMBER_ID), eq("CLAIM_VIEW"), eq("claim"),
                eq(String.valueOf(CLAIM_ID)), anyString());
    }

    @Test
    public void owningMemberCanReadOwnClaim() {
        HttpServletRequest request = requestFor(OWNER_ID, "MEMBER");

        String view = controller.getClaim(CLAIM_ID, request, model);

        assertEquals("claims/detail", view);
        verify(model).addAttribute(eq("claim"), any(Claim.class));
    }

    @Test
    public void adjusterCanReadAnyClaim() {
        HttpServletRequest request = requestFor(OTHER_MEMBER_ID, "ADJUSTER");

        String view = controller.getClaim(CLAIM_ID, request, model);

        assertEquals("claims/detail", view);
        verify(model).addAttribute(eq("claim"), any(Claim.class));
    }

    @Test
    public void adminCanReadAnyClaim() {
        HttpServletRequest request = requestFor(OTHER_MEMBER_ID, "ADMIN");

        String view = controller.getClaim(CLAIM_ID, request, model);

        assertEquals("claims/detail", view);
        verify(model).addAttribute(eq("claim"), any(Claim.class));
    }
}
