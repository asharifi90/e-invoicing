package com.einvoicing.invoice.adapter.out.persistence.mapper;

import com.einvoicing.invoice.adapter.out.persistence.entity.InvoiceJpaEntity;
import com.einvoicing.invoice.adapter.out.persistence.entity.InvoiceLineJpaEntity;
import com.einvoicing.invoice.domain.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InvoiceMapper {

    public InvoiceJpaEntity toJpaEntity(Invoice invoice) {
        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setId(invoice.getId().value());
        entity.setInvoiceNumber(invoice.getInvoiceNumber());
        entity.setSellerVatNumber(invoice.getSellerVatNumber());
        entity.setBuyerVatNumber(invoice.getBuyerVatNumber());
        entity.setTotalAmount(invoice.getTotalAmount().amount());
        entity.setCurrency(invoice.getTotalAmount().currency());
        entity.setStatus(invoice.getStatus().name());
        entity.setReceivedAt(invoice.getReceivedAt());
        entity.setRejectionReason(invoice.getRejectionReason());

        invoice.getLines().forEach(line -> {
            InvoiceLineJpaEntity lineEntity = toLineJpaEntity(line);
            entity.addLine(lineEntity);
        });

        return entity;
    }

    public Invoice toDomain(InvoiceJpaEntity entity) {
        List<InvoiceLine> lines = entity.getLines().stream()
                .map(this::toLineDomain)
                .toList();

        return Invoice.reconstitute(
                InvoiceId.from(entity.getId().toString()),
                entity.getInvoiceNumber(),
                entity.getSellerVatNumber(),
                entity.getBuyerVatNumber(),
                lines,
                new Money(entity.getTotalAmount(), entity.getCurrency()),
                InvoiceStatus.valueOf(entity.getStatus()),
                entity.getReceivedAt(),
                entity.getRejectionReason()
        );

    }

    private InvoiceLineJpaEntity toLineJpaEntity(InvoiceLine line) {
        InvoiceLineJpaEntity entity = new InvoiceLineJpaEntity();
        entity.setDescription(line.getDescription());
        entity.setQuantity(line.getQuantity());
        entity.setUnitPrice(line.getUnitPrice().amount());
        entity.setLineTotal(line.getLineTotal().amount());
        entity.setCurrency(line.getUnitPrice().currency());
        return entity;
    }

    private InvoiceLine toLineDomain(InvoiceLineJpaEntity entity) {
        return new InvoiceLine(
                entity.getDescription(),
                entity.getQuantity(),
                new Money(entity.getUnitPrice(), entity.getCurrency())
        );
    }
}
