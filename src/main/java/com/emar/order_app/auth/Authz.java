package com.emar.order_app.auth;

import org.springframework.security.core.Authentication;

import java.util.Arrays;


public final class Authz {

    private Authz() {}

    public static boolean hasRole(Authentication auth, String role) {
        if (auth == null || auth.getAuthorities() == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
    }

    public static boolean hasAnyRole(Authentication auth, String... roles) {
        if (roles == null || roles.length == 0) return false;
        return Arrays.stream(roles).anyMatch(r -> hasRole(auth, r));
    }

    public static boolean isAdmin(Authentication auth) {
        return hasRole(auth, "ROLE_ADMIN");
    }

    public static boolean isUser(Authentication auth) {
        return hasRole(auth, "ROLE_USER") || isAdmin(auth);
    }

    public static boolean isViewer(Authentication auth) {
        return hasRole(auth, "ROLE_VIEWER") && !isUser(auth);
    }

     public static boolean canView(Authentication auth) {
        return auth != null && hasAnyRole(auth, "ROLE_VIEWER", "ROLE_USER", "ROLE_ADMIN");
    }

    public static boolean canEdit(Authentication auth) {
        return isUser(auth); // USER includes ADMIN via isUser()
    }

    public static boolean canSubmit(Authentication auth) {
        return isUser(auth);
    }

    public static boolean canApprove(Authentication auth) {
        return isAdmin(auth);
    }

    public static boolean canRevise(Authentication auth) {
        return isUser(auth);
    }

    public static boolean canConvert(Authentication auth) {
        return isUser(auth);
    }

}
