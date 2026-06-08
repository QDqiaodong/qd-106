package com.campus.study.service;

import com.campus.study.entity.Subject;
import com.campus.study.repository.SubjectRepository;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SubjectService {

    private static final String CACHE_KEY = "subject:list:enabled";
    private static final long CACHE_TIMEOUT = 30;

    @Resource
    private SubjectRepository subjectRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<Subject> getEnabledList() {
        List<Subject> cachedList = (List<Subject>) redisTemplate.opsForValue().get(CACHE_KEY);
        if (cachedList != null) {
            return cachedList;
        }
        List<Subject> list = subjectRepository.findByStatusOrderBySortAsc(1);
        redisTemplate.opsForValue().set(CACHE_KEY, list, CACHE_TIMEOUT, TimeUnit.MINUTES);
        return list;
    }

    public void clearCache() {
        redisTemplate.delete(CACHE_KEY);
    }
}
