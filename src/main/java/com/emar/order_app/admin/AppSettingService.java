package com.emar.order_app.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AppSettingService {

    private final AppSettingRepository repo;

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public AppSettingService(AppSettingRepository repo) {
        this.repo = repo;
    }

    public List<SettingDef> defaultDefinitions() {
        return List.of(
                new SettingDef("ui.brandName", "Uygulama adı", "mr-CRM"),
                new SettingDef("ui.footerText", "Footer metni", "© mr-CRM"),

                // Firma
                new SettingDef("company.name", "Firma adı", "mr-CRM"),
                new SettingDef("company.address", "Adres", ""),
                new SettingDef("company.taxOffice", "Vergi Dairesi", ""),
                new SettingDef("company.taxNo", "Vergi No", ""),

                // Yerelleştirme / Finans
                new SettingDef("locale.dateFormat", "Tarih formatı", "dd.MM.yyyy"),
                new SettingDef("finance.defaultCurrency", "Varsayılan para birimi", "TRY"),
                new SettingDef("finance.defaultVatRate", "Varsayılan KDV oranı (%)", "20"),
                new SettingDef("finance.defaultVatIncluded", "Varsayılan: KDV dahil mi? (true/false)", "true"),

                // Entegrasyonlar
                new SettingDef("sync.enabled", "Sync aktif mi? (true/false)", "true"),
                new SettingDef("fx.enabled", "Kur servisi aktif mi? (true/false)", "true"),
                new SettingDef("logo.enabled", "Logo entegrasyonu aktif mi? (true/false)", "true")
        );
    }

    @Transactional
    public Map<String, AppSettingEntity> ensureDefaultsExist() {
        Map<String, AppSettingEntity> map = new LinkedHashMap<>();
        for (SettingDef def : defaultDefinitions()) {
            AppSettingEntity e = repo.findById(def.key()).orElseGet(() -> repo.save(new AppSettingEntity(def.key(), def.defaultValue())));
            map.put(def.key(), e);
        }
        return map;
    }

    public List<AppSettingEntity> findAllOrdered() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(AppSettingEntity::getKey))
                .toList();
    }

    @Transactional
    public void updateMany(Map<String, String> updates) {
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<String, String> it : updates.entrySet()) {
            String key = it.getKey();
            String val = Optional.ofNullable(it.getValue()).orElse("").trim();
            AppSettingEntity entity = repo.findById(key).orElse(new AppSettingEntity());
            entity.setKey(key);
            entity.setValue(val);
            entity.setUpdatedAt(now);
            repo.save(entity);
        }
        cache.clear();
    }

    public String get(String key, String defaultValue) {
        return cache.computeIfAbsent(key, k ->
                repo.findById(k).map(AppSettingEntity::getValue).orElse(defaultValue)
        );
    }

    public boolean getBool(String key, boolean defaultValue) {
        String v = get(key, Boolean.toString(defaultValue));
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    public record SettingDef(String key, String label, String defaultValue) {}
}
