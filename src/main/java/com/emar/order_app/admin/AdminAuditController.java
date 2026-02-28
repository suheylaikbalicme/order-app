package com.emar.order_app.admin;

import com.emar.order_app.audit.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.*;

@Controller
@RequestMapping("/admin")
public class AdminAuditController {

    private final AuditLogService auditLogService;

    public AdminAuditController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/audit")
    public String audit(
            @RequestParam(value = "entityType", required = false) AuditEntityType entityType,
            @RequestParam(value = "action", required = false) AuditAction action,
            @RequestParam(value = "actor", required = false) String actor,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 10), 200));

        Instant dateFrom = null;
        Instant dateTo = null;
        ZoneId zone = ZoneId.systemDefault();
        if (from != null) {
            dateFrom = from.atStartOfDay(zone).toInstant();
        }
        if (to != null) {
            // inclusive end of day
            dateTo = to.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1);
        }

        Page<AuditLogEntity> result = auditLogService.search(entityType, action, actor, dateFrom, dateTo, pageable);

        model.addAttribute("result", result);
        model.addAttribute("entityType", entityType);
        model.addAttribute("action", action);
        model.addAttribute("actor", actor);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("size", size);

        model.addAttribute("entityTypes", AuditEntityType.values());
        model.addAttribute("actions", AuditAction.values());

        return "admin/audit";
    }
}
