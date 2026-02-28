package com.emar.order_app.customer;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerApiController {

    private final CustomerService customerService;

    public CustomerApiController(CustomerService customerService) {
        this.customerService = customerService;
    }

    public record CreateCustomerRequest(
            String customerCode,
            String customerName,
            String phone,
            String email,
            String address,
            String notes
    ) {}

    public record CreateCustomerResponse(Long id, String syncStatus) {}

    public record CustomerDetailResponse(
            Long id,
            String customerCode,
            String customerName,
            String phone,
            String email,
            String address,
            String notes,
            String syncStatus
    ) {}

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateCustomerResponse> create(@RequestBody CreateCustomerRequest req, Authentication auth) {
        String username = auth.getName();
        CustomerEntity saved = customerService.create(
                req.customerCode(),
                req.customerName(),
                req.phone(),
                req.email(),
                req.address(),
                req.notes(),
                username
        );
        return ResponseEntity.ok(new CreateCustomerResponse(saved.getId(), saved.getSyncStatus().name()));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomerDetailResponse> getOne(@PathVariable Long id) {
        CustomerEntity c = customerService.getById(id);
        return ResponseEntity.ok(new CustomerDetailResponse(
                c.getId(),
                c.getCustomerCode(),
                c.getCustomerName(),
                c.getPhone(),
                c.getEmail(),
                c.getAddress(),
                c.getNotes(),
                c.getSyncStatus().name()
        ));
    }
}
