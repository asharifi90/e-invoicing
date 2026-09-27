package com.einvoicing.payment.application.port.out.dto;

public class PaymentProviderResult {

    private final boolean success;
    private final String providerReference;
    private final String failureReason;

    public PaymentProviderResult(boolean success, String providerReference, String failureReason) {
        this.success = success;
        this.providerReference = providerReference;
        this.failureReason = failureReason;
    }

    public static  PaymentProviderResult success(String providerReference) {
        return new PaymentProviderResult(true, providerReference, null);
    }

    public static  PaymentProviderResult failure(String failureReason) {
        return new PaymentProviderResult(false, null, failureReason);
    }

    public boolean isSuccess() {
        return success;
    }
    public String getProviderReference() {
        return providerReference;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
