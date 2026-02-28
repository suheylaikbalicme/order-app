package com.emar.order_app.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userAdminService.findAllUsers());
        model.addAttribute("roles", userAdminService.findAllRoles());
        return "admin/users";
    }

    @PostMapping
    public String createUser(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam Set<String> roles,
                             @RequestParam(defaultValue = "true") boolean enabled) {

        userAdminService.createUser(username, password, roles, enabled);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/roles")
    public String updateRoles(@PathVariable("id") Long id,
                              @RequestParam Set<String> roles) {
        userAdminService.updateUserRoles(id, roles);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/enabled")
    public String updateEnabled(@PathVariable("id") Long id,
                                @RequestParam boolean enabled) {
        userAdminService.setEnabled(id, enabled);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable("id") Long id,
                                @RequestParam("newPassword") String newPassword) {
        userAdminService.resetPassword(id, newPassword);
        return "redirect:/admin/users";
    }
}
