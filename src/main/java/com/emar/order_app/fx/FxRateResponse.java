package com.emar.order_app.fx;

import java.math.BigDecimal;

public record FxRateResponse(
        String base,
        String currency,
        BigDecimal rate,
        String date,
        String source
) {}
