package com.einvoicing.approval.domain.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ApprovalPolicy {
    private final BigDecimal autoApproveLimit = new BigDecimal("1000.00");

    public boolean canAutoApprove(BigDecimal totalAmount) {
        if (totalAmount == null) {
            return false;
        }
        return totalAmount.compareTo(autoApproveLimit) < 0;
    }
}
