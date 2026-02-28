package com.emar.order_app;

import java.util.List;

import com.emar.order_app.order.OrderEntity;
import com.emar.order_app.order.OrderRepository;
import com.emar.order_app.order.OrderStatus;
import com.emar.order_app.sync.SyncStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final OrderRepository orderRepository;

    public HomeController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalOrders = orderRepository.count();
        long draftOrders = orderRepository.countByStatus(OrderStatus.DRAFT);
        long approvedOrders = orderRepository.countByStatus(OrderStatus.APPROVED);
        long pendingSync = orderRepository.countBySyncStatus(SyncStatus.PENDING);
        long synced = orderRepository.countBySyncStatus(SyncStatus.SYNCED);
        long failed = orderRepository.countBySyncStatus(SyncStatus.FAILED);

        List<OrderEntity> recentOrders = orderRepository.findAllByOrderByIdDesc();
        if (recentOrders.size() > 8) {
            recentOrders = recentOrders.subList(0, 8);
        }

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("draftOrders", draftOrders);
        model.addAttribute("approvedOrders", approvedOrders);
        model.addAttribute("pendingSync", pendingSync);
        model.addAttribute("synced", synced);
        model.addAttribute("failed", failed);
        model.addAttribute("recentOrders", recentOrders);

        return "dashboard";
    }
}
