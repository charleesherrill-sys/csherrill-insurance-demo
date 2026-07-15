package com.aegis.reporting.service;

import com.aegis.reporting.repository.ReportingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Builds the numbers shown on the reporting page. */
@Service
public class ReportingService {

    private final ReportingRepository reportingRepository;

    @Autowired
    public ReportingService(ReportingRepository reportingRepository) {
        this.reportingRepository = reportingRepository;
    }

    public Map<String, Long> paidByClaimType() {
        return reportingRepository.paidByClaimType();
    }

    public long totalOutstandingInvoiceCents() {
        return reportingRepository.totalOutstandingInvoiceCents();
    }
}
