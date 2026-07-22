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
 * <p>SECURITY (CWE-306): the whole {@code /admin/**} area is behind
 * {@code AuthInterceptor} (see WebConfig), so an authenticated session is
 * required, and every handler additionally enforces the ADMIN role via
 * {@link #requireAdmin(HttpServletRequest)}. This protects the sensitive
 * endpoints — {@link #listUsers(HttpServletRequest)} (which returns user
 * records) and {@link #runReconciliation(HttpServletRequest)} (which triggers a
 * financial batch job) — from non-admin callers.
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

    /**
     * Authorization guard: rejects the request unless the authenticated user has
     * the ADMIN role. Authentication itself is guaranteed by {@code AuthInterceptor}.
     */
    private void requireAdmin(HttpServletRequest request) {
        UserSession user = CurrentUser.from(request);
        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    @GetMapping("/admin")
    public String portal(HttpServletRequest request, Model model) {
        requireAdmin(request);
        model.addAttribute("claimCounts", adminService.claimCountsByStatus());
        return "admin/portal";
    }

    /** ADMIN only: returns all users as JSON. */
    @GetMapping("/admin/users")
    @ResponseBody
    public List<Map<String, Object>> listUsers(HttpServletRequest request) {
        requireAdmin(request);
        return adminService.listAllUsers();
    }

    /** ADMIN only: kicks off the financial reconciliation batch on demand. */
    @PostMapping("/admin/reconciliation/run")
    @ResponseBody
    public ReconciliationService.ReconciliationResult runReconciliation(HttpServletRequest request) {
        requireAdmin(request);
        return reconciliationService.run();
    }
}
