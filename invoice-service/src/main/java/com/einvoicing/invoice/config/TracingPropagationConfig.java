package com.einvoicing.invoice.config;

import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.OpenTelemetry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingPropagationConfig {

    @Bean
    Propagator propagator(OpenTelemetry openTelemetry) {
        return new OtelPropagator(
                openTelemetry.getPropagators(),
                openTelemetry.getTracerProvider().get("io.micrometer.micrometer-tracing")
        );
    }
}
