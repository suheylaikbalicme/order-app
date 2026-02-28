package com.emar.order_app.order;

import com.emar.order_app.customer.CustomerQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import com.emar.order_app.auth.Authz;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/orders")
public class OrderPagesController {

    private final OrderQueryService queryService;
    private final OrderService orderService;
    private final CustomerQueryService customerQueryService;

    @Value("${order.approval.enabled:false}")
    private boolean approvalEnabled;

    public OrderPagesController(
            OrderQueryService queryService,
            OrderService orderService,
            CustomerQueryService customerQueryService
    ) {
        this.queryService = queryService;
        this.orderService = orderService;
        this.customerQueryService = customerQueryService;
    }

    @GetMapping
    public String list(
            Model model,
            Authentication auth,
            @RequestParam(name = "status", required = false) String status
    ) {
        // status filter (UI): ALL | DRAFT | APPROVED | CANCELLED
        OrderStatus filter = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                filter = OrderStatus.valueOf(status.trim().toUpperCase());
            } catch (Exception ignored) {
                filter = null;
            }
        }

        var all = queryService.listFor(auth);


        orderService.recalcAndPersistIfMissingForList(all);
        all = queryService.listFor(auth);

        final OrderStatus finalFilter = filter;
        if (finalFilter != null) {
            all = all.stream().filter(o -> {
                OrderStatus st = o.getStatus();
                if (finalFilter == OrderStatus.WAITING_APPROVAL) {
                    return st == OrderStatus.WAITING_APPROVAL || st == OrderStatus.SUBMITTED;
                }
                if (finalFilter == OrderStatus.SYNCED) {
                    return st == OrderStatus.SYNCED
                            || st == OrderStatus.SENT_TO_LOGO
                            || st == OrderStatus.COMPLETED;
                }
                return st == finalFilter;
            }).toList();
        }

        model.addAttribute("orders", all);
        model.addAttribute("statusFilter", finalFilter != null ? finalFilter.name() : "ALL");
        return "orders/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication auth) {
        orderService.recalcAndPersistIfMissing(id);
        OrderEntity order = queryService.getByIdWithItemsFor(auth, id);

        model.addAttribute("order", order);
        model.addAttribute("subTotal", queryService.calcSubTotal(order));
        model.addAttribute("afterDiscount", queryService.calcAfterDiscount(order));
        model.addAttribute("discountTotal",
                order.getDiscountTotal() != null ? order.getDiscountTotal() : java.math.BigDecimal.ZERO);
        model.addAttribute("vatTotal",
                order.getVatTotal() != null ? order.getVatTotal() : java.math.BigDecimal.ZERO);
        model.addAttribute("grandTotal", queryService.calcGrandTotal(order));

        model.addAttribute("canEdit", queryService.canEdit(auth, order));
        model.addAttribute("canRevise", queryService.canRevise(auth, order));
        model.addAttribute("canApprove", queryService.canApprove(auth, order, approvalEnabled));
        model.addAttribute("approvalEnabled", approvalEnabled);

        return "orders/detail";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, Authentication auth) {
        OrderEntity order = queryService.getByIdWithItemsFor(auth, id);

        if (!(queryService.canEdit(auth, order) || queryService.canRevise(auth, order))) {
            throw new org.springframework.security.access.AccessDeniedException("Bu siparişi düzenleyemezsin.");
        }

        model.addAttribute("orderId", order.getId());
        model.addAttribute("customers", customerQueryService.findAll());
        return "orders/edit";
    }

    @GetMapping("/{id}/revisions")
    public String revisions(@PathVariable Long id, Model model, Authentication auth) {
        OrderEntity order = queryService.getByIdWithItemsFor(auth, id);
        model.addAttribute("order", order);
        return "orders/revisions";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication auth) {
        String username = auth.getName();
        orderService.cancelOrder(id, username, Authz.isAdmin(auth), Authz.isUser(auth));
        return "redirect:/orders";
    }

}