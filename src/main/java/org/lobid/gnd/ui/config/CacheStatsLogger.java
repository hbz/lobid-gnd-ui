package org.lobid.gnd.ui.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.lobid.gnd.ui.LobidGndApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CacheStatsLogger {

    private static final Logger log = LoggerFactory.getLogger(CacheStatsLogger.class);

    @Scheduled(fixedDelayString = "1h", initialDelayString = "1m")
    public void logCacheStats() {
        log.info(
                "[stats] cache sizes/hits/evictions: entity {}, search {}, label {}",
                details(LobidGndApiService.entityCache),
                details(LobidGndApiService.searchCache),
                details(LobidGndApiService.labelCache));
    }

    private <T> String details(Cache<String, Mono<T>> cache) {
        CacheStats stats = cache.stats();
        return String.format(
                "%s/%s/%s", cache.estimatedSize(), hitRate(stats), stats.evictionCount());
    }

    private String hitRate(CacheStats stats) {
        return String.format("%.0f", stats.hitRate() * 100) + "%";
    }
}
