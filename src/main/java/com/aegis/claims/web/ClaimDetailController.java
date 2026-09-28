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
import java.io.IOException;

/**
 * Serves the claim-detail page: {@code GET /claims/{id}}.
 *
 * <p>Access control: ADJUSTER and ADMIN users may view any claim. MEMBER users
 * may only view claims they own (CWE-639 fix).
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
                           Model model) throws IOException {
        UserSession user = CurrentUser.from(request);

        Claim claim = claimService.getClaim(id);
        if (claim == null) {
            model.addAttribute("user", user);
            return "claims/not-found";
        }

        // Ownership check: members may only view their own claims.
        if (!user.canViewAllMembers() && claim.getMemberUserId() != user.getUserId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        auditService.record(user.getUserId(), "CLAIM_VIEW", "claim",
                String.valueOf(id), "self read");

        model.addAttribute("user", user);
        model.addAttribute("claim", claim);
        return "claims/detail";
    }
}
