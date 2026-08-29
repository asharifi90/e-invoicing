package com.einvoicing.validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class ValidationInvoiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationInvoiceApplication.class, args);
    }
}
