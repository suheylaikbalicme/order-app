package com.emar.order_app.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CustomerInteractionService {

    private final CustomerRepository customerRepository;
    private final CustomerInteractionRepository interactionRepository;

    public CustomerInteractionService(CustomerRepository customerRepository, CustomerInteractionRepository interactionRepository) {
        this.customerRepository = customerRepository;
        this.interactionRepository = interactionRepository;
    }

    public List<CustomerInteractionEntity> listForCustomer(Long customerId) {
        return interactionRepository.findByCustomerIdOrderByInteractionDateDescIdDesc(customerId);
    }

    public List<CustomerInteractionEntity> listForCustomerFiltered(Long customerId, String type, LocalDate from, LocalDate to) {
        String t = (type == null || type.isBlank()) ? null : type.trim().toUpperCase();
        return interactionRepository.findFiltered(customerId, t, from, to);
    }

    @Transactional
    public CustomerInteractionEntity addInteraction(Long customerId,
                                                    LocalDate date,
                                                    String type,
                                                    String title,
                                                    String description,
                                                    String createdByUsername) {
        if (customerId == null) throw new IllegalArgumentException("customerId zorunlu");
        if (date == null) throw new IllegalArgumentException("interactionDate zorunlu");
        if (type == null || type.isBlank()) throw new IllegalArgumentException("interactionType zorunlu");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title zorunlu");

        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        CustomerInteractionEntity e = new CustomerInteractionEntity();
        e.setCustomer(customer);
        e.setInteractionDate(date);
        e.setInteractionType(type.trim().toUpperCase());
        e.setTitle(title.trim());
        e.setDescription(description == null ? null : description.trim());
        e.setCreatedByUsername(createdByUsername);
        return interactionRepository.save(e);
    }

    @Transactional
    public CustomerInteractionEntity updateInteraction(Long customerId, Long interactionId, LocalDate date, String type, String title, String description) {
        CustomerInteractionEntity e = interactionRepository.findByIdAndCustomerId(interactionId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Interaction not found"));
        if (date != null) e.setInteractionDate(date);
        if (type != null && !type.isBlank()) e.setInteractionType(type.trim().toUpperCase());
        if (title != null && !title.isBlank()) e.setTitle(title.trim());
        e.setDescription(description == null ? null : description.trim());
        return interactionRepository.save(e);
    }

    @Transactional
    public void deleteInteraction(Long customerId, Long interactionId) {
        CustomerInteractionEntity e = interactionRepository.findByIdAndCustomerId(interactionId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Interaction not found"));
        interactionRepository.delete(e);
    }
}
