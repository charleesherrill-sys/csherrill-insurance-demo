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
 * <p>SECURITY (CWE-639): {@link #getClaim} enforces per-record authorization.
 * After loading the claim it verifies ownership: a member may only view their own
 * claims, while privileged roles (ADMIN/ADJUSTER) may view any claim. Unauthorized
 * cross-account reads are audited and then rejected with a not-found response so the
 * endpoint does not leak the existence of other members' claims.
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
        if (user == null) {
            return "redirect:/login";
        }

        Claim claim = claimService.getClaim(id);
        if (claim == null) {
            model.addAttribute("user", user);
            return "claims/not-found";
        }

        // Access is logged before the authorization decision so cross-account
        // attempts are captured for the audit trail / alerting.
        boolean crossAccount = claim.getMemberUserId() != user.getUserId();
        auditService.record(user.getUserId(), "CLAIM_VIEW", "claim",
                String.valueOf(id),
                crossAccount
                        ? "cross-account read: viewer " + user.getUserId()
                          + " owner " + claim.getMemberUserId()
                        : "self read");

        // Authorization check (CWE-639): members may only view their own claims;
        // ADMIN/ADJUSTER may view any. Reject cross-account reads as not-found so
        // the endpoint does not leak the existence of other members' claims.
        if (crossAccount && !user.canViewAllMembers()) {
            model.addAttribute("user", user);
            return "claims/not-found";
        }

        model.addAttribute("user", user);
        model.addAttribute("claim", claim);
        return "claims/detail";
    }
}
