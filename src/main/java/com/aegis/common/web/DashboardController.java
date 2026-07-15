package com.aegis.common.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.billing.service.BillingService;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.ClaimService;
import com.aegis.policy.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/** Landing dashboard after login: a member's policies, recent claims, and balance. */
@Controller
public class DashboardController {

    private final ClaimService claimService;
    private final BillingService billingService;
    private final PolicyService policyService;

    @Autowired
    public DashboardController(ClaimService claimService,
                              BillingService billingService,
                              PolicyService policyService) {
        this.claimService = claimService;
        this.billingService = billingService;
        this.policyService = policyService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request, Model model) {
        UserSession user = CurrentUser.from(request);
        List<Claim> claims = claimService.getClaimsForMember(user.getUserId());
        model.addAttribute("user", user);
        model.addAttribute("claims", claims);
        model.addAttribute("policies", policyService.getPoliciesForMember(user.getUserId()));
        model.addAttribute("totalOutstandingCents", billingService.totalOutstandingCents(user.getUserId()));
        return "dashboard";
    }
}
