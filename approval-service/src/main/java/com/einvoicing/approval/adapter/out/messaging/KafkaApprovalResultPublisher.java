package com.einvoicing.approval.adapter.out.messaging;

import com.einvoicing.approval.application.port.out.ApprovalResultPublisher;
import com.einvoicing.approval.domain.event.InvoiceApprovalRequiredEvent;
import com.einvoicing.approval.domain.event.InvoiceApprovedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaApprovalResultPublisher implements ApprovalResultPublisher {

    public static final String TOPIC_APPROVED = "invoice.approved";
    public static final String TOPIC_NOT_APPROVED = "invoice.not.approved";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaApprovalResultPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishApproved(InvoiceApprovedEvent event) {
        kafkaTemplate.send(TOPIC_APPROVED, event.getInvoiceId().toString(), event);
    }

    @Override
    public void publishApprovalRequired(InvoiceApprovalRequiredEvent event) {
        kafkaTemplate.send(TOPIC_NOT_APPROVED, event.getInvoiceId().toString(), event);
    }
}
