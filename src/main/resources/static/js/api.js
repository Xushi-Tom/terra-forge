// API基础配置
const API_BASE_URL = window.location.origin;

// API调用封装
class TerraForgeAPI {
    constructor() {
        this.baseURL = API_BASE_URL;
    }

    // 通用请求方法
    async request(url, options = {}) {
        const fullUrl = url.startsWith('http') ? url : `${this.baseURL}${url}`;
        
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
            },
            ...options
        };

        try {
            const response = await fetch(fullUrl, defaultOptions);
            
            // 处理非JSON响应（如XML、图片等）
            const contentType = response.headers.get('content-type');
            if (contentType && !contentType.includes('application/json')) {
                if (contentType.includes('text/')) {
                    return await response.text();
                }
                return response;
            }

            // 处理JSON响应
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('API请求错误:', error);
            throw error;
        }
    }

    // GET请求
    async get(url, params = {}) {
        const urlObj = new URL(url.startsWith('http') ? url : `${this.baseURL}${url}`);
        Object.keys(params).forEach(key => {
            if (params[key] !== undefined && params[key] !== null) {
                urlObj.searchParams.append(key, params[key]);
            }
        });
        
        return this.request(urlObj.toString(), { method: 'GET' });
    }

    // POST请求
    async post(url, data = {}) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    // PUT请求
    async put(url, data = {}) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    }

    // DELETE请求
    async delete(url) {
        return this.request(url, { method: 'DELETE' });
    }

    // 文件上传
    async upload(url, formData) {
        return this.request(url, {
            method: 'POST',
            body: formData,
            headers: {} // 不设置Content-Type，让浏览器自动设置multipart/form-data
        });
    }

    // ==================== 通用功能接口 ====================
    
    // 获取系统信息
    async getSystemInfo() {
        return this.get('/api/system/info');
    }

    // 获取健康状态
    async getHealthStatus() {
        return this.get('/api/health');
    }

    // 浏览数据源
    async browseDatasources(path = '', bounds = '') {
        return this.get('/api/datasources', { path, bounds });
    }

    // 获取数据源详情
    async getDatasourceInfo(filename, tileType = '') {
        return this.get(`/api/datasources/info/${filename}`, { tileType });
    }

    // 获取文件详细信息
    async getFileInfo(filename) {
        return this.get(`/api/datasources/info/${filename}`);
    }

    // 浏览结果目录
    async browseResults(path = '') {
        console.log('调用浏览结果目录API, 路径:', path); // 添加日志
        const params = path ? { path } : {};
        const response = await this.get('/api/results', params);
        console.log('浏览结果目录API响应:', response); // 添加日志
        return response;
    }

    // ==================== 任务管理接口 ====================

    // 获取任务状态
    async getTaskStatus(taskId) {
        return this.get(`/api/tasks/${taskId}`);
    }

    // 获取所有任务
    async getAllTasks() {
        return this.get('/api/tasks');
    }

    // 停止任务
    async stopTask(taskId) {
        return this.post(`/api/tasks/${taskId}/stop`);
    }

    // 删除任务
    async deleteTask(taskId) {
        return this.delete(`/api/tasks/${taskId}`);
    }

    // 清理任务
    async cleanupTasks(params = {}) {
        return this.post('/api/tasks/cleanup', params);
    }

    // ==================== 地图切片接口 ====================

    // 创建索引瓦片（推荐）
    async createIndexedTiles(params) {
        return this.post('/map/cut/tile/indexedTiles', params);
    }

    // 地图切片（文件上传）
    async mapCutOfFile(formData) {
        return this.upload('/map/cut/mapCutOfFile', formData);
    }

    // 地图切片（路径）
    async mapCutOfPath(params) {
        return this.post('/map/cut/mapCutOfPath', params);
    }

    // 瓦片格式转换
    async convertTileFormat(params) {
        return this.post('/map/cut/tile/convert?' + new URLSearchParams(params));
    }

    // 扫描透明瓦片
    async scanNodataTiles(params) {
        return this.post('/map/cut/tiles/nodata/scan?' + new URLSearchParams(params));
    }

    // 删除透明瓦片
    async deleteNodataTiles(params) {
        return this.post('/map/cut/tiles/nodata/delete?' + new URLSearchParams(params));
    }

    // ==================== 地形切片接口 ====================

    // 创建地形瓦片（推荐）
    async createTerrainTiles(params) {
        return this.post('/terrain/cut/tile/terrain', params);
    }

    // 地形切片（文件上传）
    async terrainCutOfFile(formData) {
        return this.upload('/terrain/cut/terrainCutOfFile', formData);
    }

    // 地形切片（路径）
    async terrainCutOfPath(params) {
        return this.post('/terrain/cut/terrainCutOfPath', params);
    }

    // ==================== 工作空间管理接口 ====================

    // 创建文件夹
    async createWorkspaceFolder(folderPath) {
        return this.post('/api/workspace/createFolder', { folderPath });
    }

    // 删除文件夹
    async deleteWorkspaceFolder(folderPath) {
        console.log('准备删除文件夹:', folderPath);
        const url = `/api/workspace/folder/${folderPath}`;
        console.log('删除文件夹请求URL:', url);
        const response = await this.delete(url);
        console.log('删除文件夹响应:', response);
        return response;
    }

    // 重命名文件夹
    async renameWorkspaceFolder(folderPath, newName) {
        return this.put(`/api/workspace/folder/${folderPath}/rename`, { newName });
    }

    // 删除文件
    async deleteWorkspaceFile(filePath) {
        console.log('准备删除文件:', filePath);
        const url = `/api/workspace/file/${filePath}`;
        console.log('删除文件请求URL:', url);
        const response = await this.delete(url);
        console.log('删除文件响应:', response);
        return response;
    }

    // 重命名文件
    async renameWorkspaceFile(filePath, newName) {
        return this.put(`/api/workspace/file/${filePath}/rename`, { newName });
    }

    // 移动项目
    async moveWorkspaceItem(sourcePath, targetPath) {
        return this.put('/api/workspace/move', { sourcePath, targetPath });
    }

    // ==================== 分析工具接口 ====================

    // 推荐配置
    async recommendConfig(params) {
        return this.post('/api/config/recommend', params);
    }

    // 获取API路由列表
    async getRoutes() {
        return this.get('/api/routes');
    }

    // 容器更新
    async updateContainer(params) {
        return this.post('/api/container/update', params);
    }

    // ==================== 地形工具接口 ====================

    // 更新Layer.json
    async updateLayerJson(params) {
        return this.post('/api/terrain/layer', params);
    }

    // 解压地形文件
    async decompressTerrain(params) {
        return this.post('/api/terrain/decompress', params);
    }

    // ==================== 图层服务接口 ====================

    // 获取瓦片图（XYZ格式）
    getTileUrl(workspaceGroup, workspace, z, x, y, format = 'xyz') {
        if (format === 'xyz') {
            return `${this.baseURL}/map/xyz/${workspaceGroup}/${workspace}/${z}/${x}/${y}.png`;
        } else if (format === 'tms') {
            return `${this.baseURL}/map/${workspaceGroup}/${workspace}/${z}/${x}/${y}.png`;
        } else {
            return `${this.baseURL}/map/${workspaceGroup}/${workspace}/${z}/${x}_${y}.png`;
        }
    }

    // 获取地形瓦片
    getTerrainTileUrl(workspaceGroup, workspace, z, x, y) {
        return `${this.baseURL}/terrain/${workspaceGroup}/${workspace}/${z}/${x}/${y}.terrain`;
    }

    // 获取Layer.json
    getLayerJsonUrl(workspaceGroup, workspace) {
        return `${this.baseURL}/terrain/${workspaceGroup}/${workspace}/layer.json`;
    }

    // 获取WMTS服务能力
    async getWmtsCapabilities() {
        return this.get('/map/wmts/capabilities');
    }

    // 获取WMTS瓦片
    getWmtsTileUrl(workspaceGroup, workspace, tileMatrix, tileRow, tileCol, format = 'png') {
        return `${this.baseURL}/map/wmts/${workspaceGroup}/${workspace}/${tileMatrix}/${tileRow}/${tileCol}.${format}`;
    }
}

// 创建全局API实例
window.terraForgeAPI = new TerraForgeAPI();

// 导出API类（如果使用模块化）
if (typeof module !== 'undefined' && module.exports) {
    module.exports = TerraForgeAPI;
} 