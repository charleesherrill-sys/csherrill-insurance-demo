package com.aegis.claims.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.AdjudicationService;
import com.aegis.claims.service.ClaimIntakeService;
import com.aegis.claims.service.ClaimService;
import com.aegis.policy.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** New-claim form plus the submit/validate/adjudicate actions. */
@Controller
public class ClaimIntakeController {

    private final ClaimIntakeService intakeService;
    private final AdjudicationService adjudicationService;
    private final PolicyService policyService;
    private final ClaimService claimService;

    @Autowired
    public ClaimIntakeController(ClaimIntakeService intakeService,
                                AdjudicationService adjudicationService,
                                PolicyService policyService,
                                ClaimService claimService) {
        this.intakeService = intakeService;
        this.adjudicationService = adjudicationService;
        this.policyService = policyService;
        this.claimService = claimService;
    }

    @GetMapping("/claims/new")
    public String newClaimForm(HttpServletRequest request, Model model) {
        UserSession user = CurrentUser.from(request);
        model.addAttribute("user", user);
        model.addAttribute("policies", policyService.getPoliciesForMember(user.getUserId()));
        return "claims/new";
    }

    @PostMapping("/claims")
    public String submitClaim(@RequestParam long policyId,
                              @RequestParam String claimType,
                              @RequestParam long amountCents,
                              @RequestParam(required = false) String diagnosisCode,
                              HttpServletRequest request) {
        UserSession user = CurrentUser.from(request);
        long claimId = intakeService.submit(user.getUserId(), policyId, claimType, amountCents, diagnosisCode);
        return "redirect:/claims/" + claimId;
    }

    @PostMapping("/claims/{id}/adjudicate")
    public String adjudicate(@org.springframework.web.bind.annotation.PathVariable long id,
                             HttpServletRequest request,
                             HttpServletResponse response,
                             Model model) {
        UserSession user = CurrentUser.from(request);
        Claim claim = claimService.getClaim(id);
        if (claim == null) {
            model.addAttribute("user", user);
            return "claims/not-found";
        }
        // Authorization (CWE-639): only the owning member or an ADJUSTER/ADMIN may
        // act on a claim; block cross-account adjudication.
        if (claim.getMemberUserId() != user.getUserId() && !user.canViewAllMembers()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            model.addAttribute("user", user);
            return "claims/not-authorized";
        }
        // Run validate then adjudicate (submit -> validate -> adjudicate -> pay).
        intakeService.validate(id);
        adjudicationService.adjudicate(id);
        return "redirect:/claims/" + id;
    }
}
