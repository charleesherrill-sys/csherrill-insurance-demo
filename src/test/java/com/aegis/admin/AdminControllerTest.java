package com.aegis.admin;

import com.aegis.admin.service.AdminService;
import com.aegis.admin.web.AdminController;
import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.batch.ReconciliationService;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdminControllerTest {

    private final AdminService adminService = mock(AdminService.class);
    private final ReconciliationService reconciliationService = mock(ReconciliationService.class);
    private final AdminController controller = new AdminController(adminService, reconciliationService);

    @Test
    public void memberIsForbidden() {
        MockHttpServletRequest request = request(new UserSession("s", 5583, "amorgan", "MEMBER"));
        assertForbidden(() -> controller.listUsers(request));
    }

    @Test
    public void missingSessionIsForbidden() {
        assertForbidden(() -> controller.listUsers(new MockHttpServletRequest()));
    }

    @Test
    public void adminCanListUsersAndRunReconciliation() {
        List<Map<String, Object>> users = Collections.singletonList(
                Collections.<String, Object>singletonMap("username", "admin"));
        ReconciliationService.ReconciliationResult result =
                new ReconciliationService.ReconciliationResult("OK", 1, 0);
        when(adminService.listAllUsers()).thenReturn(users);
        when(reconciliationService.run()).thenReturn(result);
        MockHttpServletRequest request = request(new UserSession("s", 1, "admin", "ADMIN"));

        assertEquals(users, controller.listUsers(request));
        assertEquals(result, controller.runReconciliation(request));
    }

    private MockHttpServletRequest request(UserSession user) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CurrentUser.ATTRIBUTE, user);
        return request;
    }

    private void assertForbidden(ThrowingCall call) {
        try {
            call.run();
            fail("expected forbidden response");
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }
    }

    private interface ThrowingCall {
        void run();
    }
}
