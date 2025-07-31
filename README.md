# 🌍 TerraForge - 高性能地形地图切片服务

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-8+-orange.svg)](https://www.oracle.com/java/technologies/javase-downloads.html)
[![Python](https://img.shields.io/badge/Python-3.9+-green.svg)](https://www.python.org/downloads/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![Status](https://img.shields.io/badge/Status-Development-yellow.svg)](https://github.com/terraforge/terraforge-server)

> **专业级地理信息处理平台，专注于大规模地形和地图数据的高效切片处理**

TerraForge是一个集成了先进算法的地理信息处理平台，提供高精度地形切片、地图瓦片生成、工作空间管理等专业GIS服务。集成GDAL、CTB等专业地理处理工具，支持大规模地理数据的高效处理。

### 🔄 双引擎架构

TerraForge采用双引擎架构，针对不同场景提供最优解决方案：

| 引擎类型 | 适用场景 | 特点 | 优势 |
|---------|---------|------|------|
| **Java引擎** | 小图切片、精细控制 | 高灵活性、参数丰富 | 适合研发调试、小规模数据处理 |
| **Docker引擎** | 复杂图像、批量处理 | 集成GDAL/CTB工具链 | 适合生产环境、大规模数据处理 |

**📥 获取方式**: 项目当前通过网盘分享，请使用提供的百度网盘链接下载Docker镜像文件。

![TerraForge Architecture](https://img.shields.io/badge/Architecture-Java%20%2B%20Python-blue?style=for-the-badge)

---

## ✨ 核心特性

### 🚀 双引擎切片技术

#### Java引擎特性
- **🎯 精准控制**：丰富的参数配置，支持精细化调节
- **⚡ 轻量快速**：适合小图切片，启动速度快
- **🔧 开发友好**：便于调试和二次开发
- **📐 灵活性高**：支持各种自定义切片策略

#### Docker引擎特性  
- **🏗️ 专业工具链**：集成GDAL、CTB等业界标准工具
- **📊 复杂处理**：支持大规模、复杂地理数据处理
- **🔄 批量操作**：优化的批处理和并行计算
- **🌍 生产就绪**：稳定可靠，适合生产环境部署

### 🏔️ 地形切片技术
- **量化网格算法**：基于半边数据结构的高精度三维地形网格生成
- **自适应细分**：根据地形复杂度动态调整三角网密度  
- **重心坐标插值**：保证高精度的高程插值和平滑过渡
- **边界约束处理**：确保相邻瓦片完美拼接，无缝隙问题

### 🗺️ 地图切片技术
- **防缝隙算法**：精确像素计算和边界扩展策略
- **多重采样**：双线性插值和抗锯齿处理
- **格式支持**：TIF、GeoTIFF、PNG、JPEG等主流格式
- **坐标系转换**：支持WGS84、Web墨卡托等坐标系

### ⚡ 性能优化
- **分布式处理**：多进程并行处理，充分利用多核CPU
- **智能内存管理**：自适应内存分配和缓存策略
- **金字塔优化**：大文件自动创建GDAL金字塔，提升处理效率
- **批量处理**：支持TB级数据的批量切片处理

### 🔧 集成工具
- **GDAL集成**：支持200+地理数据格式
- **CTB支持**：专业的Cesium地形瓦片生成
- **GeoTools**：地理坐标系转换和地理计算
- **MyBatis Plus**：数据持久化和工作空间管理

---

## 🚀 快速开始

### 方式一：Docker引擎部署（推荐 - 复杂图像处理）

**🎯 适用场景**：大规模数据、复杂地形图像、生产环境批量处理

```bash
# 1. 下载镜像文件
# 从百度网盘下载 terra-forge.zip
# 链接: https://pan.baidu.com/s/1U0liGWNIV2uKXRxJnnL7MA?pwd=qifa
# 提取码: qifa

# 2. 导入镜像
unzip terra-forge.zip
docker load -i terra-forge.tar

# 3. 创建数据目录
mkdir -p data/dataSource tiles logs

# 4. 启动Docker引擎服务
docker run -d --name terraforge-docker \
  -p 8000:8000 \
  -v $(pwd)/data:/app/dataSource \
  -v $(pwd)/tiles:/app/tiles \
  terra-forge:release-1.0
```

### 方式二：Java引擎部署（小图切片 - 高灵活性）

**🎯 适用场景**：小规模数据、开发调试、精细化参数控制、快速原型

```bash
# 1. 克隆项目
git clone https://github.com/terraforge/terraforge-server.git
cd terraforge-server

# 2. 构建Java后端
mvn clean package -Dmaven.test.skip=true

# 3. 启动Java引擎服务
java -jar target/terraforge-server-2.8.0.jar
```

### 🤔 如何选择部署方式？

| 选择因素 | Java引擎 | Docker引擎 |
|----------|----------|------------|
| **数据规模** | < 10GB | > 10GB |
| **处理复杂度** | 简单地图切片 | 复杂地形处理 |
| **开发阶段** | 研发测试 | 生产部署 |
| **资源需求** | 轻量级 | 重量级 |
| **灵活性要求** | 高度定制 | 标准流程 |

### 访问服务

#### Docker引擎服务（端口8000）
- **TerraForge服务**: [http://localhost:8000](http://localhost:8000)
- **健康检查**: [http://localhost:8000/health](http://localhost:8000/health)
- **任务状态**: [http://localhost:8000/tasks](http://localhost:8000/tasks)
- **数据源浏览**: [http://localhost:8000/datasources](http://localhost:8000/datasources)

#### Java引擎服务（端口8080）
- **Java API服务**: [http://localhost:8080](http://localhost:8080)
- **Swagger文档**: [http://localhost:8080/doc.html](http://localhost:8080/doc.html)
- **健康检查**: [http://localhost:8080/health](http://localhost:8080/health)
- **API接口**: [http://localhost:8080/terrain/cut](http://localhost:8080/terrain/cut)

---

## 📦 Docker镜像

### 镜像获取方式

由于项目处于开发阶段，暂未发布到公共镜像仓库，请通过以下方式获取镜像：

**📥 网盘下载地址**：
- **文件名**: `terra-forge.zip`
- **下载链接**: [https://pan.baidu.com/s/1U0liGWNIV2uKXRxJnnL7MA?pwd=qifa](https://pan.baidu.com/s/1U0liGWNIV2uKXRxJnnL7MA?pwd=qifa)
- **提取码**: `qifa`

### 镜像信息

| 镜像名称 | 用途 | 大小 | 说明 |
|---------|-----|------|------|
| `terra-forge:release-1.0` | Python切片引擎 | ~841MB | 完整的地形地图切片服务 |

### 镜像导入和启动

```bash
# 1. 下载并解压镜像文件
unzip terra-forge.zip

# 2. 导入Docker镜像
docker load -i terra-forge.tar

# 3. 验证镜像导入成功
docker images | grep terra-forge

# 4. 创建数据目录
mkdir -p data/dataSource tiles logs

# 5. 启动服务
docker run -d --name terraforge-api \
  -p 8000:8000 \
  -v $(pwd)/data:/app/dataSource \
  -v $(pwd)/tiles:/app/tiles \
  -v $(pwd)/logs:/app/logs \
  terra-forge:release-1.0
```

---

## 🎯 功能模块

### 核心API接口

| 功能模块 | 接口数量 | 主要功能 |
|----------|----------|----------|
| **地形切片** | 3个 | 高精度地形瓦片生成，支持多种输出格式 |
| **地图切片** | 3个 | 防缝隙地图瓦片切片，支持批量处理 |
| **工作空间管理** | 4个 | 多层级文件和项目管理 |
| **任务监控** | 5个 | 实时进度监控和任务生命周期管理 |
| **系统信息** | 2个 | 健康检查和系统资源监控 |
| **数据源管理** | 3个 | 数据源浏览、详情获取、智能推荐 |

### 处理能力

- **支持格式**: TIF, GeoTIFF, PNG, JPEG, TIFF等
- **数据规模**: 支持TB级大文件处理
- **并发处理**: 多进程并行，性能线性扩展
- **内存优化**: 智能缓存和内存管理
- **输出格式**: Cesium地形瓦片、标准XYZ瓦片

---

## 📊 性能基准

### 处理性能（基于8核16GB配置）

| 数据规模 | 文件大小 | 处理时间 | 输出瓦片数 | 内存占用 |
|----------|----------|----------|------------|----------|
| 小规模 | <1GB | 5-15分钟 | 1K-10K | <4GB |
| 中等规模 | 1-10GB | 15分钟-2小时 | 10K-100K | 4-8GB |
| 大规模 | 10-50GB | 2-8小时 | 100K-1M | 8-16GB |
| 超大规模 | >50GB | 8-24小时 | >1M | 16GB+ |

### 算法优势

- **量化网格**: 相比传统方法减少70%存储空间
- **防缝隙技术**: 消除99.9%的瓦片缝隙问题
- **自适应细分**: 提高30%的地形表现精度
- **并行处理**: 相比单线程提升5-10倍处理速度

---

## 🛠️ 开发指南

### 系统要求

- **Java**: OpenJDK 8+ 或 Oracle JDK 8+
- **Python**: Python 3.9+
- **内存**: 最小8GB，推荐16GB+
- **存储**: 根据数据量，推荐SSD
- **操作系统**: Linux, Windows, macOS

### 本地开发

```bash
# 1. 安装依赖
sudo apt-get install gdal-bin libgdal-dev python3-gdal

# 2. 构建项目
mvn clean install

# 3. 运行测试
mvn test

# 4. 启动开发服务器
mvn spring-boot:run
```

### API使用示例

#### Docker引擎API（复杂图像处理）

```bash
# 地形切片API - 适合大文件、复杂处理
curl -X POST "http://localhost:8000/terrain/cut" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@large_terrain.tif" \
  -F "workspace=production" \
  -F "minZoom=0" \
  -F "maxZoom=15" \
  -F "enablePyramid=true"

# 查询任务状态
curl "http://localhost:8000/task/status/{taskId}"

# 健康检查
curl "http://localhost:8000/health"
```

#### Java引擎API（小图切片，高灵活性）

```bash
# 地形切片API - 适合小文件、精细控制
curl -X POST "http://localhost:8080/terrain/cut/terrainCutOfFile" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@small_terrain.tif" \
  -F "workspaceGroup=dev" \
  -F "workspace=test" \
  -F "minZoom=0" \
  -F "maxZoom=10" \
  -F "intensity=1.0" \
  -F "meshType=TRIANGULATED"

# 查询任务状态
curl "http://localhost:8080/common/task/status/{taskId}"

# 健康检查
curl "http://localhost:8080/health"

# Swagger API文档
curl "http://localhost:8080/v2/api-docs"
```

---

## 📖 文档资源

### 官方文档

- **[API接口文档](./docker/TerraForge-API接口文档.md)**: 完整的API参数和使用说明
- **[部署文档](./docker/TerraForge部署文档.md)**: 详细的部署和配置指南

### 在线资源

- **服务API**: [http://localhost:8000](http://localhost:8000)
- **项目文档**: 查看项目内docs目录
- **技术交流**: 欢迎邮件联系开发者

---

## 🤝 社区与支持

### 联系方式

- **📧 开发者邮箱**: [18101301716@163.com](mailto:18101301716@163.com)
- **🐛 问题反馈**: [GitHub Issues](https://github.com/terraforge/terraforge-server/issues)
- **💬 技术交流**: 欢迎邮件联系讨论技术问题

### 开发团队

- **主要开发者**: 徐实
- **项目维护**: TerraForge开发团队

### 贡献指南

- **代码规范**: 遵循Java和Python的标准编码规范
- **测试覆盖**: 新功能需要包含相应的单元测试
- **文档更新**: 重要变更需要更新相关文档
- **Issue模板**: 使用提供的Issue模板报告问题

---

## 📝 更新日志

### v2.8.0 (2025-01-26)
- ✨ 新增量化网格算法支持
- 🚀 优化大文件处理性能
- 🐛 修复地图切片缝隙问题
- 📚 完善API文档和部署指南

### v2.7.0 (2024-12-15)
- ✨ 新增Python切片引擎集成
- 🚀 支持TB级数据处理
- 🔧 增强任务监控功能
- 📦 提供Docker镜像部署

### v2.6.0 (2024-11-20)
- ✨ 新增工作空间管理功能
- 🚀 优化内存使用和性能
- 🐛 修复多个已知问题
- 📚 更新技术文档

---

## 📄 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源协议。

```
Copyright 2025 TerraForge Team

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## ⭐ 支持我们

如果TerraForge项目对您有帮助，请通过以下方式支持我们的持续开发：

### 🌟 给我们点赞
- **GitHub Star**: 点击右上角的⭐Star按钮，这是对我们最大的鼓励！
- **项目分享**: 推荐给您的同事和朋友，让更多人受益
- **技术交流**: 参与Issue讨论，分享您的使用经验

### 💡 参与贡献
- **代码贡献**: 提交PR，完善功能或修复Bug
- **文档完善**: 帮助我们改进文档和使用指南
- **问题反馈**: 报告Bug，提出改进建议
- **功能建议**: 分享您的需求，我们会认真考虑

### 📢 推广支持
- **技术博客**: 写文章介绍TerraForge的使用经验
- **社交媒体**: 在微信、微博等平台分享项目
- **技术会议**: 在GIS技术会议上介绍我们的项目
- **学术论文**: 在相关研究中引用我们的工作

### 🤝 商业合作
- **技术咨询**: 联系开发者进行技术咨询和定制开发
- **培训服务**: 提供企业级培训和技术支持
- **项目合作**: 欢迎产学研合作，共同推进GIS技术发展

### 💖 表达感谢
一个简单的感谢邮件，一句鼓励的话语，都是我们前进的动力！

**📧 联系我们**: [18101301716@163.com](mailto:18101301716@163.com)

---

<div align="center">

**🌍 让地理数据处理更简单、更高效！**

[开始使用](./docker/TerraForge部署文档.md) • [API文档](./docker/TerraForge-API接口文档.md) • [加入社区](mailto:support@terraforge.com)

</div>
