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

        try {
            if (cache.getNativeCache() instanceof java.util.Map<?, ?> map) {
                map.keySet().removeIf(key -> key instanceof String str && str.startsWith(keyPrefix));
                log.debug("Evicted {} entries from cache {} by prefix {}", map.size(), cacheName, keyPrefix);
                return;
            }
        } catch (Exception ex) {
            log.warn("Failed to evict cache {} by prefix, falling back to clear all: {}", cacheName, ex.getMessage());
        }

        cache.clear();
    }
}
