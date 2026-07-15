package com.aegis.reporting.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.reporting.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/** Reporting page (aggregate paid amounts and outstanding balances). */
@Controller
public class ReportingController {

    private final ReportingService reportingService;

    @Autowired
    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/reports")
    public String reports(HttpServletRequest request, Model model) {
        UserSession user = CurrentUser.from(request);
        model.addAttribute("user", user);
        model.addAttribute("paidByType", reportingService.paidByClaimType());
        model.addAttribute("totalOutstandingCents", reportingService.totalOutstandingInvoiceCents());
        return "reports/view";
    }
}
