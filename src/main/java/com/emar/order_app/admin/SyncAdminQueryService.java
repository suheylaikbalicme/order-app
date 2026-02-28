package com.emar.order_app.admin;

import java.util.List;

import org.springframework.stereotype.Service;

import com.emar.order_app.customer.CustomerEntity;
import com.emar.order_app.customer.CustomerRepository;
import com.emar.order_app.order.OrderEntity;
import com.emar.order_app.order.OrderRepository;
import com.emar.order_app.sync.SyncStatus;

import java.time.OffsetDateTime;

@Service
public class SyncAdminQueryService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public SyncAdminQueryService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    public long pendingCustomers() { return customerRepository.countBySyncStatus(SyncStatus.PENDING); }
    public long failedCustomers()  { return customerRepository.countBySyncStatus(SyncStatus.FAILED); }
    public long syncedCustomers()  { return customerRepository.countBySyncStatus(SyncStatus.SYNCED); }

    public long pendingOrders() { return orderRepository.countBySyncStatus(SyncStatus.PENDING); }
    public long failedOrders()  { return orderRepository.countBySyncStatus(SyncStatus.FAILED); }
    public long syncedOrders()  { return orderRepository.countBySyncStatus(SyncStatus.SYNCED); }

    public List<CustomerEntity> lastFailedCustomers() {
        return customerRepository.findTop10BySyncStatusOrderByIdDesc(SyncStatus.FAILED);
    }

    public List<OrderEntity> lastFailedOrders() {
        return orderRepository.findTop10BySyncStatusOrderByIdDesc(SyncStatus.FAILED);
    }

    public OffsetDateTime lastSuccessfulCustomerSyncAt() {
        return customerRepository.findTop1ByLastSyncAtIsNotNullOrderByLastSyncAtDesc()
                .map(CustomerEntity::getLastSyncAt)
                .orElse(null);
    }

    public OffsetDateTime lastSuccessfulOrderSyncAt() {
        return orderRepository.findTop1ByLastSyncAtIsNotNullOrderByLastSyncAtDesc()
                .map(OrderEntity::getLastSyncAt)
                .orElse(null);
    }
}
