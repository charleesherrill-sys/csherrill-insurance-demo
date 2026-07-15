package com.aegis.auth.web;

import com.aegis.auth.service.UserSession;

import javax.servlet.http.HttpServletRequest;

/**
 * Convenience accessor for the authenticated {@link UserSession} that
 * {@link AuthInterceptor} places on the request under {@link #ATTRIBUTE}.
 */
public final class CurrentUser {

    public static final String ATTRIBUTE = "aegisCurrentUser";

    private CurrentUser() {
    }

    public static UserSession from(HttpServletRequest request) {
        Object attr = request.getAttribute(ATTRIBUTE);
        if (attr instanceof UserSession) {
            return (UserSession) attr;
        }
        return null;
    }
}
