package com.einvoicing.invoice.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class InvoiceJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private String sellerVatNumber;

    @Column(nullable = false)
    private String buyerVatNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant receivedAt;

    private String rejectionReason;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineJpaEntity> lines = new ArrayList<>();

    public InvoiceJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getSellerVatNumber() { return sellerVatNumber; }
    public void setSellerVatNumber(String sellerVatNumber) { this.sellerVatNumber = sellerVatNumber; }

    public String getBuyerVatNumber() { return buyerVatNumber; }
    public void setBuyerVatNumber(String buyerVatNumber) { this.buyerVatNumber = buyerVatNumber; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public List<InvoiceLineJpaEntity> getLines() { return lines; }
    public void setLines(List<InvoiceLineJpaEntity> lines) { this.lines = lines; }

    public void addLine(InvoiceLineJpaEntity line) {
        lines.add(line);
        line.setInvoice(this);
    }
}
