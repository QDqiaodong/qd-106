SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `grade` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '年级名称',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级表';

CREATE TABLE IF NOT EXISTS `subject` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '学科名称',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学科表';

CREATE TABLE IF NOT EXISTS `category` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

CREATE TABLE IF NOT EXISTS `user` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT '' COMMENT '头像',
  `phone` VARCHAR(20) DEFAULT '' COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT '' COMMENT '邮箱',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1正常 0禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `material` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NOT NULL COMMENT '资料标题',
  `description` TEXT COMMENT '资料描述',
  `cover` VARCHAR(255) DEFAULT '' COMMENT '封面图',
  `category_id` INT UNSIGNED DEFAULT 0 COMMENT '分类ID',
  `grade_id` INT UNSIGNED DEFAULT 0 COMMENT '年级ID',
  `subject_id` INT UNSIGNED DEFAULT 0 COMMENT '学科ID',
  `file_url` VARCHAR(255) DEFAULT '' COMMENT '文件地址',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小',
  `total_pages` INT DEFAULT 0 COMMENT '总页数',
  `download_count` INT DEFAULT 0 COMMENT '下载次数',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `user_id` INT UNSIGNED DEFAULT 0 COMMENT '上传用户ID',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1正常 0下架',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_grade` (`grade_id`),
  KEY `idx_subject` (`subject_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资料表';

CREATE TABLE IF NOT EXISTS `favorite` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL COMMENT '用户ID',
  `material_id` INT UNSIGNED NOT NULL COMMENT '资料ID',
  `review_status` TINYINT DEFAULT 0 COMMENT '复习状态 0未标记 1已看过 2待精读 3待打印',
  `reviewed_at` DATETIME DEFAULT NULL COMMENT '复习标记时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_material` (`user_id`, `material_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_material` (`material_id`),
  KEY `idx_review_status` (`review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

CREATE TABLE IF NOT EXISTS `bookmark` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL COMMENT '用户ID',
  `material_id` INT UNSIGNED NOT NULL COMMENT '资料ID',
  `page_number` INT DEFAULT NULL COMMENT '页码',
  `chapter_name` VARCHAR(100) DEFAULT '' COMMENT '章节名称',
  `note` VARCHAR(255) DEFAULT '' COMMENT '备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_material` (`user_id`, `material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书签表';

CREATE TABLE IF NOT EXISTS `reading_progress` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL COMMENT '用户ID',
  `material_id` INT UNSIGNED NOT NULL COMMENT '资料ID',
  `page_number` INT DEFAULT 1 COMMENT '当前阅读页码',
  `last_read_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后阅读时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_material` (`user_id`, `material_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阅读进度表';

CREATE TABLE IF NOT EXISTS `filter_snapshot` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` INT UNSIGNED NOT NULL COMMENT '用户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '快照名称',
  `keyword` VARCHAR(255) DEFAULT '' COMMENT '关键词',
  `category_id` INT UNSIGNED DEFAULT NULL COMMENT '分类ID',
  `grade_id` INT UNSIGNED DEFAULT NULL COMMENT '年级ID',
  `subject_id` INT UNSIGNED DEFAULT NULL COMMENT '学科ID',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='筛选快照表';

INSERT IGNORE INTO `grade` (`id`, `name`, `sort`) VALUES
(1, '一年级', 1),
(2, '二年级', 2),
(3, '三年级', 3),
(4, '四年级', 4),
(5, '五年级', 5),
(6, '六年级', 6),
(7, '七年级', 7),
(8, '八年级', 8),
(9, '九年级', 9),
(10, '高一', 10),
(11, '高二', 11),
(12, '高三', 12);

INSERT IGNORE INTO `subject` (`id`, `name`, `sort`) VALUES
(1, '语文', 1),
(2, '数学', 2),
(3, '英语', 3),
(4, '物理', 4),
(5, '化学', 5),
(6, '生物', 6),
(7, '政治', 7),
(8, '历史', 8),
(9, '地理', 9),
(10, '科学', 10);

INSERT IGNORE INTO `category` (`id`, `name`, `sort`) VALUES
(1, '试卷', 1),
(2, '课件', 2),
(3, '教案', 3),
(4, '习题', 4),
(5, '知识点总结', 5),
(6, '竞赛资料', 6);

INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `phone`) VALUES
(1, 'admin', 'admin123', '管理员', '13800138000');

INSERT IGNORE INTO `material` (`id`, `title`, `description`, `category_id`, `grade_id`, `subject_id`, `view_count`, `download_count`, `user_id`, `status`) VALUES
(1, '高一数学必修一第一章知识点总结', '包含集合、函数概念与表示、函数的基本性质等核心知识点的详细梳理，配有典型例题解析。', 5, 10, 2, 128, 45, 1, 1),
(2, '九年级物理期中测试卷（附答案）', '2024学年第一学期九年级物理期中检测试卷，涵盖力学、热学重点内容，含详细答案解析。', 1, 9, 4, 95, 32, 1, 1),
(3, '初二英语语法专项练习题', '初中英语时态、语态、从句等核心语法专项训练，共120道精选习题，附答案详解。', 4, 8, 3, 156, 67, 1, 1),
(4, '高中语文古诗文背诵篇目汇总', '整理高中阶段所有必背古诗文篇目，包含原文、注释、赏析及背诵技巧，方便学生复习备考。', 5, 10, 1, 203, 89, 1, 1),
(5, '小学六年级数学奥数竞赛训练题', '精选小学奥数竞赛典型题型，包含计算、几何、数论、应用题等模块，适合培优提升。', 6, 6, 2, 78, 28, 1, 1),
(6, '高一化学必修一教学课件PPT', '完整的高一化学必修一教学课件，共24课时，包含原子结构、元素周期律、化学反应等内容。', 2, 10, 5, 112, 56, 1, 1),
(7, '七年级生物知识点思维导图', '以思维导图形式梳理七年级生物全部知识点，直观清晰，便于学生建立知识体系。', 5, 7, 6, 89, 41, 1, 1),
(8, '高三历史二轮复习专题教案', '高三历史二轮复习全套教案，包含中国古代史、近代史、现代史及世界史专题模块。', 3, 12, 8, 167, 72, 1, 1);

SET FOREIGN_KEY_CHECKS = 1;
