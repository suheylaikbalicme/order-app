package com.emar.order_app.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRevisionRepository extends JpaRepository<OrderRevisionEntity, Long> {
    List<OrderRevisionEntity> findByOrderIdOrderByRevisionNoDesc(Long orderId);
}
