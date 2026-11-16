package com.einvoicing.invoice.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class TracingKafkaSupport {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TracingKafkaSupport.class);

    private final Tracer tracer;
    private final Propagator propagator;

    public TracingKafkaSupport(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }

    /** Call only while a span is active (e.g. inside receive). */
    public String currentTraceParent() {
        Span current = tracer.currentSpan();
        log.info("currentSpan={}", current);
        if (current == null) {
            return null;
        }

        var ctx = current.context();
        String traceId = ctx.traceId();
        String spanId = ctx.spanId();
        if (traceId == null || traceId.isBlank() || spanId == null || spanId.isBlank()) {
            return null;
        }

        // W3C: version - traceid - spanid - flags (01 = sampled)
        String sampled = Boolean.TRUE.equals(ctx.sampled()) ? "01" : "00";
        String traceparent = "00-" + traceId + "-" + spanId + "-" + sampled;
        log.info("traceParent={}", traceparent);
        return traceparent;
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