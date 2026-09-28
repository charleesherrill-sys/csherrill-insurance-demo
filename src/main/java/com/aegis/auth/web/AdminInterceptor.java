package com.aegis.auth.web;

import com.aegis.auth.service.UserSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Enforces that only authenticated ADMIN-role users can access {@code /admin/**}.
 * Must be registered after {@link AuthInterceptor} so that the current user is
 * already resolved on the request.
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserSession session = CurrentUser.from(request);
        if (session == null || !session.isAdmin()) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        return true;
    }
}
