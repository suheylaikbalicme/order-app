package com.emar.order_app.logo;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class LogoApiService {

    private final RestClient restClient;
    private final LogoTokenService tokenService;
    private final LogoProperties props;

    public LogoApiService(
            RestClient.Builder restClientBuilder,
            LogoTokenService tokenService,
            LogoProperties props
    ) {
        this.restClient = restClientBuilder.build();
        this.tokenService = tokenService;
        this.props = props;
    }

    public String getArps(int offset, int limit, String sort) {
        return getList("arps", offset, limit, sort);
    }

    public String getItems(int offset, int limit, String sort) {
        return getList("items", offset, limit, sort);
    }

    private String getList(String resource, int offset, int limit, String sort) {
        if (!props.isEnabled()) {
            throw new IllegalStateException("Logo integration is disabled (logo.enabled=false).");
        }

        URI uri = buildListUri(resource, offset, limit, sort);

        try {
            ResponseEntity<String> resp = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("access-token", tokenService.getAccessToken())
                    .header("firm", props.getFirm())
                    .header("TenantId", props.getTenantId())
                    .retrieve()
                    .toEntity(String.class);

            // 1) Redirect gelirse (308/302 vs) bunu “başarılı” sanmayalım
            if (resp.getStatusCode().is3xxRedirection()) {
                throw new IllegalStateException("Logo upstream redirect returned: " + resp.getStatusCode()
                        + " for uri=" + uri);
            }

            String body = resp.getBody() == null ? "" : resp.getBody();

            String trimmed = body.trim();
            if (trimmed.startsWith("<!DOCTYPE html") || trimmed.startsWith("<html") || trimmed.contains("<center>nginx</center>")) {
                throw new IllegalStateException("Logo API returned HTML (likely redirect/proxy page) for uri=" + uri
                        + ". Body starts with: " + trimmed.substring(0, Math.min(120, trimmed.length())));
            }

            return body;

        } catch (RestClientResponseException ex) {
            throw ex;
        }
    }

    private URI buildListUri(String resource, int offset, int limit, String sort) {
        String apiGateway = trimTrailingSlash(props.getApiGateway());
        String instance = trimSlashes(props.getInstance());

        return UriComponentsBuilder
                .fromUriString(apiGateway)
                .pathSegment(instance, "logo", "restservices", "rest", "v2.0", resource, "list")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .queryParam("sort", sort)
                .build(true)
                .toUri();
    }

    private String trimTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private String trimSlashes(String s) {
        if (s == null) return "";
        String x = s;
        while (x.startsWith("/")) x = x.substring(1);
        while (x.endsWith("/")) x = x.substring(0, x.length() - 1);
        return x;
    }
}
