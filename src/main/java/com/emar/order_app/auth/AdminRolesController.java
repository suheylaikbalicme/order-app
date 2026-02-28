package com.emar.order_app.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/roles")
public class AdminRolesController {

    private final RoleAdminService roleAdminService;

    public AdminRolesController(RoleAdminService roleAdminService) {
        this.roleAdminService = roleAdminService;
    }

    @GetMapping
    public String roles(Model model,
                        @RequestParam(name = "error", required = false) String error,
                        @RequestParam(name = "ok", required = false) String ok) {
        var roles = roleAdminService.findAll();
        java.util.Map<Long, Long> counts = new java.util.HashMap<>();
        for (var r : roles) {
            counts.put(r.getId(), roleAdminService.userCount(r.getId()));
        }
        model.addAttribute("roles", roles);
        model.addAttribute("counts", counts);
        model.addAttribute("error", error);
        model.addAttribute("ok", ok);
        return "admin/roles";
    }

    @PostMapping("/create")
    public String create(@RequestParam String name) {
        try {
            roleAdminService.createRole(name);
            return "redirect:/admin/roles?ok=created";
        } catch (Exception ex) {
            return "redirect:/admin/roles?error=" + urlSafe(ex.getMessage());
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Long id) {
        try {
            roleAdminService.deleteRole(id);
            return "redirect:/admin/roles?ok=deleted";
        } catch (Exception ex) {
            return "redirect:/admin/roles?error=" + urlSafe(ex.getMessage());
        }
    }

    private String urlSafe(String s) {
        if (s == null) return "";
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
