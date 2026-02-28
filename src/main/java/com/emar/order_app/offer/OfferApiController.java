package com.emar.order_app.offer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.emar.order_app.auth.Authz;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/offers")
public class OfferApiController {

    private final OfferService offerService;
    private final OfferQueryService offerQueryService;

    public OfferApiController(OfferService offerService, OfferQueryService offerQueryService) {
        this.offerService = offerService;
        this.offerQueryService = offerQueryService;
    }

    public record OfferItemDto(
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountRate,
            BigDecimal vatRate
    ) {}

    public record OfferDto(
            Long id,
            String customerCode,
            String customerName,
            LocalDate offerDate,
            Integer validityDays,
            Integer paymentDays,
            String currency,
            BigDecimal exchangeRate,
            String note,
            String status,
            Integer revisionNo,
            String lastRevisedByUsername,
            List<OfferItemDto> items
    ) {}

    public record OfferUpsertRequest(
            String customerCode,
            String customerName,
            LocalDate offerDate,
            Integer validityDays,
            Integer paymentDays,
            String currency,
            BigDecimal exchangeRate,
            String note,
            String action,           // draft | submit | revise
            String revisionReason,   // optional
            List<OfferItemDto> items
    ) {}

    public record OfferUpsertResponse(Long id, String status) {}

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OfferDto> get(@PathVariable Long id, Authentication auth) {
        OfferEntity o = offerQueryService.getByIdWithItemsFor(auth, id);
        var items = (o.getItems() == null ? List.<OfferItemDto>of() : o.getItems().stream().map(it ->
                new OfferItemDto(it.getItemCode(), it.getItemName(), it.getQuantity(), it.getUnitPrice(), it.getDiscountRate(), it.getVatRate())
        ).toList());

        OfferDto dto = new OfferDto(
                o.getId(),
                o.getCustomerCode(),
                o.getCustomerName(),
                o.getOfferDate(),
                o.getValidityDays(),
                o.getPaymentDays(),
                o.getCurrency(),
                o.getExchangeRate(),
                o.getNote(),
                o.getStatus() != null ? o.getStatus().name() : null,
                o.getRevisionNo(),
                o.getLastRevisedByUsername(),
                items
        );
        return ResponseEntity.ok(dto);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OfferUpsertResponse> create(@RequestBody OfferUpsertRequest req, Authentication auth) {
        if (!Authz.canEdit(auth)) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");
        }
        OfferStatus st = "submit".equalsIgnoreCase(req.action())
                ? OfferStatus.WAITING_APPROVAL
                : OfferStatus.DRAFT;

        OfferEntity saved = offerService.create(
                req.customerCode(),
                req.customerName(),
                req.offerDate(),
                req.validityDays(),
                req.paymentDays(),
                req.currency(),
                req.exchangeRate(),
                req.note(),
                st,
                req.items() == null ? List.of() : req.items().stream().map(i ->
                        new OfferService.OfferItemInput(
                                i.itemCode(),
                                i.itemName(),
                                i.quantity(),
                                i.unitPrice(),
                                i.discountRate(),
                                i.vatRate()
                        )
                ).toList(),
                auth.getName()
        );

        return ResponseEntity.ok(new OfferUpsertResponse(saved.getId(), saved.getStatus().name()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OfferUpsertResponse> update(@PathVariable Long id, @RequestBody OfferUpsertRequest req, Authentication auth) {
        if (!Authz.canEdit(auth)) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");
        }
        String username = auth.getName();
        boolean isAdmin = Authz.isAdmin(auth);
        boolean isUser = Authz.isUser(auth);

        OfferEntity saved = offerService.update(
                id,
                req.customerCode(),
                req.customerName(),
                req.offerDate(),
                req.validityDays(),
                req.paymentDays(),
                req.currency(),
                req.exchangeRate(),
                req.note(),
                req.action(),
                req.revisionReason(),
                req.items() == null ? List.of() : req.items().stream().map(i ->
                        new OfferService.OfferItemInput(
                                i.itemCode(),
                                i.itemName(),
                                i.quantity(),
                                i.unitPrice(),
                                i.discountRate(),
                                i.vatRate()
                        )
                ).toList(),
                username,
                isAdmin,
                isUser
        );

        return ResponseEntity.ok(new OfferUpsertResponse(saved.getId(), saved.getStatus().name()));
    }
}