package com.einvoicing.invoice.adapter.in.web;


import com.einvoicing.invoice.AbstractIntegrationTest;
import com.einvoicing.invoice.adapter.out.persistence.repository.OutboxInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
class ReceiveInvoiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutboxInvoiceRepository outboxRepository;

    private static final String ENDPOINT = "/api/invoices";

    @BeforeEach
    void cleanUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void receiveInvoice_shouldCreateOnce_replayOnSameKey_andReturn409OnMismatch() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8);

        String requestBody = """
                {
                  "invoiceNumber": "%s",
                  "sellerVatNumber": "BE0123456789",
                  "buyerVatNumber": "NL001234567B01",
                  "lines": [
                    {
                      "description": "Consulting",
                      "quantity": 2,
                      "unitPrice": 15.00,
                      "currency": "EUR"
                    }
                  ]
                }
                """.formatted(invoiceNumber);

        mockMvc.perform(post(ENDPOINT)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceNumber").value(invoiceNumber));

        assertThat(outboxRepository.count()).isEqualTo(1);

        mockMvc.perform(post(ENDPOINT)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value(invoiceNumber));

        assertThat(outboxRepository.count()).isEqualTo(1);

        String differentBody = requestBody.replace("\"quantity\": 2", "\"quantity\": 5");

        mockMvc.perform(post(ENDPOINT)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(differentBody))
                .andExpect(status().isConflict());
    }

    @Test
    void receiveInvoice_shouldHandleConcurrentRequestsWithSameIdempotencyKey() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8);

        String requestBody = """
                {
                  "invoiceNumber": "%s",
                  "sellerVatNumber": "BE0123456789",
                  "buyerVatNumber": "NL001234567B01",
                  "lines": [
                    {
                      "description": "Consulting",
                      "quantity": 2,
                      "unitPrice": 15.00,
                      "currency": "EUR"
                    }
                  ]
                }
                """.formatted(invoiceNumber);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Integer> sendRequest = () -> {
            readyLatch.countDown();
            startLatch.await();
            return mockMvc.perform(post(ENDPOINT)
                            .header("Idempotency-Key", idempotencyKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        try {
            Future<Integer> future1 = executor.submit(sendRequest);
            Future<Integer> future2 = executor.submit(sendRequest);

            readyLatch.await();
            startLatch.countDown();

            List<Integer> statuses = List.of(future1.get(), future2.get());


            assertThat(statuses).containsExactlyInAnyOrder(201, 200);
            assertThat(outboxRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
