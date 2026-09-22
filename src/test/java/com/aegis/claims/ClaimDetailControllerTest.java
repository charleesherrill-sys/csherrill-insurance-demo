package com.aegis.claims;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.ClaimService;
import com.aegis.claims.web.ClaimDetailController;
import com.aegis.common.audit.AuditService;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClaimDetailControllerTest {

    private final ClaimService claimService = mock(ClaimService.class);
    private final AuditService auditService = mock(AuditService.class);
    private final ClaimDetailController controller = new ClaimDetailController(claimService, auditService);

    @Test
    public void memberCannotViewAnotherMembersClaim() {
        Claim claim = claim();
        when(claimService.getClaim(90311L)).thenReturn(claim);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.getClaim(90311L, request(new UserSession("s", 5583, "amorgan", "MEMBER")), model);

        assertEquals("claims/not-found", view);
        assertFalse(model.containsAttribute("claim"));
        verify(auditService).record(5583L, "CLAIM_VIEW", "claim", "90311",
                "cross-account read denied: viewer 5583 owner 4471");
    }

    @Test
    public void ownerCanViewClaim() {
        Claim claim = claim();
        when(claimService.getClaim(90311L)).thenReturn(claim);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.getClaim(90311L,
                request(new UserSession("s", 4471, "bhopkins", "MEMBER")), model);

        assertEquals("claims/detail", view);
        assertEquals(claim, model.get("claim"));
    }

    @Test
    public void adjusterCanViewAnyClaim() {
        Claim claim = claim();
        when(claimService.getClaim(90311L)).thenReturn(claim);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.getClaim(90311L,
                request(new UserSession("s", 2, "jadjuster", "ADJUSTER")), model);

        assertEquals("claims/detail", view);
        assertEquals(claim, model.get("claim"));
    }

    @Test
    public void adminCanViewAnyClaim() {
        Claim claim = claim();
        when(claimService.getClaim(90311L)).thenReturn(claim);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = controller.getClaim(90311L,
                request(new UserSession("s", 1, "admin", "ADMIN")), model);

        assertEquals("claims/detail", view);
        assertEquals(claim, model.get("claim"));
    }

    private Claim claim() {
        Claim claim = new Claim();
        claim.setId(90311L);
        claim.setMemberUserId(4471L);
        return claim;
    }

    private MockHttpServletRequest request(UserSession user) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUser.ATTRIBUTE, user);
        return request;
    }
}
