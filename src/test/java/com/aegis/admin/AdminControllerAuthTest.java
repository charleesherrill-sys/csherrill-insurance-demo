package com.aegis.admin;

import com.aegis.admin.service.AdminService;
import com.aegis.admin.web.AdminController;
import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.batch.ReconciliationService;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Regression tests for CWE-306 on the admin area: every {@link AdminController}
 * handler must reject non-admin callers with 403 and must not reach the backing
 * services. Authentication itself is enforced by {@code AuthInterceptor}
 * (registered for {@code /admin/**} in {@code WebConfig}); these tests cover the
 * per-handler role check that runs once a session is present.
 */
public class AdminControllerAuthTest {

    private final AdminService adminService = mock(AdminService.class);
    private final ReconciliationService reconciliationService = mock(ReconciliationService.class);
    private final AdminController controller = new AdminController(adminService, reconciliationService);

    private MockHttpServletRequest requestWithRole(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (role != null) {
            request.setAttribute(CurrentUser.ATTRIBUTE, new UserSession("sid", 1L, "user", role));
        }
        return request;
    }

    @Test
    public void listUsersForbiddenWhenNoSession() {
        assertForbidden(() -> controller.listUsers(requestWithRole(null)));
        verifyNoInteractions(adminService);
    }

    @Test
    public void listUsersForbiddenForNonAdmin() {
        assertForbidden(() -> controller.listUsers(requestWithRole("MEMBER")));
        verify(adminService, never()).listAllUsers();
    }

    @Test
    public void listUsersAllowedForAdmin() {
        when(adminService.listAllUsers()).thenReturn(Collections.emptyList());
        controller.listUsers(requestWithRole("ADMIN"));
        verify(adminService).listAllUsers();
    }

    @Test
    public void runReconciliationForbiddenForNonAdmin() {
        assertForbidden(() -> controller.runReconciliation(requestWithRole("ADJUSTER")));
        verifyNoInteractions(reconciliationService);
    }

    @Test
    public void portalForbiddenForNonAdmin() {
        Model model = new ExtendedModelMap();
        MockHttpServletRequest request = requestWithRole("MEMBER");
        try {
            controller.portal(request, model);
            fail("expected ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }
        verifyNoInteractions(adminService);
    }

    private void assertForbidden(Runnable call) {
        try {
            call.run();
            fail("expected ResponseStatusException");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }
    }
}
