package com.emar.order_app;

import java.net.URI;

import com.emar.order_app.logo.LogoProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Profile("dev")
@RestController
@RequestMapping("/logo")
public class LogoProxyController {

    private final RestClient restClient;
    private final LogoProperties props;

    public LogoProxyController(RestClient.Builder restClientBuilder, LogoProperties props) {
        this.restClient = restClientBuilder.build();
        this.props = props;
    }

    @GetMapping(value = "/arps", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getArps(
            @RequestHeader(value = "access-token", required = true) String accessToken,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "Code") String sort
    ) {
        String base = props.getApiGateway().endsWith("/")
                ? props.getApiGateway().substring(0, props.getApiGateway().length() - 1)
                : props.getApiGateway();

        URI uri = UriComponentsBuilder
                .fromUriString(base)
                .pathSegment(props.getInstance())
                .path("/logo/restservices/rest/v2.0/arps/list")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .queryParam("sort", sort)
                .build(true)
                .toUri();

        try {
            ResponseEntity<String> resp = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("access-token", accessToken)
                    .header("firm", props.getFirm())
                    .header("TenantId", props.getTenantId())
                    .retrieve()
                    .toEntity(String.class);

            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }

    @GetMapping(value = "/items", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getItems(
            @RequestHeader(value = "access-token", required = true) String accessToken,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "Code") String sort
    ) {
        String base = props.getApiGateway().endsWith("/")
                ? props.getApiGateway().substring(0, props.getApiGateway().length() - 1)
                : props.getApiGateway();

        URI uri = UriComponentsBuilder
                .fromUriString(base)
                .pathSegment(props.getInstance())
                .path("/logo/restservices/rest/v2.0/items/list")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .queryParam("sort", sort)
                .build(true)
                .toUri();

        try {
            ResponseEntity<String> resp = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("access-token", accessToken)
                    .header("firm", props.getFirm())
                    .header("TenantId", props.getTenantId())
                    .retrieve()
                    .toEntity(String.class);

            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());
        }
    }
}
