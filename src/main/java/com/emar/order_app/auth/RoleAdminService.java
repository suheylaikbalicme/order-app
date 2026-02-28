package com.emar.order_app.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RoleAdminService {

    private static final Set<String> CORE_ROLES = Set.of("ADMIN", "USER", "VIEWER");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public RoleAdminService(RoleRepository roleRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public List<RoleEntity> findAll() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(RoleEntity::getName))
                .toList();
    }

    public long userCount(Long roleId) {
        return userRepository.countByRoles_Id(roleId);
    }

    @Transactional
    public void createRole(String name) {
        String normalized = normalizeRoleName(name);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Rol adı boş olamaz");
        }
        if (roleRepository.existsByName(normalized)) {
            throw new IllegalArgumentException("Bu rol zaten var: " + normalized);
        }
        RoleEntity r = new RoleEntity();
        r.setName(normalized);
        roleRepository.save(r);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        RoleEntity r = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        if (CORE_ROLES.contains(r.getName())) {
            throw new IllegalArgumentException("Çekirdek roller silinemez: " + r.getName());
        }

        long used = userRepository.countByRoles_Id(roleId);
        if (used > 0) {
            throw new IllegalArgumentException("Bu rol " + used + " kullanıcıda tanımlı. Önce kullanıcı rollerini güncelle.");
        }

        roleRepository.delete(r);
    }

    private String normalizeRoleName(String input) {
        if (input == null) return "";
        String s = input.trim();
        s = s.replace("ROLE_", "");
        s = s.replaceAll("[^A-Za-z0-9_\\-]", "");
        return s.toUpperCase(Locale.ROOT);
    }
}
