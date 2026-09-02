package com.einvoicing.approval.domain.service;


import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;


public class ApprovalPolicyTest {

    private final ApprovalPolicy approvalPolicy = new ApprovalPolicy();

    @Test
    void belowLimit_canAutoApprove(){
        assertThat(approvalPolicy.canAutoApprove(new BigDecimal("999.99"))).isTrue();
    }

    @Test
    void belowLimit_cannotAutoApprove(){
        assertThat(approvalPolicy.canAutoApprove(new BigDecimal("1000.00"))).isFalse();
    }
}
