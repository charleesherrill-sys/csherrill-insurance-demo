package com.aegis.billing.service;

import com.aegis.billing.model.Invoice;
import com.aegis.billing.model.Payment;
import com.aegis.billing.repository.BillingRepository;
import com.aegis.integration.PaymentGatewayClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Disburses payments through the (mocked) external payment gateway. Invoked at the
 * end of the claim adjudication flow to "trigger payment".
 */
@Service
public class PaymentService {

    private final BillingRepository billingRepository;
    private final PaymentGatewayClient gateway;

    @Autowired
    public PaymentService(BillingRepository billingRepository, PaymentGatewayClient gateway) {
        this.billingRepository = billingRepository;
        this.gateway = gateway;
    }

    /**
     * Disburses {@code amountCents} against the given invoice via the gateway
     * (synchronous, blocking) and records the resulting payment.
     */
    public Payment disburse(long invoiceId, long amountCents, String method) {
        Invoice invoice = billingRepository.findInvoiceById(invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("no such invoice: " + invoiceId);
        }
        // Blocking downstream call.
        String externalRef = gateway.disburse(invoiceId, amountCents, method);

        Payment payment = new Payment();
        payment.setInvoiceId(invoiceId);
        payment.setAmountCents(amountCents);
        payment.setMethod(method);
        payment.setStatus("SETTLED");
        payment.setExternalRef(externalRef);
        long id = billingRepository.insertPayment(payment);
        payment.setId(id);

        // Mark the invoice paid if fully covered.
        if (amountCents >= invoice.getOutstandingCents()) {
            billingRepository.updateInvoiceStatus(invoiceId, "PAID");
        }
        return payment;
    }
}
