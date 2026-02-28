package com.emar.order_app.order;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import com.emar.order_app.sync.SyncStatus;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query("""
           select distinct o.customerCode
           from OrderEntity o
           where o.customerCode is not null and o.customerCode <> ''
           """)
    List<String> findDistinctCustomerCodes();

    @Query("""
           select distinct o.customerCode
           from OrderEntity o
           where o.customerCode is not null and o.customerCode <> ''
             and o.createdBy.username = :username
           """)
    List<String> findDistinctCustomerCodesByCreatedByUsername(@Param("username") String username);

    long countByStatus(OrderStatus status);

    long countBySyncStatus(SyncStatus status);

    java.util.List<OrderEntity> findTop10BySyncStatusOrderByIdDesc(SyncStatus status);

    @Query("""
           select o from OrderEntity o
           left join fetch o.items
           where o.id = :id
           """)
    Optional<OrderEntity> findByIdWithItems(@Param("id") Long id);

    // Admin değilse sadece kendi oluşturduklarını görebilsin
    @Query("""
           select o from OrderEntity o
           where o.createdBy.username = :username
           """)
    List<OrderEntity> findAllByCreatedByUsername(@Param("username") String username);

    @Query("""
           select o from OrderEntity o
           left join fetch o.items
           where o.id = :id
             and o.createdBy.username = :username
           """)
    Optional<OrderEntity> findByIdWithItemsAndCreatedByUsername(
            @Param("id") Long id,
            @Param("username") String username
    );


    @Query("""
           select distinct o from OrderEntity o
           left join fetch o.items
           where o.syncStatus = :status
           order by o.id asc
           """)
    List<OrderEntity> findPendingWithItems(@Param("status") SyncStatus status, Pageable pageable);

    @Query("""
       select o from OrderEntity o
       order by o.id desc
       """)
    List<OrderEntity> findAllByOrderByIdDesc();

    @Query("""
       select o from OrderEntity o
       where o.createdBy.username = :username
       order by o.id desc
       """)
    List<OrderEntity> findAllByCreatedByUsernameOrderByIdDesc(@Param("username") String username);

    @Query("""
       select o from OrderEntity o
       where o.customerCode = :customerCode
       order by o.id desc
       """)
    List<OrderEntity> findAllByCustomerCodeOrderByIdDesc(@Param("customerCode") String customerCode);

    @Query("""
       select o from OrderEntity o
       where o.customerCode = :customerCode
         and o.createdBy.username = :username
       order by o.id desc
       """)
    List<OrderEntity> findAllByCustomerCodeAndCreatedByUsernameOrderByIdDesc(@Param("customerCode") String customerCode,
                                                                             @Param("username") String username);

    Optional<OrderEntity> findTop1ByLastSyncAtIsNotNullOrderByLastSyncAtDesc();


}
