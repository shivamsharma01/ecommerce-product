package com.mcart.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Security toggles for JWT resource-server mode (align with auth-issued tokens).
 */
@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * When true, {@code /api/**} requires a valid JWT (issuer / JWK must be configured).
     */
    private boolean enabled = false;

    /**
     * Browser origins allowed for CORS (e.g. https://app.example.com). Empty = no CORS filter.
     */
    private List<String> corsAllowedOrigins = new ArrayList<>();

    /**
     * If set, every method on {@code /api/**} (including GET/HEAD) requires this OAuth2 scope
     * (e.g. {@code product.admin} → authority {@code SCOPE_product.admin}).
     */
    private String requiredScope;
}
