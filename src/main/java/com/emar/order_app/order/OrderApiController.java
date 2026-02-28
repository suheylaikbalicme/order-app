package com.emar.order_app.order;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.emar.order_app.auth.Authz;

import com.emar.order_app.order.dto.CreateOrderRequest;
import com.emar.order_app.order.dto.CreateOrderResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final OrderQueryService queryService;

    public OrderApiController(OrderService orderService, OrderQueryService queryService) {
        this.orderService = orderService;
        this.queryService = queryService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateOrderResponse> create(@RequestBody CreateOrderRequest req, Authentication auth) {
        if (!Authz.canEdit(auth)) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");
        }
        String username = auth.getName();
        OrderEntity saved = orderService.createOrder(req, username);
        return ResponseEntity.ok(new CreateOrderResponse(saved.getId(), saved.getStatus().name()));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderDto> get(@PathVariable Long id, Authentication auth) {
        OrderEntity o = queryService.getByIdWithItemsFor(auth, id);
        return ResponseEntity.ok(OrderDto.from(o));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateOrderResponse> update(@PathVariable Long id, @RequestBody CreateOrderRequest req, Authentication auth) {
        if (!Authz.canEdit(auth)) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");
        }
        String username = auth.getName();
        boolean isAdmin = Authz.isAdmin(auth);
        boolean isUser = Authz.isUser(auth);
        OrderEntity saved = orderService.updateOrder(id, req, username, isAdmin, isUser);
        return ResponseEntity.ok(new CreateOrderResponse(saved.getId(), saved.getStatus().name()));
    }

    public record OrderDto(
            Long id,
            String customerCode,
            String customerName,
            LocalDate orderDate,
            String currency,
            BigDecimal exchangeRate,
            String note,
            String status,
            String createdByUsername,
            Integer revisionNo,
            String lastRevisedByUsername,
            String syncStatus,
            List<ItemDto> items,
            BigDecimal discountRate,
            BigDecimal vatRate,
            BigDecimal subtotalAmount,
            BigDecimal discountTotal,
            BigDecimal vatTotal,
            BigDecimal grandTotal
    ) {
        public static OrderDto from(OrderEntity o) {
            return new OrderDto(
                    o.getId(),
                    o.getCustomerCode(),
                    o.getCustomerName(),
                    o.getOrderDate(),
                    o.getCurrency(),
                    o.getExchangeRate(),
                    o.getNote(),
                    o.getStatus() != null ? o.getStatus().name() : null,
                    o.getCreatedByUsername(),
                    o.getRevisionNo(),
                    o.getLastRevisedByUsername(),
                    o.getSyncStatus() != null ? o.getSyncStatus().name() : null,
                    (o.getItems() == null ? List.of() : o.getItems().stream().map(ItemDto::from).toList()),
                    o.getDiscountRate(),
                    o.getVatRate(),
                    o.getSubtotalAmount(),
                    o.getDiscountTotal(),
                    o.getVatTotal(),
                    o.getGrandTotal()
            );
        }
    }

    public record ItemDto(
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountRate,
            BigDecimal vatRate
    ) {
        public static ItemDto from(OrderItemEntity it) {
            return new ItemDto(
                    it.getItemCode(),
                    it.getItemName(),
                    it.getQuantity(),
                    it.getUnitPrice(),
                    it.getDiscountRate(),
                    it.getVatRate()
            );
        }
    }
}