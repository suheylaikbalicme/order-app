package com.emar.order_app.offer;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.emar.order_app.auth.Authz;

@Service
public class OfferQueryService {

    private final OfferRepository offerRepository;

    public OfferQueryService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    public List<OfferEntity> listFor(Authentication auth) {
        boolean isAdmin = Authz.isAdmin(auth);
        boolean isViewer = Authz.isViewer(auth);
        // Viewer: read-only internal role -> can view all offers
        if (isAdmin || isViewer) return offerRepository.findAllByOrderByIdDesc();
        return offerRepository.findAllByCreatedByUsernameOrderByIdDesc(auth.getName());
    }

    public OfferEntity getByIdWithItemsFor(Authentication auth, Long id) {
        boolean isAdmin = Authz.isAdmin(auth);
        boolean isViewer = Authz.isViewer(auth);
        return ((isAdmin || isViewer)
                ? offerRepository.findByIdWithItems(id)
                : offerRepository.findByIdWithItemsAndCreatedByUsername(id, auth.getName()))
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));
    }

    public List<OfferEntity> listByCustomerCodeFor(Authentication auth, String customerCode) {
        if (customerCode == null || customerCode.isBlank()) return List.of();
        String code = customerCode.trim();
        boolean isAdmin = Authz.isAdmin(auth);
        boolean isViewer = Authz.isViewer(auth);
        if (isAdmin || isViewer) return offerRepository.findAllByCustomerCodeOrderByIdDesc(code);
        return offerRepository.findAllByCustomerCodeAndCreatedByUsernameOrderByIdDesc(code, auth.getName());
    }

    /**
     * Customer KPI: CRM'de teklif oluşturulmuş (en az 1 teklif kaydı olan) müşteri kodları.
     * - Admin/Viewer: tüm tekliflerin customerCode'ları
     * - User: sadece kendi oluşturduğu tekliflerin customerCode'ları
     */
    public List<String> distinctCustomerCodesFor(Authentication auth) {
        if (auth == null) return List.of();
        boolean isAdmin = Authz.isAdmin(auth);
        boolean isViewer = Authz.isViewer(auth);
        if (isAdmin || isViewer) return offerRepository.findDistinctCustomerCodes();
        return offerRepository.findDistinctCustomerCodesByCreatedByUsername(auth.getName());
    }


    public boolean canEdit(Authentication auth, OfferEntity offer) {
        if (auth == null || offer == null) return false;
        if (Authz.isViewer(auth)) return false;
        if (Authz.isAdmin(auth)) return true;
        return auth.getName().equals(offer.getCreatedByUsername());
    }

    public boolean canRevise(Authentication auth, OfferEntity offer) {
        if (!canEdit(auth, offer)) return false;
        if (offer == null) return false;
        if (offer.getConvertedOrderId() != null) return false;
        // allow revise for any status except DRAFT edit will also work via revise
        return true;
    }

}
