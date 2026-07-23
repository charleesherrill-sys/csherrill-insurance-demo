package com.aegis.admin.web;

import com.aegis.admin.service.AdminService;
import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.batch.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Admin portal.
 *
 * <p>SECURITY: the whole {@code /admin/**} area is registered with
 * {@code AuthInterceptor} (see WebConfig), so an authenticated session is required
 * for every endpoint here. Each handler additionally enforces the {@code ADMIN}
 * role via {@link #requireAdmin(HttpServletRequest)} so that ordinary members
 * cannot list all users or drive the financial reconciliation batch (CWE-306:
 * Missing Authentication for Critical Function). {@link AdminService#listAllUsers()}
 * no longer returns password hashes.
 */
@Controller
public class AdminController {

    private final AdminService adminService;
    private final ReconciliationService reconciliationService;

    @Autowired
    public AdminController(AdminService adminService, ReconciliationService reconciliationService) {
        this.adminService = adminService;
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/admin")
    public String portal(HttpServletRequest request, Model model) {
        requireAdmin(request);
        model.addAttribute("claimCounts", adminService.claimCountsByStatus());
        return "admin/portal";
    }

    /** Admin only: returns all users (without password hashes) as JSON. */
    @GetMapping("/admin/users")
    @ResponseBody
    public List<Map<String, Object>> listUsers(HttpServletRequest request) {
        requireAdmin(request);
        return adminService.listAllUsers();
    }

    /** Admin only: kicks off the financial reconciliation batch on demand. */
    @PostMapping("/admin/reconciliation/run")
    @ResponseBody
    public ReconciliationService.ReconciliationResult runReconciliation(HttpServletRequest request) {
        requireAdmin(request);
        return reconciliationService.run();
    }

    /**
     * Enforces that the current session belongs to an {@code ADMIN} user. Throws
     * {@code 403 Forbidden} otherwise. Authentication itself is guaranteed by
     * {@code AuthInterceptor} (see WebConfig), which redirects anonymous callers
     * to the login page before they reach this handler.
     */
    private void requireAdmin(HttpServletRequest request) {
        UserSession user = CurrentUser.from(request);
        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
