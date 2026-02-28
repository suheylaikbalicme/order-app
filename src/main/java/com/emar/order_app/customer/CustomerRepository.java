package com.emar.order_app.customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.emar.order_app.sync.SyncStatus;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    java.util.Optional<CustomerEntity> findByCustomerCode(String customerCode);

    java.util.Optional<CustomerEntity> findFirstByCustomerCodeIgnoreCase(String customerCode);

    java.util.Optional<CustomerEntity> findFirstByLogoRefIgnoreCase(String logoRef);


    long countBySyncStatus(SyncStatus status);

    java.util.List<CustomerEntity> findTop10BySyncStatusOrderByIdDesc(SyncStatus status);

    List<CustomerEntity> findAllByOrderByIdDesc();

    List<CustomerEntity> findAllByCreatedByUsernameOrderByIdDesc(String username);

    List<CustomerEntity> findTop50BySyncStatusOrderByIdAsc(SyncStatus syncStatus);

    java.util.Optional<CustomerEntity> findTop1ByLastSyncAtIsNotNullOrderByLastSyncAtDesc();
}
