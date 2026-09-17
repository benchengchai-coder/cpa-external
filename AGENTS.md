# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## 项目定位与不可违反的约束

本项目是 CLIProxyAPI 项目的外部扩展程序，不是 CLIProxyAPI 的实现或分支。

- 严禁修改 CLIProxyAPI 项目代码；需要适配时只能通过其公开接口、配置、插件扩展点或外部 API 完成。
- 本项目不再实现 AI 请求路由转发、渠道路由、故障降级、上游凭据代理、SSE/WS 中继或 OpenAI 兼容 `/v1/*` 网关。
- `ruoyi-aigate` 中的中继、路由、适配器、鉴权过滤器、限流和 OAuth 上游调用代码已移除；后续不得恢复类似功能。
- 业务管理、用户、支付、邀请、签到、兑换码、文件等独立扩展可以继续维护，但不得通过本项目转发 AI 请求。
- 这组约束是项目长期记忆；每次新增模块、接口、配置或 Docker 反向代理时都必须先检查是否重新引入路由转发。

## 项目概述

基于 RuoYi-Vue 3.9.2 二次开发的 CLIProxyAPI 外部管理扩展。后端负责后台管理和独立业务扩展，不承载 AI 上游请求转发。

- 后端：Spring Boot 4.0.3 + Java 21 + MyBatis + Druid + Redis
- 前端：Vue 3 + TypeScript + Element Plus + Vite（`ruoyi-ui`）
- 部署：后端镜像由 GitHub Actions 自动构建并推送 ghcr.io（`.github/workflows/build-image.yml`），服务器 `docker/deploy-server.sh update` 拉取更新（MySQL + Redis + cpa-external 三容器）；前端由 CI 构建后 rsync 同步到服务器目录（依赖仓库 SSH secrets），由宿主机 Caddy 直接托管；完整流程见 `docker/DEPLOY.md`

## 常用命令

后端（在仓库根目录执行）：

```bash
# 完整构建验证（跳过测试；CI 打包镜像用的同一条命令）
mvn clean package -Dmaven.test.skip=true

# 仅编译后台入口及其依赖
mvn -pl ruoyi-admin -am compile

# 启动后端（端口 8080）—— bin/run.bat 等价命令
java -Xms256m -Xmx1024m -jar ruoyi-admin/target/ruoyi-admin.jar

# 运行全部测试
mvn test

# 运行单个测试类 / 单个方法（按实际模块选择）
mvn test -pl ruoyi-admin -Dtest=ExampleTest
```

本机环境：JDK 21 安装路径为 `F:\install\jdk\jdk21`（Git Bash 中为 `/f/install/jdk/jdk21`）。系统默认 `JAVA_HOME` 指向 jdk8，直接执行 Maven 会报「无效的目标发行版: 21」，需在命令前覆盖：`JAVA_HOME=/f/install/jdk/jdk21 mvn ...`。

前端（`ruoyi-ui/`）：

```bash
npm install
npm run dev          # 本地开发
npm run build:prod   # 生产构建
npm run build:stage  # 预发构建
```

数据库初始化：`sql/cpa-plugin.sql`（本项目业务表）+ `sql/quartz.sql`（定时任务表）。历史 AI 网关表不会被运行时代码使用，清理数据库结构需单独制定迁移方案。

## 模块职责

| 模块 | 职责 |
|------|------|
| `ruoyi-admin` | Spring Boot 启动入口（`RuoYiApplication`），所有 HTTP 控制器和运行配置位于此模块。 |
| `ruoyi-cpa-external` | CLIProxyAPI 外部扩展业务：API Key、模型、`ai_log` 日志管理与 CLIProxyAPI 日志落库，以及 API Key 登录凭证校验；不负责 AI 请求转发。 |
| `ruoyi-framework` | Spring 框架基础设施：安全（Spring Security）、拦截器、缓存、AOP。 |
| `ruoyi-system` | 系统管理（用户/角色/菜单/部门/字典）。 |
| `ruoyi-common` | 通用工具、注解、常量、异常。 |
| `ruoyi-quartz` | 定时任务。 |

## 与 CLIProxyAPI 的边界

CLIProxyAPI 是外部系统。本项目只提供管理端和独立业务能力；任何 CLIProxyAPI 请求必须由 CLIProxyAPI 自己处理。本项目不得新增 `/v1/*` 中继控制器、上游 HTTP/WS 客户端、渠道路由或故障转移逻辑。`ai_log` 只接收 CLIProxyAPI 已完成请求产生的日志，不参与请求处理；落库时通过 `api_key` 查询本项目 API Key 对应的 `user_id`。

## 编码规范

- 服务端**禁止使用 `var`**，所有局部变量必须显式声明类型。
- 注释、commit message、文档使用中文。
- 代码风格沿用若依既有约定（4 空格缩进，`{` 换行）。
- **Jackson 版本边界必须严格遵守**：本项目基于 Spring Boot 4，Spring MVC 的 HTTP 请求/响应数据绑定使用 Jackson 3。所有会出现在 Controller 入参、返回值或其 DTO 字段中的 `ObjectMapper`、`JsonNode` 等数据绑定类型，必须使用 `tools.jackson.databind.*`，不得使用 `com.fasterxml.jackson.databind.*`。`com.fasterxml.jackson.annotation.*` 注解包仍可正常使用。项目内部遗留代码可以继续使用 Jackson 2，但严禁在同一条序列化链路中混用两代 `ObjectMapper`/`JsonNode`；新增 HTTP JSON DTO 后必须增加一次真实请求结构的反序列化测试。

## 开发提示

- 新增 CLIProxyAPI 集成时，优先使用其公开 API 或独立客户端；不得把请求流转经过本项目。
- 修改后台控制器或独立业务模块后，至少运行 `mvn -pl ruoyi-admin -am test` 或针对性测试。
- Docker 部署配置在 `docker/docker-compose.yml`，`.env.example` 是必读的环境变量模板，完整部署与发版流程见 `docker/DEPLOY.md`；禁止新增 `/v1/*` 反向代理规则。
