CREATE DATABASE IF NOT EXISTS exam_user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_user_db;
CREATE TABLE t_user ( id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(64) UNIQUE NOT NULL, password VARCHAR(128) NOT NULL, nickname VARCHAR(64), role VARCHAR(20) NOT NULL );
INSERT INTO t_user (id, username, password, nickname, role) VALUES (1, 'teacher_01', '123456', '李德兴 教师', 'TEACHER'), (2, 'student_01', '123456', '陈明同学', 'STUDENT'), (3, 'student_02', '123456', '张敏同学', 'STUDENT');

CREATE DATABASE IF NOT EXISTS exam_exam_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_exam_db;
CREATE TABLE t_exam ( id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(256) NOT NULL, start_time DATETIME NOT NULL, end_time DATETIME NOT NULL, question_ids VARCHAR(512) NOT NULL, total_score INT NOT NULL, creator_id BIGINT NOT NULL, status VARCHAR(20) DEFAULT 'UPCOMING' );
INSERT INTO t_exam (id, title, start_time, end_time, question_ids, total_score, creator_id, status) VALUES (1, '2026年Spring Cloud微服务专题小测', '2026-05-26 10:00:00', '2026-05-26 12:00:00', '1,2,3', 100, 1, 'RUNNING');

CREATE DATABASE IF NOT EXISTS exam_question_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_question_db;
CREATE TABLE t_question ( id BIGINT AUTO_INCREMENT PRIMARY KEY, title TEXT NOT NULL, option_a VARCHAR(256), option_b VARCHAR(256), option_c VARCHAR(256), option_d VARCHAR(256), answer VARCHAR(10) NOT NULL, score INT NOT NULL, category VARCHAR(64) );
INSERT INTO t_question (id, title, option_a, option_b, option_c, option_d, answer, score, category) VALUES (1, 'Spring Cloud的网关核心路由组件是哪个？', 'Ribbon', 'Spring Cloud Gateway', 'Nacos', 'Eureka', 'B', 30, 'Java开发'), (2, '以下不属于 Spring Cloud 核心组件的是哪一个？', 'OpenFeign', 'RocketMQ', 'Sentinel', 'MyBatis', 'D', 30, 'Java开发'), (3, 'Nacos 在微服务中的主要作用是什么？', '网络请求代理', '服务注册与配置中心', '分布式缓存锁', 'JVM内存诊断', 'B', 40, 'Java开发');

CREATE DATABASE IF NOT EXISTS exam_score_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_score_db;
CREATE TABLE t_score ( id BIGINT AUTO_INCREMENT PRIMARY KEY, exam_id BIGINT NOT NULL, exam_title VARCHAR(256), student_id BIGINT NOT NULL, student_name VARCHAR(64), final_score INT, my_answers TEXT, submit_time DATETIME );