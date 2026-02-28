package com.emar.order_app.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import com.emar.order_app.logo.LogoApiService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final LogoApiService logoApiService;

    private final boolean logoEnabled;

    public ApiController(
            LogoApiService logoApiService,
            @Value("${logo.enabled:true}") boolean logoEnabled
    ) {
        this.logoApiService = logoApiService;
        this.logoEnabled = logoEnabled;
    }

    @GetMapping(value = "/arps", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> arps(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "Code") String sort
    ) {
        if (!logoEnabled) {
            return ResponseEntity.ok(mockArpsJson());
        }

        try {
            String body = logoApiService.getArps(offset, limit, sort);
            return ResponseEntity.ok(body);

        } catch (RestClientResponseException ex) {
            String loc = ex.getResponseHeaders() != null
                    ? ex.getResponseHeaders().getFirst("Location")
                    : null;

            return ResponseEntity
                    .status(ex.getStatusCode())
                    .header("X-Upstream-Location", loc != null ? loc : "N/A")
                    .body(ex.getResponseBodyAsString());

        } catch (Exception ex) {
            return ResponseEntity
                    .status(500)
                    .body("API error: " + ex.getMessage());
        }
    }

    @GetMapping(value = "/items", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> items(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "Code") String sort
    ) {
        if (!logoEnabled) {
            return ResponseEntity.ok(mockItemsJson());
        }

        try {
            String body = logoApiService.getItems(offset, limit, sort);
            return ResponseEntity.ok(body);

        } catch (RestClientResponseException ex) {
            String loc = ex.getResponseHeaders() != null
                    ? ex.getResponseHeaders().getFirst("Location")
                    : null;

            return ResponseEntity
                    .status(ex.getStatusCode())
                    .header("X-Upstream-Location", loc != null ? loc : "N/A")
                    .body(ex.getResponseBodyAsString());

        } catch (Exception ex) {
            return ResponseEntity
                    .status(500)
                    .body("API error: " + ex.getMessage());
        }
    }

    private String mockArpsJson() {
        return """
                [
                  {"Code":"CARI001","Name":"Mock Müşteri 1"},
                  {"Code":"CARI002","Name":"Mock Müşteri 2"},
                  {"Code":"CARI003","Name":"Mock Müşteri 3"}
                ]
                """;
    }

    private String mockItemsJson() {
        return """
                [
                  {"Code":"STK001","Name":"Mock Stok 1"},
                  {"Code":"STK002","Name":"Mock Stok 2"},
                  {"Code":"STK003","Name":"Mock Stok 3"}
                ]
                """;
    }
}
