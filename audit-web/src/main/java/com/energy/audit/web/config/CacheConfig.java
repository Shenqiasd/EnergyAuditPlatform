package com.energy.audit.web.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // JCache (Ehcache 3) auto-configured via spring.cache.jcache.config in application.yml
}
