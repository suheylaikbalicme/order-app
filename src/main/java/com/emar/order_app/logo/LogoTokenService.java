package com.emar.order_app.logo;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LogoTokenService {

    private final RestClient restClient;
    private final LogoProperties props;

    private final AtomicReference<String> cachedToken = new AtomicReference<>(null);
    private volatile Instant expiresAt = Instant.EPOCH;
    private final Object lock = new Object();

    public LogoTokenService(RestClient.Builder restClientBuilder, LogoProperties props) {
        this.restClient = restClientBuilder.build();
        this.props = props;
    }

    public String getAccessToken() {
        // token varsa ve bitmesine 30 sn'den fazla varsa onu kullan
        if (cachedToken.get() != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
            return cachedToken.get();
        }

        synchronized (lock) {
            if (cachedToken.get() != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
                return cachedToken.get();
            }

            LogoProperties.Idm idm = props.getIdm();

            URI uri = UriComponentsBuilder
                    .fromUriString(idm.getTokenUrl())
                    .queryParam("grant_type", "password")
                    .queryParam("username", idm.getUsername())
                    .queryParam("password", idm.getPassword())
                    // bazı ortamlarda clientId, bazılarında client_id bekliyor diye ikisini de gönderiyoruz
                    .queryParam("clientId", idm.getClientId())
                    .queryParam("client_id", idm.getClientId())
                    .queryParam("client_secret", idm.getClientSecret())
                    .build(true)
                    .toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> json = restClient.post()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            if (json == null || !json.containsKey("access_token")) {
                throw new IllegalStateException("Token response içinde access_token yok: " + json);
            }

            String token = String.valueOf(json.get("access_token"));

            long expiresIn = 3600;
            Object exp = json.get("expires_in");
            if (exp != null) {
                try {
                    expiresIn = Long.parseLong(String.valueOf(exp));
                } catch (NumberFormatException ignored) {}
            }

            cachedToken.set(token);
            expiresAt = Instant.now().plusSeconds(expiresIn);
            return token;
        }
    }
}
