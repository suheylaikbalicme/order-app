package com.emar.order_app.sync;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.emar.order_app.order.OrderEntity;
import com.emar.order_app.order.OrderRepository;

@Service
public class OrderSyncService {

    private static final Logger log = LoggerFactory.getLogger(OrderSyncService.class);

    private final OrderRepository orderRepository;

    public OrderSyncService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void syncPending(int batchSize) {
        List<OrderEntity> pending = orderRepository.findPendingWithItems(SyncStatus.PENDING, PageRequest.of(0, Math.max(1, batchSize)));

        if (pending.isEmpty()) {
            log.debug("[LogoSync] Order pending=0");
            return;
        }

        log.info("[LogoSync] Order pending={} (processing {})", pending.size(), pending.size());

        for (OrderEntity o : pending) {

            log.info("[LogoSync] Order would sync: id={}, customerCode={}, items={}", o.getId(), o.getCustomerCode(), (o.getItems() != null ? o.getItems().size() : 0));
        }
    }
}
