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
 * <p>{@link #getClaim} enforces per-record authorization (CWE-639 remediation): a
 * claim is only rendered when it belongs to the authenticated member, or when the
 * member holds an elevated role ({@link UserSession#canViewAllMembers()}). Any
 * other cross-account access is rejected with HTTP 403.
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

        boolean crossAccount = claim.getMemberUserId() != user.getUserId();
        auditService.record(user.getUserId(), "CLAIM_VIEW", "claim",
                String.valueOf(id),
                crossAccount
                        ? "cross-account read: viewer " + user.getUserId()
                          + " owner " + claim.getMemberUserId()
                        : "self read");

        // Authorization (CWE-639): the claim is only viewable by its owner, or by
        // members with an elevated role that may view all members' claims.
        if (crossAccount && !user.canViewAllMembers()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            model.addAttribute("user", user);
            return "claims/forbidden";
        }

        model.addAttribute("user", user);
        model.addAttribute("claim", claim);
        return "claims/detail";
    }
}
