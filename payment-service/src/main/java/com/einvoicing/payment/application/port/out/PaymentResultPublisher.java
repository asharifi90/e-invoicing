package com.einvoicing.payment.application.port.out;

import com.einvoicing.payment.domain.event.PaymentFailedEvent;
import com.einvoicing.payment.domain.event.PaymentSucceededEvent;

public interface PaymentResultPublisher {

    void paymentSucceeded(PaymentSucceededEvent event);
    void paymentFailed(PaymentFailedEvent event);
}
