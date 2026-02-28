package com.emar.order_app.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public record CreateOrderRequest(
        String customerCode,
        String customerName,

        // CRM header
        LocalDate orderDate,
        String currency,
        BigDecimal exchangeRate,
        String note,

        // Legacy header rates (kept for backward compatibility)
        BigDecimal discountRate,
        BigDecimal vatRate,

        // draft | submit | revise
        String action,

        // optional (stored in revision history when action=revise)
        String revisionReason,

        List<CreateOrderItemRequest> items
) {}
