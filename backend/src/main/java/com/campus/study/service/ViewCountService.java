package com.campus.study.service;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
public class ViewCountService {

    private static final Logger log = LoggerFactory.getLogger(ViewCountService.class);

    private static final String VIEW_UNIQUE_KEY_PREFIX = "view:unique:";
    private static final String VIEW_COUNT_KEY_PREFIX = "view:count:";
    private static final String VIEW_DAILY_KEY_PREFIX = "view:daily:";
    private static final String IP_RATE_LIMIT_KEY_PREFIX = "view:rate:ip:";
    private static final String IP_BLOCK_KEY_PREFIX = "view:block:ip:";
    private static final String ABNORMAL_LOG_KEY_PREFIX = "view:abnormal:";
    private static final String DIRTY_MATERIALS_KEY = "view:dirty:materials";

    private static final long VIEW_UNIQUE_EXPIRE_HOURS = 24;
    private static final long VIEW_DAILY_EXPIRE_DAYS = 180;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;
    private static final int RATE_LIMIT_MAX_REQUESTS = 100;
    private static final long IP_BLOCK_DURATION_MINUTES = 30;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public boolean recordView(Long materialId, Long userId, String ip, String userAgent) {
        if (isIpBlocked(ip)) {
            logAbnormalView(materialId, userId, ip, userAgent, "IP_BLOCKED", "IP is blocked due to frequent requests");
            return false;
        }

        if (!checkRateLimit(ip, materialId)) {
            blockIp(ip);
            logAbnormalView(materialId, userId, ip, userAgent, "RATE_LIMIT_EXCEEDED",
                    "IP exceeded rate limit: " + RATE_LIMIT_MAX_REQUESTS + " requests in " + RATE_LIMIT_WINDOW_SECONDS + "s");
            return false;
        }

        String uniqueKey = generateUniqueKey(materialId, userId, ip, userAgent);
        String redisKey = VIEW_UNIQUE_KEY_PREFIX + uniqueKey;

        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, 1, VIEW_UNIQUE_EXPIRE_HOURS, TimeUnit.HOURS);

        if (Boolean.TRUE.equals(isNew)) {
            incrementViewCount(materialId);
            return true;
        }

        return false;
    }

    public int getViewCount(Long materialId) {
        String key = VIEW_COUNT_KEY_PREFIX + materialId;
        Object count = redisTemplate.opsForValue().get(key);
        if (count instanceof Number) {
            return ((Number) count).intValue();
        }
        return 0;
    }

    public void setViewCount(Long materialId, int count) {
        String key = VIEW_COUNT_KEY_PREFIX + materialId;
        redisTemplate.opsForValue().set(key, count);
    }

    public void incrementViewCount(Long materialId) {
        String key = VIEW_COUNT_KEY_PREFIX + materialId;
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.opsForSet().add(DIRTY_MATERIALS_KEY, materialId);

        String dailyKey = VIEW_DAILY_KEY_PREFIX + java.time.LocalDate.now().toString();
        redisTemplate.opsForZSet().incrementScore(dailyKey, String.valueOf(materialId), 1);
        redisTemplate.expire(dailyKey, VIEW_DAILY_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    public java.util.Map<Long, Integer> getViewCountsInDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        java.util.Map<Long, Integer> result = new java.util.HashMap<>();
        java.time.LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            String dailyKey = VIEW_DAILY_KEY_PREFIX + date.toString();
            var zSetEntries = redisTemplate.opsForZSet().rangeWithScores(dailyKey, 0, -1);
            if (zSetEntries != null) {
                for (var entry : zSetEntries) {
                    try {
                        Long materialId = Long.valueOf(String.valueOf(entry.getValue()));
                        int score = entry.getScore() != null ? entry.getScore().intValue() : 0;
                        result.merge(materialId, score, Integer::sum);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            date = date.plusDays(1);
        }
        return result;
    }

    private boolean checkRateLimit(String ip, Long materialId) {
        String key = IP_RATE_LIMIT_KEY_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key, 1);

        if (count != null && count == 1) {
            redisTemplate.expire(key, RATE_LIMIT_WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        return count != null && count <= RATE_LIMIT_MAX_REQUESTS;
    }

    private boolean isIpBlocked(String ip) {
        String key = IP_BLOCK_KEY_PREFIX + ip;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void blockIp(String ip) {
        String key = IP_BLOCK_KEY_PREFIX + ip;
        redisTemplate.opsForValue().set(key, 1, IP_BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        log.warn("IP {} has been blocked for {} minutes due to frequent view requests", ip, IP_BLOCK_DURATION_MINUTES);
    }

    private void logAbnormalView(Long materialId, Long userId, String ip, String userAgent,
                                  String type, String reason) {
        String logKey = ABNORMAL_LOG_KEY_PREFIX + System.currentTimeMillis() + ":" + ip;
        java.util.Map<String, Object> logData = new java.util.HashMap<>();
        logData.put("materialId", materialId);
        logData.put("userId", userId);
        logData.put("ip", ip);
        logData.put("userAgent", userAgent);
        logData.put("type", type);
        logData.put("reason", reason);
        logData.put("timestamp", System.currentTimeMillis());

        redisTemplate.opsForValue().set(logKey, logData, 7, TimeUnit.DAYS);

        log.warn("Abnormal view detected - type: {}, materialId: {}, userId: {}, ip: {}, reason: {}",
                type, materialId, userId, ip, reason);
    }

    private String generateUniqueKey(Long materialId, Long userId, String ip, String userAgent) {
        String raw = materialId + ":" + (userId != null ? userId : "guest") + ":" + ip + ":" + userAgent;
        return md5(raw);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
