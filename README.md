<p align="center">
  <h1 align="center">MatrixCloud</h1>
  <p align="center"><strong>企业级协作办公平台</strong> — 即时通讯 · 团队管理 · 视频会议 · AI 纪要 · 日程待办</p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2.5-brightgreen?logo=springboot" alt="Spring Boot 3.2.5">
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs" alt="Vue 3">
  <img src="https://img.shields.io/badge/Vite-5.2-646CFF?logo=vite" alt="Vite 5">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql" alt="MySQL 8.0">
  <img src="https://img.shields.io/badge/Redis-7-DC382D?logo=redis" alt="Redis 7">
  <img src="https://img.shields.io/badge/Docker-✓-2496ED?logo=docker" alt="Docker">
  <img src="https://img.shields.io/badge/license-MIT-blue" alt="MIT License">
</p>

---

## 📖 目录

- [项目简介](#项目简介)
- [功能模块](#功能模块)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [数据库设计](#数据库设计)
- [API 文档](#api-文档)
- [技术亮点](#技术亮点)
- [配置说明](#配置说明)
- [部署指南](#部署指南)
- [开发路线图](#开发路线图)
- [许可证](#许可证)

---

## 项目简介

**MatrixCloud** 是一款面向企业的协作办公平台，功能对标 Microsoft Teams，采用前后端分离架构。它整合了即时通讯、团队管理、视频会议、AI 智能会议纪要、语音转文字及日历待办等核心协作场景，同时提供完善的管理后台。

- **架构模式**：前后端分离 + Docker 容器化基础设施
- **后端**：Spring Boot 3.2.5 + Spring Security (JWT) + Spring Data JPA
- **前端**：Vue 3 (Composition API) + Element Plus + Pinia
- **通信协议**：REST API + 双通道 WebSocket (Raw + STOMP)
- **AI 能力**：LangChain4j 驱动的会议纪要自动生成 + SiliconFlow 语音转文字

---

## 功能模块

```
MatrixCloud 企业协作平台
│
├── 🔐 认证授权
│   ├── 邮箱 + 密码登录
│   ├── JWT 无状态认证（HMAC-SHA256 / 24h 过期）
│   ├── RBAC 角色控制（ADMIN / USER）
│   └── Redis Token 黑名单，支持主动登出
│
├── 💬 即时通讯
│   ├── 一对一私聊 / 团队群聊
│   ├── 消息类型：文本 · 图片 · 文件 · 视频 · 音频 · Emoji
│   ├── 消息搜索 / 置顶
│   ├── 文件上传至 MinIO 对象存储
│   └── Raw WebSocket 实时点对点推送
│
├── 👥 团队管理
│   ├── 创建 / 解散团队
│   ├── 成员邀请与移除
│   ├── 角色管理（管理员 / 成员）
│   └── 自动创建团队聊天室
│
├── 📹 视频会议
│   ├── 创建 / 加入会议（房间名 + 密码）
│   ├── WebRTC 音视频通话 + 屏幕共享
│   ├── LiveKit SFU 媒体服务器（支持大规模并发）
│   ├── STOMP + SockJS 信令服务
│   └── AI_SMART 会议模式（自动转录 + 纪要）
│
├── 🤖 AI 会议纪要
│   ├── 发言人感知的智能文本分块
│   ├── Map-Reduce 并行摘要流水线
│   ├── LangChain4j + OpenAI 兼容 API
│   ├── 结构化中文 Markdown 输出
│   └── MinIO 持久化缓存
│
├── 🎙️ 语音转文字
│   ├── 会议录音上传与转写
│   ├── SiliconFlow SenseVoice 模型
│   └── 发言人 + 时间戳结构化存储
│
├── 📅 日历与待办
│   ├── 月度日历视图
│   ├── 每日待办事项 CRUD
│   └── 完成状态切换
│
├── 👤 个人中心
│   ├── 个人信息编辑（昵称、头像、备注）
│   ├── 密码修改
│   └── 邮箱修改
│
├── 📇 人员目录
│   ├── 全公司人员列表
│   ├── 按昵称 / 邮箱搜索
│   └── 一键发起私聊
│
└── 🛡️ 管理后台
    ├── 数据仪表盘（ECharts 可视化）
    ├── 用户管理（搜索 · 封禁 · 解封 · 删除）
    ├── 团队与消息审计
    ├── 系统资源实时监控（CPU · 内存 · JVM）
    └── 管理员设置（密码 · 邮箱）
```

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|---|---|---|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.5 | 核心框架 |
| Spring Security | 6.x | JWT 认证 + RBAC 鉴权 |
| Spring Data JPA | 3.x (Hibernate) | ORM 持久层 |
| Spring WebSocket | 6.x | 双通道实时通信 |
| Spring Data Redis | 3.x (Lettuce) | 缓存 / 在线状态 / Token 黑名单 |
| MySQL | 8.0 | 主数据库 |
| Redis | 7.x Alpine | 缓存中间件 |
| LiveKit Server SDK | 0.5.11 | WebRTC SFU 接入令牌 |
| MinIO Client | 8.5.9 | S3 兼容对象存储 |
| LangChain4j | 0.36.2 | AI 服务编排 |
| LangChain4j OpenAI | 0.36.2 | 多模型兼容接口 |
| jjwt | 0.12.5 | JWT 令牌签发与校验 |
| Lombok | latest | 代码简化 |
| Maven | 3.x | 构建管理 |

### 前端

| 技术 | 版本 | 用途 |
|---|---|---|
| Vue 3 | 3.4.21 | Composition API 框架 |
| Vite | 5.2.8 | 构建工具 |
| Element Plus | 2.7.0 | UI 组件库 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.3.0 | 路由与导航守卫 |
| Axios | 1.6.8 | HTTP 客户端 |
| ECharts | 6.0.0 | 管理后台数据可视化 |
| LiveKit Client | 2.0.0 | WebRTC 客户端 SDK |
| @stomp/stompjs | 7.3.0 | STOMP 消息协议 |
| SockJS | 1.6.1 | WebSocket 降级兼容 |
| marked | 18.0.4 | Markdown 渲染 |
| Sass | 1.72.0 | CSS 预处理 |

### 基础设施

| 服务 | 镜像 | 端口 | 用途 |
|---|---|---|---|
| MySQL | `mysql:8.0` | 3306 | 主数据库 |
| Redis | `redis:7-alpine` | 6379 | 缓存 / 在线状态 |
| LiveKit | `livekit/livekit-server:latest` | 7880-7882 / 50000-50010 | WebRTC SFU |
| MinIO | `minio/minio:latest` | 9870 (API) / 9871 (Console) | 对象存储 |

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                         浏览器 (Vue 3)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────┐  │
│  │ 即时通讯  │ │ 视频会议  │ │ AI 纪要  │ │  管理后台     │  │
│  │ Raw WS   │ │ STOMP WS │ │ REST API │ │  ECharts      │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └───────┬───────┘  │
└───────┼────────────┼────────────┼───────────────┼───────────┘
        │            │            │               │
   ┌────▼────┐  ┌────▼─────┐ ┌───▼───────────────▼──────────┐
   │ /ws/chat│  │/ws/signal│ │      /api/* (REST)            │
   │  Raw WS │  │  STOMP   │ │  Spring Boot 3.2.5            │
   │         │  │ + SockJS │ │  JWT Filter → Controller     │
   └────┬────┘  └────┬─────┘ │       → Service → Repository  │
        │            │       └───┬───────────┬───────────────┘
        │            │           │           │
        │       ┌────▼────┐  ┌───▼───┐  ┌───▼────┐
        │       │ LiveKit │  │ MySQL │  │ Redis  │
        │       │  SFU    │  │  8.0  │  │   7    │
        │       └─────────┘  └───────┘  └────────┘
        │                                   │
        │    ┌──────────┐  ┌──────────┐     │
        └───►│  MinIO   │  │ 外部 AI  │     │
             │ 对象存储  │  │ LLM API │     │
             └──────────┘  └──────────┘     │
                                     ┌──────▼─────┐
                                     │ SiliconFlow│
                                     │ 语音转文字  │
                                     └────────────┘
```

### 双通道 WebSocket 设计

| 通道 | 端点 | 协议 | 用途 |
|---|---|---|---|
| 聊天通道 | `/ws/chat` | Raw WebSocket | 即时通讯消息点对点实时推送 |
| 信令通道 | `/ws/signaling` | STOMP over SockJS | WebRTC 信令交换（Offer/Answer/ICE） |

两条通道职责分离，互不干扰：聊天通道追求低延迟点对点送达，信令通道利用 STOMP Topic 广播机制将同一会议室的信令消息分发至所有参与者。

---

## 快速开始

### 前置要求

- **Java 17+** 与 Maven 3.x
- **Node.js 18+** 与 npm
- **Docker** 与 Docker Compose

### 1. 克隆项目

```bash
git clone https://github.com/your-org/matrix-cloud.git
cd matrix-cloud
```

### 2. 启动基础设施

```bash
# 一键启动 MySQL、Redis、LiveKit、MinIO
docker-compose up -d

# 确认服务均已启动
docker-compose ps
```

### 3. 配置APIKey

编辑 `backend/src/main/resources/application.properties`，按需覆盖以下关键配置：

```properties
# LLM API（支持 DeepSeek / 阿里百炼 / 智谱 / Moonshot 等 OpenAI 兼容接口）
langchain4j.open-ai.api-key=${LLM_API_KEY:your-api-key}

# 语音转文字（SiliconFlow）
siliconflow.api.key=${SILICONFLOW_API_KEY:your-siliconflow-ke}
```

其他配置项（数据库、Redis、JWT、MinIO 等）已内置合理的开发环境默认值，开箱即用。

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:3000`

### 6. 登录系统

| 角色 | 邮箱 | 密码 |
|---|---|---|
| 管理员 | `admin@matrix.com` | `admin123` |
| 普通用户 | `zhangsan@matrix.com` | `test123` |
| 普通用户 | `lisi@matrix.com` | `test123` |
| 普通用户 | `wangwu@matrix.com` | `test123` |

管理员登录后自动跳转至管理后台 `/admin/dashboard`；普通用户登录后进入聊天页面 `/chat`。

---

## 项目结构

```
MatrixCloud/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/matrix/cloud/
│   │   ├── config/                   # Spring 配置类
│   │   │   ├── SecurityConfig.java       # Spring Security + JWT
│   │   │   ├── WebSocketConfig.java      # Raw WebSocket 端点
│   │   │   ├── StompWebSocketConfig.java  # STOMP 信令端点
│   │   │   ├── RedisConfig.java          # Redis 连接配置
│   │   │   ├── AiConfig.java             # LangChain4j + 线程池
│   │   │   └── WebConfig.java            # CORS / 静态资源
│   │   ├── controller/               # REST 控制器（14 个）
│   │   │   ├── AuthController.java       # 登录 / 登出
│   │   │   ├── UserController.java       # 用户 CRUD / 搜索
│   │   │   ├── ChatController.java       # 聊天 / 文件上传
│   │   │   ├── TeamController.java       # 团队管理
│   │   │   ├── VideoMeetingController.java # 视频会议
│   │   │   ├── MeetingSummaryController.java  # AI 纪要
│   │   │   ├── MeetingTranscriptController.java # 语音转文字
│   │   │   ├── CalendarTodoController.java  # 日历待办
│   │   │   ├── AdminController.java      # 管理后台
│   │   │   ├── AsrWebhookController.java # ASR 回调
│   │   │   ├── WebRTCSignalingController.java # WebRTC 信令
│   │   │   ├── HealthController.java     # 健康检查
│   │   │   └── SttTestController.java    # 语音测试
│   │   ├── dto/                      # 数据传输对象
│   │   │   ├── ApiResponse.java          # 统一响应包装
│   │   │   ├── LoginRequest.java         # 登录请求
│   │   │   ├── CreateMeetingRequest.java  # 创建会议
│   │   │   ├── JoinMeetingRequest.java    # 加入会议
│   │   │   ├── GenerateSummaryRequest.java # 生成摘要
│   │   │   ├── SaveTranscriptRequest.java  # 保存转录
│   │   │   ├── CalendarTodoRequest.java   # 日历待办
│   │   │   └── AsrTranscriptionRequest.java # ASR 回调
│   │   ├── entity/                   # JPA 实体（11 张表）
│   │   │   ├── User.java                 # 用户
│   │   │   ├── Team.java                 # 团队
│   │   │   ├── TeamMember.java           # 团队成员
│   │   │   ├── ChatSession.java          # 聊天会话
│   │   │   ├── ChatParticipant.java      # 聊天参与者
│   │   │   ├── ChatMessage.java          # 聊天消息
│   │   │   ├── VideoMeeting.java         # 视频会议
│   │   │   ├── MeetingParticipant.java   # 会议参与者
│   │   │   ├── MeetingSummary.java       # AI 摘要
│   │   │   ├── MeetingTranscript.java    # 语音转录
│   │   │   └── CalendarTodo.java         # 日历待办
│   │   ├── repository/               # Spring Data JPA 仓库
│   │   ├── service/                  # 业务逻辑层（12 个 Service）
│   │   │   ├── UserService.java          # 用户注册 / 登录 / 管理
│   │   │   ├── AdminService.java         # 管理后台统计 / 审计
│   │   │   ├── ChatService.java          # 消息 / 文件管理
│   │   │   ├── TeamService.java          # 团队 CRUD / 成员管理
│   │   │   ├── VideoMeetingService.java  # 会议生命周期
│   │   │   ├── LiveKitService.java       # LiveKit Token 生成
│   │   │   ├── FileStorageService.java   # MinIO 对象存储
│   │   │   ├── MeetingSummaryManager.java # Map-Reduce 摘要编排
│   │   │   ├── MeetingSummaryAiService.java # LLM 提示词定义
│   │   │   ├── MeetingTranscriptService.java # 语音转文字处理
│   │   │   ├── SpeechToTextService.java  # SiliconFlow API 调用
│   │   │   └── CalendarTodoService.java  # 待办 CRUD
│   │   ├── security/                 # 安全组件
│   │   │   ├── JwtAuthenticationFilter.java    # JWT 验证过滤器
│   │   │   └── CustomUserDetailsService.java   # 用户详情加载
│   │   ├── websocket/               # WebSocket 处理
│   │   │   ├── ChatWebSocketHandler.java       # 聊天消息路由
│   │   │   └── WebSocketInterceptor.java       # JWT 握手拦截
│   │   └── exception/               # 异常处理
│   │       ├── GlobalExceptionHandler.java     # 全局异常映射
│   │       └── ResourceNotFoundException.java   # 404 异常
│   ├── src/main/resources/
│   │   └── application.properties   # 应用配置
│   └── pom.xml                       # Maven 依赖
│
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/
│   │   │   ├── request.js            # Axios 实例 + 拦截器
│   │   │   └── index.js              # API 模块封装
│   │   ├── assets/
│   │   │   └── style.css             # 全局样式
│   │   ├── components/
│   │   │   └── EmojiPicker.vue       # Emoji 选择器
│   │   ├── router/
│   │   │   └── index.js              # 路由配置 + 导航守卫
│   │   ├── store/
│   │   │   └── user.js               # Pinia 用户状态
│   │   ├── utils/
│   │   │   └── webrtc.js             # WebRTC 服务封装
│   │   ├── views/
│   │   │   ├── Login.vue             # 登录页
│   │   │   ├── Home.vue              # 主布局（侧边栏导航）
│   │   │   ├── Chat.vue              # 即时通讯
│   │   │   ├── Teams.vue             # 团队管理
│   │   │   ├── Meetings.vue          # 视频会议
│   │   │   ├── Calendar.vue          # 日历待办
│   │   │   ├── People.vue            # 人员目录
│   │   │   ├── Profile.vue           # 个人中心
│   │   │   ├── AdminLayout.vue       # 管理后台布局
│   │   │   ├── AdminDashboard.vue    # 数据仪表盘
│   │   │   ├── AdminUsers.vue        # 用户管理
│   │   │   ├── AdminTeams.vue        # 团队管理
│   │   │   └── AdminSettings.vue     # 管理员设置
│   │   ├── App.vue                   # 根组件
│   │   └── main.js                   # 应用入口
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
├── mysql/
│   └── init.sql                      # 数据库初始化脚本（11张表 + 默认数据）
│
├── livekit/
│   └── config.yaml                   # LiveKit SFU 配置
│
├── docker-compose.yml                # Docker 服务编排
└── README.md
```

---

## 数据库设计

### 核心 ER 关系

```
users ──┬── team_members ──── teams
        │
        ├── chat_participants ── chat_sessions ── chat_messages
        │
        ├── meeting_participants ── video_meetings
        │
        ├── meeting_summaries (AI 纪要)
        ├── meeting_transcripts (语音转录)
        └── calendar_todos
```

### 表清单

| 表名 | 实体 | 说明 |
|---|---|---|
| `users` | User | 用户账户（邮箱、密码、昵称、头像、部门、职位、角色、状态） |
| `teams` | Team | 团队（名称唯一、创建者） |
| `team_members` | TeamMember | 团队-用户多对多关联（角色 ADMIN/MEMBER） |
| `chat_sessions` | ChatSession | 聊天会话（类型 PRIVATE/TEAM） |
| `chat_participants` | ChatParticipant | 会话-用户关联 |
| `chat_messages` | ChatMessage | 消息（内容、类型、文件元数据、置顶标志） |
| `video_meetings` | VideoMeeting | 视频会议（房间名、LiveKit Room ID、密码、状态） |
| `meeting_participants` | MeetingParticipant | 会议参与者（角色、静音状态、加入/离开时间） |
| `meeting_summaries` | MeetingSummary | AI 纪要（原始文本、摘要、分块数、模型名、状态） |
| `meeting_transcripts` | MeetingTranscript | 语音转录（说话人、内容、时间戳） |
| `calendar_todos` | CalendarTodo | 日历待办（日期、内容、完成状态） |

数据库在首次 Docker 启动时通过卷挂载 `mysql/init.sql` 自动初始化，使用 `CREATE TABLE IF NOT EXISTS` 确保幂等安全。

---

## API 文档

所有 API 返回统一格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

### 认证

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/api/auth/login` | 用户登录（返回 JWT） | 否 |
| POST | `/api/auth/logout` | 用户登出（Token 加入黑名单） | Bearer Token |

### 用户

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| GET | `/api/users/me` | 获取当前用户信息 | Bearer Token |
| PUT | `/api/users/me` | 更新个人资料 | Bearer Token |
| GET | `/api/users/search?nickname=` | 搜索用户 | Bearer Token |
| GET | `/api/users/{id}` | 获取用户详情 | Bearer Token |

### 聊天

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/api/chat/private?targetUserId=` | 创建私聊会话 | Bearer Token |
| GET | `/api/chat/private/sessions` | 获取私聊会话列表 | Bearer Token |
| GET | `/api/chat/teams/{teamId}/session` | 获取团队聊天会话 | Bearer Token |
| GET | `/api/chat/sessions/{id}/messages` | 获取消息列表（支持 keyword 搜索） | Bearer Token |
| POST | `/api/chat/sessions/{id}/messages` | 发送消息 | Bearer Token |
| POST | `/api/chat/sessions/{id}/upload` | 上传文件 | Bearer Token |
| GET | `/api/chat/sessions/{id}/pinned` | 获取置顶消息 | Bearer Token |
| PUT | `/api/chat/messages/{id}/pin` | 置顶消息 | Bearer Token |
| PUT | `/api/chat/messages/{id}/unpin` | 取消置顶 | Bearer Token |
| GET | `/api/chat/files/{fileName}` | 下载文件 | Bearer Token |

### 团队

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/api/teams` | 创建团队 | Bearer Token |
| GET | `/api/teams/my-teams` | 获取我的团队列表 | Bearer Token |
| GET | `/api/teams/{teamId}/members` | 获取团队成员 | Bearer Token |
| POST | `/api/teams/{teamId}/members` | 添加成员 | Bearer Token |
| PUT | `/api/teams/{teamId}/members/{userId}/role` | 修改成员角色 | Bearer Token |
| DELETE | `/api/teams/{teamId}/members/{userId}` | 移除成员 | Bearer Token |
| DELETE | `/api/teams/{teamId}` | 解散团队 | Bearer Token |

### 视频会议

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/api/meetings` | 创建会议 | Bearer Token |
| POST | `/api/meetings/join` | 加入会议 | Bearer Token |
| GET | `/api/meetings/room/{roomName}` | 获取会议信息 | Bearer Token |
| POST | `/api/meetings/{id}/end` | 结束会议 | Bearer Token |
| GET | `/api/meetings` | 列出活跃会议 | Bearer Token |
| POST | `/api/meetings/token` | 获取 LiveKit 接入令牌 | Bearer Token |
| GET | `/api/meetings/history` | 会议历史 | Bearer Token |
| GET | `/api/meetings/records` | 会议记录（含参与者） | Bearer Token |

### AI 会议纪要

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/api/meetings/{id}/summary` | 触发 AI 摘要生成 | Bearer Token |
| GET | `/api/meetings/{id}/summary` | 获取最新摘要 | Bearer Token |
| GET | `/api/meetings/summaries/history` | 摘要历史 | Bearer Token |

### 语音转文字

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| POST | `/api/meetings/{id}/transcripts` | 上传音频文件并转写 | Bearer Token |
| GET | `/api/meetings/{id}/transcripts` | 获取转录记录 | Bearer Token |
| POST | `/api/meetings/{id}/transcripts/generate-summary` | 从转录生成摘要 | Bearer Token |
| GET | `/api/meetings/{id}/history` | 会议历史详情 | Bearer Token |

### 日历待办

| 方法 | 路径 | 说明 | 认证 |
|---|---|---|---|
| GET | `/api/calendar/todos?date=` | 获取指定日期待办 | Bearer Token |
| GET | `/api/calendar/month?year=&month=` | 获取月度待办 | Bearer Token |
| POST | `/api/calendar/todos` | 添加待办 | Bearer Token |
| PUT | `/api/calendar/todos/{id}` | 更新待办 | Bearer Token |
| PUT | `/api/calendar/todos/{id}/toggle` | 切换完成状态 | Bearer Token |
| DELETE | `/api/calendar/todos/{id}` | 删除待办 | Bearer Token |

### 管理后台（需 ADMIN 角色）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/admin/statistics` | 获取统计数据 |
| GET | `/api/admin/system-info` | 获取系统资源信息 |
| GET | `/api/admin/users` | 获取所有用户 |
| POST | `/api/admin/users` | 创建用户 |
| PUT | `/api/admin/users/{id}` | 更新用户信息 |
| DELETE | `/api/admin/users/{id}` | 删除用户 |
| POST | `/api/admin/users/{id}/ban` | 封禁用户 |
| POST | `/api/admin/users/{id}/unban` | 解封用户 |
| GET | `/api/admin/teams` | 获取所有团队 |
| GET | `/api/admin/teams/{id}/messages` | 查看团队聊天记录 |
| POST | `/api/admin/change-password` | 修改管理员密码 |
| POST | `/api/admin/change-email` | 修改管理员邮箱 |

---

## 技术亮点

### 1. AI 会议纪要的 Map-Reduce 流水线

针对长会议记录超出 LLM Token 上限的问题，设计了一套**发言人感知的分块并行摘要方案**：

1. **智能分块** — 按发言人维度切割文本，保持语义连贯性；块间 15% 重叠防止信息断裂
2. **并行摘要** — `CompletableFuture` + 专用线程池（4-8 线程）并发调用 LLM
3. **合并精炼** — 所有分块摘要二次送入 LLM，合并为完整纪要
4. **结构化输出** — LangChain4j `@AiService` 驱动，输出标准化 Markdown（含会议主题、议题讨论、决议事项、待办任务等章节）

配置灵活可调：块大小 2000 字符、重叠率 15%、最大 20 块。

### 2. 双通道 WebSocket 架构

| 通道 | 协议 | 适用场景 |
|---|---|---|
| `/ws/chat` | Raw WebSocket | 聊天消息点对点实时推送 |
| `/ws/signaling` | STOMP over SockJS | WebRTC 信令 Topic 广播 |

两条通道物理隔离、职责分离。聊天通道维护 `ConcurrentHashMap<Long, WebSocketSession>` 实现精确路由；信令通道利用 STOMP 订阅机制将 Offer/Answer/ICE Candidate 分发至房间内所有参与者。

### 3. 无状态 JWT + Redis 黑名单

- JWT (HMAC-SHA256, 24h) 签发后无服务端状态
- Redis 黑名单存储已登出 Token（TTL 对齐 Token 剩余有效时间），解决无状态 Token 无法主动失效的固有问题
- `online_users` Set 追踪在线用户，支撑管理后台实时在线统计

### 4. WebRTC 工程化封装

前端 `WebRTCService` 类将复杂流程封装为声明式服务：

- **双 RTCPeerConnection 模型** — `_send` / `_recv` 独立连接，双向媒体流隔离
- **屏幕共享** — 运行时动态添加屏幕轨道，`replaceTrack()` 无缝切换
- **STOMP 事件总线** — 六类信令消息统一通过 STOMP 主题订阅处理
- **完整生命周期** — 自动处理连接状态变更、轨道移除、对等端断开清理

### 5. JMX 系统资源监控

管理后台通过 Java `OperatingSystemMXBean` 采集操作系统级 CPU / 内存指标，前端 ECharts 仪表盘实时可视化。零额外依赖，与 Spring Boot 原生集成。

### 6. 统一 API 契约

- `ApiResponse<T>` 泛型包装器统一所有接口返回格式
- `@RestControllerAdvice` 全局异常处理：`ResourceNotFoundException` → 404、业务异常 → 400、未知异常 → 500
- 前端 Axios 拦截器统一处理 401 自动跳转、错误消息弹窗

---

## 配置说明

### 应用配置 (`application.properties`)

关键配置项及环境变量覆盖：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` | 8080 | 后端端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/matrix_cloud` | 数据库连接 |
| `spring.data.redis.host` | localhost | Redis 地址 |
| `spring.data.redis.password` | redis123456 | Redis 密码 |
| `jwt.secret` | (内置) | ⚠️ 生产环境必须修改为强密钥 |
| `jwt.expiration` | 86400000 (24h) | Token 过期时间 |
| `minio.endpoint` | http://localhost:9870 | MinIO API 地址 |
| `livekit.url` | ws://172.17.27.24:7880 | LiveKit WebSocket 地址 |
| `${LLM_API_KEY}` | (环境变量) | LLM API Key |
| `${LLM_BASE_URL}` | https://api.deepseek.com | LLM 接口地址 |
| `${LLM_MODEL_NAME}` | deepseek-chat | 模型名称 |
| `${SILICONFLOW_API_KEY}` | (环境变量) | 语音转文字 API Key |
| `meeting.summary.chunk-size` | 2000 | AI 摘要分块大小（字符） |
| `meeting.summary.chunk-overlap-ratio` | 0.15 | 分块重叠比例 |
| `meeting.summary.max-chunks` | 20 | 最大并行分块数 |

### MinIO 对象存储

| 配置 | 值 |
|---|---|
| Console | http://localhost:9871 |
| Access Key | minioadmin |
| Secret Key | minioadmin123 |
| Bucket | matrix-cloud |

存储内容：用户头像、聊天图片/文件、AI 会议纪要 Markdown。

### Redis Key 设计

| Key 模式 | 类型 | TTL | 说明 |
|---|---|---|---|
| `blacklist:{token}` | String | Token 剩余时间 | JWT 登出黑名单 |
| `online_users` | Set | 永久 | 在线用户 ID 集合 |

---

## 部署指南

### Docker 生产部署

```bash
# 1. 构建前端
cd frontend
npm install && npm run build

# 2. 构建后端
cd backend
mvn clean package -DskipTests

# 3. 编写 Dockerfile 将前后端打包为单一镜像，或分别部署

# 4. 修改 docker-compose.yml 中的默认密码和密钥
#    - MySQL 密码
#    - Redis 密码
#    - MinIO Access/Secret Key
#    - JWT Secret
#    - LiveKit API Key/Secret

# 5. 启动所有服务
docker-compose up -d
```

### 生产环境检查清单

- [ ] 修改所有默认密码（数据库、Redis、MinIO、LiveKit）
- [ ] 设置强随机 JWT Secret（至少 256 位）
- [ ] 配置 LLM API Key 和语音转文字 API Key
- [ ] LiveKit `node_ip` 设为服务器公网 IP
- [ ] 配置 HTTPS（Nginx 反向代理）
- [ ] 前端 `vite.config.js` 中配置正确的 API 代理地址
- [ ] 启用 Redis AOF 持久化（已默认开启）
- [ ] 配置 MySQL 定期备份

---

## 开发路线图

- [x] 用户认证（JWT + Redis 黑名单）
- [x] 即时通讯（Raw WebSocket 实时推送）
- [x] 团队管理（创建 / 成员 / 角色）
- [x] 视频会议（WebRTC + LiveKit SFU）
- [x] AI 会议纪要（Map-Reduce 并行摘要）
- [x] 语音转文字（SiliconFlow SenseVoice）
- [x] 日历待办
- [x] 管理后台（仪表盘 / 用户管理 / 系统监控）
- [x] 文件管理（MinIO 对象存储）
- [ ] 消息已读/未读状态
- [ ] 消息引用回复
- [ ] @提及通知
- [ ] 文件预览（图片 / PDF / Office）
- [ ] 第三方登录（OAuth 2.0）
- [ ] 移动端适配（PWA）
- [ ] 国际化（i18n）
- [ ] 单元测试与集成测试覆盖

---

## 许可证

本项目基于 [MIT License](LICENSE) 开源。

---

<p align="center">
  <sub>Built with ❤️ by the MatrixCloud Team</sub>
</p>
