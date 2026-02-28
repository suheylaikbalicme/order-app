package com.emar.order_app.customer;

public record CustomerImportRequest(
        String customerCode,
        String customerName,
        String phone,
        String email,
        String address,
        String logoRef
) {}
