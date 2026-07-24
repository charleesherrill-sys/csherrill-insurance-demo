package com.aegis.auth;

import com.aegis.auth.service.SessionManager;
import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.AuthInterceptor;
import com.aegis.auth.web.CurrentUser;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Authentication and admin-role authorization enforced by the interceptor. */
public class AuthInterceptorTest {

    private AuthInterceptor interceptorFor(UserSession session) {
        SessionManager sm = mock(SessionManager.class);
        when(sm.get("sid")).thenReturn(session);
        return new AuthInterceptor(sm);
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        req.setRequestURI(uri);
        req.setCookies(new javax.servlet.http.Cookie(SessionManager.COOKIE_NAME, "sid"));
        return req;
    }

    @Test
    public void unauthenticatedIsRedirectedToLogin() throws Exception {
        AuthInterceptor interceptor = interceptorFor(null);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/dashboard");
        req.setRequestURI("/dashboard");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(req, res, new Object());

        assertFalse(proceed);
        assertEquals("/login", res.getRedirectedUrl());
    }

    @Test
    public void adminPathRequiresAdminRole() throws Exception {
        AuthInterceptor interceptor = interceptorFor(new UserSession("sid", 5583L, "amorgan", "MEMBER"));
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(request("/admin/users"), res, new Object());

        assertFalse(proceed);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, res.getStatus());
    }

    @Test
    public void adminPathAllowsAdmin() throws Exception {
        AuthInterceptor interceptor = interceptorFor(new UserSession("sid", 1L, "admin", "ADMIN"));
        MockHttpServletRequest req = request("/admin/users");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(req, res, new Object());

        assertTrue(proceed);
        assertNotNull(CurrentUser.from(req));
    }

    @Test
    public void nonAdminPathAllowsAuthenticatedMember() throws Exception {
        AuthInterceptor interceptor = interceptorFor(new UserSession("sid", 5583L, "amorgan", "MEMBER"));
        MockHttpServletRequest req = request("/claims/90233");
        MockHttpServletResponse res = new MockHttpServletResponse();

        boolean proceed = interceptor.preHandle(req, res, new Object());

        assertTrue(proceed);
        assertNull(res.getRedirectedUrl());
    }
}
