package com.aegis.claims.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A benefits claim submitted against a policy. */
public class Claim {

    private long id;
    private String claimNumber;
    private long policyId;
    private long memberUserId;
    private String claimType;
    private String status;
    private long amountCents;
    private Long approvedCents;
    private String diagnosisCode;
    private String adjudicatorNotes;
    private LocalDateTime submittedAt;
    private LocalDateTime adjudicatedAt;

    // Populated lazily/per-row by the claims-list path (contributes to the N+1).
    private List<ClaimLine> lines = new ArrayList<>();
    private long outstandingBalanceCents;
    private String policyNumber;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
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

    public String getClaimType() {
        return claimType;
    }

    public void setClaimType(String claimType) {
        this.claimType = claimType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public Long getApprovedCents() {
        return approvedCents;
    }

    public void setApprovedCents(Long approvedCents) {
        this.approvedCents = approvedCents;
    }

    public String getDiagnosisCode() {
        return diagnosisCode;
    }

    public void setDiagnosisCode(String diagnosisCode) {
        this.diagnosisCode = diagnosisCode;
    }

    public String getAdjudicatorNotes() {
        return adjudicatorNotes;
    }

    public void setAdjudicatorNotes(String adjudicatorNotes) {
        this.adjudicatorNotes = adjudicatorNotes;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getAdjudicatedAt() {
        return adjudicatedAt;
    }

    public void setAdjudicatedAt(LocalDateTime adjudicatedAt) {
        this.adjudicatedAt = adjudicatedAt;
    }

    public List<ClaimLine> getLines() {
        return lines;
    }

    public void setLines(List<ClaimLine> lines) {
        this.lines = lines;
    }

    public long getOutstandingBalanceCents() {
        return outstandingBalanceCents;
    }

    public void setOutstandingBalanceCents(long outstandingBalanceCents) {
        this.outstandingBalanceCents = outstandingBalanceCents;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    /** Dollar-formatted amount for templates. */
    public String getAmountDisplay() {
        return String.format("$%,.2f", amountCents / 100.0);
    }

    public String getApprovedDisplay() {
        return approvedCents == null ? "—" : String.format("$%,.2f", approvedCents / 100.0);
    }
}
