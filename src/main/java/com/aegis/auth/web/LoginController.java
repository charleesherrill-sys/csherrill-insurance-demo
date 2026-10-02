package com.aegis.auth.web;

import com.aegis.auth.model.User;
import com.aegis.auth.service.AuthService;
import com.aegis.auth.service.SessionManager;
import com.aegis.auth.service.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Login / logout and the root redirect. */
@Controller
public class LoginController {

    private final AuthService authService;
    private final SessionManager sessionManager;

    @Autowired
    public LoginController(AuthService authService, SessionManager sessionManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpServletResponse response,
                          Model model) {
        User user = authService.authenticate(username, password);
        if (user == null) {
            model.addAttribute("error", "Invalid username or password.");
            return "login";
        }
        UserSession session = sessionManager.create(user);
        ResponseCookie cookie = ResponseCookie.from(SessionManager.COOKIE_NAME, session.getSessionId())
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SessionManager.COOKIE_NAME.equals(cookie.getName())) {
                    sessionManager.invalidate(cookie.getValue());
                }
            }
        }
        ResponseCookie clear = ResponseCookie.from(SessionManager.COOKIE_NAME, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());
        return "redirect:/login";
    }
}
