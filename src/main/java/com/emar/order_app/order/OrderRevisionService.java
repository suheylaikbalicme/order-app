package com.emar.order_app.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OrderRevisionService {

    private final OrderRevisionRepository repo;
    private final ObjectMapper objectMapper;

    public OrderRevisionService(OrderRevisionRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void snapshot(OrderEntity order, String revisedBy, String reason) {
        try {
            OrderRevisionEntity rev = new OrderRevisionEntity();
            rev.setOrder(order);
            int nextNo = (order.getRevisionNo() == null ? 0 : order.getRevisionNo()) + 1;
            rev.setRevisionNo(nextNo);
            rev.setRevisedByUsername(revisedBy);
            rev.setReason(reason);

            rev.setSnapshot(objectMapper.writeValueAsString(OrderSnapshot.from(order)));
            repo.save(rev);

            order.setRevisionNo(nextNo);
            order.setLastRevisedAt(OffsetDateTime.now());
            order.setLastRevisedByUsername(revisedBy);
        } catch (Exception e) {
            throw new IllegalStateException("Order snapshot alınamadı: " + e.getMessage(), e);
        }
    }


    public record OrderSnapshot(
            Long id,
            String customerCode,
            String customerName,
            java.time.LocalDate orderDate,
            String currency,
            BigDecimal exchangeRate,
            String note,

            String status,
            String syncStatus,

            BigDecimal discountRate,
            BigDecimal vatRate,
            BigDecimal subtotalAmount,
            BigDecimal discountTotal,
            BigDecimal vatTotal,
            BigDecimal grandTotal,

            Integer revisionNo,
            OffsetDateTime lastRevisedAt,
            String lastRevisedByUsername,

            String createdByUsername,
            OffsetDateTime createdAt,

            List<OrderItemSnapshot> items
    ) {
        public static OrderSnapshot from(OrderEntity o) {
            return new OrderSnapshot(
                    o.getId(),
                    o.getCustomerCode(),
                    o.getCustomerName(),
                    o.getOrderDate(),
                    o.getCurrency(),
                    o.getExchangeRate(),
                    o.getNote(),
                    o.getStatus() != null ? o.getStatus().name() : null,
                    o.getSyncStatus() != null ? o.getSyncStatus().name() : null,
                    o.getDiscountRate(),
                    o.getVatRate(),
                    o.getSubtotalAmount(),
                    o.getDiscountTotal(),
                    o.getVatTotal(),
                    o.getGrandTotal(),
                    o.getRevisionNo(),
                    o.getLastRevisedAt(),
                    o.getLastRevisedByUsername(),
                    o.getCreatedByUsername(),
                    o.getCreatedAt(),
                    (o.getItems() == null ? List.of() : o.getItems().stream().map(OrderItemSnapshot::from).toList())
            );
        }
    }

    public record OrderItemSnapshot(
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountRate,
            BigDecimal vatRate
    ) {
        public static OrderItemSnapshot from(OrderItemEntity it) {
            return new OrderItemSnapshot(
                    it.getItemCode(),
                    it.getItemName(),
                    it.getQuantity(),
                    it.getUnitPrice(),
                    it.getDiscountRate(),
                    it.getVatRate()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<OrderRevisionEntity> list(Long orderId) {
        return repo.findByOrderIdOrderByRevisionNoDesc(orderId);
    }
}
