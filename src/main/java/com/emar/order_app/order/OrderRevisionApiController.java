package com.emar.order_app.order;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/revisions")
public class OrderRevisionApiController {

    private final OrderQueryService orderQueryService;
    private final OrderRevisionService orderRevisionService;

    public OrderRevisionApiController(OrderQueryService orderQueryService, OrderRevisionService orderRevisionService) {
        this.orderQueryService = orderQueryService;
        this.orderRevisionService = orderRevisionService;
    }

    public record RevisionDto(
            Long id,
            Integer revisionNo,
            String revisedAt,
            String revisedByUsername,
            String reason,
            String snapshot
    ) {}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RevisionDto>> list(@PathVariable Long orderId, Authentication auth) {
        // ensure access allowed
        OrderEntity order = orderQueryService.getByIdWithItemsFor(auth, orderId);

        List<RevisionDto> rows = orderRevisionService.list(order.getId()).stream()
                .map(r -> new RevisionDto(
                        r.getId(),
                        r.getRevisionNo(),
                        r.getRevisedAt() != null ? r.getRevisedAt().toString() : null,
                        r.getRevisedByUsername(),
                        r.getReason(),
                        r.getSnapshot()
                ))
                .toList();

        return ResponseEntity.ok(rows);
    }
}
