package com.emar.order_app.audit;

public enum AuditAction {
    CREATE,
    UPDATE,
    SUBMIT,
    RESUBMIT,
    REVISE,
    APPROVE,
    REJECT,
    /** Tekliften siparişe dönüşüm gibi aksiyonlar için. */
    CONVERT_TO_ORDER,
    CANCEL,
    SYNC
}
