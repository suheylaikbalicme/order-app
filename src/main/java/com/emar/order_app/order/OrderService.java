package com.emar.order_app.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emar.order_app.auth.UserEntity;
import com.emar.order_app.auth.UserRepository;
import com.emar.order_app.auth.Ownership;
import com.emar.order_app.audit.AuditAction;
import com.emar.order_app.audit.AuditEntityType;
import com.emar.order_app.audit.AuditLogService;
import com.emar.order_app.customer.CustomerRepository;
import com.emar.order_app.customer.CustomerEntity;
import com.emar.order_app.order.dto.CreateOrderItemRequest;
import com.emar.order_app.order.dto.CreateOrderRequest;
import com.emar.order_app.sync.SyncStatus;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final OrderRevisionService orderRevisionService;
    private final AuditLogService audit;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository, CustomerRepository customerRepository, OrderRevisionService orderRevisionService, AuditLogService audit) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.orderRevisionService = orderRevisionService;
        this.audit = audit;
    }

    /**
     * Backward compatibility / data hygiene:
     *
     * - Eski siparişlerde (özellikle header totals alanları sonradan eklendiyse) subtotal/vat/grand total 0 kalabiliyor.
     * - Satır bazlı KDV geldiğinde, header vatRate alanı da satırlardan türetilmeli (karışıksa NULL).
     *
     * Bu method, ilgili siparişin items'larını yükleyip header totals + header vatRate alanlarını yeniden hesaplar
     * ve gerektiğinde DB'ye yazar.
     */
    @Transactional
    public void recalcAndPersistIfMissing(Long orderId) {
        if (orderId == null) return;

        OrderEntity order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));

        boolean needsTotals = isNullOrZero(order.getGrandTotal())
                || isNullOrZero(order.getSubtotalAmount())
                || isNullOrZero(order.getVatTotal());

        BigDecimal derivedVat = deriveHeaderVatRate(order);

        boolean needsVatSummary;
        if (derivedVat == null) {
            // Satırlar karışık KDV ise header'da NULL olmalı
            needsVatSummary = order.getVatRate() != null;
        } else {
            // Satırlar tek KDV ise header o oran olmalı
            needsVatSummary = (order.getVatRate() == null) || order.getVatRate().compareTo(derivedVat) != 0;
        }

        if (!needsTotals && !needsVatSummary) return;

        // totals
        Totals totals = calcTotals(order.getItems());
        order.setSubtotalAmount(totals.subtotal);
        order.setDiscountTotal(totals.discountTotal);
        order.setVatTotal(totals.vatTotal);
        order.setGrandTotal(totals.grandTotal);

        // vat summary
        order.setVatRate(derivedVat);

        orderRepository.save(order);
    }

    @Transactional
    public void recalcAndPersistIfMissingForList(java.util.List<OrderEntity> orders) {
        if (orders == null || orders.isEmpty()) return;
        for (OrderEntity o : orders) {
            if (o == null || o.getId() == null) continue;

            boolean needs = isNullOrZero(o.getGrandTotal())
                    || isNullOrZero(o.getVatTotal())
                    || isNullOrZero(o.getSubtotalAmount())
                    || (o.getVatRate() != null && o.getVatRate().compareTo(new BigDecimal("20")) == 0);
            if (needs) {
                recalcAndPersistIfMissing(o.getId());
            }
        }
    }

    @Transactional
    public OrderEntity createOrder(CreateOrderRequest req, String username) {
        if (req == null) throw new IllegalArgumentException("request null");
        if (req.customerCode() == null || req.customerCode().isBlank())
            throw new IllegalArgumentException("customerCode zorunlu");

        UserEntity createdBy = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        OrderEntity order = new OrderEntity();
        order.setCustomerCode(req.customerCode().trim());
        order.setCustomerName(req.customerName() != null ? req.customerName().trim() : null);

        // Header (CRM)
        order.setOrderDate(req.orderDate());
        order.setCurrency(req.currency());
        order.setExchangeRate(defaultBd(req.exchangeRate(), BigDecimal.ONE));
        order.setNote(req.note());

        // Status: draft / submit
        String action = (req.action() == null ? "draft" : req.action().trim().toLowerCase());
        order.setStatus("submit".equals(action) ? OrderStatus.WAITING_APPROVAL : OrderStatus.DRAFT);

        // Legacy rates: keep stored for backward compatibility, but totals are calculated per-line.
        order.setDiscountRate(defaultBd(req.discountRate(), BigDecimal.ZERO));
        order.setVatRate(defaultBd(req.vatRate(), new BigDecimal("20")));

        order.setCreatedBy(createdBy);
        order.setCreatedByUsername(username);

        // yeni oluşturulan her sipariş Logo'ya push edilecek (sonra) => pending
        order.setSyncStatus(SyncStatus.PENDING);
        order.setSyncError(null);
        order.setLogoRef(null);
        order.setLastSyncAt(null);

        if (req.items() != null) {
            for (CreateOrderItemRequest it : req.items()) {
                validateItem(it);

                OrderItemEntity item = new OrderItemEntity();
                item.setItemCode(it.itemCode().trim());
                item.setItemName(it.itemName());
                item.setQuantity(it.quantity());
                item.setUnitPrice(defaultBd(it.unitPrice(), BigDecimal.ZERO));
                item.setDiscountRate(defaultBd(it.discountRate(), BigDecimal.ZERO));
                // Per-line KDV (new UI). If not provided, fallback to legacy header vatRate.
                item.setVatRate(defaultBd(it.vatRate(), order.getVatRate()));

                order.addItem(item);
            }
        }

        order.setVatRate(deriveHeaderVatRate(order));

        // Totals
        Totals totals = calcTotals(order.getItems());
        order.setSubtotalAmount(totals.subtotal);
        order.setDiscountTotal(totals.discountTotal);
        order.setVatTotal(totals.vatTotal);
        order.setGrandTotal(totals.grandTotal);

        String act = (req.action() == null) ? "draft" : req.action().trim().toLowerCase();
        if (!act.equals("draft") && !act.equals("submit")) act = "draft";

        // Status based on action
        if ("submit".equals(act)) {
            order.setStatus(OrderStatus.WAITING_APPROVAL);
        } else {
            // draft or revise
            order.setStatus(OrderStatus.DRAFT);
        }

        OrderEntity saved = orderRepository.save(order);
        audit.log(
                username,
                AuditEntityType.ORDER,
                saved.getId(),
                AuditAction.CREATE,
                "Order created",
                meta(
                        "status", saved.getStatus() != null ? saved.getStatus().name() : null,
                        "currency", saved.getCurrency(),
                        "grandTotal", saved.getGrandTotal()
                )
        );
        return saved;
    }

    @Transactional
    public OrderEntity updateOrder(Long id, CreateOrderRequest req, String username, boolean isAdmin, boolean isUser) {
        OrderEntity order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + id));

        Ownership.assertOwnerOrAdmin(
                isAdmin,
                isUser,
                username,
                order.getCreatedByUsername(),
                "Bu siparişi güncelleme yetkin yok."
        );

        String act = (req.action() == null ? "draft" : req.action().trim().toLowerCase());
        // action: draft | submit | revise | resubmit
        boolean revise = "revise".equals(act) || "resubmit".equals(act);

        OrderTransitions.assertEditable(order.getStatus(), revise);

        // Revise requires reason (UX + audit)
        if (revise && (req.revisionReason() == null || req.revisionReason().isBlank())) {
            throw new IllegalArgumentException("Revizyon sebebi zorunludur.");
        }

        // snapshot BEFORE changes
        orderRevisionService.snapshot(order, username, req.revisionReason());

        // Header
        if (req.customerCode() == null || req.customerCode().isBlank())
            throw new IllegalArgumentException("customerCode zorunlu");

        order.setCustomerCode(req.customerCode().trim());
        order.setCustomerName(req.customerName() != null ? req.customerName().trim() : null);

        // Header (CRM)
        order.setOrderDate(req.orderDate());
        order.setCurrency(req.currency());
        order.setExchangeRate(defaultBd(req.exchangeRate(), BigDecimal.ONE));
        order.setNote(req.note());

        // Legacy rates (kept)
        order.setDiscountRate(defaultBd(req.discountRate(), BigDecimal.ZERO));
        order.setVatRate(defaultBd(req.vatRate(), new BigDecimal("20")));

        // Items: replace
        order.clearItems();
        if (req.items() != null) {
            for (CreateOrderItemRequest it : req.items()) {
                validateItem(it);

                OrderItemEntity item = new OrderItemEntity();
                item.setItemCode(it.itemCode().trim());
                item.setItemName(it.itemName());
                item.setQuantity(it.quantity());
                item.setUnitPrice(defaultBd(it.unitPrice(), BigDecimal.ZERO));
                item.setDiscountRate(defaultBd(it.discountRate(), BigDecimal.ZERO));
                item.setVatRate(defaultBd(it.vatRate(), order.getVatRate()));

                order.addItem(item);
            }
        }

        // Header vatRate'ı satırların gerçek değerinden türet.
        order.setVatRate(deriveHeaderVatRate(order));

        // Logo sync: UI'da değiştiyse tekrar push gerekir
        order.setSyncStatus(SyncStatus.PENDING);
        order.setSyncError(null);
        order.setLastSyncAt(OffsetDateTime.now()); // local not: 'touched' (gerçek sync'te güncelleriz)

        // Totals
        Totals totals = calcTotals(order.getItems());
        order.setSubtotalAmount(totals.subtotal);
        order.setDiscountTotal(totals.discountTotal);
        order.setVatTotal(totals.vatTotal);
        order.setGrandTotal(totals.grandTotal);

        // Status based on action
        if ("submit".equals(act) || "resubmit".equals(act)) {
            order.setStatus(OrderStatus.WAITING_APPROVAL);
        } else {
            // draft or revise
            order.setStatus(OrderStatus.DRAFT);
        }

        OrderEntity saved = orderRepository.save(order);

        AuditAction aa;
        if ("resubmit".equals(act)) aa = AuditAction.RESUBMIT;
        else if ("submit".equals(act)) aa = AuditAction.SUBMIT;
        else if (revise) aa = AuditAction.REVISE;
        else aa = AuditAction.UPDATE;

        audit.log(
                username,
                AuditEntityType.ORDER,
                saved.getId(),
                aa,
                (revise ? "Order revised" : "Order updated"),
                meta(
                        "status", saved.getStatus() != null ? saved.getStatus().name() : null,
                        "currency", saved.getCurrency(),
                        "grandTotal", saved.getGrandTotal()
                )
        );

        return saved;
    }

    @Transactional
    public OrderEntity cancelOrder(Long id, String username, boolean isAdmin, boolean isUser) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + id));

        Ownership.assertOwnerOrAdmin(
                isAdmin,
                isUser,
                username,
                order.getCreatedByUsername(),
                "Bu siparişi iptal etme yetkin yok."
        );

        OrderTransitions.assertCancellable(order.getStatus());

        order.setStatus(OrderStatus.CANCELLED);
        order.setSyncStatus(SyncStatus.PENDING);
        order.setSyncError(null);
        order.setLastSyncAt(OffsetDateTime.now());

        OrderEntity saved = orderRepository.save(order);
        audit.log(username, AuditEntityType.ORDER, saved.getId(), AuditAction.UPDATE, "Order cancelled",
                meta("status", saved.getStatus() != null ? saved.getStatus().name() : null));
        return saved;
    }

    /**
     * USER: DRAFT siparişi onaya gönderir.
     */
    @Transactional
    public OrderEntity submitForApproval(Long id, String username, boolean isAdmin, boolean isUser) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + id));

        Ownership.assertOwnerOrAdmin(
                isAdmin,
                isUser,
                username,
                order.getCreatedByUsername(),
                "Bu siparişi onaya gönderme yetkin yok."
        );

        OrderTransitions.assertSubmittable(order.getStatus());

        order.setStatus(OrderStatus.WAITING_APPROVAL);
        order.setLastSyncAt(OffsetDateTime.now());
        OrderEntity saved = orderRepository.save(order);
        audit.log(username, AuditEntityType.ORDER, saved.getId(), AuditAction.SUBMIT, "Order submitted for approval",
                meta("status", saved.getStatus() != null ? saved.getStatus().name() : null));
        return saved;
    }

    @Transactional
    public OrderEntity approveOrder(Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + id));

        OrderTransitions.assertApprovable(order.getStatus());

        order.setStatus(OrderStatus.APPROVED);
        order.setSyncStatus(SyncStatus.PENDING);
        order.setSyncError(null);
        order.setLastSyncAt(OffsetDateTime.now());
        OrderEntity saved = orderRepository.save(order);
        audit.log(null, AuditEntityType.ORDER, saved.getId(), AuditAction.APPROVE, "Order approved",
                meta("status", saved.getStatus() != null ? saved.getStatus().name() : null));
        return saved;
    }


    @Transactional
    public OrderEntity syncApprovedOrder(Long id) {
        OrderEntity order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + id));

        OrderTransitions.assertSyncable(order.getStatus());

        String customerCode = order.getCustomerCode();
        if (customerCode == null || customerCode.isBlank()) {
            throw new IllegalStateException("Bu siparişte müşteri bilgisi eksik. Sync yapılamaz.");
        }
        CustomerEntity customer = customerRepository.findByCustomerCode(customerCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Müşteri CRM'de bulunamadı (code=" + customerCode + "). Sync yapılamaz."));
        if (customer.getSyncStatus() != SyncStatus.SYNCED) {
            throw new IllegalStateException(
                    "Bu siparişi Logo'ya göndermek için müşteri önce SYNCED olmalı. (Müşteri syncStatus="
                            + customer.getSyncStatus() + ")");
        }


        order.setSyncStatus(SyncStatus.FAILED);
        order.setSyncError("Logo order sync endpoint not configured yet.");
        order.setLastSyncAt(OffsetDateTime.now());
        order.setStatus(OrderStatus.FAILED);
        OrderEntity saved = orderRepository.save(order);
        audit.log(null, AuditEntityType.ORDER, saved.getId(), AuditAction.SYNC, "Order sync attempted",
                meta(
                        "syncStatus", saved.getSyncStatus() != null ? saved.getSyncStatus().name() : null,
                        "error", saved.getSyncError()
                ));
        return saved;
    }


    private BigDecimal deriveHeaderVatRate(OrderEntity order) {
        if (order == null) return new BigDecimal("20");

        var items = order.getItems();
        if (items == null || items.isEmpty()) {
            // Item yoksa eldeki header değerini koru
            return order.getVatRate();
        }

        BigDecimal first = null;
        for (OrderItemEntity it : items) {
            if (it == null) continue;
            BigDecimal vr = it.getVatRate();
            if (vr == null) continue;

            if (first == null) {
                first = vr;
            } else if (vr.compareTo(first) != 0) {
                return null; // mixed
            }
        }

        if (first == null) {
            return order.getVatRate();
        }
        return first.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateItem(CreateOrderItemRequest it) {
        if (it == null) throw new IllegalArgumentException("item null");
        if (it.itemCode() == null || it.itemCode().isBlank())
            throw new IllegalArgumentException("itemCode zorunlu");
        if (it.quantity() == null || it.quantity().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("quantity > 0 olmalı");
    }

    private BigDecimal defaultBd(BigDecimal v, BigDecimal def) {
        return Objects.requireNonNullElse(v, def);
    }

    private boolean isNullOrZero(BigDecimal v) {
        return v == null || v.compareTo(BigDecimal.ZERO) == 0;
    }

    private record Totals(BigDecimal subtotal, BigDecimal discountTotal, BigDecimal vatTotal, BigDecimal grandTotal) {}

    private Totals calcTotals(java.util.List<OrderItemEntity> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        if (items == null) items = java.util.List.of();

        for (OrderItemEntity it : items) {
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

        // round money
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        discountTotal = discountTotal.setScale(2, RoundingMode.HALF_UP);
        vatTotal = vatTotal.setScale(2, RoundingMode.HALF_UP);
        grandTotal = grandTotal.setScale(2, RoundingMode.HALF_UP);

        return new Totals(subtotal, discountTotal, vatTotal, grandTotal);
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

    private BigDecimal nz(BigDecimal v, BigDecimal def) {
        return v != null ? v : def;
    }
}
