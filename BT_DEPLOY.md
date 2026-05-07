# 宝塔面板上线部署教程

本教程按你截图中的宝塔面板菜单来写：`软件商店`、`数据库`、`文件`、`终端`、`网站`、`SSL`、`安全`。

项目部署方式：

```text
前端：宝塔网站 / Nginx 静态站点
后端：Spring Boot Jar，监听 8080
数据库：宝塔 MySQL，数据库名 exam
访问：浏览器访问网站根路径，前端通过 /api 反向代理调用后端
```

推荐最终访问方式：

```text
有域名：https://你的域名
暂时没域名：http://你的服务器IP
```

截图里的服务器 IP 是：

```text
106.54.30.124
```

下面示例会同时写“有域名”和“暂时只用 IP”的配置。你没有域名时，先按 IP 部署即可。

## 0. 部署前先看这个

宝塔首页提示当前面板端口是默认 `8888`，这有安全风险。部署项目不影响这个问题，但上线后建议处理：

```text
宝塔左侧 -> 设置 -> 面板设置 -> 修改面板端口
腾讯云/服务器安全组 -> 只允许自己的 IP 访问宝塔面板端口
```

网站业务只需要放行：

```text
80
443
```

后端 `8080` 和数据库 `3306` 不建议对公网开放，只给本机 Nginx 和本机后端使用。

## 1. 本地准备上传文件

在本地项目根目录执行：

```bash
mvn clean test
mvn package -DskipTests
```

需要上传到服务器的文件：

```text
target/online-exam-system-1.0.0.jar
frontend/index.html
frontend/app.css
frontend/app.js
frontend/assets/
deploy/mysql-init.sql
```

这些文件分别对应：

```text
online-exam-system-1.0.0.jar    后端程序
index.html/app.css/app.js       前端页面
assets/                         前端图片资源
mysql-init.sql                  创建 MySQL 数据库和应用账号
```

`frontend/server.js` 只用于本地测试，宝塔部署时不用上传。

## 2. 宝塔安装运行环境

进入宝塔面板：

```text
左侧 -> 软件商店
```

安装：

```text
Nginx
MySQL 8.0
Java 项目管理器
```

如果软件商店里没有 Java 项目管理器，就用下面的终端方式安装 JDK：

```text
左侧 -> 终端
```

执行：

```bash
java -version
```

如果提示没有 Java，OpenCloudOS 可尝试：

```bash
yum install -y java-17-openjdk java-17-openjdk-devel
java -version
```

看到 `17` 或更高版本即可。

## 3. 创建服务器目录

进入：

```text
左侧 -> 终端
```

执行：

```bash
mkdir -p /www/wwwroot/online-exam-frontend
mkdir -p /www/wwwroot/online-exam-backend
```

目录作用：

```text
/www/wwwroot/online-exam-frontend    前端网站目录
/www/wwwroot/online-exam-backend     后端 Jar 和部署文件目录
```

## 4. 上传文件

进入：

```text
左侧 -> 文件
```

上传前端文件到：

```text
/www/wwwroot/online-exam-frontend
```

需要有：

```text
index.html
app.css
app.js
```

上传后端文件到：

```text
/www/wwwroot/online-exam-backend
```

需要有：

```text
online-exam-system-1.0.0.jar
mysql-init.sql
```

## 5. 初始化 MySQL 数据库

本项目使用的数据库信息：

```text
数据库名：exam
应用用户：exam
应用密码：pnZEx6hiXTCkNCJ8
```

如果你正在使用截图里的“添加 mysql 数据库”弹窗，就按上面三项填写，并保持：

```text
字符集：utf8mb4
访问权限：本地服务器
添加至：本地服务器（127.0.0.1）
```

然后点击“确定”。这种方式创建成功后，可以跳过下面的 `mysql-init.sql` 导入命令，直接看“验证数据库”。

如果你不想用宝塔弹窗，也可以用 `mysql-init.sql` 在终端创建。

进入：

```text
左侧 -> 终端
```

执行：

```bash
cd /www/wwwroot/online-exam-backend
mysql -uroot -p < mysql-init.sql
```

终端会提示输入 MySQL root 密码，输入你的 root 密码即可。输入密码时屏幕不会显示，这是正常的。

验证数据库：

```bash
mysql -uexam -p'pnZEx6hiXTCkNCJ8' -e "SHOW DATABASES LIKE 'exam';"
```

如果能看到 `exam`，说明数据库和应用账号创建成功。

也可以在宝塔里查看：

```text
左侧 -> 数据库
```

能看到 `exam` 即可。

## 6. 启动后端

后端端口固定使用：

```text
8080
```

### 方式 A：Java 项目管理器启动

如果你安装了宝塔 Java 项目管理器：

```text
左侧 -> 网站 或 软件商店中的 Java 项目管理器 -> 添加 Java 项目
```

填写：

```text
项目类型：Spring Boot
项目名称：online-exam-backend
Jar 路径：/www/wwwroot/online-exam-backend/online-exam-system-1.0.0.jar
项目端口：8080
JDK：17+
```

启动参数建议填：

```bash
--spring.profiles.active=prod
--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/exam?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
--spring.datasource.username=exam
--spring.datasource.password=pnZEx6hiXTCkNCJ8
```

如果面板支持环境变量，也可以填：

```text
SPRING_PROFILES_ACTIVE=prod
EXAM_DB_URL=jdbc:mysql://127.0.0.1:3306/exam?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
EXAM_DB_DRIVER=com.mysql.cj.jdbc.Driver
EXAM_DB_USERNAME=exam
EXAM_DB_PASSWORD=pnZEx6hiXTCkNCJ8
```

根据你的访问方式再加 Cookie 配置：

如果暂时只用 IP、没有 HTTPS：

```text
FRONTEND_ORIGINS=http://106.54.30.124
SESSION_COOKIE_SECURE=false
```

如果使用域名并开启 HTTPS：

```text
FRONTEND_ORIGINS=https://你的域名
SESSION_COOKIE_SECURE=true
```

### 方式 B：用 systemd 启动，推荐稳定上线用

如果 Java 项目管理器不好用，就用系统服务。

进入：

```text
左侧 -> 终端
```

创建服务文件：

```bash
cat > /etc/systemd/system/online-exam.service <<'EOF'
[Unit]
Description=Online Exam System
After=network.target mysqld.service mysql.service

[Service]
Type=simple
WorkingDirectory=/www/wwwroot/online-exam-backend
Environment=SPRING_PROFILES_ACTIVE=prod
Environment=EXAM_DB_URL=jdbc:mysql://127.0.0.1:3306/exam?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
Environment=EXAM_DB_DRIVER=com.mysql.cj.jdbc.Driver
Environment=EXAM_DB_USERNAME=exam
Environment=EXAM_DB_PASSWORD=pnZEx6hiXTCkNCJ8
Environment=FRONTEND_ORIGINS=http://106.54.30.124
Environment=SESSION_COOKIE_SECURE=false
ExecStart=/usr/bin/java -jar /www/wwwroot/online-exam-backend/online-exam-system-1.0.0.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

如果你已经绑定域名并开启 HTTPS，把这两行改掉：

```text
Environment=FRONTEND_ORIGINS=https://你的域名
Environment=SESSION_COOKIE_SECURE=true
```

启动后端：

```bash
systemctl daemon-reload
systemctl enable online-exam
systemctl start online-exam
systemctl status online-exam
```

查看后端日志：

```bash
journalctl -u online-exam -f
```

检查后端健康状态：

```bash
curl http://127.0.0.1:8080/actuator/health
```

正常应返回类似：

```json
{"status":"UP","groups":["liveness","readiness"]}
```

第一次启动连接空 MySQL 库时，后端会自动建表并初始化演示账号。

## 7. 创建前端网站

进入：

```text
左侧 -> 网站 -> 添加站点
```

如果你有域名，填写：

```text
域名：你的域名
根目录：/www/wwwroot/online-exam-frontend
PHP版本：纯静态
数据库：不创建
```

如果你暂时没有域名，可以先填写服务器 IP：

```text
域名：106.54.30.124
根目录：/www/wwwroot/online-exam-frontend
PHP版本：纯静态
数据库：不创建
```

创建完成后，先访问：

```text
http://106.54.30.124
```

如果能看到在线考试系统登录页，说明前端静态文件部署成功。

## 8. 配置 Nginx 反向代理

进入：

```text
左侧 -> 网站 -> 找到刚创建的网站 -> 设置 -> 配置文件
```

在 `server { ... }` 里面加入下面两段。注意是放在 `server` 块内部，不要放到最外面。

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location /actuator/health {
    proxy_pass http://127.0.0.1:8080/actuator/health;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

保存后，宝塔会自动检查 Nginx 配置。没有报错就重载 Nginx。

检查：

```text
http://106.54.30.124/actuator/health
```

正常应看到：

```json
{"status":"UP"}
```

然后访问：

```text
http://106.54.30.124
```

使用默认账号登录：

| 身份 | 用户名 | 密码 |
| --- | --- | --- |
| 学生 | student | student123 |
| 教师 | teacher | teacher123 |
| 管理员 | admin | admin123 |

上线后请立刻登录管理员账号，重置默认密码。

## 9. 配置 SSL，有域名时做

如果你有域名：

```text
左侧 -> 网站 -> 站点设置 -> SSL
```

申请 Let’s Encrypt 或上传证书，开启强制 HTTPS。

开启 HTTPS 后，后端配置要同步改成：

```text
FRONTEND_ORIGINS=https://你的域名
SESSION_COOKIE_SECURE=true
```

如果用 systemd，修改：

```bash
vim /etc/systemd/system/online-exam.service
```

改完重启：

```bash
systemctl daemon-reload
systemctl restart online-exam
```

## 10. 腾讯云/服务器安全组放行

你的宝塔服务器外层还有云厂商安全组。腾讯云轻量云或云服务器里需要放行：

```text
80
443
```

如果还要继续访问宝塔面板，也要放行宝塔面板端口。截图里当前是：

```text
8888
```

不建议放行：

```text
3306
8080
```

因为数据库和后端端口只需要服务器内部访问。

## 11. 重新发布项目

### 重新发布后端

本地重新打包：

```bash
mvn package -DskipTests
```

上传覆盖：

```text
/www/wwwroot/online-exam-backend/online-exam-system-1.0.0.jar
```

重启：

```bash
systemctl restart online-exam
```

或在 Java 项目管理器里点“重启”。

### 重新发布前端

上传覆盖：

```text
/www/wwwroot/online-exam-frontend/index.html
/www/wwwroot/online-exam-frontend/app.css
/www/wwwroot/online-exam-frontend/app.js
/www/wwwroot/online-exam-frontend/assets/
```

这些文件要一起覆盖，尤其是 `index.html`，里面带了新的前端版本号，用来避免浏览器继续加载旧的 `app.js`。

覆盖后在浏览器按 `Ctrl + F5` 强制刷新。如果仍然看到旧页面，可以在宝塔网站设置里清理缓存，或打开浏览器开发者工具勾选 Disable cache 后再刷新。

## 12. 数据库备份

宝塔方式：

```text
左侧 -> 数据库 -> exam -> 备份
```

命令方式：

```bash
mysqldump -u exam -p exam > /www/backup/exam.sql
```

恢复：

```bash
mysql -u exam -p exam < /www/backup/exam.sql
```

生产环境 MySQL 数据不在项目目录里，项目里的 `data/examdb.mv.db` 只是本地 H2 开发库。

## 13. 常见问题

### 页面能打开，但登录失败

检查：

```bash
curl http://127.0.0.1:8080/actuator/health
curl http://106.54.30.124/actuator/health
```

第一个不通：后端没启动。

第二个不通：Nginx `/api` 或 `/actuator/health` 反向代理没配好。

### 登录后马上变成未登录

如果用 IP + HTTP：

```text
SESSION_COOKIE_SECURE=false
FRONTEND_ORIGINS=http://106.54.30.124
```

如果用域名 + HTTPS：

```text
SESSION_COOKIE_SECURE=true
FRONTEND_ORIGINS=https://你的域名
```

浏览器开发者工具里应能看到 `JSESSIONID` Cookie。

### 502 Bad Gateway

说明 Nginx 找不到后端。检查：

```bash
systemctl status online-exam
curl http://127.0.0.1:8080/actuator/health
```

如果后端没有运行，查看日志：

```bash
journalctl -u online-exam -n 100
```

### 数据库连接失败

检查 MySQL：

```bash
systemctl status mysqld
mysql -uexam -p'pnZEx6hiXTCkNCJ8' -e "SHOW DATABASES LIKE 'exam';"
```

如果没有 `exam`，重新执行：

```bash
cd /www/wwwroot/online-exam-backend
mysql -uroot -p < mysql-init.sql
```

### 教师无法上传题目

先确认用教师账号登录：

```text
teacher / teacher123
```

再确认后端日志没有数据库错误：

```bash
journalctl -u online-exam -n 100
```

如果是生产环境，优先检查 MySQL 是否运行、`exam` 用户是否有 `exam` 数据库权限。
