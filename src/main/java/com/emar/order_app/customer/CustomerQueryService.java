package com.emar.order_app.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CustomerQueryService {

    private final CustomerRepository customerRepository;

    public CustomerQueryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public List<CustomerEntity> listFor(Authentication auth) {

        return customerRepository.findAllByOrderByIdDesc();
    }

    public List<CustomerEntity> findAll() {
        return customerRepository.findAllByOrderByIdDesc();
    }

    public Optional<CustomerEntity> findByCode(String customerCode) {
        if (customerCode == null || customerCode.isBlank()) return Optional.empty();
        return customerRepository.findByCustomerCode(customerCode.trim());
    }
}
