package com.emar.order_app.fx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FxRateService {

    private static final String SOURCE_NAME = "frankfurter.app";
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public FxRateService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.frankfurter.app")
                .build();
        this.objectMapper = objectMapper;
    }

    public FxRateResponse getRate(String base, String currency) {
        String b = norm(base);
        String c = norm(currency);

        if (b.isBlank() || c.isBlank()) {
            throw new IllegalArgumentException("base ve currency zorunludur");
        }
        if (b.equals(c)) {
            return new FxRateResponse(b, c, BigDecimal.ONE, null, SOURCE_NAME);
        }

        String key = b + "->" + c;
        CacheEntry cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }

        try {
            String json = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/latest")
                            .queryParam("from", b)
                            .queryParam("to", c)
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(json);
            String date = root.path("date").asText(null);
            JsonNode rates = root.path("rates");
            JsonNode node = rates.path(c);
            if (node.isMissingNode() || node.isNull()) {
                throw new IllegalStateException("Kur bulunamadı: " + key);
            }
            BigDecimal rate = node.decimalValue();

            FxRateResponse resp = new FxRateResponse(b, c, rate, date, SOURCE_NAME);
            cache.put(key, new CacheEntry(resp, Instant.now().plus(CACHE_TTL)));
            return resp;

        } catch (RestClientException ex) {
            throw new IllegalStateException("Kur servisine erişilemedi (" + SOURCE_NAME + ")", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Kur cevabı parse edilemedi", ex);
        }
    }

    private static String norm(String s) {
        return (s == null ? "" : s.trim().toUpperCase(Locale.ROOT));
    }

    private record CacheEntry(FxRateResponse value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
