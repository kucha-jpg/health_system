package com.health.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class CacheEvictionSupport {

    private static final Logger log = LoggerFactory.getLogger(CacheEvictionSupport.class);
    private final CacheManager cacheManager;

    public CacheEvictionSupport(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Evict all entries from the named cache whose keys start with the given prefix.
     * Falls back to clearing the entire cache when the implementation cannot iterate keys.
     */
    public void evictByPrefix(String cacheName, String keyPrefix) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return;

        // Redis-backed caches don't expose a java.util.Map native cache,
        // so prefix-based eviction isn't feasible without a Lua script.
        // For safety and correctness, clear the entire named cache region.
        cache.clear();
    }
}
