package com.emar.order_app.offer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import com.emar.order_app.auth.UserEntity;
import com.emar.order_app.auth.UserRepository;
import com.emar.order_app.auth.Ownership;
import com.emar.order_app.audit.AuditAction;
import com.emar.order_app.audit.AuditEntityType;
import com.emar.order_app.audit.AuditLogService;
import com.emar.order_app.order.OrderEntity;
import com.emar.order_app.order.OrderItemEntity;
import com.emar.order_app.order.OrderRepository;
import com.emar.order_app.order.OrderStatus;
import com.emar.order_app.sync.SyncStatus;
import org.springframework.security.access.AccessDeniedException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OfferRevisionService offerRevisionService;
    private final AuditLogService audit;

    public OfferService(OfferRepository offerRepository, UserRepository userRepository, OrderRepository orderRepository, OfferRevisionService offerRevisionService, AuditLogService audit) {
        this.offerRepository = offerRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.offerRevisionService = offerRevisionService;
        this.audit = audit;
    }

    public record OfferItemInput(
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountRate,
            BigDecimal vatRate
    ) {}

    @Transactional
    public OfferEntity create(
            String customerCode,
            String customerName,
            LocalDate offerDate,
            Integer validityDays,
            Integer paymentDays,
            String currency,
            BigDecimal exchangeRate,
            String note,
            OfferStatus initialStatus,
            List<OfferItemInput> items,
            String username
    ) {

        if (customerCode == null || customerCode.isBlank()) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least 1 item is required");
        }

        OfferEntity o = new OfferEntity();
        o.setCustomerCode(customerCode);
        o.setCustomerName(customerName);
        o.setOfferDate(offerDate != null ? offerDate : LocalDate.now());
        o.setValidityDays(validityDays);
        o.setPaymentDays(paymentDays);
        o.setCurrency(currency);
        o.setExchangeRate(exchangeRate);
        o.setNote(note);
        o.setStatus(initialStatus != null ? initialStatus : OfferStatus.DRAFT);

        UserEntity u = userRepository.findByUsername(username).orElse(null);
        o.setCreatedBy(u);
        o.setCreatedByUsername(username);

        // items
        o.clearItems();
        for (OfferItemInput in : items) {
            if (in.itemCode() == null || in.itemCode().isBlank()) continue;
            OfferItemEntity it = new OfferItemEntity();
            it.setItemCode(in.itemCode());
            it.setItemName(in.itemName());
            it.setQuantity(nz(in.quantity(), BigDecimal.ZERO));
            it.setUnitPrice(nz(in.unitPrice(), BigDecimal.ZERO));
            it.setDiscountRate(nz(in.discountRate(), BigDecimal.ZERO));
            it.setVatRate(nz(in.vatRate(), new BigDecimal("20")));
            o.addItem(it);
        }
        if (o.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least 1 item is required");
        }

        // totals
        Totals t = calcTotals(o.getItems());
        o.setSubtotalAmount(t.subtotal());
        o.setDiscountTotal(t.discountTotal());
        o.setVatTotal(t.vatTotal());
        o.setGrandTotal(t.grandTotal());

        OfferEntity saved = offerRepository.save(o);
        audit.log(
                username,
                AuditEntityType.OFFER,
                saved.getId(),
                AuditAction.CREATE,
                "Offer created",
                meta(
                        "status", saved.getStatus() != null ? saved.getStatus().name() : null,
                        "currency", saved.getCurrency(),
                        "grandTotal", saved.getGrandTotal()
                )
        );
        return saved;
    }

    @Transactional
    public OfferEntity update(
            Long id,
            String customerCode,
            String customerName,
            LocalDate offerDate,
            Integer validityDays,
            Integer paymentDays,
            String currency,
            BigDecimal exchangeRate,
            String note,
            String action,
            String revisionReason,
            List<OfferItemInput> items,
            String username,
            boolean isAdmin,
            boolean isUser
    ) {
        OfferEntity o = offerRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Teklif bulunamadı: " + id));

        Ownership.assertOwnerOrAdmin(
                isAdmin,
                isUser,
                username,
                o.getCreatedByUsername(),
                "Bu teklifi güncelleme yetkin yok."
        );

        if (o.getConvertedOrderId() != null) {
            throw new IllegalStateException("Siparişe dönüştürülmüş teklif revize edilemez.");
        }

        String act = (action == null ? "draft" : action.trim().toLowerCase());
        boolean revise = "revise".equals(act) || "resubmit".equals(act);

        OfferTransitions.assertEditable(o.getStatus(), revise);

        if (revise && (revisionReason == null || revisionReason.isBlank())) {
            throw new IllegalArgumentException("Revizyon sebebi zorunludur.");
        }

        offerRevisionService.snapshot(o, username, revisionReason);

        if (customerCode == null || customerCode.isBlank()) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least 1 item is required");
        }

        // header
        o.setCustomerCode(customerCode.trim());
        o.setCustomerName(customerName != null ? customerName.trim() : null);
        o.setOfferDate(offerDate != null ? offerDate : LocalDate.now());
        o.setValidityDays(validityDays);
        o.setPaymentDays(paymentDays);
        o.setCurrency(currency);
        o.setExchangeRate(exchangeRate);
        o.setNote(note);

        // items replace
        o.clearItems();
        for (OfferItemInput in : items) {
            if (in.itemCode() == null || in.itemCode().isBlank()) continue;
            OfferItemEntity it = new OfferItemEntity();
            it.setItemCode(in.itemCode());
            it.setItemName(in.itemName());
            it.setQuantity(nz(in.quantity(), BigDecimal.ZERO));
            it.setUnitPrice(nz(in.unitPrice(), BigDecimal.ZERO));
            it.setDiscountRate(nz(in.discountRate(), BigDecimal.ZERO));
            it.setVatRate(nz(in.vatRate(), new BigDecimal("20")));
            o.addItem(it);
        }
        if (o.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least 1 item is required");
        }

        // totals
        Totals t2 = calcTotals(o.getItems());
        o.setSubtotalAmount(t2.subtotal());
        o.setDiscountTotal(t2.discountTotal());
        o.setVatTotal(t2.vatTotal());
        o.setGrandTotal(t2.grandTotal());

        // status
        if ("submit".equals(act) || "resubmit".equals(act)) {
            o.setStatus(OfferStatus.WAITING_APPROVAL);
        } else {
            // draft or revise -> DRAFT
            o.setStatus(OfferStatus.DRAFT);
        }

        OfferEntity saved = offerRepository.save(o);

        AuditAction aa;
        if ("resubmit".equals(act)) aa = AuditAction.RESUBMIT;
        else if ("submit".equals(act)) aa = AuditAction.SUBMIT;
        else if (revise) aa = AuditAction.REVISE;
        else aa = AuditAction.UPDATE;

        audit.log(
                username,
                AuditEntityType.OFFER,
                saved.getId(),
                aa,
                (revise ? "Offer revised" : "Offer updated"),
                meta(
                        "status", saved.getStatus() != null ? saved.getStatus().name() : null,
                        "currency", saved.getCurrency(),
                        "grandTotal", saved.getGrandTotal(),
                        "reason", revisionReason
                )
        );

        return saved;
    }

    @Transactional
    public OfferEntity submit(Long id, String username, boolean isAdmin, boolean isUser) {
        OfferEntity o = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teklif bulunamadı: " + id));

        assertNotExpired(o);

        Ownership.assertOwnerOrAdmin(
                isAdmin,
                isUser,
                username,
                o.getCreatedByUsername(),
                "Bu teklifi onaya gönderme yetkin yok."
        );

        OfferTransitions.assertSubmittable(o.getStatus());
        o.setStatus(OfferStatus.WAITING_APPROVAL);
        OfferEntity saved = offerRepository.save(o);
        audit.log(username, AuditEntityType.OFFER, saved.getId(), AuditAction.SUBMIT, "Offer submitted for approval",
                meta("status", saved.getStatus() != null ? saved.getStatus().name() : null));
        return saved;
    }

    @Transactional
    public OfferEntity approve(Long id) {
        OfferEntity o = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teklif bulunamadı: " + id));

        assertNotExpired(o);

        OfferTransitions.assertApprovable(o.getStatus());
        o.setStatus(OfferStatus.APPROVED);
        OfferEntity saved = offerRepository.save(o);
        audit.log(null, AuditEntityType.OFFER, saved.getId(), AuditAction.APPROVE, "Offer approved",
                meta("status", saved.getStatus() != null ? saved.getStatus().name() : null));
        return saved;
    }

    @Transactional
    public OfferEntity reject(Long id) {
        OfferEntity o = offerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teklif bulunamadı: " + id));

        OfferTransitions.assertRejectable(o.getStatus());
        o.setStatus(OfferStatus.REJECTED);
        OfferEntity saved = offerRepository.save(o);
        audit.log(null, AuditEntityType.OFFER, saved.getId(), AuditAction.REJECT, "Offer rejected",
                meta("status", saved.getStatus() != null ? saved.getStatus().name() : null));
        return saved;
    }

    @Transactional
    public OrderEntity convertToOrder(Long offerId, String username, boolean isAdmin, boolean isUser) {
        OfferEntity offer = offerRepository.findByIdWithItems(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Teklif bulunamadı: " + offerId));

        assertNotExpired(offer);

        Ownership.assertOwnerOrAdmin(
                isAdmin,
                isUser,
                username,
                offer.getCreatedByUsername(),
                "Bu teklifi siparişe dönüştürme yetkin yok."
        );

        OfferTransitions.assertConvertibleToOrder(offer.getStatus());
        if (offer.getConvertedOrderId() != null) {
            throw new IllegalStateException("Bu teklif zaten siparişe dönüştürülmüş.");
        }
        if (offer.getItems() == null || offer.getItems().isEmpty()) {
            throw new IllegalStateException("Teklif satırları yok. Dönüştürülemez.");
        }

        // Prefer original creator for ownership; fallback to converter.
        UserEntity createdBy = offer.getCreatedBy();
        if (createdBy == null) {
            createdBy = userRepository.findByUsername(username).orElse(null);
        }

        OrderEntity order = new OrderEntity();
        order.setCustomerCode(offer.getCustomerCode());
        order.setCustomerName(offer.getCustomerName());
        order.setStatus(OrderStatus.DRAFT);
        order.setSyncStatus(SyncStatus.PENDING);
        order.setCreatedBy(createdBy);
        order.setCreatedByUsername(createdBy != null ? createdBy.getUsername() : username);

        // Optional header extensions (if present in entity/table)
        try {
            order.getClass().getMethod("setOrderDate", java.time.LocalDate.class)
                    .invoke(order, offer.getOfferDate());
            order.getClass().getMethod("setCurrency", String.class)
                    .invoke(order, offer.getCurrency());
            order.getClass().getMethod("setExchangeRate", java.math.BigDecimal.class)
                    .invoke(order, offer.getExchangeRate());
            order.getClass().getMethod("setNote", String.class)
                    .invoke(order, offer.getNote());
            order.getClass().getMethod("setSubtotalAmount", java.math.BigDecimal.class)
                    .invoke(order, offer.getSubtotalAmount());
            order.getClass().getMethod("setDiscountTotal", java.math.BigDecimal.class)
                    .invoke(order, offer.getDiscountTotal());
            order.getClass().getMethod("setVatTotal", java.math.BigDecimal.class)
                    .invoke(order, offer.getVatTotal());
            order.getClass().getMethod("setGrandTotal", java.math.BigDecimal.class)
                    .invoke(order, offer.getGrandTotal());
        } catch (Exception ignore) {
            // If order header fields are not available in this version, ignore.
        }

        // Lines: carry per-line discount & vat rates.
        for (OfferItemEntity oi : offer.getItems()) {
            OrderItemEntity it = new OrderItemEntity();
            it.setItemCode(oi.getItemCode());
            it.setItemName(oi.getItemName());
            it.setQuantity(nz(oi.getQuantity(), BigDecimal.ZERO));
            it.setUnitPrice(nz(oi.getUnitPrice(), BigDecimal.ZERO));
            it.setDiscountRate(nz(oi.getDiscountRate(), BigDecimal.ZERO));
            it.setVatRate(nz(oi.getVatRate(), new BigDecimal("20")));
            order.addItem(it);
        }

        OrderEntity savedOrder = orderRepository.save(order);
        offer.setConvertedOrderId(savedOrder.getId());
        offerRepository.save(offer);

        audit.log(
                username,
                AuditEntityType.OFFER,
                offer.getId(),
                AuditAction.CONVERT_TO_ORDER,
                "Offer converted to order",
                meta(
                        "orderId", savedOrder.getId(),
                        "status", offer.getStatus() != null ? offer.getStatus().name() : null,
                        "currency", offer.getCurrency(),
                        "grandTotal", offer.getGrandTotal()
                )
        );

        return savedOrder;
    }


    private static java.util.Map<String, Object> meta(Object... kv) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        if (kv == null) return m;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object k = kv[i];
            Object v = kv[i + 1];
            if (k == null) continue;
            m.put(String.valueOf(k), v);
        }
        return m;
    }

    private void assertNotExpired(OfferEntity offer) {
        if (offer == null) return;
        if (offer.isExpired()) {
            LocalDate until = offer.getValidUntil();
            String suffix = until != null ? (" (Son geçerlilik: " + until + ")") : "";
            throw new IllegalStateException("Teklifin süresi dolmuş. Bu işlem yapılamaz." + suffix);
        }
    }

    private record Totals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal vatTotal, BigDecimal grandTotal) {}

    private Totals calcTotals(List<OfferItemEntity> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (OfferItemEntity it : items) {
            BigDecimal qty = nz(it.getQuantity(), BigDecimal.ZERO);
            BigDecimal unit = nz(it.getUnitPrice(), BigDecimal.ZERO);
            BigDecimal lineBase = qty.multiply(unit);

            BigDecimal discRate = nz(it.getDiscountRate(), BigDecimal.ZERO)
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
            BigDecimal lineAfterDisc = lineBase.multiply(BigDecimal.ONE.subtract(discRate));
            BigDecimal lineDisc = lineBase.subtract(lineAfterDisc);

            BigDecimal vatRate = nz(it.getVatRate(), new BigDecimal("20"))
                    .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
            BigDecimal lineVat = lineAfterDisc.multiply(vatRate);
            BigDecimal lineGross = lineAfterDisc.add(lineVat);

            subtotal = subtotal.add(lineBase);
            discountTotal = discountTotal.add(lineDisc);
            vatTotal = vatTotal.add(lineVat);
            grandTotal = grandTotal.add(lineGross);
        }

        // round money fields
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        discountTotal = discountTotal.setScale(2, RoundingMode.HALF_UP);
        vatTotal = vatTotal.setScale(2, RoundingMode.HALF_UP);
        grandTotal = grandTotal.setScale(2, RoundingMode.HALF_UP);
        return new Totals(subtotal, discountTotal, vatTotal, grandTotal);
    }

    private static BigDecimal nz(BigDecimal v, BigDecimal fallback) {
        return v == null ? fallback : v;
    }
}
