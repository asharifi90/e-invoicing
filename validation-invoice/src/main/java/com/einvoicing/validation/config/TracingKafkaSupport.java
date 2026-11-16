package com.einvoicing.validation.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class TracingKafkaSupport {

    private final Tracer tracer;
    private final Propagator propagator;

    public TracingKafkaSupport(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    /** Call only while a span is active (e.g. inside receive). */
    public String currentTraceParent() {
        Span current = tracer.currentSpan();
        if (current == null) {
            return null;
        }
        Map<String, String> carrier = new HashMap<>();
        propagator.inject(current.context(), carrier, Map::put);
        return carrier.get("traceParent");
    }

    public void putTraceParent(ProducerRecord<String, String> record, String traceParent) {
        if (traceParent == null || traceParent.isBlank()) {
            return;
        }
        record.headers().remove("traceParent");
        record.headers().add("traceParent", traceParent.getBytes(StandardCharsets.UTF_8));
    }

    public Span startConsumerSpan(String name, ConsumerRecord<?, ?> record) {
        Span.Builder extracted = propagator.extract(
                record.headers(),
                (headers, key) -> {
                    Header h = headers.lastHeader(key);
                    return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
                });
        if (extracted != null) {
            return extracted.name(name).start();
        }
        return tracer.nextSpan().name(name).start();
    }
}
