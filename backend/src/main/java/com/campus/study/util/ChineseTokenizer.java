package com.campus.study.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChineseTokenizer {

    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "他", "她", "它", "们", "那", "些", "什么", "为", "所", "以",
            "但", "而", "与", "或", "及", "等", "被", "把", "从", "对", "中", "里",
            "后", "前", "下", "外", "内", "之", "其", "此", "个", "每", "各", "该",
            "本", "些", "则", "却", "又", "且", "则", "若", "如", "因", "此", "故",
            "可以", "可", "能", "将", "将", "会", "应", "该", "需", "须", "必",
            "还", "更", "最", "已", "曾", "正", "在", "于", "及", "等",
            "包括", "包含", "涵盖", "涉及", "有关", "关于", "为了", "通过",
            "方便", "适合", "便于", "配有", "附有", "含有",
            "详细", "完整", "全套", "全部", "所有", "各种", "多个",
            "进行", "使用", "提供", "支持", "帮助", "整理", "梳理",
            "如", "如下", "例如", "比如", "即", "亦", "还是",
            "以下", "以上", "之内", "之外", "之间", "之中",
            "第", "共", "份", "项", "次", "期", "册", "套",
            "篇", "章", "节", "部", "课", "道"
    );

    private static final Set<String> EDUCATIONAL_COMPOUNDS = Set.of(
            "知识点", "考点", "重点", "难点", "热点", "考点归纳", "知识体系",
            "思维导图", "专项训练", "专项练习", "复习专题", "复习资料",
            "期中考试", "期末考试", "期中测试", "期末测试", "期中检测",
            "月考", "模拟考", "模拟卷", "真题", "试题", "试卷", "测试卷",
            "练习题", "习题", "考题", "例题", "解答题", "选择题",
            "填空题", "判断题", "计算题", "证明题", "应用题",
            "必修", "选修", "上册", "下册", "全册", "全解",
            "教案", "课件", "学案", "导学案", "教学设计",
            "答案", "解析", "详解", "答案解析", "答案详解",
            "归纳", "总结", "汇总", "梳理", "概括", "整理",
            "备考", "复习", "预习", "巩固", "提升", "冲刺",
            "基础", "进阶", "提高", "培优", "拓展", "强化",
            "上学期", "下学期", "第一学期", "第二学期",
            "一轮复习", "二轮复习", "三轮复习",
            "竞赛", "奥数", "培优", "拔高",
            "函数", "方程", "不等式", "集合", "数列", "概率", "统计",
            "几何", "代数", "三角", "向量", "矩阵", "微积分",
            "力学", "热学", "光学", "电学", "电磁学", "原子物理",
            "有机化学", "无机化学", "化学反应", "元素周期",
            "细胞", "遗传", "进化", "生态", "分子", "基因",
            "古诗文", "现代文", "阅读理解", "作文", "文言文", "语法",
            "时态", "语态", "从句", "词汇", "完形填空", "听力",
            "中国古代史", "近代史", "现代史", "世界史",
            "自然地理", "人文地理", "区域地理"
    );

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");
    private static final Pattern DELIMITER_PATTERN = Pattern.compile(
            "[\\s,，。.!！?？;；:：、/\\\\|()（）\\[\\]【】{}\"''\"《》<>\\-—_=+@#$%^&*~`·]+"
    );

    public static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        Map<String, Integer> wordFreq = new LinkedHashMap<>();
        String[] segments = DELIMITER_PATTERN.split(text);

        for (String segment : segments) {
            if (segment.isBlank()) continue;
            extractFromSegment(segment, wordFreq);
        }

        return wordFreq.entrySet().stream()
                .filter(e -> e.getKey().length() >= 2)
                .filter(e -> !STOP_WORDS.contains(e.getKey()))
                .filter(e -> !isMostlyStopWords(e.getKey()))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static void extractFromSegment(String segment, Map<String, Integer> wordFreq) {
        Matcher matcher = CHINESE_PATTERN.matcher(segment);
        while (matcher.find()) {
            String chineseChunk = matcher.group();
            extractChineseNgrams(chineseChunk, wordFreq);
        }

        String cleaned = segment.replaceAll("[\\u4e00-\\u9fa5]+", " ").trim();
        if (!cleaned.isBlank()) {
            String[] nonChineseParts = cleaned.split("\\s+");
            for (String part : nonChineseParts) {
                if (part.length() >= 2 && part.length() <= 10) {
                    wordFreq.merge(part, 1, Integer::sum);
                }
            }
        }
    }

    private static void extractChineseNgrams(String chunk, Map<String, Integer> wordFreq) {
        if (chunk.length() < 2) return;

        for (String compound : EDUCATIONAL_COMPOUNDS) {
            int idx = chunk.indexOf(compound);
            while (idx >= 0) {
                wordFreq.merge(compound, 2, Integer::sum);

                if (idx > 0) {
                    String before = chunk.substring(Math.max(0, idx - 2), idx);
                    if (before.length() >= 2) {
                        wordFreq.merge(before, 1, Integer::sum);
                    }
                }
                if (idx + compound.length() < chunk.length()) {
                    int end = Math.min(chunk.length(), idx + compound.length() + 2);
                    String after = chunk.substring(idx + compound.length(), end);
                    if (after.length() >= 2) {
                        wordFreq.merge(after, 1, Integer::sum);
                    }
                }

                idx = chunk.indexOf(compound, idx + compound.length());
            }
        }

        for (int n = 4; n >= 2; n--) {
            for (int i = 0; i <= chunk.length() - n; i++) {
                String gram = chunk.substring(i, i + n);
                if (!EDUCATIONAL_COMPOUNDS.contains(gram)) {
                    wordFreq.merge(gram, 1, Integer::sum);
                }
            }
        }
    }

    private static boolean isMostlyStopWords(String word) {
        if (word.length() <= 2) {
            return STOP_WORDS.contains(word);
        }
        int stopCount = 0;
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            if (STOP_WORDS.contains(ch)) {
                stopCount++;
            }
        }
        return stopCount > word.length() / 2;
    }
}
