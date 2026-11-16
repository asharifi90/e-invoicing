package com.einvoicing.validation.config;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.micrometer.tracing.propagation.Propagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Value("${spring.application.name:validation-invoice}")
    private String serviceName;

    @Value("${management.otlp.tracing.endpoint:http://localhost:4318/v1/traces}")
    private String otlpEndpoint;

    @Bean(destroyMethod = "close")
    public SdkTracerProvider sdkTracerProvider() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ServiceAttributes.SERVICE_NAME, serviceName
                )));

        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        return SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build();
    }

    @Bean(destroyMethod = "close")
    public OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }

    @Bean
    public Tracer micrometerTracer(OpenTelemetry openTelemetry) {
        var otelTracer = openTelemetry.getTracer(serviceName);
        var context = new OtelCurrentTraceContext();
        return new OtelTracer(
                otelTracer,
                context,
                event -> {
                }
        );
    }

    @Bean
    Propagator propagator(OpenTelemetry openTelemetry) {
        return new OtelPropagator(
                openTelemetry.getPropagators(),
                openTelemetry.getTracerProvider().get("io.micrometer.micrometer-tracing")
        );
    }
}

