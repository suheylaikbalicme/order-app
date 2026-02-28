package com.emar.order_app.auth;

import org.springframework.security.access.AccessDeniedException;

public final class Ownership {

    private Ownership() {}

    public static void assertOwnerOrAdmin(boolean isAdmin,
                                          boolean isUser,
                                          String actorUsername,
                                          String ownerUsername,
                                          String denyMessage) {

        if (isAdmin) return;

        if (!isUser) {
            throw new AccessDeniedException(denyMessage != null ? denyMessage : "Yetkin yok.");
        }

        if (ownerUsername == null || !ownerUsername.equals(actorUsername)) {
            throw new AccessDeniedException(denyMessage != null ? denyMessage : "Yetkin yok.");
        }
    }
}
