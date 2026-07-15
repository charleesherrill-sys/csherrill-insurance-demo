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
 * <p>IMPORTANT: this interceptor only proves <em>authentication</em> (you are
 * logged in). It does NOT enforce per-record <em>authorization</em>. Ownership
 * checks are the responsibility of individual controllers/services — and the
 * claim-detail endpoint is missing that check (CWE-639). See REVIEW.md.
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
        request.setAttribute(CurrentUser.ATTRIBUTE, session);
        return true;
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
