package com.aegis.claims.model;

/** A single billed service line on a claim. */
public class ClaimLine {

    private long id;
    private long claimId;
    private String serviceCode;
    private String description;
    private long billedCents;
    private Long allowedCents;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getClaimId() {
        return claimId;
    }

    public void setClaimId(long claimId) {
        this.claimId = claimId;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getBilledCents() {
        return billedCents;
    }

    public void setBilledCents(long billedCents) {
        this.billedCents = billedCents;
    }

    public Long getAllowedCents() {
        return allowedCents;
    }

    public void setAllowedCents(Long allowedCents) {
        this.allowedCents = allowedCents;
    }

    public String getBilledDisplay() {
        return String.format("$%,.2f", billedCents / 100.0);
    }

    public String getAllowedDisplay() {
        return allowedCents == null ? "—" : String.format("$%,.2f", allowedCents / 100.0);
    }
}
