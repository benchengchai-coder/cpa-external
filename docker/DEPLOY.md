# cpa-external 部署说明

本文档描述当前部署架构与完整发版流程。

## 架构总览

```
浏览器 ──> 宿主机 Caddy（80/443，自动 HTTPS）
              ├─ /prod-api/*  剥离前缀后反代 ──> cpa-external 容器（127.0.0.1:8080）
              └─ 其余路径：直接托管 /opt/cpa-external/frontend 下的前端静态文件

Docker Compose 管理三个容器：
  cpa-external-mysql   MySQL 8.0    （仅绑定宿主机回环 3306）
  cpa-external-redis   Redis 7.2    （仅绑定宿主机回环 6379）
  cpa-external         Spring Boot  （镜像来自 ghcr.io，仅绑定宿主机回环 8080）

CLIProxyAPI 独立运行在宿主机上（默认 8317 端口），由应用容器通过
host.docker.internal 访问；其自身流量不经过本项目。
```

要点：

- **后端**：代码推送到 GitHub（master 分支）后，GitHub Actions 自动执行
  Maven 打包（含测试）→ 构建镜像 → 推送 `ghcr.io/<owner>/cpa-external`
  （`latest` + commit 短哈希 + 版本标签），见 `.github/workflows/build-image.yml`。
  服务器只需 `bash deploy-server.sh update` 拉取更新。
- **前端**：本地 `npm run build:prod` 产出 `dist`，单独上传到服务器
  `/opt/cpa-external/frontend`，由宿主机 Caddy 直接托管，与后端镜像互不影响。
- **Caddy 装在宿主机**：负责域名、HTTPS 证书、前端静态托管与反向代理，
  与容器生命周期解耦。
- 所有容器端口只绑定 `127.0.0.1`，公网唯一入口是宿主机 Caddy。

## 一、服务器初始化（仅首次）

以部署目录 `/opt/cpa-external` 为例：

```bash
# 1. 安装 Docker（官方脚本）与 Compose 插件
curl -fsSL https://get.docker.com | sh

# 2. 准备目录，从仓库复制部署文件（无需源码）
mkdir -p /opt/cpa-external/frontend
#   需要复制的文件：
#   docker-compose.yml、deploy-server.sh、.env.example
#   mysql/conf/my.cnf、mysql/init/*.sql、redis/conf/redis.conf
#   caddy/Caddyfile（作为宿主机 Caddy 的配置模板）

# 3. 配置环境变量
cd /opt/cpa-external
cp .env.example .env
nano .env        # 必填：GHCR_OWNER（GitHub 用户名小写）、MYSQL_ROOT_PASSWORD、REDIS_PASSWORD、TOKEN_SECRET、RUOYI_MAIL_SECRET

# 4. 拉取私有镜像前登录 GHCR（创建 PAT 时勾选 read:packages）
#    若在 GitHub Package 设置中将镜像改为 public，可跳过本步
echo "<你的GitHub访问令牌>" | docker login ghcr.io -u <GitHub用户名> --password-stdin
```

### 宿主机 Caddy（仅首次）

```bash
# Debian/Ubuntu 为例（其他系统见 https://caddyserver.com/docs/install）
sudo apt install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt update && sudo apt install -y caddy

# 用仓库内模板覆盖默认配置，按需修改站点域名与前端目录
sudo cp caddy/Caddyfile /etc/caddy/Caddyfile
sudo nano /etc/caddy/Caddyfile
sudo systemctl enable --now caddy
sudo systemctl reload caddy
```

> 域名直接写在 `/etc/caddy/Caddyfile` 站点地址处，修改后 `systemctl reload caddy` 生效。
> 证书由 Caddy 自动申请与续期（要求域名 A 记录已指向服务器、80/443 端口可从公网访问）。

## 二、日常发版

### 后端（改了 Java 代码 / 配置）

```bash
git push origin master          # GitHub Actions 自动：Maven 打包（含测试）→ 构建镜像 → 推送 GHCR
# 到仓库 Actions 页确认构建成功后，服务器上执行：
ssh user@server
cd /opt/cpa-external
bash deploy-server.sh update    # 拉取最新镜像并滚动更新容器
```

也可打 `v*` 标签触发构建（如 `git tag v1.2.0 && git push origin v1.2.0`），
镜像会额外带上同名版本标签。

### 前端（只改了页面）

```bash
cd ruoyi-ui
npm run build:prod

# 上传前建议在服务器备份当前版本（见「回滚」）
scp -r dist/* user@server:/opt/cpa-external/frontend/
```

上传完成即生效，无需重启任何服务。构建产物带内容哈希，
旧文件残留不影响访问；定期登录服务器清理即可
（或两端都有 rsync 时改用 `rsync -av --delete dist/ user@server:/opt/cpa-external/frontend/`）。

## 三、回滚

后端（镜像按 commit 短哈希留存，随时可切）：

```bash
cd /opt/cpa-external
# 短哈希可在 GitHub 仓库 Actions 构建日志或 ghcr 镜像的 Tags 页查到
nano .env            # IMAGE_TAG=abc1234（要回滚到的 commit 短哈希）
bash deploy-server.sh update
```

前端：

```bash
# 发版前备份
ssh user@server "cp -r /opt/cpa-external/frontend /opt/cpa-external/frontend.bak-$(date +%m%d%H%M)"

# 回滚：换回备份目录内容后即生效（无需 reload）
ssh user@server "rm -rf /opt/cpa-external/frontend && mv /opt/cpa-external/frontend.bak-XXXXXX /opt/cpa-external/frontend"
```

## 四、常用运维命令

```bash
bash deploy-server.sh status            # 容器状态
bash deploy-server.sh logs cpa-external # 跟踪应用日志（宿主机 ./logs 同步落盘）
bash deploy-server.sh restart           # 重启容器（不重建镜像）
docker image prune -f                   # 定期清理发版堆积的旧镜像层

sudo systemctl reload caddy             # 修改 Caddyfile 后生效
sudo journalctl -u caddy -f             # Caddy 日志

# 远程访问 MySQL/Redis（端口仅绑定回环，走 SSH 隧道）
ssh -L 3306:127.0.0.1:3306 user@server  # 之后本地连 127.0.0.1:3306
```

## 五、数据与备份

- MySQL 数据：`./mysql/data`（bind mount）
- 上传文件：`./uploads`；应用日志：`./logs`
- 建议为 MySQL 配置定时备份，例如 crontab：

```bash
# 每日 3:00 备份并保留 7 天（密码含特殊字符时需加引号）
0 3 * * * docker exec cpa-external-mysql sh -c 'exec mysqldump -uroot -p"MYSQL_ROOT_PASSWORD" --single-transaction cpa_external' | gzip > /opt/cpa-external/backup/cpa-external-$(date +\%Y\%m\%d).sql.gz && find /opt/cpa-external/backup -mtime +7 -delete
```

## 六、注意事项

- 服务器 DNS 需可解析公网域名（Caddy 申请证书用），国内服务器注意 Let's Encrypt
  的访问连通性。
- `deploy-server.sh` 会在启动时校验 `.env` 必填项，未修改默认占位值会拒绝启动。
- 应用 JVM 内存通过 `.env` 的 `JAVA_OPTS` 调整，改完执行
  `bash deploy-server.sh update` 生效。
- 首次部署时 MySQL 会自动执行 `mysql/init/` 下的初始化 SQL（仅数据目录为空时执行）。
