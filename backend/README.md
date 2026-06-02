# AssessFlow Backend

自适应在线测评引擎后端服务

## 技术栈
- **Java 17**
- **Spring Boot 3.2**
- **MyBatis-Plus 3.5.5**
- **MySQL 8.0+**
- **Redis** (分布式锁+缓存)

## 快速启动

### 1. 环境准备
```bash
# 启动 MySQL，创建数据库
mysql -u root -p < backend/src/main/resources/db/schema.sql

# 启动 Redis (默认端口 6379)
redis-server
```

### 2. 配置修改
编辑 `backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/assess_flow
    username: your_username
    password: your_password
```

### 3. 运行项目
```bash
cd backend
mvn spring-boot:run
```

服务启动在 `http://localhost:8080/api`

### 4. 运行集成测试
```bash
cd backend
mvn test -Dtest=ExamFlowIntegrationTest
```

## 核心接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/exams/{paperId}/sessions` | POST | 开考创建会话 |
| `/api/sessions/{sessionId}` | GET | 查询会话快照/断线续考 |
| `/api/sessions/{sessionId}/submit` | POST | 提交答案 |
| `/api/admin/questions` | POST | GM造题入库 |
| `/api/admin/sessions/{id}/jump` | POST | GM跳环节 |
| `/api/admin/sessions/{id}/finish` | POST | GM一键交卷 |

## 项目结构

```
backend/
├── src/main/java/com/bless/assess/
│   ├── config/          # Redis、缓存配置
│   ├── controller/      # 接口层（薄Controller）
│   ├── service/         # 业务逻辑层（核心）
│   ├── mapper/          # MyBatis-Plus数据访问层
│   ├── entity/          # 数据实体
│   ├── dto/             # 数据传输对象
│   ├── vo/              # 视图对象（返回前端）
│   ├── enums/           # 枚举定义
│   ├── exception/       # 全局异常处理
│   └── grader/          # 评分器策略模式实现
├── src/main/resources/
│   ├── application.yml  # 应用配置
│   └── db/schema.sql    # 数据库建表SQL
└── src/test/            # 集成测试
```
