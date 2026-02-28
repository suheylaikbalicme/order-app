package com.emar.order_app.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "logo.sync", name = "enabled", havingValue = "true")
public class LogoSyncJob {

    private static final Logger log = LoggerFactory.getLogger(LogoSyncJob.class);

    private final LogoSyncProperties props;
    private final CustomerSyncService customerSyncService;
    private final OrderSyncService orderSyncService;

    public LogoSyncJob(LogoSyncProperties props, CustomerSyncService customerSyncService, OrderSyncService orderSyncService) {
        this.props = props;
        this.customerSyncService = customerSyncService;
        this.orderSyncService = orderSyncService;
    }

    @Scheduled(fixedDelayString = "${logo.sync.fixed-delay-ms:60000}")
    public void run() {
        int batchSize = props.getBatchSize();
        log.info("[LogoSync] Tick (batchSize={})", batchSize);
        customerSyncService.syncPending(batchSize);
        orderSyncService.syncPending(batchSize);
    }
}
