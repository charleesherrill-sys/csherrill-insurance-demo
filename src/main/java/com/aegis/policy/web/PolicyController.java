package com.aegis.policy.web;

import com.aegis.auth.web.CurrentUser;
import com.aegis.auth.service.UserSession;
import com.aegis.policy.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/** Lists the current member's policies. */
@Controller
public class PolicyController {

    private final PolicyService policyService;

    @Autowired
    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping("/policies")
    public String listPolicies(HttpServletRequest request, Model model) {
        UserSession user = CurrentUser.from(request);
        model.addAttribute("user", user);
        model.addAttribute("policies", policyService.getPoliciesForMember(user.getUserId()));
        return "policies/list";
    }
}
