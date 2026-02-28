package com.emar.order_app.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CustomerInteractionRepository extends JpaRepository<CustomerInteractionEntity, Long> {

    List<CustomerInteractionEntity> findByCustomerIdOrderByInteractionDateDescIdDesc(Long customerId);

    List<CustomerInteractionEntity> findByCustomerIdAndInteractionDate(Long customerId, LocalDate interactionDate);

    @Query("""
           select i from CustomerInteractionEntity i
           where i.customer.id = :customerId
             and (:type is null or i.interactionType = :type)
             and (:from is null or i.interactionDate >= :from)
             and (:to is null or i.interactionDate <= :to)
           order by i.interactionDate desc, i.id desc
           """)
    List<CustomerInteractionEntity> findFiltered(
            @Param("customerId") Long customerId,
            @Param("type") String type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    java.util.Optional<CustomerInteractionEntity> findByIdAndCustomerId(Long id, Long customerId);
}
