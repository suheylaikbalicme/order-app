package com.emar.order_app.order;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderQueryService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> listAll() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(OrderEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }


    @Transactional(readOnly = true)
    public List<OrderEntity> listFor(Authentication auth) {
        if (auth == null) throw new AccessDeniedException("Unauthorized");

        if (isAdmin(auth) || isViewer(auth)) {
            return orderRepository.findAllByOrderByIdDesc();
        }

        String username = auth.getName();
        return orderRepository.findAllByCreatedByUsernameOrderByIdDesc(username);
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> listByCustomerCodeFor(Authentication auth, String customerCode) {
        if (auth == null) throw new AccessDeniedException("Unauthorized");
        if (customerCode == null || customerCode.isBlank()) return List.of();
        String code = customerCode.trim();
        if (isAdmin(auth) || isViewer(auth)) {
            return orderRepository.findAllByCustomerCodeOrderByIdDesc(code);
        }
        return orderRepository.findAllByCustomerCodeAndCreatedByUsernameOrderByIdDesc(code, auth.getName());
    }

    @Transactional(readOnly = true)
    public OrderEntity getByIdWithItems(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + id));
    }


    @Transactional(readOnly = true)
    public OrderEntity getByIdWithItemsFor(Authentication auth, Long id) {
        if (auth == null) throw new AccessDeniedException("Unauthorized");

        OrderEntity order = getByIdWithItems(id);

        if (isAdmin(auth) || isViewer(auth)) return order;

        String username = auth.getName();
        String owner = order.getCreatedByUsername();
        if (owner == null) owner = (order.getCreatedBy() != null ? order.getCreatedBy().getUsername() : null);

        if (owner == null || !owner.equals(username)) {
            throw new AccessDeniedException("Bu siparişi görüntüleme yetkin yok.");
        }
        return order;
    }

    public boolean canEdit(Authentication auth, OrderEntity order) {
        if (auth == null) return false;

              if (isAdmin(auth)) return order.getStatus() == OrderStatus.DRAFT;

        if (isUser(auth)) {
            String username = auth.getName();
            String owner = order.getCreatedByUsername();
            return order.getStatus() == OrderStatus.DRAFT
                    && username != null
                    && username.equals(owner);
        }

        return false;
    }


    public boolean canApprove(Authentication auth, OrderEntity order, boolean approvalEnabled) {
        if (!approvalEnabled) return false;
        if (auth == null) return false;
        return isAdmin(auth) && (order.getStatus() == OrderStatus.WAITING_APPROVAL || order.getStatus() == OrderStatus.SUBMITTED);
    }


    @Transactional(readOnly = true)
    public List<String> distinctCustomerCodesFor(Authentication auth) {
        if (auth == null) return List.of();
        if (isAdmin(auth) || isViewer(auth)) return orderRepository.findDistinctCustomerCodes();
        return orderRepository.findDistinctCustomerCodesByCreatedByUsername(auth.getName());
    }

    // Totals: prefer stored header totals (CRM style).
    public BigDecimal calcSubTotal(OrderEntity o) {
        return o.getSubtotalAmount() != null ? o.getSubtotalAmount() : BigDecimal.ZERO;
    }

    public BigDecimal calcAfterDiscount(OrderEntity o) {
        BigDecimal sub = calcSubTotal(o);
        BigDecimal disc = o.getDiscountTotal() != null ? o.getDiscountTotal() : BigDecimal.ZERO;
        return sub.subtract(disc);
    }

    public BigDecimal calcGrandTotal(OrderEntity o) {
        return o.getGrandTotal() != null ? o.getGrandTotal() : BigDecimal.ZERO;
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
    private boolean isViewer(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_VIEWER".equals(a.getAuthority()));
    }
    private boolean isUser(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_USER".equals(a.getAuthority()));
    }


    public boolean canRevise(Authentication auth, OrderEntity order) {
        if (auth == null || order == null) return false;
        if (isViewer(auth)) return false;
        if (order.getStatus() == OrderStatus.CANCELLED) return false;

        // Admin: any status except CANCELLED can be revised
        if (isAdmin(auth)) return true;

        // User: only owner can revise
        if (isUser(auth)) {
            String username = auth.getName();
            String owner = order.getCreatedByUsername();
            return username != null && username.equals(owner);
        }
        return false;
    }

}
