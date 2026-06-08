# 校园学习资料互助圈

面向在校师生打造的学习资料共享平台，用户上传课件、笔记、习题等电子资料，支持在线查阅、资料检索与收藏。

## 技术栈

- **前端**: Vue 3 + Vite + Element Plus
- **后端**: Spring Boot 3.3 + JDK 17 + Spring Data JPA
- **数据库**: MySQL 8.0
- **缓存**: Redis 7
- **部署**: Docker Compose

## 项目端口表（固定端口，禁止修改）

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 (Nginx) | **13008** | 静态资源服务 |
| 后端 (SpringBoot) | **18088** | REST API 服务 |
| MySQL | **13309** | 数据库服务 |
| Redis | **16380** | 缓存服务 |

> 所有服务均绑定 `127.0.0.1`，禁止外部访问。

## 快速开始

### 环境要求

- Docker 20.10+
- Docker Compose v2+
- 至少 4GB 可用内存

### 一键启动

```bash
# 进入项目目录
cd /path/to/project

# 执行启动脚本（推荐）
./start.sh

# 或者直接使用 docker compose
docker compose up -d --build
```

### 访问地址

启动成功后，访问以下地址：

- **前端首页**: http://localhost:13008
- **前端首页 (IPv4)**: http://127.0.0.1:13008
- **后端 API**: http://127.0.0.1:18088

## 目录结构

```
.
├── backend/                    # 后端 SpringBoot 项目
│   ├── src/main/java/
│   │   └── com/campus/study/
│   │       ├── entity/         # 实体类
│   │       ├── repository/     # JPA 数据访问层
│   │       ├── service/        # 业务逻辑层
│   │       ├── controller/     # REST API 控制层
│   │       ├── config/         # 配置类
│   │       ├── common/         # 通用类
│   │       └── util/           # 工具类
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-docker.yml
│   ├── settings.xml            # Maven 国内镜像配置
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                   # 前端 Vue3 项目
│   ├── src/
│   │   ├── views/              # 页面组件
│   │   ├── api/                # API 接口封装
│   │   ├── router/             # 路由配置
│   │   ├── store/              # Pinia 状态管理
│   │   ├── components/         # 公共组件
│   │   ├── App.vue
│   │   └── main.js
│   ├── .npmrc                  # npm 国内镜像配置
│   ├── vite.config.js
│   ├── package.json
│   └── Dockerfile
├── mysql/
│   └── init.sql                # 数据库初始化脚本
├── nginx/
│   └── conf.d/default.conf     # Nginx 反向代理配置
├── .env                        # 全局环境变量配置
├── docker-compose.yml          # Docker Compose 编排
├── start.sh                    # 一键启动脚本
└── README.md
```

## 核心功能

### 1. 学习资料上传
- 选择对应学科、年级分类
- 上传文档、图片类学习资料
- 补充资料简介与使用说明
- 支持的文件格式：PDF、Word、Excel、PPT、TXT、JPG、PNG 等

### 2. 资料检索查阅
- 通过学科、年级、分类多维度筛选
- 关键词搜索
- 按上传时间排序
- 支持在线预览文件内容

### 3. 资料收藏管理
- 一键收藏实用学习资料
- 个人中心统一管理
- 快速调取收藏的资料

### 4. 个人资料中心
- 查看本人上传的全部资料
- 支持编辑简介
- 下架失效内容

## Docker 构建优化说明

### 分层缓存机制

**后端 Dockerfile 分层策略：**
1. 先复制 `settings.xml` 和 `pom.xml`
2. 执行 `mvn dependency:go-offline` 下载依赖
3. 再复制源代码 `src/` 目录
4. 执行编译打包

**效果：** 仅当 `pom.xml` 变更时才重新下载依赖，源代码修改只触发编译。

**前端 Dockerfile 分层策略：**
1. 先复制 `package*.json` 和 `.npmrc`
2. 执行 `npm install` 安装依赖
3. 再复制源代码
4. 执行 `npm run build`

**效果：** 仅当 `package.json` 变更时才重新安装依赖。

### 国内镜像源

| 类别 | 镜像源 |
|------|--------|
| Docker 基础镜像 | DaoCloud 镜像 (`docker.m.daocloud.io`) |
| Maven 依赖 | 华为云镜像 (`repo.huaweicloud.com`) |
| npm 包 | 淘宝 NPM 镜像 (`registry.npmmirror.com`) |

### 全局镜像仓库

所有基础镜像统一通过 `DOCKER_REGISTRY` 环境变量前缀访问：
- `${DOCKER_REGISTRY}/mysql:8.0`
- `${DOCKER_REGISTRY}/redis:7-alpine`
- `${DOCKER_REGISTRY}/maven:3.8.6-openjdk-17-slim`
- `${DOCKER_REGISTRY}/node:18-alpine`
- `${DOCKER_REGISTRY}/nginx:alpine`

如需更换镜像仓库，只需修改 `.env` 文件中的 `DOCKER_REGISTRY` 即可。

## 端口配置说明

所有端口配置统一在 `.env` 文件中管理：

```dotenv
# 前端 Nginx 端口
FRONTEND_PORT=13008

# 后端 SpringBoot 端口
BACKEND_PORT=18088

# MySQL 端口
MYSQL_PORT=13309

# Redis 端口
REDIS_PORT=16380
```

### 端口占用检查

启动脚本会自动检查端口占用情况，如发现端口被占用会明确提示：

```
❌ 端口冲突: 前端(Nginx) 端口 3008 已被占用
   占用进程 PID: 12345
   进程名称: nginx
   请先停止占用进程或修改 .env 中的端口配置
```

## 常用命令

```bash
# 启动所有服务
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
docker compose logs -f frontend
docker compose logs -f backend

# 停止服务
docker compose down

# 停止并删除数据卷（慎用！会清空数据库）
docker compose down -v

# 重新构建单个服务
docker compose build backend
docker compose up -d backend
```

## 数据库连接信息

| 参数 | 值 |
|------|-----|
| Host | 127.0.0.1 |
| Port | 13309 |
| Database | qd_db |
| Username | qd_user |
| Password | qd123456 |
| Root Password | root123456 |

## Redis 连接信息

| 参数 | 值 |
|------|-----|
| Host | 127.0.0.1 |
| Port | 16380 |
| Password | redis123456 |

## 内置初始化数据

数据库自动初始化包含：

- **12个年级**：一年级、二年级、...、高三
- **10个学科**：语文、数学、英语、物理、化学、生物、政治、历史、地理、科学
- **6个分类**：试卷、课件、教案、习题、知识点总结、竞赛资料
- **1个管理员用户**：admin
- **8份示例资料**

## 开发说明

### 本地开发（前端）

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器默认运行在 `http://127.0.0.1:13008`

### 本地开发（后端）

使用 IDEA 或 Eclipse 导入 Maven 项目，运行主类 `Application.java`

后端默认运行在 `http://127.0.0.1:8080`（本地开发配置）

## 注意事项

1. **端口固定**：所有端口均为固定分配，禁止自动切换端口
2. **绑定地址**：所有服务仅绑定 `127.0.0.1`，不对外暴露
3. **数据持久化**：MySQL、Redis、上传文件均使用数据卷持久化
4. **首次构建**：首次构建需要下载依赖和基础镜像，耗时较长
5. **后续构建**：依赖未变更时会复用构建缓存，速度很快

## License

仅供校内学习交流使用，禁止商业化用途。
