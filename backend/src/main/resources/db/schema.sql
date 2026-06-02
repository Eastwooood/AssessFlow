-- =====================================================
-- AssessFlow 自适应在线测评引擎 数据库建表SQL
-- 技术栈: MySQL 8.0+
-- =====================================================

CREATE DATABASE IF NOT EXISTS assess_flow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE assess_flow;

-- 1. 考试会话主表
DROP TABLE IF EXISTS `exam_session`;
CREATE TABLE `exam_session` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `paper_id` BIGINT NOT NULL COMMENT '试卷模板ID',
    `step_cursor` INT NOT NULL DEFAULT 0 COMMENT '步骤游标，指向下一个未结算环节',
    `status` VARCHAR(32) NOT NULL DEFAULT 'INIT' COMMENT '状态: INIT/IN_PROGRESS/AWAIT_ANSWER/FINISHED/ABANDONED',
    `total_score` DOUBLE NOT NULL DEFAULT 0.0 COMMENT '累计总分',
    `started_at` DATETIME NOT NULL COMMENT '开始时间',
    `updated_at` DATETIME NOT NULL COMMENT '更新时间',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_paper_id` (`paper_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试会话主表';

-- 2. 会话步骤子表
DROP TABLE IF EXISTS `session_step`;
CREATE TABLE `session_step` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `session_id` BIGINT NOT NULL COMMENT '所属会话ID',
    `step_index` INT NOT NULL COMMENT '步骤索引（从0开始）',
    `type` VARCHAR(32) NOT NULL COMMENT '环节类型: INFO/SCORE_SNAPSHOT/SINGLE/MULTI/JUDGE/BLANK',
    `question_id` BIGINT NULL COMMENT '题目ID（作答环节有值）',
    `candidate_options` TEXT NULL COMMENT '候选选项JSON（服务端打乱存储）',
    `user_answer` TEXT NULL COMMENT '用户答案JSON',
    `got_score` DOUBLE NULL COMMENT '本题得分',
    `settled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已结算: 0-未结算 1-已结算',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    UNIQUE KEY `uk_session_index` (`session_id`, `step_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话步骤子表';

-- 3. 题库表
DROP TABLE IF EXISTS `question_bank`;
CREATE TABLE `question_bank` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `stem` TEXT NOT NULL COMMENT '题干',
    `question_type` VARCHAR(32) NOT NULL COMMENT '题型: SINGLE/MULTI/JUDGE/BLANK',
    `options` TEXT NOT NULL COMMENT '选项JSON [{"optionId":1,"text":"A","isCorrect":true},...]',
    `correct_answer` TEXT NOT NULL COMMENT '正确答案JSON [1,2] 或文本',
    `difficulty` INT NOT NULL DEFAULT 3 COMMENT '难度等级 1-5',
    `tags` TEXT NULL COMMENT '标签JSON ["tag1","tag2"]',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_type` (`question_type`),
    INDEX `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='题库表';

-- 4. 试卷模板配置
DROP TABLE IF EXISTS `paper_template_conf`;
CREATE TABLE `paper_template_conf` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `paper_name` VARCHAR(128) NOT NULL COMMENT '试卷名称',
    `step_sequence` TEXT NOT NULL COMMENT '环节序列JSON',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='试卷模板配置';

-- 5. 题型规则配置
DROP TABLE IF EXISTS `question_type_conf`;
CREATE TABLE `question_type_conf` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `question_type` VARCHAR(32) NOT NULL COMMENT '题型',
    `shuffle_options` TINYINT NOT NULL DEFAULT 1 COMMENT '是否打乱选项',
    `grading_method` VARCHAR(64) NOT NULL COMMENT '评分方式',
    `extra_params` JSON NULL COMMENT '附加参数JSON',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type` (`question_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='题型规则配置';

-- 6. 评分规则配置
DROP TABLE IF EXISTS `grading_rule_conf`;
CREATE TABLE `grading_rule_conf` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `question_type` VARCHAR(32) NOT NULL COMMENT '适用题型',
    `rule_name` VARCHAR(64) NOT NULL COMMENT '规则名称',
    `score_per_question` DOUBLE NOT NULL DEFAULT 10.0 COMMENT '每题分值',
    `full_score_on_all_correct` TINYINT NOT NULL DEFAULT 1 COMMENT '全对是否满分',
    `allow_partial_score` TINYINT NOT NULL DEFAULT 0 COMMENT '是否支持部分得分',
    `zero_on_wrong_choice` TINYINT NOT NULL DEFAULT 1 COMMENT '错选是否0分',
    `partial_score_ratio` DOUBLE NULL COMMENT '部分得分比例(0-1)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type` (`question_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='评分规则配置';

-- 7. 题目黑名单配置
DROP TABLE IF EXISTS `question_blacklist_conf`;
CREATE TABLE `question_blacklist_conf` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `reason` VARCHAR(256) NULL COMMENT '下架原因',
    `disabled_at` DATETIME NOT NULL COMMENT '下架时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='题目黑名单配置';

-- =====================================================
-- 初始化数据：插入示例配置和测试题
-- =====================================================

-- 插入题型规则配置
INSERT INTO `question_type_conf` (`id`, `question_type`, `shuffle_options`, `grading_method`) VALUES
(1, 'SINGLE', 1, 'EXACT_MATCH'),
(2, 'MULTI', 1, 'PARTIAL_SCORE'),
(3, 'JUDGE', 0, 'EXACT_MATCH'),
(4, 'BLANK', 0, 'TEXT_COMPARE');

-- 插入评分规则配置
INSERT INTO `grading_rule_conf` (`id`, `question_type`, `rule_name`, `score_per_question`, 
                                  `full_score_on_all_correct`, `allow_partial_score`, `zero_on_wrong_choice`, `partial_score_ratio`) VALUES
(1, 'SINGLE', '单选题标准', 10.0, 1, 0, 1, NULL),
(2, 'MULTI', '多选题标准', 15.0, 1, 1, 1, 0.5),
(3, 'JUDGE', '判断题标准', 5.0, 1, 0, 1, NULL),
(4, 'BLANK', '填空题标准', 10.0, 1, 0, 1, NULL);

-- 插入测试题库（3道单选 + 2道多选 + 1道判断）
INSERT INTO `question_bank` (`id`, `stem`, `question_type`, `options`, `correct_answer`, `difficulty`, `tags`, `enabled`, `created_at`, `updated_at`) VALUES
(1001, 'Java中哪个关键字用于定义类？', 'SINGLE', 
 '[{"optionId":1,"text":"class","isCorrect":true},{"optionId":2,"text":"struct","isCorrect":false},{"optionId":3,"text":"define","isCorrect":false},{"optionId":4,"text":"type","isCorrect":false}]',
 '[1]', 1, '["Java","基础"]', 1, NOW(), NOW()),

(1002, 'Spring Boot的核心注解是？', 'SINGLE',
 '[{"optionId":1,"text":"@SpringBootApplication","isCorrect":true},{"optionId":2,"text":"@EnableAutoConfiguration","isCorrect":false},{"optionId":3,"text":"@ComponentScan","isCorrect":false},{"optionId":4,"text":"@Configuration","isCorrect":false}]',
 '[1]', 2, '["SpringBoot"]', 1, NOW(), NOW()),

(1003, 'MySQL中InnoDB支持的隔离级别不包括？', 'SINGLE',
 '[{"optionId":1,"text":"Read Uncommitted","isCorrect":false},{"optionId":2,"text":"Read Committed","isCorrect":false},{"optionId":3,"text":"Repeatable Read","isCorrect":false},{"optionId":4,"text":"Serial Snapshot","isCorrect":true}]',
 '[4]', 3, '["MySQL"]', 1, NOW(), NOW()),

(2001, '以下哪些是Spring框架的核心特性？（多选）', 'MULTI',
 '[{"optionId":1,"text":"IOC容器","isCorrect":true},{"optionId":2,"text":"AOP面向切面","isCorrect":true},{"optionId":3,"text":"事务管理","isCorrect":true},{"optionId":4,"text":"GUI界面开发","isCorrect":false}]',
 '[1,2,3]', 3, '["Spring"]', 1, NOW(), NOW()),

(2002, '以下哪些属于设计模式？（多选）', 'MULTI',
 '[{"optionId":1,"text":"单例模式","isCorrect":true},{"optionId":2,"text":"工厂模式","isCorrect":true},{"optionId":3,"text":"观察者模式","isCorrect":true},{"optionId":4,"text":"瀑布模型","isCorrect":false}]',
 '[1,2,3]', 2, '["设计模式"]', 1, NOW(), NOW()),

(3001, 'Java是面向对象的编程语言。', 'JUDGE',
 '[{"optionId":1,"text":"正确","isCorrect":true},{"optionId":2,"text":"错误","isCorrect":false}]',
 '[1]', 1, '["Java"]', 1, NOW(), NOW());

-- 插入示例试卷模板（INFO → 3道单选 → SCORE_SNAPSHOT → 2道多选 → 1道判断）
INSERT INTO `paper_template_conf` (`id`, `paper_name`, `step_sequence`, `enabled`, `created_at`, `updated_at`) VALUES
(1, '综合能力测评卷', 
 '[{"type":"INFO","title":"欢迎参加本次测评"},{"type":"SINGLE","questionCount":3},
   {"type":"SCORE_SNAPSHOT","title":"当前得分"},{"type":"MULTI","questionCount":2},
   {"type":"JUDGE","questionCount":1}]',
 1, NOW(), NOW());

SELECT '✅ AssessFlow 数据库初始化完成！' AS message;
