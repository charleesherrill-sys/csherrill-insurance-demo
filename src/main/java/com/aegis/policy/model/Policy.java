package com.aegis.policy.model;

import java.time.LocalDate;

/** An insurance policy held by a member. */
public class Policy {

    private long id;
    private String policyNumber;
    private long holderUserId;
    private String product;
    private String status;
    private long premiumCents;
    private LocalDate effectiveDate;
    private LocalDate endDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public long getHolderUserId() {
        return holderUserId;
    }

    public void setHolderUserId(long holderUserId) {
        this.holderUserId = holderUserId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPremiumCents() {
        return premiumCents;
    }

    public void setPremiumCents(long premiumCents) {
        this.premiumCents = premiumCents;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
