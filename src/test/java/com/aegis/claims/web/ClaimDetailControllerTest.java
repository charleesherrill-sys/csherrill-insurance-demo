package com.aegis.claims.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.ClaimService;
import com.aegis.common.audit.AuditService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Authorization (IDOR / CWE-639) behaviour of the claim-detail endpoint. */
public class ClaimDetailControllerTest {

    private ClaimService claimService;
    private ClaimDetailController controller;

    private static final long OWNER_ID = 5583L;
    private static final long CLAIM_ID = 90233L;

    @Before
    public void setUp() {
        claimService = mock(ClaimService.class);
        AuditService auditService = mock(AuditService.class);
        controller = new ClaimDetailController(claimService, auditService);

        Claim claim = new Claim();
        claim.setId(CLAIM_ID);
        claim.setMemberUserId(OWNER_ID);
        when(claimService.getClaim(CLAIM_ID)).thenReturn(claim);
    }

    private String invoke(UserSession user, MockHttpServletResponse response) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUser.ATTRIBUTE, user);
        Model model = new ExtendedModelMap();
        return controller.getClaim(CLAIM_ID, request, response, model);
    }

    @Test
    public void ownerCanViewOwnClaim() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = invoke(new UserSession("s", OWNER_ID, "amorgan", "MEMBER"), response);
        assertEquals("claims/detail", view);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void otherMemberIsForbidden() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = invoke(new UserSession("s", 4471L, "bhopkins", "MEMBER"), response);
        assertEquals("claims/not-authorized", view);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void adjusterCanViewAnyClaim() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = invoke(new UserSession("s", 2L, "jadjuster", "ADJUSTER"), response);
        assertEquals("claims/detail", view);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void adminCanViewAnyClaim() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = invoke(new UserSession("s", 1L, "admin", "ADMIN"), response);
        assertEquals("claims/detail", view);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    public void missingClaimReturnsNotFound() {
        when(claimService.getClaim(CLAIM_ID)).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        String view = invoke(new UserSession("s", OWNER_ID, "amorgan", "MEMBER"), response);
        assertEquals("claims/not-found", view);
    }
}
