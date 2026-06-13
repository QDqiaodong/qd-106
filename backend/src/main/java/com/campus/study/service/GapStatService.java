package com.campus.study.service;

import com.campus.study.entity.Category;
import com.campus.study.entity.Grade;
import com.campus.study.entity.Subject;
import com.campus.study.repository.CategoryRepository;
import com.campus.study.repository.GradeRepository;
import com.campus.study.repository.MaterialRepository;
import com.campus.study.repository.SubjectRepository;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class GapStatService {

    private static final String GAP_STAT_CACHE_KEY = "stat:gap";
    private static final long GAP_CACHE_TIMEOUT = 30;

    @Resource
    private MaterialRepository materialRepository;

    @Resource
    private GradeRepository gradeRepository;

    @Resource
    private SubjectRepository subjectRepository;

    @Resource
    private CategoryRepository categoryRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public Map<String, Object> getGapStatistics() {
        @SuppressWarnings("unchecked")
        Map<String, Object> cached = (Map<String, Object>) redisTemplate.opsForValue().get(GAP_STAT_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        List<Grade> grades = gradeRepository.findByStatusOrderBySortAsc(1);
        List<Subject> subjects = subjectRepository.findByStatusOrderBySortAsc(1);
        List<Category> categories = categoryRepository.findByStatusOrderBySortAsc(1);

        List<Object[]> rawCounts = materialRepository.countGroupByGradeSubjectCategory();

        Map<String, Long> countMap = new HashMap<>();
        Map<Long, Long> gradeCountMap = new HashMap<>();
        Map<Long, Long> subjectCountMap = new HashMap<>();
        Map<Long, Long> categoryCountMap = new HashMap<>();
        long totalMaterials = 0;

        for (Object[] row : rawCounts) {
            Long gradeId = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            Long subjectId = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            Long categoryId = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            Long count = ((Number) row[3]).longValue();

            String key = gradeId + "_" + subjectId + "_" + categoryId;
            countMap.put(key, count);
            totalMaterials += count;

            gradeCountMap.merge(gradeId, count, Long::sum);
            subjectCountMap.merge(subjectId, count, Long::sum);
            categoryCountMap.merge(categoryId, count, Long::sum);
        }

        int totalSlots = grades.size() * subjects.size() * categories.size();
        int coveredSlots = 0;
        List<Map<String, Object>> gapList = new ArrayList<>();
        List<Map<String, Object>> coverageMatrix = new ArrayList<>();

        Map<Long, String> gradeNameMap = new HashMap<>();
        Map<Long, String> subjectNameMap = new HashMap<>();
        Map<Long, String> categoryNameMap = new HashMap<>();

        for (Grade g : grades) gradeNameMap.put(g.getId(), g.getName());
        for (Subject s : subjects) subjectNameMap.put(s.getId(), s.getName());
        for (Category c : categories) categoryNameMap.put(c.getId(), c.getName());

        for (Grade grade : grades) {
            for (Subject subject : subjects) {
                Map<String, Object> row = new HashMap<>();
                row.put("gradeId", grade.getId());
                row.put("gradeName", grade.getName());
                row.put("subjectId", subject.getId());
                row.put("subjectName", subject.getName());

                int rowTotal = 0;
                List<Map<String, Object>> categoryDetails = new ArrayList<>();

                for (Category category : categories) {
                    String key = grade.getId() + "_" + subject.getId() + "_" + category.getId();
                    long count = countMap.getOrDefault(key, 0L);
                    rowTotal += count;

                    Map<String, Object> cell = new HashMap<>();
                    cell.put("categoryId", category.getId());
                    cell.put("categoryName", category.getName());
                    cell.put("count", count);
                    categoryDetails.add(cell);

                    if (count > 0) {
                        coveredSlots++;
                    } else {
                        Map<String, Object> gap = new HashMap<>();
                        gap.put("gradeId", grade.getId());
                        gap.put("gradeName", grade.getName());
                        gap.put("subjectId", subject.getId());
                        gap.put("subjectName", subject.getName());
                        gap.put("categoryId", category.getId());
                        gap.put("categoryName", category.getName());
                        gap.put("count", 0L);
                        gapList.add(gap);
                    }
                }

                row.put("total", rowTotal);
                row.put("categories", categoryDetails);
                coverageMatrix.add(row);
            }
        }

        List<Map<String, Object>> gradeStats = new ArrayList<>();
        for (Grade grade : grades) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("gradeId", grade.getId());
            stat.put("gradeName", grade.getName());
            stat.put("count", gradeCountMap.getOrDefault(grade.getId(), 0L));
            gradeStats.add(stat);
        }

        List<Map<String, Object>> subjectStats = new ArrayList<>();
        for (Subject subject : subjects) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("subjectId", subject.getId());
            stat.put("subjectName", subject.getName());
            stat.put("count", subjectCountMap.getOrDefault(subject.getId(), 0L));
            subjectStats.add(stat);
        }

        List<Map<String, Object>> categoryStats = new ArrayList<>();
        for (Category category : categories) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("categoryId", category.getId());
            stat.put("categoryName", category.getName());
            stat.put("count", categoryCountMap.getOrDefault(category.getId(), 0L));
            categoryStats.add(stat);
        }

        double coverageRate = totalSlots > 0 ? (double) coveredSlots / totalSlots * 100 : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalMaterials", totalMaterials);
        result.put("totalSlots", totalSlots);
        result.put("coveredSlots", coveredSlots);
        result.put("gapCount", totalSlots - coveredSlots);
        result.put("coverageRate", Math.round(coverageRate * 100.0) / 100.0);
        result.put("gradeStats", gradeStats);
        result.put("subjectStats", subjectStats);
        result.put("categoryStats", categoryStats);
        result.put("coverageMatrix", coverageMatrix);
        result.put("gapList", gapList);

        redisTemplate.opsForValue().set(GAP_STAT_CACHE_KEY, result, GAP_CACHE_TIMEOUT, TimeUnit.MINUTES);
        return result;
    }

    public void clearCache() {
        redisTemplate.delete(GAP_STAT_CACHE_KEY);
    }
}
