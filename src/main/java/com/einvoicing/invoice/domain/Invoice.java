package com.einvoicing.invoice.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Invoice {

    private final InvoiceId id;
    private final String invoiceNumber;
    private final String sellerVatNumber;
    private final String buyerVatNumber;
    private final List<InvoiceLine> lines;
    private final Money totalAmount;
    private InvoiceStatus status;
    private final Instant receivedAt;
    private String rejectionReason;

    public Invoice(InvoiceId id, String invoiceNumber, String sellerVatNumber, String buyerVatNumber, List<InvoiceLine> lines, Money totalAmount) {
        this.id = Objects.requireNonNull(id);
        this.invoiceNumber = Objects.requireNonNull(invoiceNumber);
        this.sellerVatNumber = Objects.requireNonNull(sellerVatNumber);
        this.buyerVatNumber = Objects.requireNonNull(buyerVatNumber);
        this.lines = List.copyOf(lines);
        this.totalAmount = Objects.requireNonNull(totalAmount);
        this.status = InvoiceStatus.RECEIVED;
        this.receivedAt = Instant.now();
    }

    public static Invoice create(String invoiceNumber,
                                 String sellerVatNumber,
                                 String buyerVatNumber,
                                 List<InvoiceLine> lines){
        if (invoiceNumber == null || invoiceNumber.isBlank()){
            throw new IllegalArgumentException("invoice number is required");
        }
        if (lines == null || lines.isEmpty()){
            throw new IllegalArgumentException("invoice must have at least one line");
        }

        Money total = lines.stream()
                .map(InvoiceLine::getLineTotal)
                .reduce(Money::add)
                .orElseThrow();

        return  new Invoice(
                InvoiceId.generate(),
                invoiceNumber,
                sellerVatNumber,
                buyerVatNumber,
                lines,
                total
        );
    }

    public void markAsValidated(){
        if (this.status != InvoiceStatus.RECEIVED){
            throw new IllegalStateException("only RECEIVED invoices can be validated");
        }
        this.status = InvoiceStatus.VALIDATED;
    }

    public void  markAsRejected(String reason){
        this.status = InvoiceStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void markAsWaitingApproval(){
        if (this.status != InvoiceStatus.VALIDATED){
            throw new IllegalStateException("only VALIDATED invoices can be waiting approval");
        }
        this.status = InvoiceStatus.WAITING_APPROVAL;
    }

    public void markAsApproved(){
        if (this.status != InvoiceStatus.WAITING_APPROVAL){
            throw new IllegalStateException("only invoices waiting for approval can be approved");
        }
        this.status = InvoiceStatus.APPROVED;
    }

    public InvoiceId getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getSellerVatNumber() {
        return sellerVatNumber;
    }

    public String getBuyerVatNumber() {
        return buyerVatNumber;
    }

    public List<InvoiceLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }
}
