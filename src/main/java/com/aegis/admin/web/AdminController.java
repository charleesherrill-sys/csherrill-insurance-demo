package com.aegis.admin.web;

import com.aegis.admin.service.AdminService;
import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.batch.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/** Admin portal. */
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

    @GetMapping("/admin/users")
    @ResponseBody
    public List<Map<String, Object>> listUsers(HttpServletRequest request) {
        requireAdmin(request);
        return adminService.listAllUsers();
    }

    @PostMapping("/admin/reconciliation/run")
    @ResponseBody
    public ReconciliationService.ReconciliationResult runReconciliation(HttpServletRequest request) {
        requireAdmin(request);
        return reconciliationService.run();
    }

    private void requireAdmin(HttpServletRequest request) {
        UserSession user = CurrentUser.from(request);
        if (user == null || !user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admin role required");
        }
    }
}
