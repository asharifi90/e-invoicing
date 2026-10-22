package com.einvoicing.invoice.application.service;

import com.einvoicing.invoice.application.port.in.ReceiveInvoiceUseCase;
import com.einvoicing.invoice.application.port.out.IdempotencyStore;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.application.port.out.OutboxWriter;
import com.einvoicing.invoice.domain.event.InvoiceReceivedEvent;
import com.einvoicing.invoice.domain.exception.IdempotencyKeyConflictException;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class ReceiveInvoiceService implements ReceiveInvoiceUseCase {

    private static final Logger log =  LoggerFactory.getLogger(ReceiveInvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final OutboxWriter outboxWriter;
    private final IdempotencyStore idempotencyStore;

    public ReceiveInvoiceService(InvoiceRepository invoiceRepository,
                                 OutboxWriter outboxWriter,
                                 IdempotencyStore idempotencyStore) {
        this.invoiceRepository = invoiceRepository;
        this.outboxWriter = outboxWriter;
        this.idempotencyStore = idempotencyStore;
    }

    /**
     * Persists the invoice and enqueues InvoiceReceivedEvent via the transactional outbox.
     * Kafka publish is done asynchronously by OutboxPublisherJob (reliable delivery).
     * Previously: domainEventPublisher.publish(...) right after save (dual-write risk).
     */
    @Override
    @Transactional
    public Invoice receive(ReceiveInvoiceCommand command) {
        return receive(command, null).invoice();
    }

    @Override
    @Transactional
    public ReceiveResult receive(ReceiveInvoiceCommand command, String idempotencyKey) {

        log.info("Receiving invoice with number={}", command.invoiceNumber());

        String requestHash = requestHash(command);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyStore.IdempotencyRequest> existingRequest =
                    idempotencyStore.findByKey(idempotencyKey);
            if (existingRequest.isPresent()) {
                if (!existingRequest.get().requestHash().equals(requestHash)) {
                    throw new IdempotencyKeyConflictException(idempotencyKey);
                }
                Invoice existingInvoice = invoiceRepository.findById(existingRequest.get().invoiceId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Idempotency key references missing invoice: " + idempotencyKey));
                return new ReceiveResult(existingInvoice, false);
            }
        }

        Optional<Invoice> duplicateInvoice =
                invoiceRepository.findByInvoiceNumber(command.invoiceNumber());
        if (duplicateInvoice.isPresent()) {
            Invoice existingInvoice = duplicateInvoice.get();
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyStore.save(idempotencyKey, requestHash, existingInvoice.getId());
            }
            log.info("Returning existing invoice with number={}", existingInvoice.getInvoiceNumber());
            return new ReceiveResult(existingInvoice, false);
        }

        Invoice invoice = Invoice.create(
                command.invoiceNumber(),
                command.sellerVatNumber(),
                command.buyerVatNumber(),
                command.lines()
        );

        Invoice saved = invoiceRepository.save(invoice);

        log.info("Invoice saved successfully. id={}, number={}",
                saved.getId().value(), saved.getInvoiceNumber());

        outboxWriter.enqueue(
                "Invoice",
                invoice.getId().toString(),
                "InvoiceReceived",
                new InvoiceReceivedEvent(saved.getId().value(), saved.getInvoiceNumber(),
                        saved.getTotalAmount().amount(), saved.getTotalAmount().currency())
        );

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyStore.save(idempotencyKey, requestHash, saved.getId());
        }

        log.info("InvoiceReceivedEvent enqueued for invoiceId={}", saved.getId().value());
        return new ReceiveResult(saved, true);
    }

    static String requestHash(ReceiveInvoiceCommand command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateDigest(digest, command.invoiceNumber());
            updateDigest(digest, command.sellerVatNumber());
            updateDigest(digest, command.buyerVatNumber());
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(command.lines().size()).array());
            command.lines().forEach(line -> {
                updateDigest(digest, line.getDescription());
                updateDigest(digest, Integer.toString(line.getQuantity()));
                updateDigest(digest, line.getUnitPrice().amount().stripTrailingZeros().toPlainString());
                updateDigest(digest, line.getUnitPrice().currency());
            });
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static void updateDigest(MessageDigest digest, String value) {
        if (value == null) {
            digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
