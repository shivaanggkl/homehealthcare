package com.homehealthcare.security.tenant;

import java.util.Optional;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CURRENT_CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static Optional<TenantContext> get() {
        return Optional.ofNullable(CURRENT_CONTEXT.get());
    }

    public static void set(TenantContext context) {
        CURRENT_CONTEXT.set(context);
    }

    public static void clear() {
        CURRENT_CONTEXT.remove();
    }
}
