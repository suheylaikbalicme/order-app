package com.emar.order_app.offer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OfferRevisionRepository extends JpaRepository<OfferRevisionEntity, Long> {
    List<OfferRevisionEntity> findByOfferIdOrderByRevisionNoDesc(Long offerId);
}
