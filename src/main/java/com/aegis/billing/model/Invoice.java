package com.aegis.billing.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A premium/cost-share invoice issued to a member. */
public class Invoice {

    private long id;
    private String invoiceNumber;
    private long policyId;
    private long memberUserId;
    private long amountCents;
    private String status;
    private LocalDate dueDate;
    private LocalDateTime createdAt;

    // Populated per-row by the billing path (contributes to the N+1).
    private List<Payment> payments = new ArrayList<>();
    private String policyNumber;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(long policyId) {
        this.policyId = policyId;
    }

    public long getMemberUserId() {
        return memberUserId;
    }

    public void setMemberUserId(long memberUserId) {
        this.memberUserId = memberUserId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public long getSettledCents() {
        long total = 0;
        for (Payment p : payments) {
            if ("SETTLED".equalsIgnoreCase(p.getStatus())) {
                total += p.getAmountCents();
            }
        }
        return total;
    }

    public long getOutstandingCents() {
        long outstanding = amountCents - getSettledCents();
        return outstanding < 0 ? 0 : outstanding;
    }

    public String getAmountDisplay() {
        return String.format("$%,.2f", amountCents / 100.0);
    }

    public String getOutstandingDisplay() {
        return String.format("$%,.2f", getOutstandingCents() / 100.0);
    }
}
