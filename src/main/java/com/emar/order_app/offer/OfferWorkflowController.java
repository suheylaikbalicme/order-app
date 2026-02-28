package com.emar.order_app.offer;

import com.emar.order_app.order.OrderEntity;
import com.emar.order_app.auth.Authz;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.AccessDeniedException;

@Controller
@RequestMapping("/offers")
public class OfferWorkflowController {

    private final OfferService offerService;

    public OfferWorkflowController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id, Authentication auth) {
        if (!Authz.canSubmit(auth)) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");
        }
        offerService.submit(id, auth.getName(), Authz.isAdmin(auth), Authz.isUser(auth));
        return "redirect:/offers/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Authentication auth) {
        if (!Authz.canApprove(auth)) {
            throw new AccessDeniedException("Sadece ADMIN onaylayabilir.");
        }
        offerService.approve(id);
        return "redirect:/offers/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, Authentication auth) {
        if (!Authz.canApprove(auth)) {
            throw new AccessDeniedException("Sadece ADMIN reddedebilir.");
        }
        offerService.reject(id);
        return "redirect:/offers/" + id;
    }

    @PostMapping("/{id}/convert-to-order")
    public String convertToOrder(@PathVariable Long id, Authentication auth) {
        if (!Authz.canConvert(auth)) {
            throw new AccessDeniedException("Bu işlem için yetkiniz yok.");
        }
        OrderEntity order = offerService.convertToOrder(id, auth.getName(), Authz.isAdmin(auth), Authz.isUser(auth));
        return "redirect:/orders/" + order.getId();
    }
}