package com.emar.order_app.offer;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers/{offerId}/revisions")
public class OfferRevisionApiController {

    private final OfferQueryService offerQueryService;
    private final OfferRevisionService offerRevisionService;

    public OfferRevisionApiController(OfferQueryService offerQueryService, OfferRevisionService offerRevisionService) {
        this.offerQueryService = offerQueryService;
        this.offerRevisionService = offerRevisionService;
    }

    public record RevisionDto(
            Long id,
            Integer revisionNo,
            String revisedAt,
            String revisedByUsername,
            String reason,
            String snapshot
    ) {}

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RevisionDto>> list(@PathVariable Long offerId, Authentication auth) {
        // ensure access allowed
        OfferEntity offer = offerQueryService.getByIdWithItemsFor(auth, offerId);

        List<RevisionDto> rows = offerRevisionService.list(offer.getId()).stream()
                .map(r -> new RevisionDto(
                        r.getId(),
                        r.getRevisionNo(),
                        r.getRevisedAt() != null ? r.getRevisedAt().toString() : null,
                        r.getRevisedByUsername(),
                        r.getReason(),
                        r.getSnapshot()
                ))
                .toList();

        return ResponseEntity.ok(rows);
    }
}
