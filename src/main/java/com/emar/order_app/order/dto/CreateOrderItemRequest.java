package com.emar.order_app.order.dto;

import java.math.BigDecimal;

public record CreateOrderItemRequest(
        String itemCode,
        String itemName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountRate,
        BigDecimal vatRate
) {}
