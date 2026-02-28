package com.emar.order_app.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminSyncPagesController {

    private final SyncAdminQueryService queryService;

    public AdminSyncPagesController(SyncAdminQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/admin/sync")
    public String syncDashboard(Model model) {
        model.addAttribute("pendingCustomers", queryService.pendingCustomers());
        model.addAttribute("syncedCustomers", queryService.syncedCustomers());
        model.addAttribute("failedCustomers", queryService.failedCustomers());

        model.addAttribute("pendingOrders", queryService.pendingOrders());
        model.addAttribute("syncedOrders", queryService.syncedOrders());
        model.addAttribute("failedOrders", queryService.failedOrders());

        model.addAttribute("lastFailedCustomers", queryService.lastFailedCustomers());
        model.addAttribute("lastFailedOrders", queryService.lastFailedOrders());

        return "admin/sync";
    }
}
