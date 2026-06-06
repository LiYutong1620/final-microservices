## 一、环境准备

| 工具           | 版本要求 | 验证命令           |
| :------------- | :------- | :----------------- |
| JDK            | 8 或 11  | `java -version`    |
| Maven          | 3.6+     | `mvn -v`           |
| Node.js        | 16+      | `node -v`          |
| Docker Desktop | 最新版   | 启动后鲸鱼图标运行 |

> 如果本地已有 MySQL（占用 3306 端口），建议先停止本地 MySQL 服务，避免端口冲突。

------

## 二、启动中间件（MySQL、Redis、Nacos、Sentinel）

1. **修改 `docker-compose.yml`**（避免端口冲突）：
   将 `sentinel-dashboard` 的端口映射从 `8080:8080` 改为 `8088:8080`。

2. **在项目根目录执行**：

   ```
   docker-compose up -d
   ```

3. **验证所有容器运行**：

   ```
   docker ps
   ```

   

   应看到 `mysql-exam`、`redis-exam`、`nacos-server`、`sentinel-dashboard` 均为 `Up` 状态。
4. **验证所有容器运行**(可选)：

   ```
   ports:
      - "3307:3306"
   ```
   修改MySQL的端口映射。把宿主机的3307端口映射到容器的3306端口。
------

## 三、数据库初始化（解决中文乱码）

1. **准备 SQL 文件**：
   在项目根目录创建 `init.sql`，内容包含创建四个库及表、插入初始数据（题库、用户、考试）。

2. **删除可能已存在的旧库**（如果之前导入过）：

   ```
   docker exec -it mysql-exam mysql -uroot -proot_password -e "DROP DATABASE IF EXISTS exam_user_db; DROP DATABASE IF EXISTS exam_exam_db; DROP DATABASE IF EXISTS exam_question_db; DROP DATABASE IF EXISTS exam_score_db;"
   ```

3. **以 UTF-8 编码导入**（避免乱码）：

   在项目根目录执行

   ```
   chcp 65001
   docker exec -i mysql-exam mysql -uroot -proot_password --default-character-set=utf8mb4 < init.sql
   ```

4. **验证中文正常**：

   ```
   docker exec -it mysql-exam mysql -uroot -proot_password -e "SELECT * FROM exam_question_db.t_question;"
   ```

------

## 四、Nacos 配置中心添加动态配置

1. 访问 `http://localhost:8848/nacos`（用户名/密码：`nacos/nacos`）

2. 进入 **配置管理 → 配置列表** → **创建配置**

   - Data ID：`exam-service-dev.yml`

   - Group：`DEFAULT_GROUP`

   - 格式：`YAML`

   - 内容：

     yaml

     ```
     exam:
       max.students: 150
     ```

3. 点击 **发布**

------

## 六、启动后端微服务（按顺序，每个服务一个新终端窗口）

编译打包或者直接在IDEA编译运行。

```
mvn clean package -DskipTests
```

| 服务     | 命令（在项目根目录执行）                                     |
| :------- | :----------------------------------------------------------- |
| 网关     | `java -jar gateway/target/gateway-1.0.0.jar`                 |
| 用户服务 | `java -jar user-service/target/user-service-1.0.0.jar`       |
| 题库服务 | `java -jar question-service/target/question-service-1.0.0.jar` |
| 考试服务 | `java -jar exam-service/target/exam-service-1.0.0.jar`       |
| 阅卷服务 | `java -jar score-service/target/score-service-1.0.0.jar`     |

> 如果使用 IDEA，可以直接运行每个模块的 `*Application` 主类，更方便。

**验证服务注册**：
访问 `http://localhost:8848/nacos` → **服务管理 → 服务列表**，应看到 5 个服务，每个都有 1 个健康实例。

------

## 七、启动前端（Vue 3）

根目录执行

```
cd frontend
npm install
npm run serve -- --port 8081
```

> npm run serve -- --port 8081可能会在其他端口启动因为有时候8081是被占用的，所以也可以直接执行npm run serve。

------

## 八、访问系统

浏览器打开 **[http://localhost:8081](http://localhost:8081/)**

- 学生账号：`student_01` / `123456` 或 `student_02` / `123456`
- 教师账号：`teacher_01` / `123456`

登录后即可进行在线考试、阅卷等操作。

