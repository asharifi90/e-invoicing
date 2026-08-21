package com.einvoicing.invoice.adapter.out.persistence;


import com.einvoicing.invoice.adapter.out.persistence.entity.InvoiceJpaEntity;
import com.einvoicing.invoice.adapter.out.persistence.mapper.InvoiceMapper;
import com.einvoicing.invoice.adapter.out.persistence.repository.SpringDataInvoiceRepository;
import com.einvoicing.invoice.application.port.out.InvoiceRepository;
import com.einvoicing.invoice.domain.Invoice;
import com.einvoicing.invoice.domain.InvoiceId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class InvoiceRepositoryAdapter implements InvoiceRepository {

    private final SpringDataInvoiceRepository springDataRepository;
    private final InvoiceMapper mapper;

    public InvoiceRepositoryAdapter(SpringDataInvoiceRepository springDataRepository,
                                    InvoiceMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity entity = mapper.toJpaEntity(invoice);
        InvoiceJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Invoice> findById(InvoiceId id) {
        return springDataRepository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByInvoiceNumber(String invoiceNumber) {
        return springDataRepository.existsByInvoiceNumber(invoiceNumber);
    }
}