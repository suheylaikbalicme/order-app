package com.emar.order_app.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.emar.order_app.sync.SyncStatus;


import com.emar.order_app.auth.UserEntity;
import com.emar.order_app.auth.UserRepository;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CustomerEntity create(String code, String name, String phone, String email, String address, String notes, String username) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("customerCode zorunlu");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("customerName zorunlu");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("phone zorunlu");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email zorunlu");
        if (address == null || address.isBlank()) throw new IllegalArgumentException("address zorunlu");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username zorunlu");

        UserEntity createdBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        CustomerEntity c = new CustomerEntity();
        c.setCustomerCode(code.trim());
        c.setCustomerName(name.trim());
        c.setPhone(phone.trim());
        c.setEmail(email.trim());
        c.setAddress(address.trim());
        c.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        c.setSyncStatus(SyncStatus.PENDING);
        c.setCreatedBy(createdBy);
        c.setCreatedByUsername(username);

        return customerRepository.save(c);
    }

    public CustomerEntity getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
    }

    @Transactional
    public CustomerEntity update(Long id,
                                 String code,
                                 String name,
                                 String phone,
                                 String email,
                                 String address,
                                 String notes,
                                 String username) {
        if (id == null) throw new IllegalArgumentException("id zorunlu");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("customerCode zorunlu");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("customerName zorunlu");
        if (phone == null || phone.isBlank()) throw new IllegalArgumentException("phone zorunlu");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email zorunlu");
        if (address == null || address.isBlank()) throw new IllegalArgumentException("address zorunlu");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username zorunlu");

        CustomerEntity c = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        c.setCustomerCode(code.trim());
        c.setCustomerName(name.trim());
        c.setPhone(phone.trim());
        c.setEmail(email.trim());
        c.setAddress(address.trim());
        c.setNotes(notes == null || notes.isBlank() ? null : notes.trim());

        c.setSyncStatus(SyncStatus.PENDING);
        c.setSyncError(null);
        c.setCreatedByUsername(username);

        return customerRepository.save(c);
    }
}
