package com.aegis.billing.web;

import com.aegis.auth.service.UserSession;
import com.aegis.auth.web.CurrentUser;
import com.aegis.billing.model.Invoice;
import com.aegis.billing.repository.BillingRepository;
import com.aegis.billing.service.BillingService;
import com.aegis.policy.model.Policy;
import com.aegis.policy.repository.PolicyRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Member billing view (the billing page).
 *
 * <p>This page is the billing half of the flagship N+1 issue (see
 * {@link BillingService}). The optional {@code status} filter is passed to
 * {@link BillingRepository#searchInvoices(long, String)}, which binds it as a
 * query parameter.
 */
@Controller
public class BillingController {

    private final BillingService billingService;
    private final BillingRepository billingRepository;
    private final PolicyRepository policyRepository;

    @Autowired
    public BillingController(BillingService billingService,
                            BillingRepository billingRepository,
                            PolicyRepository policyRepository) {
        this.billingService = billingService;
        this.billingRepository = billingRepository;
        this.policyRepository = policyRepository;
    }

    @GetMapping("/billing")
    public String billing(@RequestParam(name = "status", required = false) String status,
                          HttpServletRequest request,
                          Model model) {
        UserSession user = CurrentUser.from(request);
        List<Invoice> invoices;
        if (StringUtils.isNotBlank(status)) {
            invoices = billingRepository.searchInvoices(user.getUserId(), status);
            // Same N+1 enrichment as the service path.
            for (Invoice invoice : invoices) {
                invoice.setPayments(billingRepository.findPaymentsByInvoice(invoice.getId()));
                Policy policy = policyRepository.findById(invoice.getPolicyId());
                if (policy != null) {
                    invoice.setPolicyNumber(policy.getPolicyNumber());
                }
            }
        } else {
            invoices = billingService.getBillingForMember(user.getUserId());
        }
        model.addAttribute("user", user);
        model.addAttribute("invoices", invoices);
        model.addAttribute("totalOutstandingCents", billingService.totalOutstandingCents(user.getUserId()));
        model.addAttribute("statusFilter", status);
        return "billing/view";
    }
}
