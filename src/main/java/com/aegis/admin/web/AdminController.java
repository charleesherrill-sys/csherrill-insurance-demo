package com.aegis.admin.web;

import com.aegis.admin.service.AdminService;
import com.aegis.batch.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * Admin portal.
 *
 * <p>SECURITY: the whole {@code /admin/**} area is registered with
 * {@code AuthInterceptor} (see WebConfig) and requires an authenticated ADMIN
 * (CWE-306 fix). {@link #listUsers()} no longer returns password hashes, and
 * {@link #runReconciliation()} — which triggers a financial batch job — is
 * reachable only by an administrator.
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
    public String portal(Model model) {
        model.addAttribute("claimCounts", adminService.claimCountsByStatus());
        return "admin/portal";
    }

    /** ADMIN only: returns all users and roles as JSON (no password hashes). */
    @GetMapping("/admin/users")
    @ResponseBody
    public List<Map<String, Object>> listUsers() {
        return adminService.listAllUsers();
    }

    /** ADMIN only: kicks off the financial reconciliation batch on demand. */
    @PostMapping("/admin/reconciliation/run")
    @ResponseBody
    public ReconciliationService.ReconciliationResult runReconciliation() {
        return reconciliationService.run();
    }
}
