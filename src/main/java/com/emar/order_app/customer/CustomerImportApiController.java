package com.emar.order_app.customer;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emar.order_app.sync.SyncStatus;

@RestController
@RequestMapping("/api/customers")
public class CustomerImportApiController {

    private final CustomerRepository customerRepository;

    public CustomerImportApiController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping(
            value = "/import-from-logo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> importFromLogo(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            String customerCode = firstNonBlank(
                    asString(body.get("customerCode")),
                    asString(body.get("code")),
                    asString(body.get("Code"))
            );

            if (isBlank(customerCode)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Validation failed",
                        "message", "customerCode boş olamaz"
                ));
            }

            String customerName = firstNonBlank(
                    asString(body.get("customerName")),
                    asString(body.get("name")),
                    asString(body.get("title")),
                    asString(body.get("Title")),
                    customerCode // fallback
            );

            String logoRef = firstNonBlank(
                    asString(body.get("logoRef")),
                    asString(body.get("LogoRef")),
                    customerCode
            );

            Optional<CustomerEntity> existing = Optional.empty();

            if (!isBlank(logoRef)) {
                existing = customerRepository.findFirstByLogoRefIgnoreCase(logoRef);
            }
            if (existing.isEmpty()) {
                existing = customerRepository.findFirstByCustomerCodeIgnoreCase(customerCode);
            }

            CustomerEntity customer = existing.orElseGet(CustomerEntity::new);

            customer.setCustomerCode(customerCode);
            customer.setCustomerName(customerName);
            customer.setLogoRef(logoRef);
            customer.setSyncStatus(SyncStatus.SYNCED);

            if (hasGetter(customer, "getPhone") && isBlank(customer.getPhone())) {
                customer.setPhone("");
            }
            if (hasGetter(customer, "getEmail") && isBlank(customer.getEmail())) {
                customer.setEmail("");
            }
            if (hasGetter(customer, "getAddress") && isBlank(customer.getAddress())) {
                customer.setAddress("");
            }

            CustomerEntity saved = customerRepository.save(customer);

            return ResponseEntity.ok(Map.of(
                    "customerId", saved.getId(),
                    "customerCode", saved.getCustomerCode(),
                    "customerName", saved.getCustomerName(),
                    "logoRef", saved.getLogoRef(),
                    "syncStatus", saved.getSyncStatus().name()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Import failed",
                    "message", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()
            ));
        }
    }


    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (!isBlank(v)) return v.trim();
        }
        return null;
    }


    private static boolean hasGetter(Object obj, String methodName) {
        try {
            obj.getClass().getMethod(methodName);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
