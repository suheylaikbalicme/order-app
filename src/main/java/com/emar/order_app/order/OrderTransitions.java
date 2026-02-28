package com.emar.order_app.order;


public final class OrderTransitions {

    private OrderTransitions() {}

    public static void assertEditable(OrderStatus status, boolean revise) {
        if (status == null) status = OrderStatus.DRAFT;
        if (!revise && status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Sipariş DRAFT değil. Güncellenemez.");
        }
        if (revise && status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("İptal edilmiş sipariş revize edilemez.");
        }
    }

    public static void assertSubmittable(OrderStatus status) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Sadece DRAFT sipariş onaya gönderilebilir.");
        }
    }

    public static void assertApprovable(OrderStatus status) {
        if (!(status == OrderStatus.WAITING_APPROVAL || status == OrderStatus.SUBMITTED)) {
            throw new IllegalStateException("Sadece WAITING_APPROVAL (onay bekleyen) sipariş onaylanabilir.");
        }
    }

    public static void assertSyncable(OrderStatus status) {
        if (!(status == OrderStatus.APPROVED || status == OrderStatus.FAILED)) {
            throw new IllegalStateException("Sadece APPROVED (veya FAILED) sipariş sync edilebilir.");
        }
    }

    public static void assertCancellable(OrderStatus status) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Sipariş DRAFT değil. İptal edilemez.");
        }
    }
}
