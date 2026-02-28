package com.emar.order_app.offer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OfferRevisionService {

    private final OfferRevisionRepository repo;
    private final ObjectMapper objectMapper;

    public OfferRevisionService(OfferRevisionRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void snapshot(OfferEntity offer, String revisedBy, String reason) {
        try {
            OfferRevisionEntity rev = new OfferRevisionEntity();
            rev.setOffer(offer);
            int nextNo = (offer.getRevisionNo() == null ? 0 : offer.getRevisionNo()) + 1;
            rev.setRevisionNo(nextNo);
            rev.setRevisedByUsername(revisedBy);
            rev.setReason(reason);

            rev.setSnapshot(objectMapper.writeValueAsString(OfferSnapshot.from(offer)));
            repo.save(rev);

            offer.setRevisionNo(nextNo);
            offer.setLastRevisedAt(OffsetDateTime.now());
            offer.setLastRevisedByUsername(revisedBy);
        } catch (Exception e) {
            throw new IllegalStateException("Offer snapshot alınamadı: " + e.getMessage(), e);
        }
    }

    public record OfferSnapshot(
            Long id,
            String customerCode,
            String customerName,
            java.time.LocalDate offerDate,
            Integer validityDays,
            Integer paymentDays,
            String currency,
            BigDecimal exchangeRate,
            String note,

            Long convertedOrderId,
            String status,

            BigDecimal subtotalAmount,
            BigDecimal discountTotal,
            BigDecimal vatTotal,
            BigDecimal grandTotal,

            Integer revisionNo,
            OffsetDateTime lastRevisedAt,
            String lastRevisedByUsername,

            String createdByUsername,
            OffsetDateTime createdAt,

            List<OfferItemSnapshot> items
    ) {
        public static OfferSnapshot from(OfferEntity o) {
            return new OfferSnapshot(
                    o.getId(),
                    o.getCustomerCode(),
                    o.getCustomerName(),
                    o.getOfferDate(),
                    o.getValidityDays(),
                    o.getPaymentDays(),
                    o.getCurrency(),
                    o.getExchangeRate(),
                    o.getNote(),
                    o.getConvertedOrderId(),
                    o.getStatus() != null ? o.getStatus().name() : null,
                    o.getSubtotalAmount(),
                    o.getDiscountTotal(),
                    o.getVatTotal(),
                    o.getGrandTotal(),
                    o.getRevisionNo(),
                    o.getLastRevisedAt(),
                    o.getLastRevisedByUsername(),
                    o.getCreatedByUsername(),
                    o.getCreatedAt(),
                    (o.getItems() == null ? List.of() : o.getItems().stream().map(OfferItemSnapshot::from).toList())
            );
        }
    }

    public record OfferItemSnapshot(
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal discountRate,
            BigDecimal vatRate
    ) {
        public static OfferItemSnapshot from(OfferItemEntity it) {
            return new OfferItemSnapshot(
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
    public List<OfferRevisionEntity> list(Long offerId) {
        return repo.findByOfferIdOrderByRevisionNoDesc(offerId);
    }
}
