package com.emar.order_app.order;

import com.emar.order_app.auth.Authz;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
public class OrderWorkflowController {

    private final OrderService orderService;

    public OrderWorkflowController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, Authentication auth) {
        if (!Authz.canSubmit(auth)) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");
        }
        orderService.submitForApproval(id, auth.getName(), Authz.isAdmin(auth), Authz.isUser(auth));
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Authentication auth) {
        if (auth == null) throw new AccessDeniedException("Unauthorized");
        if (!Authz.canApprove(auth)) {
            throw new AccessDeniedException("Sadece ADMIN onaylayabilir.");
        }
        orderService.approveOrder(id);
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/sync")
    public String sync(@PathVariable Long id, Authentication auth) {
        if (auth == null) throw new AccessDeniedException("Unauthorized");
        if (!Authz.canApprove(auth)) {
            throw new AccessDeniedException("Sadece ADMIN sync edebilir.");
        }
        try {
            orderService.syncApprovedOrder(id);
            return "redirect:/orders/" + id + "?syncOk=1";
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Sync failed.";
            return "redirect:/orders/" + id + "?syncError=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        }
    }

}
