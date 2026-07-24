package com.aegis.auth.web;

import com.aegis.auth.service.SessionManager;
import com.aegis.auth.service.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Establishes the current user for each request from the {@code AEGIS_SESSION}
 * cookie and redirects unauthenticated users to the login page.
 *
 * <p>This interceptor enforces <em>authentication</em> for every intercepted path
 * and, for the {@code /admin/**} area, coarse-grained <em>role</em> authorization
 * (ADMIN only). Per-record ownership authorization (e.g. claim detail, CWE-639)
 * remains the responsibility of individual controllers/services.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final SessionManager sessionManager;

    @Autowired
    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserSession session = resolveSession(request);
        if (session == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        // Role-based access for the admin area (CWE-306): authenticated but
        // non-admin users are refused with 403.
        if (isAdminPath(request) && !session.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return false;
        }
        request.setAttribute(CurrentUser.ATTRIBUTE, session);
        return true;
    }

    private boolean isAdminPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        return path.equals("/admin") || path.startsWith("/admin/");
    }

    private UserSession resolveSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SessionManager.COOKIE_NAME.equals(cookie.getName())) {
                return sessionManager.get(cookie.getValue());
            }
        }
        return null;
    }
}
