package com.emar.order_app.customer;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.emar.order_app.offer.OfferQueryService;
import com.emar.order_app.order.OrderQueryService;

import java.time.LocalDate;

@Controller
public class CustomerPagesController {

    private final CustomerQueryService customerQueryService;
    private final CustomerService customerService;
    private final CustomerInteractionService interactionService;
    private final CustomerFileService fileService;
    private final OfferQueryService offerQueryService;
    private final OrderQueryService orderQueryService;

    public CustomerPagesController(CustomerQueryService customerQueryService,
                                   CustomerService customerService,
                                   CustomerInteractionService interactionService,
                                   CustomerFileService fileService,
                                   OfferQueryService offerQueryService,
                                   OrderQueryService orderQueryService) {
        this.customerQueryService = customerQueryService;
        this.customerService = customerService;
        this.interactionService = interactionService;
        this.fileService = fileService;
        this.offerQueryService = offerQueryService;
        this.orderQueryService = orderQueryService;
    }

    @GetMapping("/customers")
    public String customers(Model model, Authentication auth) {
        var localCustomers = (auth == null ? java.util.List.<CustomerEntity>of() : customerQueryService.listFor(auth));
        model.addAttribute("localCustomers", localCustomers);

        java.util.Set<String> activeCodes = new java.util.HashSet<>();
        if (auth != null) {
            activeCodes.addAll(offerQueryService.distinctCustomerCodesFor(auth));
            activeCodes.addAll(orderQueryService.distinctCustomerCodesFor(auth));
        }
        model.addAttribute("activeCrmCount", activeCodes.size());
        return "customers/list";
    }

    @GetMapping("/customers/new")
    public String newCustomer() {
        return "customers/new";
    }

    @GetMapping("/customers/{id}")
    public String detail(@PathVariable Long id,
                         @RequestParam(value = "type", required = false) String type,
                         @RequestParam(value = "from", required = false) String from,
                         @RequestParam(value = "to", required = false) String to,
                         Authentication auth,
                         Model model) {
        CustomerEntity c = customerService.getById(id);
        model.addAttribute("customer", c);
        java.time.LocalDate fromDate = (from == null || from.isBlank()) ? null : java.time.LocalDate.parse(from);
        java.time.LocalDate toDate = (to == null || to.isBlank()) ? null : java.time.LocalDate.parse(to);
        model.addAttribute("interactions", interactionService.listForCustomerFiltered(id, type, fromDate, toDate));
        model.addAttribute("filterType", type);
        model.addAttribute("filterFrom", from);
        model.addAttribute("filterTo", to);
        model.addAttribute("files", fileService.listForCustomer(id));
        // Bu müşterinin teklif/siparişleri
        model.addAttribute("customerOffers", auth == null ? java.util.List.of() : offerQueryService.listByCustomerCodeFor(auth, c.getCustomerCode()));
        model.addAttribute("customerOrders", auth == null ? java.util.List.of() : orderQueryService.listByCustomerCodeFor(auth, c.getCustomerCode()));
        return "customers/detail";
    }

    @GetMapping("/customers/{customerId}/interactions/{interactionId}/edit")
    public String editInteraction(@PathVariable Long customerId, @PathVariable Long interactionId, Model model) {
        CustomerEntity c = customerService.getById(customerId);
        CustomerInteractionEntity i = interactionService.listForCustomer(customerId).stream()
                .filter(x -> x.getId().equals(interactionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Interaction not found"));
        model.addAttribute("customer", c);
        model.addAttribute("interaction", i);
        return "customers/interaction_edit";
    }

    @PostMapping("/customers/{customerId}/interactions/{interactionId}/edit")
    public String updateInteraction(@PathVariable Long customerId,
                                    @PathVariable Long interactionId,
                                    @RequestParam("interactionDate") String interactionDate,
                                    @RequestParam("interactionType") String interactionType,
                                    @RequestParam("title") String title,
                                    @RequestParam(value = "description", required = false) String description) {
        LocalDate date = LocalDate.parse(interactionDate);
        interactionService.updateInteraction(customerId, interactionId, date, interactionType, title, description);
        return "redirect:/customers/" + customerId;
    }

    @PostMapping("/customers/{customerId}/interactions/{interactionId}/delete")
    public String deleteInteraction(@PathVariable Long customerId, @PathVariable Long interactionId) {
        interactionService.deleteInteraction(customerId, interactionId);
        return "redirect:/customers/" + customerId;
    }

    @GetMapping("/customers/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam(value = "return", required = false) String returnTo,
                       Model model) {
        CustomerEntity c = customerService.getById(id);
        model.addAttribute("customer", c);
        model.addAttribute("returnTo", (returnTo != null && returnTo.startsWith("/")) ? returnTo : ("/customers/" + id));
        return "customers/edit";
    }

    @PostMapping("/customers/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam("customerCode") String customerCode,
                         @RequestParam("customerName") String customerName,
                         @RequestParam("phone") String phone,
                         @RequestParam("email") String email,
                         @RequestParam("address") String address,
                         @RequestParam(value = "notes", required = false) String notes,
                         @RequestParam(value = "return", required = false) String returnTo,
                         Authentication auth) {
        String username = auth == null ? null : auth.getName();
        customerService.update(id, customerCode, customerName, phone, email, address, notes, username);

        String safeReturn = (returnTo != null && returnTo.startsWith("/")) ? returnTo : ("/customers/" + id);
        return "redirect:" + safeReturn;
    }

    @PostMapping("/customers/{id}/interactions")
    public String addInteraction(@PathVariable Long id,
                                 @RequestParam("interactionDate") String interactionDate,
                                 @RequestParam("interactionType") String interactionType,
                                 @RequestParam("title") String title,
                                 @RequestParam(value = "description", required = false) String description,
                                 Authentication auth) {
        LocalDate date = LocalDate.parse(interactionDate);
        String username = auth == null ? null : auth.getName();
        interactionService.addInteraction(id, date, interactionType, title, description, username);
        return "redirect:/customers/" + id;
    }

    @PostMapping("/customers/{id}/files")
    public String uploadFile(@PathVariable Long id,
                             @RequestParam("file") MultipartFile file,
                             Authentication auth) throws Exception {
        String username = auth == null ? null : auth.getName();
        try {
            fileService.upload(id, file, username);
            return "redirect:/customers/" + id;
        } catch (Exception ex) {

            String msg = ex.getMessage() == null ? "Dosya yüklenemedi" : ex.getMessage();
            return "redirect:/customers/" + id + "?fileError=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/customers/{customerId}/files/{fileId}/delete")
    public String deleteFile(@PathVariable Long customerId,
                             @PathVariable Long fileId) throws Exception {
        fileService.delete(customerId, fileId);
        return "redirect:/customers/" + customerId;
    }

    @GetMapping("/customers/{customerId}/files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long customerId, @PathVariable Long fileId) {
        CustomerFileEntity meta = fileService.getMeta(customerId, fileId);
        Resource res = fileService.loadAsResource(customerId, fileId);

        String ct = meta.getContentType();
        if (ct == null || ct.isBlank()) ct = "application/octet-stream";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getFileName().replace("\"", "") + "\"")
                .header(HttpHeaders.CONTENT_TYPE, ct)
                .body(res);
    }
}
