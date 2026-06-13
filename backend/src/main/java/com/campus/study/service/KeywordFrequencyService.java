package com.campus.study.service;

import com.campus.study.entity.Category;
import com.campus.study.entity.KeywordFrequency;
import com.campus.study.entity.Material;
import com.campus.study.entity.Subject;
import com.campus.study.repository.CategoryRepository;
import com.campus.study.repository.KeywordFrequencyRepository;
import com.campus.study.repository.MaterialRepository;
import com.campus.study.repository.SubjectRepository;
import com.campus.study.util.ChineseTokenizer;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class KeywordFrequencyService {

    private static final Logger log = LoggerFactory.getLogger(KeywordFrequencyService.class);

    private static final String TRENDING_GLOBAL_CACHE_KEY = "keyword:trending:global";
    private static final String TRENDING_SUBJECT_CACHE_KEY_PREFIX = "keyword:trending:subject:";
    private static final String TRENDING_RECENT_CACHE_KEY = "keyword:trending:recent:";
    private static final long CACHE_TIMEOUT = 15;
    private static final int MAX_KEYWORDS_PER_SUBJECT = 50;
    private static final int MAX_KEYWORDS_GLOBAL = 100;

    @Resource
    private KeywordFrequencyRepository keywordFrequencyRepository;

    @Resource
    private MaterialRepository materialRepository;

    @Resource
    private SubjectRepository subjectRepository;

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void indexMaterial(Material material) {
        if (material == null || material.getTitle() == null) {
            return;
        }

        StringBuilder textBuilder = new StringBuilder();
        textBuilder.append(material.getTitle()).append(" ");
        if (material.getDescription() != null && !material.getDescription().isBlank()) {
            textBuilder.append(material.getDescription()).append(" ");
        }

        if (material.getCategoryId() != null && material.getCategoryId() > 0) {
            Category category = categoryRepository.findById(material.getCategoryId()).orElse(null);
            if (category != null) {
                textBuilder.append(category.getName()).append(" ");
            }
        }

        if (material.getSubjectId() != null && material.getSubjectId() > 0) {
            Subject subject = subjectRepository.findById(material.getSubjectId()).orElse(null);
            if (subject != null) {
                textBuilder.append(subject.getName()).append(" ");
            }
        }

        List<String> keywords = ChineseTokenizer.tokenize(textBuilder.toString());
        LocalDateTime now = LocalDateTime.now();

        Set<String> seen = new HashSet<>();
        for (String word : keywords) {
            if (seen.contains(word)) continue;
            seen.add(word);

            if (material.getSubjectId() != null && material.getSubjectId() > 0) {
                upsertKeyword(word, material.getSubjectId(), now);
            }
            upsertKeyword(word, 0L, now);
        }

        clearTrendingCache();
    }

    @Transactional
    public void indexMaterialBatch(List<Material> materials) {
        for (Material material : materials) {
            indexMaterial(material);
        }
        clearTrendingCache();
    }

    private void upsertKeyword(String word, Long subjectId, LocalDateTime now) {
        Optional<KeywordFrequency> existing = keywordFrequencyRepository.findByWordAndSubjectId(word, subjectId);
        if (existing.isPresent()) {
            KeywordFrequency kf = existing.get();
            kf.setFrequency(kf.getFrequency() + 1);
            kf.setLastSeenAt(now);
            keywordFrequencyRepository.save(kf);
        } else {
            KeywordFrequency kf = new KeywordFrequency();
            kf.setWord(word);
            kf.setSubjectId(subjectId);
            kf.setFrequency(1);
            kf.setLastSeenAt(now);
            keywordFrequencyRepository.save(kf);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTrendingGlobal(int limit) {
        String cacheKey = TRENDING_GLOBAL_CACHE_KEY;
        List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        List<KeywordFrequency> allKeywords = keywordFrequencyRepository.findBySubjectIdOrderByFrequencyDesc(0L);
        List<Map<String, Object>> result = allKeywords.stream()
                .limit(MAX_KEYWORDS_GLOBAL)
                .map(this::toMap)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, result, CACHE_TIMEOUT, TimeUnit.MINUTES);
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTrendingBySubject(Long subjectId, int limit) {
        String cacheKey = TRENDING_SUBJECT_CACHE_KEY_PREFIX + subjectId;
        List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        List<KeywordFrequency> keywords = keywordFrequencyRepository.findBySubjectIdOrderByFrequencyDesc(subjectId);
        List<Map<String, Object>> result = keywords.stream()
                .limit(MAX_KEYWORDS_PER_SUBJECT)
                .map(kf -> {
                    Map<String, Object> map = toMap(kf);
                    map.put("subjectId", subjectId);
                    return map;
                })
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, result, CACHE_TIMEOUT, TimeUnit.MINUTES);
        return result.stream().limit(limit).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentTrending(Long subjectId, int days, int limit) {
        String cacheKey = TRENDING_RECENT_CACHE_KEY + (subjectId != null ? subjectId : "all") + ":" + days;
        List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<KeywordFrequency> keywords;

        if (subjectId != null && subjectId > 0) {
            keywords = keywordFrequencyRepository.findBySubjectIdAndLastSeenAtAfterOrderByFrequencyDesc(subjectId, since);
        } else {
            keywords = keywordFrequencyRepository.findGlobalRecent(since);
        }

        List<Map<String, Object>> result = keywords.stream()
                .limit(limit)
                .map(this::toMap)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, result, CACHE_TIMEOUT, TimeUnit.MINUTES);
        return result;
    }

    public Map<String, Object> getSubjectTrendingOverview() {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Subject> subjects = subjectRepository.findByStatusOrderBySortAsc(1);
        for (Subject subject : subjects) {
            List<Map<String, Object>> trending = getTrendingBySubject(subject.getId(), 10);
            if (!trending.isEmpty()) {
                Map<String, Object> subjectData = new LinkedHashMap<>();
                subjectData.put("subjectId", subject.getId());
                subjectData.put("subjectName", subject.getName());
                subjectData.put("keywords", trending);
                result.put(subject.getName(), subjectData);
            }
        }

        return result;
    }

    @Transactional
    public void rebuildIndex() {
        log.info("开始重建知识点词频索引...");
        keywordFrequencyRepository.deleteAllKeywords();
        clearTrendingCache();

        List<Material> allMaterials = materialRepository.findByStatus(1);
        log.info("共找到 {} 份有效资料，开始建立索引", allMaterials.size());

        int count = 0;
        for (Material material : allMaterials) {
            indexMaterial(material);
            count++;
            if (count % 100 == 0) {
                log.info("已处理 {}/{} 份资料", count, allMaterials.size());
            }
        }

        log.info("知识点词频索引重建完成，共处理 {} 份资料", count);
        clearTrendingCache();
    }

    private Map<String, Object> toMap(KeywordFrequency kf) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("word", kf.getWord());
        map.put("frequency", kf.getFrequency());
        map.put("lastSeenAt", kf.getLastSeenAt());
        return map;
    }

    private void clearTrendingCache() {
        redisTemplate.delete(TRENDING_GLOBAL_CACHE_KEY);
        redisTemplate.delete(TRENDING_RECENT_CACHE_KEY + "all:7");
        redisTemplate.delete(TRENDING_RECENT_CACHE_KEY + "all:30");
        var keys = redisTemplate.keys(TRENDING_SUBJECT_CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        keys = redisTemplate.keys(TRENDING_RECENT_CACHE_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
