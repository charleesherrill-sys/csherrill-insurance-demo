package com.aegis.billing;

import com.aegis.billing.model.Invoice;
import com.aegis.billing.model.Payment;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/** Outstanding/settled math on an invoice. */
public class InvoiceMathTest {

    private Payment payment(long amount, String status) {
        Payment p = new Payment();
        p.setAmountCents(amount);
        p.setStatus(status);
        return p;
    }

    @Test
    public void outstandingSubtractsSettledPayments() {
        Invoice invoice = new Invoice();
        invoice.setAmountCents(31500);
        invoice.setPayments(Arrays.asList(payment(15000, "SETTLED"), payment(16500, "FAILED")));
        assertEquals(15000, invoice.getSettledCents());
        assertEquals(16500, invoice.getOutstandingCents());
    }

    @Test
    public void outstandingNeverNegative() {
        Invoice invoice = new Invoice();
        invoice.setAmountCents(10000);
        invoice.setPayments(Arrays.asList(payment(12000, "SETTLED")));
        assertEquals(0, invoice.getOutstandingCents());
    }
}
