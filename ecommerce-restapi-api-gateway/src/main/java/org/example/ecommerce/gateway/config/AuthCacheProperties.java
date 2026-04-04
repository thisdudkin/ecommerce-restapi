package org.example.ecommerce.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

@ConfigurationProperties(prefix = "gateway.auth-cache")
public record AuthCacheProperties(
    Duration ttl,
    String keyPrefix
) {

    public AuthCacheProperties {
        ttl = ttl == null
            ? Duration.ofSeconds(60)
            : ttl;
        keyPrefix = StringUtils.hasText(keyPrefix)
            ? keyPrefix
            : "auth:validate:";
    }

}
