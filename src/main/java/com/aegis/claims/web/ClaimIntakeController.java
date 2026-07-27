package com.aegis.claims.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.AdjudicationService;
import com.aegis.claims.service.ClaimIntakeService;
import com.aegis.claims.service.ClaimService;
import com.aegis.policy.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

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

    /**
     * Runs validate then adjudicate (submit -> validate -> adjudicate -> pay).
     *
     * <p>Adjudication is a state-changing, money-moving operation, so the caller must
     * either own the claim or be staff (ADJUSTER/ADMIN), and the claim must still be
     * awaiting adjudication. The auth interceptor only proves authentication.
     */
    @PostMapping("/claims/{id}/adjudicate")
    public String adjudicate(@PathVariable long id, HttpServletRequest request) {
        UserSession user = CurrentUser.from(request);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not authenticated");
        }

        Claim claim = claimService.getClaim(id);
        if (claim == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no such claim: " + id);
        }
        if (!user.canViewAllMembers() && claim.getMemberUserId() != user.getUserId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "not authorized to adjudicate this claim");
        }
        if (!isAwaitingAdjudication(claim.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "claim is not awaiting adjudication: " + claim.getStatus());
        }

        intakeService.validate(id);
        adjudicationService.adjudicate(id);
        return "redirect:/claims/" + id;
    }

    /** Only submitted/validated claims may be (re-)driven through adjudication and payment. */
    private boolean isAwaitingAdjudication(String status) {
        return "SUBMITTED".equalsIgnoreCase(status) || "VALIDATED".equalsIgnoreCase(status);
    }
}
