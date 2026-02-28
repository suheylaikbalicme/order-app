package com.emar.order_app;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(Redirect.ALWAYS)
                .build();

        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }


    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
