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
 * <p>SECURITY: {@link #getClaim} enforces per-record authorization (CWE-639).
 * A member may only read their own claims; ADJUSTER and ADMIN roles may read any
 * claim. Cross-account reads by other roles are rejected with the not-found view
 * so the existence of another member's claim is not disclosed. The access attempt
 * is still audited before the authorization decision is applied.
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

        // Access is logged before the authorization decision. The audit row is
        // exactly what the production alert in demo/trigger-artifact.md is built
        // from: it shows a user reading a claim that belongs to a different member.
        boolean crossAccount = claim.getMemberUserId() != user.getUserId();
        auditService.record(user.getUserId(), "CLAIM_VIEW", "claim",
                String.valueOf(id),
                crossAccount
                        ? "cross-account read: viewer " + user.getUserId()
                          + " owner " + claim.getMemberUserId()
                        : "self read");

        // Per-record authorization (CWE-639): non-privileged users may only read
        // their own claims. Return the not-found view for cross-account reads so
        // the existence of another member's claim is not disclosed.
        if (crossAccount && !user.canViewAllMembers()) {
            model.addAttribute("user", user);
            return "claims/not-found";
        }

        model.addAttribute("user", user);
        model.addAttribute("claim", claim);
        return "claims/detail";
    }
}
