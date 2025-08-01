# TerraForge 管理界面

这是一个基于HTML、CSS和JavaScript的TerraForge地理数据处理系统管理界面，提供了友好的OA风格操作界面。

## 功能特性

### 🎛️ 仪表盘
- 系统状态监控
- 任务统计概览
- 实时健康检查

### 📂 数据源管理
- 浏览数据源目录
- 文件选择器
- 支持TIF格式文件

### 🗺️ 地图切片
- 索引瓦片切片（推荐）
- 支持多种缩放级别
- 智能配置推荐

### ⛰️ 地形切片
- CTB地形瓦片生成
- 可配置三角形数量
- 支持压缩和解压

### 📋 任务管理
- 实时任务状态监控
- 任务详情查看
- 任务清理功能

### 🔧 工具集合
- 瓦片格式转换
- 透明瓦片处理
- Layer.json更新

### 👁️ 瓦片预览
- 支持XYZ、TMS格式
- 瓦片URL生成
- 实时预览功能

## 使用方法

### 1. 启动应用
确保TerraForge Java服务运行在8083端口：
```bash
java -jar terraforge-server.jar
```

### 2. 访问界面
在浏览器中打开：
```
http://localhost:8083/index.html
```

### 3. 基本操作流程

#### 地图切片流程：
1. 点击左侧菜单"地图切片"
2. 点击"浏览"按钮选择TIF文件
3. 设置输出路径和缩放级别
4. 点击"开始切片"启动任务
5. 在"任务管理"中查看进度

#### 地形切片流程：
1. 点击左侧菜单"地形切片"
2. 选择输入文件和设置参数
3. 启动切片任务
4. 监控任务状态

#### 瓦片预览：
1. 点击左侧菜单"瓦片预览"
2. 输入工作空间组和工作空间名称
3. 点击"加载预览"查看瓦片

## 文件结构

```
src/main/resources/
├── index.html          # 主页面
├── css/
│   └── style.css      # 样式文件
├── js/
│   ├── api.js         # API调用封装
│   └── app.js         # 主要业务逻辑
└── README.md          # 使用说明
```

## 技术特点

- **响应式设计**: 支持桌面和移动设备
- **OA风格界面**: 简洁专业的办公系统风格
- **模块化架构**: API层和业务逻辑分离
- **实时监控**: 自动轮询任务状态
- **错误处理**: 完善的错误提示机制

## API支持

界面完全基于TerraForge Java API构建，支持所有核心功能：

- ✅ 健康检查
- ✅ 数据源浏览
- ✅ 地图/地形切片
- ✅ 任务管理
- ✅ 工作空间管理
- ✅ 分析工具
- ✅ 瓦片服务

## 浏览器兼容性

- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+

## 注意事项

1. 确保TerraForge后端服务正常运行
2. 文件路径使用相对路径（相对于数据源目录）
3. 大文件处理可能需要较长时间
4. 建议在切片前使用智能推荐功能

## 开发说明

如需自定义或扩展功能：

1. 修改 `js/api.js` 添加新的API调用
2. 在 `js/app.js` 中添加业务逻辑
3. 更新 `css/style.css` 调整界面样式
4. 在 `index.html` 中添加新的UI组件

## 许可证

此管理界面遵循与TerraForge主项目相同的许可证。 