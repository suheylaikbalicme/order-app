package com.emar.order_app.offer;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.emar.order_app.auth.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "offers")
public class OfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "offer_date")
    private LocalDate offerDate;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "payment_days")
    private Integer paymentDays;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "exchange_rate", precision = 12, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "note", length = 2000)
    private String note;

    @Column(name = "subtotal_amount", precision = 14, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_total", precision = 14, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "vat_total", precision = 14, scale = 2)
    private BigDecimal vatTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 14, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    // Revision metadata
    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo = 0;

    @Column(name = "last_revised_at")
    private OffsetDateTime lastRevisedAt;

    @Column(name = "last_revised_by_username", length = 120)
    private String lastRevisedByUsername;

    @Column(name = "converted_order_id")
    private Long convertedOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OfferStatus status = OfferStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @Column(name = "created_by_username", length = 120)
    private String createdByUsername;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OfferItemEntity> items = new ArrayList<>();

    public void addItem(OfferItemEntity item) {
        items.add(item);
        item.setOffer(this);
    }

    public void clearItems() {
        for (OfferItemEntity it : items) {
            it.setOffer(null);
        }
        items.clear();
    }

    public Long getId() { return id; }

    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public LocalDate getOfferDate() { return offerDate; }
    public void setOfferDate(LocalDate offerDate) { this.offerDate = offerDate; }

    public Integer getValidityDays() { return validityDays; }
    public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }

    public Integer getPaymentDays() { return paymentDays; }
    public void setPaymentDays(Integer paymentDays) { this.paymentDays = paymentDays; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getExchangeRate() { return exchangeRate; }
    public void setExchangeRate(BigDecimal exchangeRate) { this.exchangeRate = exchangeRate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }

    public BigDecimal getDiscountTotal() { return discountTotal; }
    public void setDiscountTotal(BigDecimal discountTotal) { this.discountTotal = discountTotal; }

    public BigDecimal getVatTotal() { return vatTotal; }
    public void setVatTotal(BigDecimal vatTotal) { this.vatTotal = vatTotal; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public Integer getRevisionNo() { return revisionNo; }
    public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }

    public OffsetDateTime getLastRevisedAt() { return lastRevisedAt; }
    public void setLastRevisedAt(OffsetDateTime lastRevisedAt) { this.lastRevisedAt = lastRevisedAt; }

    public String getLastRevisedByUsername() { return lastRevisedByUsername; }
    public void setLastRevisedByUsername(String lastRevisedByUsername) { this.lastRevisedByUsername = lastRevisedByUsername; }

    public Long getConvertedOrderId() { return convertedOrderId; }
    public void setConvertedOrderId(Long convertedOrderId) { this.convertedOrderId = convertedOrderId; }

    public OfferStatus getStatus() { return status; }
    public void setStatus(OfferStatus status) { this.status = status; }

    public UserEntity getCreatedBy() { return createdBy; }
    public void setCreatedBy(UserEntity createdBy) { this.createdBy = createdBy; }

    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public List<OfferItemEntity> getItems() { return items; }
    public void setItems(List<OfferItemEntity> items) { this.items = items; }

      @Transient
    public LocalDate getValidUntil() {
        if (offerDate == null) return null;
        if (validityDays == null) return null;
        if (validityDays <= 0) return offerDate;
        return offerDate.plusDays(validityDays.longValue());
    }

    @Transient
    public boolean isExpired() {
        LocalDate until = getValidUntil();
        return until != null && LocalDate.now().isAfter(until);
    }
}
