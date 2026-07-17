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
import javax.servlet.http.HttpServletResponse;

/**
 * Serves the claim-detail page: {@code GET /claims/{id}}.
 *
 * <p>Ownership is enforced: {@link #getClaim} loads the claim by id and only
 * renders it when the claim belongs to the authenticated member, or the member
 * is privileged (ADMIN/ADJUSTER may view any claim). Non-privileged members may
 * only view their own claims; requests for another member's claim are rejected
 * with HTTP 403. CWE-639 (Authorization Bypass Through User-Controlled Key /
 * IDOR) is remediated here.
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
                           HttpServletResponse response,
                           Model model) {
        UserSession user = CurrentUser.from(request);

        Claim claim = claimService.getClaim(id);
        if (claim == null) {
            model.addAttribute("user", user);
            return "claims/not-found";
        }

        // Access is logged. The audit row is exactly what the production alert in
        // demo/trigger-artifact.md is built from: it shows a user reading a claim
        // that belongs to a different member.
        boolean crossAccount = claim.getMemberUserId() != user.getUserId();
        auditService.record(user.getUserId(), "CLAIM_VIEW", "claim",
                String.valueOf(id),
                crossAccount
                        ? "cross-account read: viewer " + user.getUserId()
                          + " owner " + claim.getMemberUserId()
                        : "self read");

        if (!user.canViewAllMembers() && claim.getMemberUserId() != user.getUserId()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            model.addAttribute("user", user);
            return "error/forbidden";
        }

        model.addAttribute("user", user);
        model.addAttribute("claim", claim);
        return "claims/detail";
    }
}
