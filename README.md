# 在线考试系统 3.0

这是一个前后端分离的在线考试系统。后端是 Spring Boot REST API，负责认证、用户、题库、试卷、提交与阅卷；前端是独立静态应用，位于 `frontend/`，只通过 `/api/**` 与后端通信。

## 技术栈

- 后端：JDK 17、Spring Boot 3.3.5、Spring Web、Spring Data JPA、Actuator
- 数据库：H2 文件库 / MySQL
- 前端：原生 HTML、CSS、JavaScript
- 认证：Session + HttpOnly Cookie

## 核心功能

- 学生：登录、查看已发布考试、限时答题、提交试卷、查看成绩与评语。
- 教师：维护题库、批量导入题目、创建试卷、发布/关闭考试、查看提交记录、批阅主观题。
- 管理员：查看运行概览、创建用户、启用/停用账号、重置密码、查看考试列表。

## 本地运行

启动后端：

```bash
mvn test
mvn package
java -jar target/online-exam-system-1.0.0.jar
```

启动前端静态服务：

```bash
cd frontend
node server.js
```

浏览器访问：

```text
http://localhost:5173
```

首次启动会初始化开发账号：

| 身份 | 用户名 | 密码 |
| --- | --- | --- |
| 学生 | student | student123 |
| 教师 | teacher | teacher123 |
| 管理员 | admin | admin123 |

## 主要 API

- `GET /api/meta`：枚举选项。
- `POST /api/auth/login`：登录。
- `GET /api/auth/me`：当前登录用户。
- `POST /api/auth/logout`：退出。
- `GET /api/student/dashboard`：学生考试中心。
- `GET /api/student/exams/{id}`：学生进入考试。
- `POST /api/student/exams/{id}/submissions`：提交答卷。
- `GET /api/student/submissions/{id}`：查看成绩。
- `GET /api/teacher/dashboard`：教师仪表盘。
- `GET|POST /api/teacher/questions`：题库列表、创建题目。
- `POST /api/teacher/questions/import`：批量导入题目。
- `DELETE /api/teacher/questions/{id}`：删除题目。
- `GET|POST /api/teacher/exams`：试卷列表、创建试卷。
- `PATCH /api/teacher/exams/{id}/status`：发布或关闭试卷。
- `GET /api/teacher/exams/{id}/submissions`：查看提交记录。
- `GET /api/teacher/submissions/{id}`：查看答卷详情。
- `POST /api/teacher/submissions/{id}/grade`：保存阅卷结果。
- `GET /api/admin/overview`：管理员概览。
- `POST /api/admin/users`：创建用户。
- `PATCH /api/admin/users/{id}/enabled`：启用/停用账号。
- `PATCH /api/admin/users/{id}/password`：重置密码。

## 生产部署

宝塔面板部署可直接查看：[BT_DEPLOY.md](./BT_DEPLOY.md)。

后端推荐使用 MySQL：

```bash
export SPRING_PROFILES_ACTIVE=prod
export EXAM_DB_URL='jdbc:mysql://127.0.0.1:3306/exam?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export EXAM_DB_DRIVER='com.mysql.cj.jdbc.Driver'
export EXAM_DB_USERNAME='exam'
export EXAM_DB_PASSWORD='pnZEx6hiXTCkNCJ8'
export FRONTEND_ORIGINS='https://exam.example.com'
export SESSION_COOKIE_SECURE=true
java -jar target/online-exam-system-1.0.0.jar
```

如果前端和后端部署在不同站点且需要跨站 Cookie，请使用 HTTPS，并设置：

```bash
export SESSION_COOKIE_SAME_SITE=none
export SESSION_COOKIE_SECURE=true
```

健康检查：

```text
GET /actuator/health
```

## 前端部署

`frontend/` 可以直接部署到 Nginx、对象存储静态站点或 CDN。默认 API 地址是：

```text
http://localhost:8080/api
```

生产环境可在 `frontend/index.html` 中设置：

```html
<script>
    window.EXAM_API_BASE = "https://api.example.com/api";
</script>
```

## 题目批量导入格式

教师可在题库中心上传本机 `.txt` / `.csv` 文件，也可以按行粘贴导入。第一行可以保留表头：

```text
题型|题干|A|B|C|D|答案|分值
选择题|JVM 的作用是什么？|运行 Java 字节码|管理数据库|编写页面|发送邮件|A|10
判断题|HTTP 是无状态协议。|||||正确|10
填空题|Spring Boot 默认内嵌容器是 ____。|||||Tomcat|10
大题|说明如何防止重复提交。|||||人工评分|20
```

## 上线前检查

- 已执行 `mvn test` 并通过。
- 已切换到 `prod` profile。
- 已配置正式数据库、备份策略和 `FRONTEND_ORIGINS`。
- 已重置默认管理员、教师、学生密码。
- 已使用 HTTPS，并按部署拓扑设置 Cookie 的 `SameSite` / `Secure`。
- `/actuator/health` 可被监控系统访问。
