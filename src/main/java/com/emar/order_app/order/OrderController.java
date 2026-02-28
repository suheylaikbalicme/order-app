package com.emar.order_app.order;

import com.emar.order_app.customer.CustomerQueryService;
import com.emar.order_app.admin.AppSettingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final CustomerQueryService customerQueryService;
    private final AppSettingService appSettingService;

    public OrderController(CustomerQueryService customerQueryService, AppSettingService appSettingService) {
        this.customerQueryService = customerQueryService;
        this.appSettingService = appSettingService;
    }

    @GetMapping("/new")
    public String newOrderPage(Model model) {
        model.addAttribute("customers", customerQueryService.findAll());

        // Finans ayarlarından default değerler
        model.addAttribute("defaultCurrency", appSettingService.get("finance.defaultCurrency", "TRY"));
        model.addAttribute("defaultVatRate", appSettingService.get("finance.defaultVatRate", "20"));
        return "orders/new";
    }
}
