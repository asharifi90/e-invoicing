package com.einvoicing.invoice.adapter.out.persistence;


import com.einvoicing.invoice.adapter.out.persistence.entity.InvoiceJpaEntity;
import com.einvoicing.invoice.adapter.out.persistence.mapper.InvoiceMapper;
import com.einvoicing.invoice.adapter.out.persistence.repository.SpringDataInvoiceRepository;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.model.aggregate.Invoice;
import com.einvoicing.invoice.domain.model.valueObject.InvoiceId;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class InvoiceRepositoryAdapter implements InvoiceRepository {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceRepositoryAdapter.class);

    private final SpringDataInvoiceRepository springDataRepository;
    private final InvoiceMapper mapper;
    private final Tracer tracer;

    public InvoiceRepositoryAdapter(SpringDataInvoiceRepository springDataRepository,
                                    InvoiceMapper mapper,
                                    Tracer tracer) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
        this.tracer = tracer;
    }

    @Override
    public Invoice save(Invoice invoice) {
        Span span = tracer.nextSpan().name("invoice.save").start();
        try(Tracer.SpanInScope ws = tracer.withSpan(span)) {
            LOG.info("invoice.save span started for invoiceId={}", invoice.getId().value());
            InvoiceJpaEntity entity = mapper.toJpaEntity(invoice);
            InvoiceJpaEntity saved = springDataRepository.save(entity);
            return mapper.toDomain(saved);
        }catch (RuntimeException e) {
            span.error(e);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        return springDataRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        return springDataRepository.findByInvoiceNumber(invoiceNumber)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByInvoiceNumber(String invoiceNumber) {
        return springDataRepository.existsByInvoiceNumber(invoiceNumber);
    }
}