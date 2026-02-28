package com.emar.order_app.fx;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/fx", produces = MediaType.APPLICATION_JSON_VALUE)
public class FxRateController {

    private final FxRateService fxRateService;

    public FxRateController(FxRateService fxRateService) {
        this.fxRateService = fxRateService;
    }

    @GetMapping("/rate")
    public FxRateResponse rate(
            @RequestParam(defaultValue = "TRY") String base,
            @RequestParam String currency
    ) {
        return fxRateService.getRate(base, currency);
    }
}
