package com.campus.study.service;

import com.campus.study.entity.Category;
import com.campus.study.repository.CategoryRepository;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CategoryService {

    private static final String CACHE_KEY = "category:list:enabled";
    private static final long CACHE_TIMEOUT = 30;

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<Category> getEnabledList() {
        List<Category> cachedList = (List<Category>) redisTemplate.opsForValue().get(CACHE_KEY);
        if (cachedList != null) {
            return cachedList;
        }
        List<Category> list = categoryRepository.findByStatusOrderBySortAsc(1);
        redisTemplate.opsForValue().set(CACHE_KEY, list, CACHE_TIMEOUT, TimeUnit.MINUTES);
        return list;
    }

    public void clearCache() {
        redisTemplate.delete(CACHE_KEY);
    }
}
