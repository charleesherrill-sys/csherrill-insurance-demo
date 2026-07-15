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
 * <p>SECURITY (INTENTIONAL — see REVIEW.md): the whole {@code /admin/**} area is
 * NOT registered with {@code AuthInterceptor} (see WebConfig), so every endpoint
 * here is reachable with no authentication and no role check. In particular
 * {@link #listUsers()} dumps all users and their password hashes, and
 * {@link #runReconciliation()} triggers a financial batch job.
 * CWE-306: Missing Authentication for Critical Function. Do NOT add auth here
 * unless that is the explicit task.
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

    /** Unauthenticated: returns all users and their password hashes as JSON. */
    @GetMapping("/admin/users")
    @ResponseBody
    public List<Map<String, Object>> listUsers() {
        return adminService.listAllUsers();
    }

    /** Unauthenticated: kicks off the financial reconciliation batch on demand. */
    @PostMapping("/admin/reconciliation/run")
    @ResponseBody
    public ReconciliationService.ReconciliationResult runReconciliation() {
        return reconciliationService.run();
    }
}
