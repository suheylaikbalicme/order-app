package com.emar.order_app.offer;

public final class OfferTransitions {

    private OfferTransitions() {}


    public static void assertEditable(OfferStatus status, boolean revise) {
        if (status == null) status = OfferStatus.DRAFT;
        if (!revise && status != OfferStatus.DRAFT) {
            throw new IllegalStateException("Teklif DRAFT değil. Güncellenemez.");
        }
        if (revise && status == OfferStatus.CANCELLED) {
            throw new IllegalStateException("İptal edilmiş teklif revize edilemez.");
        }
    }

    public static void assertSubmittable(OfferStatus status) {
        if (status != OfferStatus.DRAFT) {
            throw new IllegalStateException("Sadece DRAFT teklif onaya gönderilebilir.");
        }
    }

    public static void assertApprovable(OfferStatus status) {
        if (status != OfferStatus.WAITING_APPROVAL) {
            throw new IllegalStateException("Sadece WAITING_APPROVAL teklif onaylanabilir.");
        }
    }

    public static void assertRejectable(OfferStatus status) {
        if (status != OfferStatus.WAITING_APPROVAL) {
            throw new IllegalStateException("Sadece WAITING_APPROVAL teklif reddedilebilir.");
        }
    }

    public static void assertConvertibleToOrder(OfferStatus status) {
        if (status != OfferStatus.APPROVED) {
            throw new IllegalStateException("Sadece APPROVED teklif siparişe dönüştürülebilir.");
        }
    }
}
