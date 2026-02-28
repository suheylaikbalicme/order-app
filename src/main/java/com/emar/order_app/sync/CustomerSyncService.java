package com.emar.order_app.sync;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.emar.order_app.customer.CustomerEntity;
import com.emar.order_app.customer.CustomerRepository;

@Service
public class CustomerSyncService {

    private static final Logger log = LoggerFactory.getLogger(CustomerSyncService.class);

    private final CustomerRepository customerRepository;

    public CustomerSyncService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }


    public void syncPending(int batchSize) {
        List<CustomerEntity> pending = customerRepository.findTop50BySyncStatusOrderByIdAsc(SyncStatus.PENDING);

        if (pending.isEmpty()) {
            log.debug("[LogoSync] Customer pending=0");
            return;
        }

        // Respect configured batch size (repo method is fixed-top for simplicity)
        int n = Math.min(batchSize, pending.size());
        log.info("[LogoSync] Customer pending={} (processing {})", pending.size(), n);

        for (int i = 0; i < n; i++) {
            CustomerEntity c = pending.get(i);
            // TODO: call Logo create customer endpoint (mentor will provide)
            // On success: c.setSyncStatus(SYNCED); c.setLogoRef(...); c.setLastSyncAt(now);
            // On failure: c.setSyncStatus(FAILED); c.setSyncError(...); c.setLastSyncAt(now);
            log.info("[LogoSync] Customer would sync: id={}, code={}, name={}", c.getId(), c.getCustomerCode(), c.getCustomerName());
        }
    }
}
