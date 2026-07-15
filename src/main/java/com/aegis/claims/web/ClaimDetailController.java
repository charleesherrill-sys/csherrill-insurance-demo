package com.aegis.claims.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.ClaimService;
import com.aegis.common.audit.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;

/**
 * Serves the claim-detail page: {@code GET /claims/{id}}.
 *
 * <p>SECURITY (INTENTIONAL — THE FLAGSHIP BUG — see REVIEW.md):
 * {@link #getClaim} loads the claim by id and renders it WITHOUT verifying that
 * the claim belongs to the authenticated member. Any logged-in user can read any
 * other member's claim by changing the id in the URL.
 * CWE-639: Authorization Bypass Through User-Controlled Key (IDOR).
 *
 * <p>The correct behavior would be: if the current user is not an ADJUSTER/ADMIN,
 * reject the request when {@code claim.getMemberUserId() != user.getUserId()}.
 * That check is deliberately absent. Do NOT add it unless that is the task.
 */
@Controller
public class ClaimDetailController {

    private final ClaimService claimService;
    private final AuditService auditService;

    @Autowired
    public ClaimDetailController(ClaimService claimService, AuditService auditService) {
        this.claimService = claimService;
        this.auditService = auditService;
    }

    @GetMapping("/claims/{id}")
    public String getClaim(@PathVariable("id") long id,
                           HttpServletRequest request,
                           Model model) {
        UserSession user = CurrentUser.from(request);

        Claim claim = claimService.getClaim(id);
        if (claim == null) {
            model.addAttribute("user", user);
            return "claims/not-found";
        }

        // Access is logged, but NOT authorized. The audit row is exactly what the
        // production alert in demo/trigger-artifact.md is built from: it shows a
        // user reading a claim that belongs to a different member.
        boolean crossAccount = claim.getMemberUserId() != user.getUserId();
        auditService.record(user.getUserId(), "CLAIM_VIEW", "claim",
                String.valueOf(id),
                crossAccount
                        ? "cross-account read: viewer " + user.getUserId()
                          + " owner " + claim.getMemberUserId()
                        : "self read");

        // MISSING AUTHORIZATION CHECK (CWE-639): the claim is returned regardless
        // of ownership. See class Javadoc.
        model.addAttribute("user", user);
        model.addAttribute("claim", claim);
        return "claims/detail";
    }
}
