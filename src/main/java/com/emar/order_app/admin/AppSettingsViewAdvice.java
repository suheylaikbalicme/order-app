package com.emar.order_app.admin;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AppSettingsViewAdvice {

    private final AppSettingService appSettingService;

    public AppSettingsViewAdvice(AppSettingService appSettingService) {
        this.appSettingService = appSettingService;
    }

    @ModelAttribute("appBrandName")
    public String brandName() {
        return appSettingService.get("ui.brandName", "mr-CRM");
    }

    @ModelAttribute("appFooterText")
    public String footerText() {
        return appSettingService.get("ui.footerText", "© mr-CRM");
    }

    @ModelAttribute("companyName")
    public String companyName() {
        return appSettingService.get("company.name", "mr-CRM");
    }
}
