package com.aegis.claims.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.claims.model.Claim;
import com.aegis.claims.service.ClaimService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Lists the current member's claims (the claims-list page).
 *
 * <p>This page is one half of the flagship N+1 performance issue (see
 * {@link ClaimService}). The optional {@code status} filter is passed to a
 * parameterized query in the repository (CWE-89 fix).
 */
@Controller
public class ClaimsController {

    private final ClaimService claimService;

    @Autowired
    public ClaimsController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @GetMapping("/claims")
    public String listClaims(@RequestParam(name = "status", required = false) String status,
                             HttpServletRequest request,
                             Model model) {
        UserSession user = CurrentUser.from(request);
        List<Claim> claims;
        if (StringUtils.isNotBlank(status)) {
            claims = claimService.searchClaimsForMember(user.getUserId(), status);
        } else {
            claims = claimService.getClaimsForMember(user.getUserId());
        }
        model.addAttribute("user", user);
        model.addAttribute("claims", claims);
        model.addAttribute("statusFilter", status);
        return "claims/list";
    }
}
