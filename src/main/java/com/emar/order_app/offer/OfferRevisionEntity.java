package com.emar.order_app.offer;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;

@Entity
@Table(name = "offer_revisions")
public class OfferRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private OfferEntity offer;

    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;

    @Column(name = "revised_at", insertable = false, updatable = false)
    private OffsetDateTime revisedAt;

    @Column(name = "revised_by_username", length = 120)
    private String revisedByUsername;

    @Column(name = "reason", length = 500)
    private String reason;

    // Hibernate 6: jsonb kolonuna String yazarken JDBC tipini JSON olarak belirtmezsek
    // PostgreSQL "jsonb vs varchar" tip uyuşmazlığı hatası fırlatabilir.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", columnDefinition = "jsonb", nullable = false)
    private String snapshot;

    public Long getId() { return id; }

    public OfferEntity getOffer() { return offer; }
    public void setOffer(OfferEntity offer) { this.offer = offer; }

    public Integer getRevisionNo() { return revisionNo; }
    public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }

    public OffsetDateTime getRevisedAt() { return revisedAt; }

    public String getRevisedByUsername() { return revisedByUsername; }
    public void setRevisedByUsername(String revisedByUsername) { this.revisedByUsername = revisedByUsername; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSnapshot() { return snapshot; }
    public void setSnapshot(String snapshot) { this.snapshot = snapshot; }
}
