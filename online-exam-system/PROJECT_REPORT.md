# 毕业设计项目报告：微服务架构在线考试系统

## 一、需求分析

### 1.1 系统背景与建设目标
传统的线上考试系统由于采用单体架构，在面临全校/全省规模的师生同一时间段进行高并发组卷、高并发交卷和自动判分时，极易因高频次的数据库锁竞争、慢查询或CPU负载过载导致系统服务瘫痪。
本系统是一套专为高并发设计的在线考试系统。通过将业务解耦为独立的微服务集群（用户管理、考试管理、题库管理、成绩阅卷），使用 **Spring Cloud Alibaba** 一站式治理方案搭建，配合 **Spring Cloud Gateway**、**Nacos 2.x** 服务治理中心、**OpenFeign** RPC远程调用、**Sentinel** 限流熔断与自愈保障，解决高并发在线考试场景下的以下核心痛点：
1. **教师出卷与动态组卷**：教师可建立分类题库，新建考试时可在前端可视组卷、设定考核时间及总分。
2. **学生高并发考试与答题**：系统在加载考试列表及答题时支持服务降级兜底；高并发交卷批改进行本地计算并对写库请求限流保护。
3. **高吞吐自动阅卷**：选择题由成绩微服务（`score-service`）调用题库服务（`question-service`）获取非对称标准参考答案进行本地自动化批阅，从而减轻核心数据库的事务负载。

### 1.2 系统核心用例
- **教师用例图（文字表述）**：
  - 核心用例：[创建/添加试题] -> [管理题库] -> [新建/发布考试（设定总分与起止时间、绑定题目IDs）] -> [获取考试统计报告与成绩排名（配合 Recharts/Element Plus 可视分析）]。
- **学生用例图（文字表述）**：
  - 核心用例：[注册/登录账户] -> [查看可参加考试列表] -> [加载试题内容答题] -> [一键完成交卷（计算总得分）] -> [查看历史历史考试档案清单]。

---

## 二、架构设计

### 2.1 微服务划分图 (ASCII)

```
                       ┌─────────────────────────┐
                       │   前端 client (Vue 3)   │
                       └────────────┬────────────┘
                                    │ HTTP / JWT
                                    ▼
                       ┌─────────────────────────┐
                       │  Gateway 网关路由 (8080) │
                       └────────────┬────────────┘
         ┌──────────────────────────┼──────────────────────────┐
         ▼ (X-User-Id, Role)        ▼                          ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│  user-service   │       │  exam-service   │       │question-service │
│  用户服务 (8081) │       │  考试服务 (8082) │       │  题库服务 (8083) │
└─────────────────┘       └─────────────────┘       └────────┬────────┘
                                                             ▲
                                                             │ Feign RPC
                                                             │ (FallbackFactory)
                                                    ┌────────┴────────┐
                                                    │  score-service  │
                                                    │  成绩服务 (8084) │
                                                    └─────────────────┘

[Nacos 注册/配置中心] ◄───── 服务心跳/拉取 / 动态配置 (RefreshScope) ─────── 
[Redis 缓存存储]      ◄───── Token 黑名单 & 考试限额缓存 (userId / lock)───
```

### 2.2 技术选型与选择理由
- **Gateway 网关**：负责微服务对外的统一路由分发、非对称跨域配置（CORS Global）、统一JWT鉴权解析，屏蔽各后台微服务的具体网络拓扑，减少客户端代码复杂度。
- **Nacos 服务治理及配置中心**：Nacos 2.x 采用底层的 gRPC 双向流模型，替代了传统的 HTTP 轮询心跳机制，服务动态感知在 2 秒以内，能够实现几乎实时的微服务弹性缩容和容错排障；同时集成配置热更新。
- **OpenFeign 远程调用**：基于声明式接口代理，极大地简化了微服务之间的 HTTP HTTP 接口调用；配合 Sentinel 后，可提供内置的兜底熔断。
- **Sentinel 熔断与限流**：用在最核心的 `score-service` 阅卷服务和 `exam-service` 考试接入处，支持按 QPS 维度硬性限流或基于平均响应时间（RT）触发秒级熔断。
- **数据库隔离**：每个微服务配备专属 schema（`exam_user_db`、`exam_exam_db`、`exam_question_db`、`exam_score_db`），互不越权操作，从底层物理结构实现高安全的多模块隔离，避免数据库单点瓶颈。

### 2.3 JWT 认证拦截机制说明
1. **令牌签发**：用户端请求 `user-service` 实现成功登录后，服务生成带有 `userId`、`username`、`role`（教师或学生的角色标识）的 JWT Token 返回并存储到 localStorage 中。
2. **网关校验**：在 Gateway 部署 `JwtAuthenticationFilter`：
   - 过滤特定白名单地址（`register`、`login`等放行）。
   - 解析 Header 的 `Authorization` Bearer 令牌合法性、时效性并解析 Claim 数据。
3. **接口角色管控**：Gateway 动态判定当前 URI 路径，特定写操作（`add`、`create` 等教师独有接口）发现角色不是 `TEACHER` 直接拦截阻断返回 `403 Forbidden`。
4. **Header 状态透传**：校验成功的请求通过多层路由之前，网关修改 Reactive Request Header，透传 `X-User-Id`、`X-User-Name`、`X-User-Role` 头部，各个子系统业务逻辑中可直接使用，解决微服务间的上下文难题，免除子模块二次解析损耗。

---

## 三、部署与运行指南

### 3.1 环境要求
- **JDK 8 / 11** & **Maven 3.6.x +**
- **Nacos Server v2.2.x / v2.0.x** (下载地址: https://github.com/alibaba/nacos/releases)
- **Redis v6.x +** / **MySQL 8.0 +**
- **Sentinel Dashboard v1.8.6** (下载地址: https://github.com/alibaba/csp/sentinel/releases)

### 3.2 数据库初始化SQL脚本
分服务建立 MySQL 数据库：
```sql
-- 1. 创建 exam_user_db
CREATE DATABASE IF NOT EXISTS exam_user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_user_db;
CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    role VARCHAR(20) NOT NULL
);
INSERT INTO t_user (id, username, password, nickname, role) VALUES 
(1, 'teacher_01', '123456', '李德兴 教师', 'TEACHER'),
(2, 'student_01', '123456', '陈明同学', 'STUDENT'),
(3, 'student_02', '123456', '张敏同学', 'STUDENT');

-- 2. 创建 exam_exam_db
CREATE DATABASE IF NOT EXISTS exam_exam_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_exam_db;
CREATE TABLE t_exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(256) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    question_ids VARCHAR(512) NOT NULL,
    total_score INT NOT NULL,
    creator_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'UPCOMING'
);
INSERT INTO t_exam (id, title, start_time, end_time, question_ids, total_score, creator_id, status) VALUES
(1, '2026年Spring Cloud微服务专题小测', '2026-05-26 10:00:00', '2026-05-26 12:00:00', '1,2,3', 100, 1, 'RUNNING');

-- 3. 创建 exam_question_db
CREATE DATABASE IF NOT EXISTS exam_question_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_question_db;
CREATE TABLE t_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title TEXT NOT NULL,
    option_a VARCHAR(256),
    option_b VARCHAR(256),
    option_c VARCHAR(256),
    option_d VARCHAR(256),
    answer VARCHAR(10) NOT NULL,
    score INT NOT NULL,
    category VARCHAR(64)
);
INSERT INTO t_question (id, title, option_a, option_b, option_c, option_d, answer, score, category) VALUES
(1, 'Spring Cloud的网关核心路由组件是哪个？', 'Ribbon', 'Spring Cloud Gateway', 'Nacos', 'Eureka', 'B', 30, 'Java开发'),
(2, '以下不属于 Spring Cloud 核心组件的是哪一个？', 'OpenFeign', 'RocketMQ', 'Sentinel', 'MyBatis', 'D', 30, 'Java开发'),
(3, 'Nacos 在微服务中的主要作用是什么？', '网络请求代理', '服务注册与配置中心', '分布式缓存锁', 'JVM内存诊断', 'B', 40, 'Java开发');

-- 4. 创建 exam_score_db
CREATE DATABASE IF NOT EXISTS exam_score_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_score_db;
CREATE TABLE t_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    exam_title VARCHAR(256),
    student_id BIGINT NOT NULL,
    student_name VARCHAR(64),
    final_score INT,
    my_answers TEXT,
    submit_time DATETIME
);
```

### 3.3 Nacos配置中心导入热更配置 (`exam-service-dev.yml`)
在 Nacos 配置列表中创建名为 `exam-service-dev.yml`（Group: `DEFAULT_GROUP`）的配置文件：
```yaml
exam:
  max.students: 150  # 动态控制：限制同一场考试的学生最大参加注册人数
```

### 3.4 微服务各个启动顺序
1. **启动基础设施中间件**（优先启动 Nacos、MySQL、Redis 及 Sentinel Dashboard）。
2. **启动微服务应用**：
   - 依次运行：`gateway` -> `user-service` -> `question-service` -> `exam-service` -> `score-service` 的主 Application 类启动。
   - 打开后台控制面板：Nacos 地址 (http://localhost:8848/nacos) 观察服务心跳注册是否成功绿亮。

---

## 四、测试与应急演练

### 4.1 Postman 接口级联功能性测试
使用随附的 `postman_collection.json` 进行全链路检验：
1. **联查 01/02**：建立包含两个角色的账号，登录生成 JWT Token 并自动关联到 Postman 环境变量中。
2. **级联 OpenFeign 调用测试**：
   - 请求 `08 学生自动阅卷提交`。此接口执行时，`score-service` 会依靠 Feign 向 `question-service` 发送批量请求。执行结束观察数据库 `t_score` 中自动判定算分成功归档（如正确作答可得 100/100）。

### 4.2 Sentinel 一级流控限流压力验证
本系统在 `ScoreController` 对应提交互接上挂载了 Sentinel 资源限流兜底。
1. **阈值设定**：在 Sentinel 控制台或通过程序设定 `submitScore` 的限流规则：**单机器 QPS 限流阈值为 100**。
2. **JMeter 压测操作**：
   - 启动 JMeter 工具，导入 `jmeter_test_plan.jmx`。
   - 设定线程数: 150 运行（或逐渐提高至 500），压测结果中可清晰看到 QPS 高于 100 的交卷请求瞬间被 Sentinel 流量整流，阻断并立即返回 HTTP 预定义信息：`Blocked by Sentinel - 阅卷服务暂时繁忙`，而处于 100/s 之内的请求悉数执行通过保存落库，保证核心 `score-service` 服务器不会因瞬间大流量触发进程内存泄露或 MySQL 连库死锁，系统稳定不崩溃。

### 4.3 故障注入与 Sentinel 熔断降级测试
1. **宕机演练**：手动终止 `question-service` 服务进程释放实例，造成 Feign 接口 RPC 请求直连失效，或将其中批量请求故意延时 sleep(5000) 设定网络严重丢包。
2. **验证结果**：当有学生再次进入交卷流程时，由于 `ScoreFeignClient` 中的 `FallbackFactory` 拦截到了熔断条件，Sentinel 自动接管并触发熔断异常抛出。阅卷流程在不到 1s 的超时判定内迅速返回：`“阅卷服务繁忙，题库信息拉取失败，请稍后重试”`，而不是一直让交卷线程处于挂起导致整个 score-service 的 tomcat 线程池被蚕食耗尽，实现完备的微服务雪崩防御！

---
