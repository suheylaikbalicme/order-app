package com.emar.order_app.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logo.sync")
public class LogoSyncProperties {

    /** Enable periodic sync job (default false). */
    private boolean enabled = false;

    /** How many pending records to process per run. */
    private int batchSize = 50;

    /** Fixed delay between runs in milliseconds. */
    private long fixedDelayMs = 60000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public long getFixedDelayMs() { return fixedDelayMs; }
    public void setFixedDelayMs(long fixedDelayMs) { this.fixedDelayMs = fixedDelayMs; }
}
