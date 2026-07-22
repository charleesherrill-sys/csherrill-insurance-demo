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
 * <p>SECURITY (REMEDIATED — was the flagship IDOR, CWE-639 — see REVIEW.md):
 * {@link #getClaim} now enforces per-record authorization. If the current user is
 * not an ADJUSTER/ADMIN, a request for a claim owned by a different member is
 * rejected (the cross-account attempt is still audited, but the claim data is not
 * returned). This closes the Authorization Bypass Through User-Controlled Key.
 *
 * <p>The check was intentionally added as part of the remediation task: if the
 * current user cannot view all members, reject the request when
 * {@code claim.getMemberUserId() != user.getUserId()}.
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

        // The cross-account attempt is still audited so the production alert in
        // demo/trigger-artifact.md still fires: it shows a user reading (or
        // attempting to read) a claim that belongs to a different member.
        boolean crossAccount = claim.getMemberUserId() != user.getUserId();
        auditService.record(user.getUserId(), "CLAIM_VIEW", "claim",
                String.valueOf(id),
                crossAccount
                        ? "cross-account read: viewer " + user.getUserId()
                          + " owner " + claim.getMemberUserId()
                        : "self read");

        // AUTHORIZATION CHECK (CWE-639 remediation): a member may only read their
        // own claims. Adjusters/admins may read any claim. Reject cross-account
        // reads for everyone else WITHOUT returning the claim data.
        if (crossAccount && !user.canViewAllMembers()) {
            model.addAttribute("user", user);
            return "claims/forbidden";
        }

        model.addAttribute("user", user);
        model.addAttribute("claim", claim);
        return "claims/detail";
    }
}
