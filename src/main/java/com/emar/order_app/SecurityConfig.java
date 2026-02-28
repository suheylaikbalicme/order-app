package com.emar.order_app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/styles.css",
                                "/app.js",
                                "/orders.js",
                                "/offers.js",
                                "/customers-import.js",
                                "/css/**",
                                "/js/**",
                                "/img/logo.png",
                                "/favicon.ico"
                        ).permitAll()

                        // admin ekranları
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ---- ORDERS (Pages) ----
                        // Not: /orders/new ve /orders/{id}/edit, /orders/* wildcard'ına takılmasın diye ÜSTTE.
                        .requestMatchers(HttpMethod.GET, "/orders/new").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/orders/*/edit").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/orders/*/cancel").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/orders/*/submit").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/orders/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/orders/*/sync").hasRole("ADMIN")

                        // Liste + detay: Viewer da görebilir (ister)
                        .requestMatchers(HttpMethod.GET, "/orders", "/orders/*").hasAnyRole("ADMIN", "USER", "VIEWER")

                        // ---- CUSTOMERS (Pages) ----
                        .requestMatchers(HttpMethod.GET, "/customers/new").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/customers/import").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/customers", "/customers/*").hasAnyRole("ADMIN", "USER", "VIEWER")
                        .requestMatchers(HttpMethod.GET, "/customers/*/edit").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/customers/*/files/*/download").hasAnyRole("ADMIN", "USER", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/customers/*/interactions", "/customers/*/files", "/customers/*/edit", "/customers/*/files/*/delete").hasAnyRole("ADMIN", "USER")

                        // ---- OFFERS (Pages) ----
                        .requestMatchers(HttpMethod.GET, "/offers/new").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/offers", "/offers/*").hasAnyRole("ADMIN", "USER", "VIEWER")

                        // Offer workflow
                        .requestMatchers(HttpMethod.POST, "/offers/*/submit").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/offers/*/approve", "/offers/*/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/offers/*/convert-to-order").hasAnyRole("ADMIN", "USER")

                        // ---- API ----
                        // dropdown veri kaynakları (cari + stok) -> görüntüleme rolleri de görebilir
                        .requestMatchers(HttpMethod.GET, "/api/arps/**", "/api/items/**").hasAnyRole("ADMIN", "USER", "VIEWER")

                        // customers api: listeleme (view), create/update (admin/user)
                        .requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole("ADMIN", "USER", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/customers/**").hasAnyRole("ADMIN", "USER")

                        // orders api: görüntüleme (view), create/update/cancel (admin/user)
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("ADMIN", "USER", "VIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasAnyRole("ADMIN", "USER")

                        // offers api
                        .requestMatchers(HttpMethod.POST, "/api/offers/**").hasAnyRole("ADMIN", "USER")

                        // geri kalan her şey login ister
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                );

        return http.build();
    }
}
