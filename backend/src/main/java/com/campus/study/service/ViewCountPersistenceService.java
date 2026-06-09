package com.campus.study.service;

import com.campus.study.repository.MaterialRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class ViewCountPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ViewCountPersistenceService.class);

    private static final String VIEW_COUNT_KEY_PREFIX = "view:count:";
    private static final String DIRTY_MATERIALS_KEY = "view:dirty:materials";
    private static final String SYNC_LOCK_KEY = "view:sync:lock";
    private static final long SYNC_LOCK_EXPIRE_SECONDS = 60;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private MaterialRepository materialRepository;

    public void markDirty(Long materialId) {
        redisTemplate.opsForSet().add(DIRTY_MATERIALS_KEY, materialId);
    }

    @Scheduled(fixedDelayString = "${view.count.sync.interval:30000}")
    @Transactional
    public void syncViewCountsToDB() {
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(SYNC_LOCK_KEY, 1, SYNC_LOCK_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(lockAcquired)) {
            return;
        }

        try {
            Set<Object> dirtyIds = redisTemplate.opsForSet().members(DIRTY_MATERIALS_KEY);
            if (dirtyIds == null || dirtyIds.isEmpty()) {
                return;
            }

            int syncedCount = 0;
            Set<Long> failedIds = new HashSet<>();

            for (Object idObj : dirtyIds) {
                Long materialId = Long.valueOf(idObj.toString());
                String countKey = VIEW_COUNT_KEY_PREFIX + materialId;

                try {
                    Object countObj = redisTemplate.opsForValue().get(countKey);
                    if (countObj == null) {
                        redisTemplate.opsForSet().remove(DIRTY_MATERIALS_KEY, materialId);
                        continue;
                    }

                    int redisCount = ((Number) countObj).intValue();
                    if (redisCount <= 0) {
                        redisTemplate.opsForSet().remove(DIRTY_MATERIALS_KEY, materialId);
                        continue;
                    }

                    materialRepository.updateViewCount(materialId, redisCount);
                    redisTemplate.opsForValue().decrement(countKey, redisCount);
                    redisTemplate.opsForSet().remove(DIRTY_MATERIALS_KEY, materialId);
                    syncedCount++;

                } catch (Exception e) {
                    failedIds.add(materialId);
                    log.error("Failed to sync view count for materialId: {}", materialId, e);
                }
            }

            if (syncedCount > 0) {
                log.info("Successfully synced view counts for {} materials", syncedCount);
            }
            if (!failedIds.isEmpty()) {
                log.warn("Failed to sync view counts for {} materials: {}", failedIds.size(), failedIds);
            }

        } finally {
            redisTemplate.delete(SYNC_LOCK_KEY);
        }
    }

    public int getPendingSyncCount() {
        Set<Object> dirtyIds = redisTemplate.opsForSet().members(DIRTY_MATERIALS_KEY);
        return dirtyIds != null ? dirtyIds.size() : 0;
    }
}
