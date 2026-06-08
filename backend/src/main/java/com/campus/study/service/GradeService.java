package com.campus.study.service;

import com.campus.study.entity.Grade;
import com.campus.study.repository.GradeRepository;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class GradeService {

    private static final String CACHE_KEY = "grade:list:enabled";
    private static final long CACHE_TIMEOUT = 30;

    @Resource
    private GradeRepository gradeRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<Grade> getEnabledList() {
        List<Grade> cachedList = (List<Grade>) redisTemplate.opsForValue().get(CACHE_KEY);
        if (cachedList != null) {
            return cachedList;
        }
        List<Grade> list = gradeRepository.findByStatusOrderBySortAsc(1);
        redisTemplate.opsForValue().set(CACHE_KEY, list, CACHE_TIMEOUT, TimeUnit.MINUTES);
        return list;
    }

    public void clearCache() {
        redisTemplate.delete(CACHE_KEY);
    }
}
