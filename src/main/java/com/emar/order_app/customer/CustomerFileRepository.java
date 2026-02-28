package com.emar.order_app.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerFileRepository extends JpaRepository<CustomerFileEntity, Long> {

    List<CustomerFileEntity> findByCustomerIdOrderByUploadedAtDescIdDesc(Long customerId);

    Optional<CustomerFileEntity> findByIdAndCustomerId(Long id, Long customerId);
}
