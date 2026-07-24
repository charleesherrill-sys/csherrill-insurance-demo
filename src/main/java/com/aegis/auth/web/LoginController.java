package com.aegis.auth.web;

import com.aegis.auth.model.User;
import com.aegis.auth.service.AuthService;
import com.aegis.auth.service.SessionManager;
import com.aegis.auth.service.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    /**
     * Whether to mark the session cookie {@code Secure} (HTTPS-only). Defaults to
     * {@code true}; can be set to {@code false} for local http-only development.
     */
    @Value("${aegis.session.cookie.secure:true}")
    private boolean secureCookie;

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
        // A new session id is minted on every login (session-fixation protection).
        UserSession session = sessionManager.create(user);
        Cookie cookie = new Cookie(SessionManager.COOKIE_NAME, session.getSessionId());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        response.addCookie(cookie);
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
        Cookie clear = new Cookie(SessionManager.COOKIE_NAME, "");
        clear.setPath("/");
        clear.setHttpOnly(true);
        clear.setSecure(secureCookie);
        clear.setMaxAge(0);
        response.addCookie(clear);
        return "redirect:/login";
    }
}
