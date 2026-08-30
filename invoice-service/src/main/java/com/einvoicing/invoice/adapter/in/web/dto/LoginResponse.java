package com.einvoicing.invoice.adapter.in.web.dto;

public record LoginResponse(
        String token, String type
) {
    public static LoginResponse bearer(String token) {
        return new LoginResponse(token, "Bearer");
    }
}
