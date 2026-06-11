package com.campus.study.service;

import com.campus.study.entity.Category;
import com.campus.study.entity.Grade;
import com.campus.study.entity.Subject;
import com.campus.study.repository.CategoryRepository;
import com.campus.study.repository.GradeRepository;
import com.campus.study.repository.SubjectRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CategoryValidationService {

    @Resource
    private GradeRepository gradeRepository;

    @Resource
    private SubjectRepository subjectRepository;

    @Resource
    private CategoryRepository categoryRepository;

    private static final Set<Long> PRIMARY_GRADES = Set.of(1L, 2L, 3L, 4L, 5L, 6L);
    private static final Set<Long> MIDDLE_GRADES = Set.of(7L, 8L, 9L);
    private static final Set<Long> HIGH_GRADES = Set.of(10L, 11L, 12L);

    private static final Set<Long> PRIMARY_ALLOWED_SUBJECTS = Set.of(1L, 2L, 3L, 10L);
    private static final Set<Long> MIDDLE_ALLOWED_SUBJECTS = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
    private static final Set<Long> HIGH_ALLOWED_SUBJECTS = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);

    private static final Set<Long> PRIMARY_FORBIDDEN_SUBJECTS = Set.of(4L, 5L, 6L, 7L, 8L, 9L);
    private static final Set<Long> MIDDLE_HIGH_FORBIDDEN_SUBJECTS = Set.of(10L);

    private static final List<KeywordGradeRule> KEYWORD_GRADE_RULES = Arrays.asList(
            new KeywordGradeRule(
                    Arrays.asList("识字", "拼音", "口算", "加减法", "乘法口诀", "生字", "看图写话", "三字经", "弟子规", "笔画", "笔顺"),
                    Set.of(1L, 2L, 3L),
                    "小学低年级（一至三年级）"
            ),
            new KeywordGradeRule(
                    Arrays.asList("小学奥数", "小升初", "小学数学思维"),
                    Set.of(4L, 5L, 6L),
                    "小学高年级（四至六年级）"
            ),
            new KeywordGradeRule(
                    Arrays.asList("初中", "中考", "八年级", "九年级", "初一", "初二", "初三"),
                    Set.of(7L, 8L, 9L),
                    "初中（七至九年级）"
            ),
            new KeywordGradeRule(
                    Arrays.asList("高中", "高考", "高一", "高二", "高三", "必修一", "必修二", "必修三", "选择性必修", "一轮复习", "二轮复习", "三轮复习"),
                    Set.of(10L, 11L, 12L),
                    "高中（高一至高三）"
            )
    );

    public void validateUpload(Long gradeId, Long subjectId, Long categoryId, String title) {
        List<String> errors = new ArrayList<>();

        if (gradeId != null && gradeId > 0) {
            if (subjectId != null && subjectId > 0) {
                String gradeSubjectError = validateGradeSubject(gradeId, subjectId);
                if (gradeSubjectError != null) {
                    errors.add(gradeSubjectError);
                }
            }
            if (title != null && !title.trim().isEmpty()) {
                String titleGradeError = validateTitleGrade(title, gradeId);
                if (titleGradeError != null) {
                    errors.add(titleGradeError);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", errors));
        }
    }

    public void validateFilter(Long gradeId, Long subjectId, Long categoryId) {
        List<String> errors = new ArrayList<>();

        if (gradeId != null && gradeId > 0 && subjectId != null && subjectId > 0) {
            String gradeSubjectError = validateGradeSubject(gradeId, subjectId);
            if (gradeSubjectError != null) {
                errors.add(gradeSubjectError);
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", errors));
        }
    }

    private String validateGradeSubject(Long gradeId, Long subjectId) {
        String gradeName = getGradeName(gradeId);
        String subjectName = getSubjectName(subjectId);

        if (PRIMARY_GRADES.contains(gradeId) && PRIMARY_FORBIDDEN_SUBJECTS.contains(subjectId)) {
            return String.format("分类组合不合法：%s不开设%s，请检查年级与学科是否匹配", gradeName, subjectName);
        }
        if ((MIDDLE_GRADES.contains(gradeId) || HIGH_GRADES.contains(gradeId))
                && MIDDLE_HIGH_FORBIDDEN_SUBJECTS.contains(subjectId)) {
            return String.format("分类组合不合法：%s不开设%s，请检查年级与学科是否匹配", gradeName, subjectName);
        }
        return null;
    }

    private String validateTitleGrade(String title, Long gradeId) {
        String lowerTitle = title.toLowerCase();

        for (KeywordGradeRule rule : KEYWORD_GRADE_RULES) {
            for (String keyword : rule.keywords) {
                if (lowerTitle.contains(keyword.toLowerCase())) {
                    if (!rule.allowedGradeIds.contains(gradeId)) {
                        String actualGradeName = getGradeName(gradeId);
                        return String.format("标题含有\"%s\"字样，更适合%s，当前选择的是%s，请确认分类是否正确",
                                keyword, rule.suitableGradeText, actualGradeName);
                    }
                    break;
                }
            }
        }
        return null;
    }

    private String getGradeName(Long gradeId) {
        if (gradeId == null || gradeId <= 0) {
            return "未选择年级";
        }
        return gradeRepository.findById(gradeId)
                .map(Grade::getName)
                .orElse("未知年级");
    }

    private String getSubjectName(Long subjectId) {
        if (subjectId == null || subjectId <= 0) {
            return "未选择学科";
        }
        return subjectRepository.findById(subjectId)
                .map(Subject::getName)
                .orElse("未知学科");
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return "未选择分类";
        }
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElse("未知分类");
    }

    public Map<String, Object> getValidationRules() {
        Map<String, Object> result = new HashMap<>();

        Map<Long, Set<Long>> gradeSubjectMap = new HashMap<>();
        for (Long grade : PRIMARY_GRADES) {
            gradeSubjectMap.put(grade, PRIMARY_ALLOWED_SUBJECTS);
        }
        for (Long grade : MIDDLE_GRADES) {
            gradeSubjectMap.put(grade, MIDDLE_ALLOWED_SUBJECTS);
        }
        for (Long grade : HIGH_GRADES) {
            gradeSubjectMap.put(grade, HIGH_ALLOWED_SUBJECTS);
        }
        result.put("gradeSubjectMap", gradeSubjectMap);

        List<Map<String, Object>> keywordRules = new ArrayList<>();
        for (KeywordGradeRule rule : KEYWORD_GRADE_RULES) {
            Map<String, Object> ruleMap = new HashMap<>();
            ruleMap.put("keywords", rule.keywords);
            ruleMap.put("allowedGradeIds", rule.allowedGradeIds);
            ruleMap.put("suitableGradeText", rule.suitableGradeText);
            keywordRules.add(ruleMap);
        }
        result.put("keywordRules", keywordRules);

        return result;
    }

    private static class KeywordGradeRule {
        final List<String> keywords;
        final Set<Long> allowedGradeIds;
        final String suitableGradeText;

        KeywordGradeRule(List<String> keywords, Set<Long> allowedGradeIds, String suitableGradeText) {
            this.keywords = keywords;
            this.allowedGradeIds = allowedGradeIds;
            this.suitableGradeText = suitableGradeText;
        }
    }
}
