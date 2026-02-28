package com.emar.order_app.offer;

import com.emar.order_app.customer.CustomerQueryService;
import com.emar.order_app.admin.AppSettingService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/offers")
public class OfferPagesController {

    private final OfferQueryService offerQueryService;
    private final CustomerQueryService customerQueryService;
    private final AppSettingService appSettingService;

    public OfferPagesController(OfferQueryService offerQueryService, CustomerQueryService customerQueryService, AppSettingService appSettingService) {
        this.offerQueryService = offerQueryService;
        this.customerQueryService = customerQueryService;
        this.appSettingService = appSettingService;
    }

    @GetMapping
    public String list(Model model, Authentication auth) {
        List<OfferEntity> offers = offerQueryService.listFor(auth);

        Map<Long, Boolean> expiredMap = new HashMap<>();
        for (OfferEntity o : offers) {
            // Derived logic is centralized in OfferEntity (validUntil + expired)
            expiredMap.put(o.getId(), o != null && o.isExpired());
        }

        model.addAttribute("offers", offers);
        model.addAttribute("expiredMap", expiredMap);
        return "offers/list";
    }

    @GetMapping("/new")
    public String newOffer(Model model) {
        model.addAttribute("customers", customerQueryService.findAll());
        model.addAttribute("defaultCurrency", appSettingService.get("finance.defaultCurrency", "TRY"));
        model.addAttribute("defaultVatRate", appSettingService.get("finance.defaultVatRate", "20"));
        return "offers/new";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication auth) {
        OfferEntity offer = offerQueryService.getByIdWithItemsFor(auth, id);

        boolean expired = offer != null && offer.isExpired();

        model.addAttribute("offer", offer);
        model.addAttribute("expired", expired);
        model.addAttribute("canEdit", offerQueryService.canEdit(auth, offer));
        model.addAttribute("canRevise", offerQueryService.canRevise(auth, offer));
        // detail.html shows "Son Tarih"; previously it was coming as '-' because this attribute was missing.
        model.addAttribute("validUntil", offer != null ? offer.getValidUntil() : null);
        model.addAttribute("customer", customerQueryService.findByCode(offer.getCustomerCode()).orElse(null));
        return "offers/detail";
    }


    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, Authentication auth) {
        OfferEntity offer = offerQueryService.getByIdWithItemsFor(auth, id);
        // edit or revise allowed
        if (!(offerQueryService.canEdit(auth, offer) || offerQueryService.canRevise(auth, offer))) {
            throw new org.springframework.security.access.AccessDeniedException("Bu teklifi düzenleme yetkin yok.");
        }
        model.addAttribute("offer", offer);
        model.addAttribute("customers", customerQueryService.findAll());
        model.addAttribute("defaultCurrency", appSettingService.get("finance.defaultCurrency", "TRY"));
        model.addAttribute("defaultVatRate", appSettingService.get("finance.defaultVatRate", "20"));
        return "offers/edit";
    }

    @GetMapping("/{id}/revisions")
    public String revisions(@PathVariable Long id, Model model, Authentication auth) {
        OfferEntity offer = offerQueryService.getByIdWithItemsFor(auth, id);
        model.addAttribute("offer", offer);
        return "offers/revisions";
    }

}
