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
 * <p>SECURITY: {@link #getClaim} enforces per-record authorization (CWE-639,
 * IDOR fix). A claim is only rendered when the authenticated user owns it
 * ({@code claim.getMemberUserId() == user.getUserId()}) or holds a role allowed
 * to view all members ({@link UserSession#canViewAllMembers()} — ADJUSTER/ADMIN).
 * Any other access is denied with HTTP 403 and recorded to the audit log.
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

        // Authorization (CWE-639): allow only the owning member or a role permitted
        // to view all members (ADJUSTER/ADMIN). Reject everything else with 403.
        boolean owns = claim.getMemberUserId() == user.getUserId();
        if (!owns && !user.canViewAllMembers()) {
            auditService.record(user.getUserId(), "CLAIM_VIEW_DENIED", "claim",
                    String.valueOf(id),
                    "cross-account access blocked: viewer " + user.getUserId()
                            + " owner " + claim.getMemberUserId());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            model.addAttribute("user", user);
            return "claims/not-authorized";
        }

        // Access is authorized; record it. Privileged cross-account reads by an
        // adjuster/admin are still logged for the audit trail.
        boolean crossAccount = !owns;
        auditService.record(user.getUserId(), "CLAIM_VIEW", "claim",
                String.valueOf(id),
                crossAccount
                        ? "privileged cross-account read: viewer " + user.getUserId()
                          + " owner " + claim.getMemberUserId()
                        : "self read");

        model.addAttribute("user", user);
        model.addAttribute("claim", claim);
        return "claims/detail";
    }
}
