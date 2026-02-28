package com.emar.order_app.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminSettingsController {

    private final AppSettingService appSettingService;

    private final SyncAdminQueryService syncQueryService;

    public AdminSettingsController(AppSettingService appSettingService, SyncAdminQueryService syncQueryService) {
        this.appSettingService = appSettingService;
        this.syncQueryService = syncQueryService;
    }

    @GetMapping("/settings")
    public String settings(Model model) {
        appSettingService.ensureDefaultsExist();
        var all = appSettingService.findAllOrdered();

        Map<String, AppSettingEntity> settingsMap = new LinkedHashMap<>();
        for (AppSettingEntity s : all) {
            settingsMap.put(s.getKey(), s);
        }

        model.addAttribute("settings", all);
        model.addAttribute("settingsMap", settingsMap);
        model.addAttribute("defs", appSettingService.defaultDefinitions());

        // ERP/Sync özet bilgileri
        model.addAttribute("syncPendingCustomers", syncQueryService.pendingCustomers());
        model.addAttribute("syncFailedCustomers", syncQueryService.failedCustomers());
        model.addAttribute("syncSyncedCustomers", syncQueryService.syncedCustomers());
        model.addAttribute("syncPendingOrders", syncQueryService.pendingOrders());
        model.addAttribute("syncFailedOrders", syncQueryService.failedOrders());
        model.addAttribute("syncSyncedOrders", syncQueryService.syncedOrders());
        model.addAttribute("lastCustomerSyncAt", syncQueryService.lastSuccessfulCustomerSyncAt());
        model.addAttribute("lastOrderSyncAt", syncQueryService.lastSuccessfulOrderSyncAt());
        return "admin/settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@RequestParam Map<String, String> params) {
        Map<String, String> updates = new LinkedHashMap<>();
        for (Map.Entry<String, String> it : params.entrySet()) {
            String k = it.getKey();
            if (!k.startsWith("s.")) continue;
            updates.put(k.substring(2), it.getValue());
        }
        appSettingService.updateMany(updates);
        return "redirect:/admin/settings?saved=1";
    }
}
