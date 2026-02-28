package com.emar.order_app.offer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfferRepository extends JpaRepository<OfferEntity, Long> {

    @Query("""
           select distinct o.customerCode
           from OfferEntity o
           where o.customerCode is not null and o.customerCode <> ''
           """)
    List<String> findDistinctCustomerCodes();

    @Query("""
           select distinct o.customerCode
           from OfferEntity o
           where o.customerCode is not null and o.customerCode <> ''
             and o.createdBy.username = :username
           """)
    List<String> findDistinctCustomerCodesByCreatedByUsername(@Param("username") String username);

    @Query("""
           select o from OfferEntity o
           order by o.id desc
           """)
    List<OfferEntity> findAllByOrderByIdDesc();

    @Query("""
           select o from OfferEntity o
           where o.createdBy.username = :username
           order by o.id desc
           """)
    List<OfferEntity> findAllByCreatedByUsernameOrderByIdDesc(@Param("username") String username);

    @Query("""
           select o from OfferEntity o
           where o.customerCode = :customerCode
           order by o.id desc
           """)
    List<OfferEntity> findAllByCustomerCodeOrderByIdDesc(@Param("customerCode") String customerCode);

    @Query("""
           select o from OfferEntity o
           where o.customerCode = :customerCode
             and o.createdBy.username = :username
           order by o.id desc
           """)
    List<OfferEntity> findAllByCustomerCodeAndCreatedByUsernameOrderByIdDesc(
            @Param("customerCode") String customerCode,
            @Param("username") String username
    );

    @Query("""
           select o from OfferEntity o
           left join fetch o.items
           where o.id = :id
           """)
    Optional<OfferEntity> findByIdWithItems(@Param("id") Long id);

    @Query("""
           select o from OfferEntity o
           left join fetch o.items
           where o.id = :id
             and o.createdBy.username = :username
           """)
    Optional<OfferEntity> findByIdWithItemsAndCreatedByUsername(
            @Param("id") Long id,
            @Param("username") String username
    );
}
