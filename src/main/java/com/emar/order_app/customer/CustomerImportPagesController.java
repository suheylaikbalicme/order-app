package com.emar.order_app.customer;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/customers")
public class CustomerImportPagesController {

    @GetMapping("/import")
    public String importFromLogoPage(@RequestParam(name = "return", required = false) String returnUrl,
                                     Model model) {

        if (returnUrl != null && !returnUrl.isBlank() && returnUrl.startsWith("/")) {
            model.addAttribute("returnUrl", returnUrl);
        } else {
            model.addAttribute("returnUrl", "/customers");
        }
        return "customers/import";
    }
}
