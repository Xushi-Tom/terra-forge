// 全局变量
let currentDatasourcePath = '';
let filePickerCallback = null;
let taskPollingIntervals = new Map();
let autoRefreshInterval = null;
let autoRefreshEnabled = false;
// 全局变量：当前工作空间路径
let currentWorkspacePath = '';

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

// 应用初始化
async function initializeApp() {
    try {
        initializeEvents();
        addRefreshIndicatorStyles(); // 添加刷新指示器样式
        await loadDashboard();
        await updateSystemInfo(); // 初始化系统信息
        startPeriodicTasks();
        startDateTimeUpdate();
        console.log('TerraForge管理系统初始化完成');
    } catch (error) {
        console.error('应用初始化失败:', error);
        showMessage('系统初始化失败，请刷新页面重试', 'error');
    }
}

// 全局错误处理
window.addEventListener('error', function(event) {
    console.error('全局错误:', event.error);
    console.error('错误位置:', event.filename, '行号:', event.lineno);
});

window.addEventListener('unhandledrejection', function(event) {
    console.error('未处理的Promise拒绝:', event.reason);
    console.error('Promise:', event.promise);
});

// 检查系统状态
async function checkSystemStatus() {
    try {
        const health = await terraForgeAPI.getHealth();
        const statusElement = document.getElementById('systemStatus');
        if (statusElement) {
            const indicator = statusElement.querySelector('.status-indicator');
            if (health.status === 'healthy') {
                indicator.style.color = '#4CAF50';
                statusElement.querySelector('span').textContent = '系统正常';
            } else {
                indicator.style.color = '#f44336';
                statusElement.querySelector('span').textContent = '系统异常';
            }
        }
    } catch (error) {
        console.error('系统状态检查失败:', error);
    }
}

// 初始化事件监听
function initializeEvents() {
    // 菜单切换事件
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', function() {
            const section = this.dataset.section;
            switchSection(section);
        });
    });

    // 表单提交事件
    initializeFormEvents();
}

// 切换页面section
function switchSection(sectionName) {
    // 更新菜单状态
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });
    document.querySelector(`[data-section="${sectionName}"]`).classList.add('active');

    // 切换内容区域
    document.querySelectorAll('.content-section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(sectionName).classList.add('active');

    // 加载对应数据
    loadSectionData(sectionName);
}

// 加载section数据
async function loadSectionData(sectionName) {
    switch (sectionName) {
        case 'dashboard':
            await loadDashboard();
            break;
        case 'datasource':
            await loadDatasources();
            break;
        case 'workspace':
            await loadWorkspace();
            break;
        case 'tasks':
            await loadTasks();
            break;
        case 'system':
            await loadSystemManagement();
            break;
    }
}

// 加载仪表盘数据
async function loadDashboard() {
    try {
        const tasks = await terraForgeAPI.getAllTasks();
        updateTaskOverviewCard(tasks);
    } catch (error) {
        console.error('仪表盘数据加载失败:', error);
    }
}


// 更新任务概览卡片
function updateTaskOverviewCard(data) {
    const container = document.getElementById('taskOverview');
    if (!data) {
        container.innerHTML = '<div class="message error">任务信息加载失败</div>';
        return;
    }
    
    const tasks = data.tasks || {};
    const taskList = Object.values(tasks);
    const completedTasks = taskList.filter(task => task.status === 'completed').length;
    const runningTasks = taskList.filter(task => task.status === 'running').length;
    const failedTasks = taskList.filter(task => task.status === 'failed').length;
    
    container.innerHTML = `
        <div class="task-stats">
            <div class="stat-item">
                <div class="stat-number">${taskList.length}</div>
                <div class="stat-label">总任务数</div>
            </div>
            <div class="stat-item">
                <div class="stat-number" style="color: #28a745">${completedTasks}</div>
                <div class="stat-label">已完成</div>
            </div>
            <div class="stat-item">
                <div class="stat-number" style="color: #ffc107">${runningTasks}</div>
                <div class="stat-label">运行中</div>
            </div>
            <div class="stat-item">
                <div class="stat-number" style="color: #dc3545">${failedTasks}</div>
                <div class="stat-label">失败</div>
            </div>
        </div>
    `;
}

// 加载数据源列表
async function loadDatasources(path = '') {
    try {
        showLoading('datasourceList');
        const data = await terraForgeAPI.browseDatasources(path);
        console.log('数据源API返回数据:', data);
        updateDatasourceList(data);
        updateDatasourceBreadcrumb(path);
        currentDatasourcePath = path;
    } catch (error) {
        console.error('数据源加载失败:', error);
        showError('datasourceList', '数据源加载失败');
    }
}

// 更新数据源列表
function updateDatasourceList(data) {
    const container = document.getElementById('datasourceList');
    if (!data || (!data.directories && !data.datasources)) {
        container.innerHTML = '<div class="message info">没有找到数据源</div>';
        return;
    }
    
    let html = '';
    
    // 添加目录
    if (data.directories) {
        data.directories.forEach(dir => {
            html += `
                <div class="file-item" onclick="navigateDatasource('${dir.path}')">
                    <i class="file-icon fas fa-folder"></i>
                    <div class="file-info">
                        <div class="file-name">${dir.name}</div>
                        <div class="file-details">目录</div>
                    </div>
                </div>
            `;
        });
    }
    
    // 添加文件
    if (data.datasources) {
        data.datasources.forEach(file => {
            html += `
                <div class="file-item" onclick="showFileDetails('${file.path}', '${file.name}')">
                    <i class="file-icon fas fa-file"></i>
                    <div class="file-info">
                        <div class="file-name">${file.name}</div>
                        <div class="file-details">${file.sizeFormatted || file.size || file.format}</div>
                    </div>
                </div>
            `;
        });
    }
    
    container.innerHTML = html;
}

// 更新数据源导航
function updateDatasourceBreadcrumb(path) {
    const container = document.getElementById('datasourceBreadcrumb');
    if (!path) {
        container.innerHTML = '<span class="breadcrumb-item active">根目录</span>';
        return;
    }
    
    const parts = path.split('/').filter(p => p);
    let html = '<span class="breadcrumb-item" onclick="loadDatasources(\'\')">根目录</span>';
    
    let currentPath = '';
    parts.forEach((part, index) => {
        currentPath += part;
        if (index === parts.length - 1) {
            html += ` / <span class="breadcrumb-item active">${part}</span>`;
        } else {
            html += ` / <span class="breadcrumb-item" onclick="loadDatasources('${currentPath}')">${part}</span>`;
        }
        currentPath += '/';
    });
    
    container.innerHTML = html;
}

// 导航到数据源目录
function navigateDatasource(path) {
    loadDatasources(path);
}

// 选择数据源文件
function selectDatasource(path) {
    if (filePickerCallback) {
        filePickerCallback(path);
        closeModal();
    } else {
        showMessage(`已选择文件: ${path}`, 'success');
    }
}

// 加载工作空间
async function loadWorkspace() {
    try {
        showLoading('workspaceList');
        await loadWorkspaceFiles();
    } catch (error) {
        console.error('工作空间加载失败:', error);
        showError('workspaceList', '工作空间加载失败');
    }
}

// 进入/刷新目录时，唯一入口
async function loadWorkspaceFiles(path) {
    if (typeof path === 'string') {
        currentWorkspacePath = path;
    }
    console.log('当前 currentWorkspacePath:', currentWorkspacePath);
    try {
        const data = await terraForgeAPI.browseResults(currentWorkspacePath);
        updateWorkspaceFileList(data, currentWorkspacePath);
        updateWorkspaceBreadcrumb(currentWorkspacePath); // 每次刷新都更新面包屑导航
    } catch (error) {
        showError('workspaceList', '加载工作空间文件列表失败');
    }
}

// 更新工作空间导航
function updateWorkspaceBreadcrumb(path) {
    console.log('更新工作空间导航, 路径:', path);
    const container = document.getElementById('workspaceBreadcrumb');
    if (!path) {
        container.innerHTML = '<span class="breadcrumb-item active">根目录</span>';
        return;
    }
    
    const parts = path.split('/').filter(p => p);
    let html = '<span class="breadcrumb-item" onclick="loadWorkspaceFiles(\'\')">根目录</span>';
    
    let currentPath = '';
    parts.forEach((part, index) => {
        currentPath += part;
        if (index === parts.length - 1) {
            html += ` / <span class="breadcrumb-item active">${part}</span>`;
        } else {
            html += ` / <span class="breadcrumb-item" onclick="loadWorkspaceFiles('${currentPath}')">${part}</span>`;
        }
        currentPath += '/';
    });
    
    container.innerHTML = html;
}

// 渲染文件列表，目录点击事件必须是 loadWorkspaceFiles('${fullPath}')
function updateWorkspaceFileList(data, currentPath) {
    const container = document.getElementById('workspaceList');
    if (!data || (!data.directories && !data.files)) {
        container.innerHTML = '<div class="message info">目录为空</div>';
        return;
    }
    let html = '';
    // 添加目录
    if (data.directories) {
        data.directories.forEach(dir => {
            const fullPath = currentPath ? `${currentPath}/${dir.name}` : dir.name;
            html += `
                <div class="file-item" onclick="loadWorkspaceFiles('${fullPath}')">
                    <i class="file-icon fas fa-folder"></i>
                    <div class="file-info">
                        <div class="file-name">${dir.name}</div>
                        <div class="file-details">目录</div>
                    </div>
                    <div class="file-actions">
                        <button class="btn btn-sm btn-danger" onclick="event.stopPropagation(); deleteWorkspaceItem('${fullPath}', 'folder')">
                            <i class="fas fa-trash"></i> 删除
                        </button>
                    </div>
                </div>
            `;
        });
    }
    // 添加文件
    if (data.files) {
        data.files.forEach(file => {
            const fullPath = currentPath ? `${currentPath}/${file.name}` : file.name;
            const modifiedTime = file.modifiedTime ? new Date(file.modifiedTime * 1000).toLocaleString() : '-';
            html += `
                <div class="file-item">
                    <i class="file-icon fas fa-file"></i>
                    <div class="file-info">
                        <div class="file-name">${file.name}</div>
                        <div class="file-details">${file.sizeFormatted || file.size || '-'} | ${modifiedTime}</div>
                    </div>
                    <div class="file-actions">
                        <button class="btn btn-sm btn-danger" onclick="deleteWorkspaceItem('${fullPath}', 'file')">
                            <i class="fas fa-trash"></i> 删除
                        </button>
                    </div>
                </div>
            `;
        });
    }
    container.innerHTML = `<div class="file-browser"><div class="file-list">${html}</div></div>`;
}

// 加载任务列表
async function loadTasks() {
    try {
        showLoading('taskList');
        const data = await terraForgeAPI.getAllTasks();
        updateTaskList(data);
    } catch (error) {
        console.error('任务列表加载失败:', error);
        showError('taskList', '任务列表加载失败');
    }
}

// 更新任务列表
function updateTaskList(data) {
    const container = document.getElementById('taskList');
    if (!data || !data.tasks) {
        container.innerHTML = '<div class="message info">暂无任务</div>';
        return;
    }
    
    const tasks = Object.values(data.tasks);
    if (tasks.length === 0) {
        container.innerHTML = '<div class="message info">暂无任务</div>';
        return;
    }
    
    // 按任务ID（时间戳）降序排序，最新的任务在上面
    tasks.sort((a, b) => {
        // 提取任务ID中的时间戳部分进行比较
        const getTimestamp = (taskId) => {
            const match = taskId.match(/\d+$/);
            return match ? parseInt(match[0]) : 0;
        };
        return getTimestamp(b.taskId) - getTimestamp(a.taskId);
    });
    
    const html = tasks.map(task => `
        <div class="task-item">
            <div class="task-status ${task.status}"></div>
            <div class="task-info">
                <div class="task-id">${task.taskId}</div>
                <div class="task-message">${task.message || '无消息'}</div>
            </div>
            <div class="task-progress">
                <div class="task-progress-bar" style="width: ${task.progress || 0}%"></div>
            </div>
            <div class="task-actions">
                <button class="btn btn-secondary" onclick="viewTaskDetails('${task.taskId}')" title="查看详情">
                    查看
                </button>
                ${task.status === 'running' ? 
                    `<button class="btn btn-warning" onclick="stopTask('${task.taskId}')" title="停止任务">
                        停止
                    </button>` : 
                    `<button class="btn btn-danger" onclick="deleteTask('${task.taskId}')" title="删除任务">
                        删除
                    </button>`
                }
            </div>
        </div>
    `).join('');
    
    container.innerHTML = html;
}

// 查看任务详情
async function viewTaskDetails(taskId) {
    try {
        const task = await terraForgeAPI.getTaskStatus(taskId);
        console.log('任务详情数据:', task); // 调试用
        
        // 处理消息换行
        let displayMessage = task.message || '无消息';
        displayMessage = displayMessage.replace(/,\s*/g, ',\n').replace(/。\s*/g, '。\n');
        
        // 格式化时间显示
        const formatTime = (timeStr) => {
            if (!timeStr) return '未知';
            try {
                return new Date(timeStr).toLocaleString('zh-CN');
            } catch (e) {
                return timeStr;
            }
        };
        
        // 构建详细信息HTML
        let detailsHtml = `
            <div style="padding: 20px;">
                <div style="margin-bottom: 15px;">
                    <strong>任务ID:</strong> ${task.taskId || taskId}
                </div>
                <div style="margin-bottom: 15px;">
                    <strong>状态:</strong> <span style="color: ${getStatusColor(task.status)}">${getStatusText(task.status)}</span>
                </div>
                <div style="margin-bottom: 15px;">
                    <strong>进度:</strong> ${task.progress || 0}%
                </div>
                <div style="margin-bottom: 15px;">
                    <strong>当前阶段:</strong> ${task.currentStage || '未知'}
                </div>
                <div style="margin-bottom: 15px;">
                    <strong>开始时间:</strong> ${formatTime(task.startTime)}
                </div>`;
        
        if (task.endTime) {
            detailsHtml += `
                <div style="margin-bottom: 15px;">
                    <strong>结束时间:</strong> ${formatTime(task.endTime)}
                </div>`;
        }
        
        detailsHtml += `
                <div style="margin-bottom: 15px;">
                    <strong>消息:</strong>
                    <div style="
                        margin-top: 8px;
                        padding: 15px;
                        background: #f8f9fa;
                        border: 1px solid #dee2e6;
                        border-radius: 6px;
                        white-space: pre-line;
                        line-height: 1.6;
                        font-size: 14px;
                        max-height: 200px;
                        overflow-y: auto;
                    ">${displayMessage}</div>
                </div>`;
        
        // 显示结果信息（如果任务完成）
        if (task.result && task.status === 'completed') {
            detailsHtml += `
                <div style="margin-bottom: 15px;">
                    <strong>任务结果:</strong>
                    <div style="
                        margin-top: 8px;
                        padding: 15px;
                        background: #e8f5e8;
                        border: 1px solid #c3e6c3;
                        border-radius: 6px;
                        font-size: 13px;
                    ">`;
            
            if (task.result.totalFiles) {
                detailsHtml += `<div>📁 总文件数: ${task.result.totalFiles}</div>`;
            }
            if (task.result.completedFiles !== undefined) {
                detailsHtml += `<div>✅ 成功处理: ${task.result.completedFiles}</div>`;
            }
            if (task.result.failedFiles !== undefined) {
                detailsHtml += `<div>❌ 失败文件: ${task.result.failedFiles}</div>`;
            }
            if (task.result.totalTerrainFiles) {
                detailsHtml += `<div>🗺️ 生成瓦片: ${task.result.totalTerrainFiles}</div>`;
            }
            if (task.result.outputPath) {
                detailsHtml += `<div>📂 输出路径: ${task.result.outputPath}</div>`;
            }
            
            detailsHtml += `</div></div>`;
        }
        
        // 显示处理日志（如果有）- 放在最下面
        if (task.processLog && task.processLog.length > 0) {
            detailsHtml += `
                <div style="margin-bottom: 15px;">
                    <strong>处理日志:</strong>
                    <div style="
                        margin-top: 8px;
                        padding: 15px;
                        background: #f1f3f4;
                        border: 1px solid #dee2e6;
                        border-radius: 6px;
                        max-height: 400px;
                        overflow-y: auto;
                        font-size: 13px;
                    ">`;
            
            // 显示最后20条日志
            const recentLogs = task.processLog.slice(-20);
            recentLogs.forEach(log => {
                const logTime = formatTime(log.timestamp);
                const logStatus = log.status === 'completed' ? '✅' : log.status === 'failed' ? '❌' : '🔄';
                detailsHtml += `
                    <div style="margin-bottom: 8px; padding: 6px; background: white; border-radius: 4px;">
                        <div style="font-weight: bold; color: #333;">
                            ${logStatus} ${log.stage || '处理中'} - ${log.progress || 0}%
                        </div>
                        <div style="color: #666; font-size: 12px; margin-top: 2px;">
                            ${log.message || ''}
                        </div>
                        <div style="color: #999; font-size: 11px; margin-top: 2px;">
                            ${logTime}
                        </div>
                    </div>`;
            });
            
            if (task.processLog.length > 20) {
                detailsHtml += `<div style="text-align: center; color: #666; font-size: 12px; margin-top: 10px;">
                    显示最后20条日志，共${task.processLog.length}条
                </div>`;
            }
            
            detailsHtml += `</div></div>`;
        }
        
        detailsHtml += `</div>`;
        
        showModal('任务详情', detailsHtml);
    } catch (error) {
        console.error('获取任务详情失败:', error);
        showMessage('获取任务详情失败', 'error');
    }
}

// 辅助函数：获取状态颜色
function getStatusColor(status) {
    switch (status) {
        case 'completed': return '#28a745';
        case 'running': return '#ffc107';
        case 'failed': return '#dc3545';
        case 'stopped': return '#6c757d';
        default: return '#333';
    }
}

// 辅助函数：获取状态文本
function getStatusText(status) {
    switch (status) {
        case 'completed': return '已完成';
        case 'running': return '运行中';
        case 'failed': return '失败';
        case 'stopped': return '已停止';
        default: return status || '未知';
    }
}

// 停止任务
async function stopTask(taskId) {
    if (!confirm('确定要停止这个任务吗？')) {
        return;
    }
    
    try {
        await terraForgeAPI.stopTask(taskId);
        showMessage('任务停止指令已发送', 'success');
        loadTasks(); // 重新加载任务列表
    } catch (error) {
        console.error('停止任务失败:', error);
        showMessage('停止任务失败', 'error');
    }
}

// 删除任务
async function deleteTask(taskId) {
    if (!confirm('确定要删除这个任务吗？删除后将无法恢复任务信息。')) {
        return;
    }
    
    try {
        await terraForgeAPI.deleteTask(taskId);
        showMessage('任务已删除', 'success');
        loadTasks(); // 重新加载任务列表
    } catch (error) {
        console.error('删除任务失败:', error);
        showMessage('删除任务失败', 'error');
    }
}

// 文件选择器
function selectFile(inputId) {
    filePickerCallback = function(path) {
        document.getElementById(inputId).value = path;
    };
    
    showModal('选择文件', `
        <div id="filePickerContent">
            <div class="file-browser">
                <div class="breadcrumb" id="pickerBreadcrumb">
                    <span class="breadcrumb-item active">根目录</span>
                </div>
                <div class="file-list" id="pickerFileList">
                    <div class="loading">加载中...</div>
                </div>
            </div>
        </div>
    `);
    
    loadPickerDatasources('');
}

// 在文件选择器中加载数据源
async function loadPickerDatasources(path = '') {
    try {
        const data = await terraForgeAPI.browseDatasources(path);
        updatePickerFileList(data, path);
    } catch (error) {
        console.error('文件选择器数据加载失败:', error);
        document.getElementById('pickerFileList').innerHTML = '<div class="message error">加载失败</div>';
    }
}

// 更新文件选择器文件列表
function updatePickerFileList(data, currentPath) {
    const container = document.getElementById('pickerFileList');
    if (!data) {
        container.innerHTML = '<div class="message error">加载失败</div>';
        return;
    }
    
    let html = '';
    
    // 返回上级目录
    if (currentPath) {
        const parentPath = currentPath.split('/').slice(0, -1).join('/');
        html += `
            <div class="file-item" onclick="loadPickerDatasources('${parentPath}')">
                <i class="file-icon fas fa-arrow-left"></i>
                <div class="file-info">
                    <div class="file-name">返回上级</div>
                </div>
            </div>
        `;
    }
    
    // 添加目录
    if (data.directories) {
        data.directories.forEach(dir => {
            const fullPath = currentPath ? `${currentPath}/${dir.name}` : dir.name;
            html += `
                <div class="file-item" onclick="loadPickerDatasources('${fullPath}')">
                    <i class="file-icon fas fa-folder"></i>
                    <div class="file-info">
                        <div class="file-name">${dir.name}</div>
                    </div>
                </div>
            `;
        });
    }
    
    // 添加文件
    if (data.datasources) {
        data.datasources.forEach(file => {
            const fullPath = currentPath ? `${currentPath}/${file.name}` : file.name;
            html += `
                <div class="file-item" onclick="selectDatasource('${fullPath}')">
                    <i class="file-icon fas fa-file"></i>
                    <div class="file-info">
                        <div class="file-name">${file.name}</div>
                    </div>
                    <div class="file-actions">
                        <button class="btn btn-primary" onclick="selectDatasource('${fullPath}')">选择</button>
                    </div>
                </div>
            `;
        });
    }
    
    container.innerHTML = html || '<div class="message info">目录为空</div>';
}

// 表单事件初始化
function initializeFormEvents() {
    // 监听文件选择状态，控制智能推荐按钮
    initializeRecommendButtons();
    
    // 地图切片表单
    const mapForm = document.getElementById('mapTileForm');
    if (mapForm) {
        mapForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const folderPaths = document.getElementById('mapFolderPaths').value;
            const filePatterns = document.getElementById('mapFilePatterns').value;
            const outputPath = document.getElementById('mapOutputPath').value;
            
            if (!folderPaths || !outputPath) {
                showMessage('请填写所有必填字段', 'warning');
                return;
            }
            
            try {
                const params = {
                    folderPaths: folderPaths.split(',').map(p => p.trim()),
                    filePatterns: filePatterns.split(',').map(p => p.trim()),
                    outputPath: outputPath, // 改为字符串，后端处理分割
                    minZoom: parseInt(document.getElementById('mapMinZoom').value),
                    maxZoom: parseInt(document.getElementById('mapMaxZoom').value),
                    tileSize: parseInt(document.getElementById('mapTileSize').value),
                    processes: parseInt(document.getElementById('mapProcesses').value),
                    maxMemory: document.getElementById('mapMaxMemory').value,
                    resampling: document.getElementById('mapResampling').value,
                    generateShpIndex: document.getElementById('mapGenerateShpIndex').checked,
                    enableIncrementalUpdate: document.getElementById('mapEnableIncrementalUpdate').checked,
                    skipNodataTiles: document.getElementById('mapSkipNodataTiles').checked
                };
                
                const result = await terraForgeAPI.createIndexedTiles(params);
                showMessage(`地图切片任务已启动: ${result.taskId}`, 'success');
                switchSection('tasks');
            } catch (error) {
                console.error('地图切片失败:', error);
                showMessage('地图切片失败', 'error');
            }
        });
    }
    
    // 地形切片表单
    const terrainForm = document.getElementById('terrainTileForm');
    if (terrainForm) {
        terrainForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const folderPaths = document.getElementById('terrainFolderPaths').value;
            const filePatterns = document.getElementById('terrainFilePatterns').value;
            const outputPath = document.getElementById('terrainOutputPath').value;
            
            if (!outputPath) {
                showMessage('请填写输出路径', 'warning');
                return;
            }
            
            try {
                // 处理地理边界
                const boundsStr = document.getElementById('terrainBounds').value;
                let bounds = null;
                if (boundsStr && boundsStr.trim()) {
                    bounds = boundsStr.split(',').map(b => parseFloat(b.trim()));
                    if (bounds.length !== 4) {
                        showMessage('地理边界格式错误，应为：west,south,east,north', 'warning');
                        return;
                    }
                }
                
                const params = {
                    folderPaths: folderPaths ? folderPaths.split(',').map(p => p.trim()) : [""],
                    filePatterns: filePatterns ? filePatterns.split(',').map(p => p.trim()) : ["*.tif"],
                    outputPath: outputPath, // 改为字符串，后端处理分割
                    startZoom: parseInt(document.getElementById('terrainStartZoom').value),
                    endZoom: parseInt(document.getElementById('terrainEndZoom').value),
                    maxTriangles: parseInt(document.getElementById('terrainMaxTriangles').value),
                    bounds: bounds,
                    compression: document.getElementById('terrainCompression').checked,
                    decompress: document.getElementById('terrainDecompress').checked,
                    threads: parseInt(document.getElementById('terrainThreads').value),
                    maxMemory: document.getElementById('terrainMaxMemory').value,
                    autoZoom: document.getElementById('terrainAutoZoom').checked,
                    zoomStrategy: document.getElementById('terrainZoomStrategy').value,
                    mergeTerrains: document.getElementById('terrainMerge').checked
                };
                
                const result = await terraForgeAPI.createTerrainTiles(params);
                showMessage(`地形切片任务已启动: ${result.taskId}`, 'success');
                switchSection('tasks');
            } catch (error) {
                console.error('地形切片失败:', error);
                showMessage('地形切片失败', 'error');
            }
        });
    }
}

// 初始化智能推荐按钮状态
function initializeRecommendButtons() {
    // 监听地图切片文件匹配模式变化
    const mapFilePatterns = document.getElementById('mapFilePatterns');
    const mapRecommendBtn = document.getElementById('mapRecommendBtn');
    
    if (mapFilePatterns && mapRecommendBtn) {
        // 初始状态检查
        updateRecommendButtonState('map');
        
        // 监听输入变化
        mapFilePatterns.addEventListener('input', () => updateRecommendButtonState('map'));
        mapFilePatterns.addEventListener('change', () => updateRecommendButtonState('map'));
    }
    
    // 监听地形切片文件匹配模式变化
    const terrainFilePatterns = document.getElementById('terrainFilePatterns');
    const terrainRecommendBtn = document.getElementById('terrainRecommendBtn');
    
    if (terrainFilePatterns && terrainRecommendBtn) {
        // 初始状态检查
        updateRecommendButtonState('terrain');
        
        // 监听输入变化
        terrainFilePatterns.addEventListener('input', () => updateRecommendButtonState('terrain'));
        terrainFilePatterns.addEventListener('change', () => updateRecommendButtonState('terrain'));
    }
}

// 更新智能推荐按钮状态
function updateRecommendButtonState(type) {
    const inputField = type === 'map' ? 'mapFilePatterns' : 'terrainFilePatterns';
    const buttonId = type === 'map' ? 'mapRecommendBtn' : 'terrainRecommendBtn';
    
    const inputElement = document.getElementById(inputField);
    const buttonElement = document.getElementById(buttonId);
    
    if (inputElement && buttonElement) {
        const inputValue = inputElement.value.trim();
        
        // 按钮始终可点击，只是在点击时进行检查
        buttonElement.disabled = false;
        buttonElement.title = '点击获取智能推荐配置';
    }
}

// 智能推荐配置
async function getRecommendation(type) {
    const inputField = type === 'map' ? 'mapFilePatterns' : 'terrainFilePatterns';
    const filePatterns = document.getElementById(inputField).value.trim();
    
    if (!filePatterns) {
        showMessage('请先选择文件', 'warning');
        return;
    }
    
    // 检查是否包含通配符
    if (filePatterns.includes('*') || filePatterns.includes('?')) {
        showMessage('智能推荐不支持通配符，请选择具体的tif文件', 'warning');
        return;
    }
    
    // 检查是否包含txt文件（txt文件不支持智能推荐）
    if (filePatterns.includes('.txt')) {
        showMessage('智能推荐不支持txt文件，请只选择tif文件', 'warning');
        return;
    }
    
    // 检查是否包含具体的tif文件
    const hasSpecificTifFiles = filePatterns.includes('.tif') || filePatterns.includes('.tiff');
    
    if (!hasSpecificTifFiles) {
        showMessage('智能推荐只支持具体的tif文件', 'warning');
        return;
    }
    
    // 检查是否只有一个tif文件
    const files = filePatterns.split(',').map(f => f.trim()).filter(f => f.length > 0);
    const tifFiles = files.filter(f => f.endsWith('.tif') || f.endsWith('.tiff'));
    
    if (files.length > 1) {
        showMessage('智能推荐只支持单个tif文件，请只选择一个文件', 'warning');
        return;
    }
    
    if (tifFiles.length !== 1) {
        showMessage('智能推荐只支持单个tif文件', 'warning');
        return;
    }
    
    // 调用智能推荐接口
    try {
        const tifFile = tifFiles[0]; // 已经验证过只有一个tif文件
        showMessage('正在分析文件，请稍候...', 'info');
        
        // 调用配置推荐接口
        const configResponse = await terraForgeAPI.recommendConfig({ 
            sourceFile: tifFile, 
            tileType: type === 'map' ? 'map' : 'terrain' 
        });
        
        if (configResponse && configResponse.success && configResponse.recommendations) {
            showRecommendationModal(type, configResponse, tifFile);
        } else {
            showMessage('未能获取推荐配置，请手动设置参数', 'warning');
        }
    } catch (error) {
        console.error('智能推荐失败:', error);
        showMessage('智能推荐失败: ' + error.message, 'error');
    }
}

// 刷新仪表盘
async function refreshDashboard() {
    await loadDashboard();
}

// 刷新数据源
function refreshDatasource() {
    loadDatasources(currentDatasourcePath);
}

// 刷新任务列表
function refreshTasks() {
    loadTasks();
}

// 清理任务
async function cleanupTasks() {
    if (!confirm('确定要清理已完成的任务吗？')) return;
    
    try {
        await terraForgeAPI.cleanupTasks();
        showMessage('任务清理完成', 'success');
        loadTasks();
    } catch (error) {
        console.error('清理任务失败:', error);
        showMessage('清理任务失败', 'error');
    }
}

// 显示智能推荐结果弹框
function showRecommendationModal(type, configData, filename) {
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.style.display = 'block';
    
    const typeText = type === 'map' ? '地图切片' : '地形切片';
    
    const recommendations = configData.recommendations;
    const systemInfo = configData.systemInfo;
    const fileSize = configData.fileSize;
    
    let recommendationHtml = `
        <div class="modal-content" style="max-width: 600px; max-height: 80vh; overflow-y: auto;">
            <div class="modal-header">
                <h3>智能推荐配置 - ${typeText}</h3>
                <span class="close" onclick="this.closest('.modal').remove()">&times;</span>
            </div>
            <div class="modal-body">
                <div style="margin-bottom: 15px;">
                    <strong>文件：</strong> ${filename} ${fileSize ? `(${fileSize.toFixed(2)} GB)` : ''}
                </div>
                <div style="margin-bottom: 15px;">
                    <strong>系统信息：</strong> ${systemInfo ? `CPU ${systemInfo.cpuCount}核, 内存 ${systemInfo.memoryTotalGb.toFixed(1)}GB` : '系统信息不可用'}
                </div>
                <div style="margin-bottom: 20px;">
                    <h4>推荐配置：</h4>
                    <div class="recommendation-list">
    `;
    
    // 根据实际返回的推荐配置显示
    if (recommendations.minZoom !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>最小缩放级别：</strong> ${recommendations.minZoom}</div>`;
    }
    if (recommendations.maxZoom !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>最大缩放级别：</strong> ${recommendations.maxZoom}</div>`;
    }
    if (recommendations.processes !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>进程数：</strong> ${recommendations.processes}</div>`;
    }
    if (recommendations.maxMemory !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>最大内存：</strong> ${recommendations.maxMemory}</div>`;
    }
    if (recommendations.tileFormat !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>瓦片格式：</strong> ${recommendations.tileFormat}</div>`;
    }
    if (recommendations.quality !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>质量：</strong> ${recommendations.quality}</div>`;
    }
    if (recommendations.resampling !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>重采样方法：</strong> ${recommendations.resampling}</div>`;
    }
    if (recommendations.compression !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>压缩：</strong> ${recommendations.compression ? '是' : '否'}</div>`;
    }
    if (recommendations.decompress !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>解压：</strong> ${recommendations.decompress ? '是' : '否'}</div>`;
    }
    if (recommendations.autoZoom !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>智能分级：</strong> ${recommendations.autoZoom ? '是' : '否'}</div>`;
    }
    if (recommendations.zoomStrategy !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>分级策略：</strong> ${recommendations.zoomStrategy}</div>`;
    }
    if (recommendations.optimizeFile !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>文件优化：</strong> ${recommendations.optimizeFile ? '是' : '否'}</div>`;
    }
    if (recommendations.createOverview !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>创建概览：</strong> ${recommendations.createOverview ? '是' : '否'}</div>`;
    }
    if (recommendations.useOptimizedMode !== undefined) {
        recommendationHtml += `<div class="recommendation-item"><strong>优化模式：</strong> ${recommendations.useOptimizedMode ? '是' : '否'}</div>`;
    }
    
    recommendationHtml += `
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-primary" onclick="this.closest('.modal').remove()">确定</button>
            </div>
        </div>
    `;
    
    modal.innerHTML = recommendationHtml;
    document.body.appendChild(modal);
}

// 新建文件夹
async function createFolder() {
    const folderName = prompt('请输入新文件夹名称');
    if (!folderName) return;
    const folderPath = currentWorkspacePath ? `${currentWorkspacePath}/${folderName}` : folderName;
    console.log('新建文件夹完整路径:', folderPath);
    try {
        await terraForgeAPI.createWorkspaceFolder(folderPath);
        // 创建后刷新当前目录
        await loadWorkspaceFiles(currentWorkspacePath);
        showMessage('文件夹创建成功', 'success');
    } catch (error) {
        showMessage('文件夹创建失败: ' + error.message, 'error');
    }
}

// 删除工作空间项目
async function deleteWorkspaceItem(path, type) {
    if (!confirm(`确定要删除这个${type === 'folder' ? '文件夹' : '文件'}吗？`)) return;
    
    try {
        console.log('开始删除操作:', { path, type });
        
        if (type === 'folder') {
            const response = await terraForgeAPI.deleteWorkspaceFolder(path);
            console.log('删除文件夹响应:', response);
        } else {
            const response = await terraForgeAPI.deleteWorkspaceFile(path);
            console.log('删除文件响应:', response);
        }
        
        showMessage('删除成功', 'success');
        
        // 获取当前路径
        const currentPath = path.split('/').slice(0, -1).join('/');
        console.log('重新加载目录:', currentPath);
        
        // 重新加载当前目录
        await loadWorkspaceFiles(currentPath);
    } catch (error) {
        console.error('删除失败:', error);
        showMessage(`删除失败: ${error.message}`, 'error');
    }
}



// 工具函数
function showTileConverter() {
    showMessage('瓦片格式转换工具开发中...', 'info');
}

function showNodataScanner() {
    showMessage('透明瓦片扫描工具开发中...', 'info');
}

function showLayerUpdater() {
    showMessage('Layer.json更新工具开发中...', 'info');
}

// 开始定时任务
function startPeriodicTasks() {
    // 每30秒检查一次系统状态
    setInterval(checkSystemStatus, 30000);
}

// 通用UI工具函数
function showMessage(message, type = 'info') {
    const messageEl = document.createElement('div');
    messageEl.className = `message ${type}`;
    messageEl.innerHTML = `
        <i class="fas ${getMessageIcon(type)}"></i>
        <span>${message}</span>
        <button onclick="this.parentElement.remove()" style="float: right; background: none; border: none; font-size: 18px; cursor: pointer;">&times;</button>
    `;
    
    // 添加样式
    const style = document.createElement('style');
    style.textContent = `
        .message {
            position: fixed;
            top: 20px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 9999;
            padding: 12px 24px;
            border-radius: 4px;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
            display: flex;
            align-items: center;
            gap: 8px;
            min-width: 300px;
            max-width: 600px;
            animation: slideDown 0.3s ease-out;
        }
        
        @keyframes slideDown {
            from {
                top: -100px;
                opacity: 0;
            }
            to {
                top: 20px;
                opacity: 1;
            }
        }
        
        .message.success {
            background-color: #f0fdf4;
            border: 1px solid #86efac;
            color: #166534;
        }
        
        .message.error {
            background-color: #fef2f2;
            border: 1px solid #fecaca;
            color: #991b1b;
        }
        
        .message.warning {
            background-color: #fffbeb;
            border: 1px solid #fde68a;
            color: #92400e;
        }
        
        .message.info {
            background-color: #f0f9ff;
            border: 1px solid #bae6fd;
            color: #075985;
        }
        
        .message i {
            font-size: 16px;
        }
        
        .message span {
            flex: 1;
        }
        
        .message button {
            color: currentColor;
            opacity: 0.6;
            transition: opacity 0.2s;
        }
        
        .message button:hover {
            opacity: 1;
        }
    `;
    document.head.appendChild(style);
    
    document.body.appendChild(messageEl);
    
    // 3秒后自动消失
    setTimeout(() => {
        if (messageEl.parentElement) {
            messageEl.style.animation = 'slideUp 0.3s ease-out forwards';
            setTimeout(() => messageEl.remove(), 300);
        }
    }, 3000);
}

function getMessageIcon(type) {
    const icons = {
        'success': 'fa-check-circle',
        'error': 'fa-exclamation-circle', 
        'warning': 'fa-exclamation-triangle',
        'info': 'fa-info-circle'
    };
    return icons[type] || icons.info;
}

function showLoading(containerId) {
    const container = document.getElementById(containerId);
    if (container) {
        container.innerHTML = '<div class="loading">加载中...</div>';
    }
}

function hideLoading(containerId) {
    const container = document.getElementById(containerId);
    if (container && container.querySelector('.loading')) {
        container.querySelector('.loading').remove();
    }
}

function showError(containerId, message) {
    const container = document.getElementById(containerId);
    if (container) {
        container.innerHTML = `<div class="message error">${message}</div>`;
    }
}

function showModal(title, body, footer = '') {
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML = body;
    document.getElementById('modalFooter').innerHTML = footer || `
        <button class="btn btn-secondary" onclick="closeModal()">关闭</button>
    `;
    document.getElementById('modal').style.display = 'block';
}



// 点击模态框外部关闭
document.getElementById('modal').addEventListener('click', function(e) {
    if (e.target === this) {
        closeModal();
    }
});

// 加载系统管理
async function loadSystemManagement() {
    try {
        // 初始加载更新信息
        document.getElementById('updateInfo').innerHTML = '<div class="message info">点击检查更新按钮获取最新信息</div>';
        document.getElementById('routesInfo').innerHTML = '<div class="message info">点击查看路由按钮获取API信息</div>';
    } catch (error) {
        console.error('系统管理信息加载失败:', error);
    }
}

// 检查更新
async function checkForUpdates() {
    try {
        document.getElementById('updateInfo').innerHTML = '<div class="loading">检查更新中...</div>';
        
        const systemInfo = await terraForgeAPI.getSystemInfo();
        const currentVersion = systemInfo.version || '未知版本';
        
        document.getElementById('updateInfo').innerHTML = `
            <div class="update-status">
                <div class="status-item">
                    <span class="status-label">当前版本</span>
                    <span class="status-value">${currentVersion}</span>
                </div>
                <div class="status-item">
                    <span class="status-label">状态</span>
                    <span class="status-value">系统运行正常</span>
                </div>
                <div class="status-item">
                    <span class="status-label">最后检查</span>
                    <span class="status-value">${new Date().toLocaleString()}</span>
                </div>
            </div>
        `;
    } catch (error) {
        console.error('检查更新失败:', error);
        document.getElementById('updateInfo').innerHTML = '<div class="message error">检查更新失败</div>';
    }
}

// 更新容器
async function updateContainer() {
    try {
        const updateParams = {
            updateType: "all",
            timezone: "Asia/Shanghai"
        };
        
        const result = await terraForgeAPI.updateContainer(updateParams);
        showMessage('容器更新请求已发送', 'success');
        
        // 更新显示
        document.getElementById('updateInfo').innerHTML = `
            <div class="message success">
                容器更新请求已发送<br/>
                请稍等片刻，系统将自动重启
            </div>
        `;
    } catch (error) {
        console.error('容器更新失败:', error);
        showMessage('容器更新失败', 'error');
    }
}

// 加载API路由
async function loadRoutes() {
    try {
        document.getElementById('routesInfo').innerHTML = '<div class="loading">加载中...</div>';
        
        const routes = await terraForgeAPI.getRoutes();
        
        // 如果返回的是字符串，尝试解析
        let routeData;
        if (typeof routes === 'string') {
            try {
                routeData = JSON.parse(routes);
            } catch {
                document.getElementById('routesInfo').innerHTML = `<pre style="white-space: pre-wrap;">${routes}</pre>`;
                return;
            }
        } else {
            routeData = routes;
        }
        
        // 如果是对象且有routes属性
        if (routeData && routeData.routes) {
            const routesList = routeData.routes.map(route => `
                <div class="route-item">
                    <span class="route-method">${route.method}</span>
                    <span class="route-path">${route.path}</span>
                    <span class="route-desc">${route.description || ''}</span>
                </div>
            `).join('');
            
            document.getElementById('routesInfo').innerHTML = `
                <div class="routes-list">
                    ${routesList}
                </div>
                <div class="routes-summary">
                    总共 ${routeData.routes.length} 个API接口
                </div>
            `;
        } else {
            document.getElementById('routesInfo').innerHTML = `<pre style="white-space: pre-wrap;">${JSON.stringify(routeData, null, 2)}</pre>`;
        }
    } catch (error) {
        console.error('加载API路由失败:', error);
        document.getElementById('routesInfo').innerHTML = '<div class="message error">加载API路由失败</div>';
    }
}

// 日期时间更新
function startDateTimeUpdate() {
    updateDateTime();
    setInterval(updateDateTime, 1000);
}

function updateDateTime() {
    const now = new Date();
    const dateElement = document.getElementById('currentDate');
    const timeElement = document.getElementById('currentTime');
    
    if (dateElement && timeElement) {
        dateElement.textContent = now.toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit'
        });
        timeElement.textContent = now.toLocaleTimeString('zh-CN', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    }
}

// 仪表盘自动刷新控制
function toggleAutoRefresh() {
    const toggle = document.getElementById('autoRefreshToggle');
    autoRefreshEnabled = toggle.checked;
    
    if (autoRefreshEnabled) {
        startAutoRefresh();
        showMessage('自动刷新已开启', 'success');
    } else {
        stopAutoRefresh();
        showMessage('自动刷新已关闭', 'info');
    }
}

function updateRefreshInterval() {
    const interval = document.getElementById('refreshInterval').value;
    if (autoRefreshEnabled) {
        stopAutoRefresh();
        startAutoRefresh();
        showMessage(`刷新间隔已更新为${interval}秒`, 'info');
    }
}

function startAutoRefresh() {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
    }
    
    const interval = document.getElementById('refreshInterval').value * 1000;
    autoRefreshInterval = setInterval(async () => {
        // 只在仪表盘页面激活时才自动刷新
        if (document.getElementById('dashboard').classList.contains('active')) {
            await updateSystemInfo();
        }
    }, interval);
}

function stopAutoRefresh() {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
        autoRefreshInterval = null;
    }
}

// 文件详情弹出框
function showFileDetails(filePath, fileName) {
    terraForgeAPI.getFileInfo(filePath).then(response => {
        console.log('Java API 原始响应:', response); // 详细调试
        console.log('文件路径:', filePath, '文件名:', fileName); // 调试参数
        
        // 现在Java API返回的是直接映射Python的结构
        const fileInfo = response;
        console.log('文件信息:', fileInfo);
        console.log('fileInfo.metadata:', fileInfo.metadata);
        console.log('fileInfo的类型:', typeof fileInfo);
        console.log('fileInfo的所有属性:', Object.keys(fileInfo));
        
        if (!fileInfo || (!fileInfo.size && !fileInfo.format)) {
            showMessage('获取文件信息失败：文件信息为空', 'error');
            return;
        }
        
        // 处理基本信息 - 适配新的数据结构
        const fileSize = fileInfo.size || 0;
        const fileSizeFormatted = formatFileSize(fileSize);
        const fileType = fileInfo.format || fileInfo.type || '未知';
        const lastModified = fileInfo.lastModified;
        
        let modifiedTimeString = '未知';
        if (lastModified) {
            try {
                modifiedTimeString = new Date(lastModified).toLocaleString('zh-CN');
            } catch (e) {
                console.warn('时间格式解析失败:', lastModified);
                modifiedTimeString = lastModified;
            }
        }
        
        const basicInfo = [
            { label: '文件名', value: fileName },
            { label: '文件路径', value: filePath },
            { label: '文件大小', value: fileSizeFormatted },
            { label: '文件类型', value: fileType },
            { label: '修改时间', value: modifiedTimeString }
        ];
        
        console.log('处理后的基本信息:', basicInfo); // 调试处理结果
        
        // 处理地理空间信息
        const geoInfo = [];
        const metadata = fileInfo.metadata;
        console.log('metadata存在:', !!metadata);
        console.log('metadata类型:', typeof metadata);
        if (metadata) {
            console.log('地理元数据:', metadata); // 调试元数据
            
            // 坐标系统
            if (metadata.srs) {
                const srsDisplay = metadata.srs.includes('WGS 84') ? 'WGS 84 (地理坐标系)' : metadata.srs;
                geoInfo.push({ label: '坐标系统', value: srsDisplay });
            }
            
            // 波段信息
            if (metadata.bandCount) {
                geoInfo.push({ 
                    label: '波段数量', 
                    value: `${metadata.bandCount} 个波段`
                });
            }
            
            // 影像尺寸
            const rasterSize = metadata.rasterSize;
            if (rasterSize && rasterSize.width && rasterSize.height) {
                geoInfo.push({ 
                    label: '影像尺寸', 
                    value: `${rasterSize.width} × ${rasterSize.height} 像素`
                });
                const totalPixels = rasterSize.width * rasterSize.height;
                geoInfo.push({ 
                    label: '总像素数', 
                    value: `${totalPixels.toLocaleString()} 像素`
                });
            }
            
            // 像素分辨率
            const pixelSize = metadata.pixelSize;
            if (pixelSize && pixelSize.x) {
                const pixelSizeX = Math.abs(pixelSize.x);
                const pixelSizeY = Math.abs(pixelSize.y);
                geoInfo.push({ 
                    label: '像素分辨率', 
                    value: `${pixelSizeX.toFixed(8)} × ${pixelSizeY.toFixed(8)} 度/像素`
                });
                
                // 转换为米（大概值）
                const meterX = pixelSizeX * 111320;
                const meterY = pixelSizeY * 111320;
                if (meterX < 1000) {
                    geoInfo.push({ 
                        label: '地面分辨率', 
                        value: `约 ${meterX.toFixed(2)} × ${meterY.toFixed(2)} 米/像素`
                    });
                } else {
                    geoInfo.push({ 
                        label: '地面分辨率', 
                        value: `约 ${(meterX/1000).toFixed(2)} × ${(meterY/1000).toFixed(2)} 千米/像素`
                    });
                }
            }
            
            // 地理边界
            const bounds = metadata.bounds;
            if (bounds && bounds.west !== undefined) {
                geoInfo.push({ 
                    label: '西经', 
                    value: `${bounds.west.toFixed(6)}°`
                });
                geoInfo.push({ 
                    label: '东经', 
                    value: `${bounds.east.toFixed(6)}°`
                });
                geoInfo.push({ 
                    label: '南纬', 
                    value: `${bounds.south.toFixed(6)}°`
                });
                geoInfo.push({ 
                    label: '北纬', 
                    value: `${bounds.north.toFixed(6)}°`
                });
                geoInfo.push({ 
                    label: '覆盖范围', 
                    value: `${bounds.widthDegrees.toFixed(4)}° × ${bounds.heightDegrees.toFixed(4)}°`
                });
            }
        }
        
        // 如果没有获取到地理信息，添加提示
        if (geoInfo.length === 0) {
            geoInfo.push({
                label: '地理信息',
                value: '该文件可能不是地理数据文件或暂无地理信息'
            });
        }
        
        showModal('文件详细信息', `
            <div class="info-list">
                ${basicInfo.map(item => `
                    <div class="info-row">
                        <span class="info-label">${item.label}</span>
                        <span class="info-value">${item.value}</span>
                    </div>
                `).join('')}
                ${geoInfo.map(item => `
                    <div class="info-row">
                        <span class="info-label">${item.label}</span>
                        <span class="info-value">${item.value}</span>
                    </div>
                `).join('')}
            </div>
        `, '<button class="btn btn-secondary" onclick="closeModal()">关闭</button>');
        
    }).catch(error => {
        console.error('获取文件详情失败:', error);
        showMessage('获取文件详情失败: ' + error.message, 'error');
    });
}



// 格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 选择数据源文件夹函数（用于地图切片和地形切片的文件夹路径）
function selectDatasourceFolder(inputId) {
    // 记录当前操作的输入框ID
    currentDatasourceInputId = inputId;
    
    // 区分是文件夹路径还是文件匹配模式
    const isFolderPath = inputId.includes('FolderPaths');
    
    if (isFolderPath) {
        // 文件夹路径：支持多选，只能选择文件夹
        filePickerCallback = (path, isFolder = true) => {
            if (!isFolder) {
                showMessage('文件夹路径只能选择文件夹', 'warning');
                return;
            }
            
            const currentValue = document.getElementById(inputId).value;
            let paths = currentValue ? currentValue.split(',').map(p => p.trim()) : [];
            
            // 避免重复添加
            if (!paths.includes(path)) {
                paths.push(path);
                document.getElementById(inputId).value = paths.join(', ');
            }
        };
    } else {
        // 文件匹配模式：支持多选，必须选择文件（tif或txt）
        filePickerCallback = (path, isFolder = false) => {
            if (isFolder) {
                showMessage('文件匹配模式必须选择具体的文件', 'warning');
                return;
            }
            
            // 检查文件类型
            const fileName = path.toLowerCase();
            if (!fileName.endsWith('.tif') && !fileName.endsWith('.tiff') && !fileName.endsWith('.txt')) {
                showMessage('文件匹配模式只能选择 .tif、.tiff 或 .txt 文件', 'warning');
                return;
            }
            
            const currentValue = document.getElementById(inputId).value;
            let patterns = currentValue ? currentValue.split(',').map(p => p.trim()) : [];
            
            // 避免重复添加
            if (!patterns.includes(path)) {
                patterns.push(path);
                document.getElementById(inputId).value = patterns.join(', ');
                
                // 更新智能推荐按钮状态
                if (inputId === 'mapFilePatterns') {
                    updateRecommendButtonState('map');
                } else if (inputId === 'terrainFilePatterns') {
                    updateRecommendButtonState('terrain');
                }
            }
        };
    }
    
    // 检查模态框是否存在，如果不存在则创建
    let modal = document.getElementById('datasourcePickerModal');
    if (!modal) {
        createDatasourcePickerModal();
        modal = document.getElementById('datasourcePickerModal');
    }
    modal.style.display = 'block';
    loadDatasourcePickerFileList(''); // 加载数据源目录
}

// 选择工作空间文件夹函数（用于结果浏览）
function selectFolder(inputId) {
    filePickerCallback = (path) => {
        document.getElementById(inputId).value = path;
    };
    // 检查模态框是否存在，如果不存在则创建
    let modal = document.getElementById('filePickerModal');
    if (!modal) {
        createFilePickerModal();
        modal = document.getElementById('filePickerModal');
    }
    modal.style.display = 'block';
    loadPickerFileList(''); // 加载工作空间目录
}

// 选择工作空间文件夹（只允许选文件夹）
function selectWorkspaceFolder(inputId) {
    filePickerCallback = (path, isDir) => {
        if (!isDir) {
            showMessage('请选择文件夹', 'warning');
            return;
        }
        document.getElementById(inputId).value = path;
    };
    // 打开文件夹选择器（复用工作空间浏览器）
    let modal = document.getElementById('filePickerModal');
    if (!modal) {
        createFilePickerModal(true); // 只选文件夹
        modal = document.getElementById('filePickerModal');
    }
    modal.style.display = 'block';
    loadPickerFileList('');
}

// 创建数据源文件夹选择器模态框
function createDatasourcePickerModal() {
    const modalHtml = `
        <div id="datasourcePickerModal" class="modal">
            <div class="modal-content">
                <div class="modal-header">
                    <h3>选择数据源文件夹</h3>
                    <span class="close" onclick="closeDatasourcePickerModal()">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="file-browser">
                        <div class="breadcrumb" id="datasourcePickerBreadcrumb">
                            <span class="breadcrumb-item active">根目录</span>
                        </div>
                        <div class="file-list" id="datasourcePickerFileList">
                            <div class="loading">加载中...</div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="clearDatasourceSelection()">清空选择</button>
                    <button class="btn btn-secondary" onclick="closeDatasourcePickerModal()">取消</button>
                    <button class="btn btn-primary" onclick="closeDatasourcePickerModal()">完成选择</button>
                </div>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

// 创建工作空间文件夹选择器模态框（只允许选文件夹）
function createFilePickerModal(onlyFolder = false) {
    const modalHtml = `
        <div id="filePickerModal" class="modal">
            <div class="modal-content">
                <div class="modal-header">
                    <h3>选择文件夹</h3>
                    <span class="close" onclick="closeModal()">&times;</span>
                </div>
                <div class="modal-body">
                    <div class="file-browser">
                        <div class="breadcrumb" id="pickerBreadcrumb">
                            <span class="breadcrumb-item active">根目录</span>
                        </div>
                        <div class="file-list" id="pickerFileList">
                            <div class="loading">加载中...</div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeModal()">取消</button>
                </div>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    window.onlyFolderPicker = onlyFolder;
}

// 加载文件夹选择器文件列表
async function loadPickerFileList(path = '') {
    const container = document.getElementById('pickerFileList');
    container.innerHTML = '<div class="loading">加载中...</div>';
    
    try {
        const data = await terraForgeAPI.browseResults(path);
        let html = '';
        
        // 更新面包屑导航
        updatePickerBreadcrumb(path);
        
        // 目录
        if (data.directories) {
            data.directories.forEach(dir => {
                const fullPath = path ? `${path}/${dir.name}` : dir.name;
                html += `
                    <div class="file-item" onclick="loadPickerFileList('${fullPath}')">
                        <i class="file-icon fas fa-folder"></i>
                        <div class="file-info">
                            <div class="file-name">${dir.name}</div>
                            <div class="file-details">目录</div>
                        </div>
                        <div class="file-actions">
                            <button class="btn btn-primary" onclick="event.stopPropagation(); selectPickedFolder('${fullPath}')">选择</button>
                        </div>
                    </div>
                `;
            });
        }
        // 只允许选文件夹，不显示文件
        container.innerHTML = html || '<div class="message info">暂无文件夹</div>';
    } catch (error) {
        console.error('加载工作空间文件夹失败:', error);
        container.innerHTML = '<div class="message error">加载失败</div>';
    }
}

// 加载数据源选择器文件列表
async function loadDatasourcePickerFileList(path = '') {
    const container = document.getElementById('datasourcePickerFileList');
    container.innerHTML = '<div class="loading">加载中...</div>';
    
    try {
        const data = await terraForgeAPI.browseDatasources(path);
        let html = '';
        
        // 更新面包屑
        updateDatasourcePickerBreadcrumb(path);
        
        // 目录
        if (data.directories && data.directories.length > 0) {
            data.directories.forEach(dir => {
                const fullPath = path ? `${path}/${dir.name}` : dir.name;
                html += `
                    <div class="file-item" onclick="loadDatasourcePickerFileList('${fullPath}')">
                        <i class="file-icon fas fa-folder"></i>
                        <div class="file-info">
                            <div class="file-name">${dir.name}</div>
                            <div class="file-details">目录</div>
                        </div>
                        <div class="file-actions">
                            <button class="btn btn-primary" onclick="event.stopPropagation(); selectPickedDatasourceFolder('${fullPath}', true)">选择</button>
                        </div>
                    </div>
                `;
            });
        }
        
        // 判断当前是否为文件夹路径选择模式
        const isFolderPathMode = currentDatasourceInputId && currentDatasourceInputId.includes('FolderPaths');
        
        // 文件（只在文件匹配模式下显示）
        if (!isFolderPathMode && data.datasources && data.datasources.length > 0) {
            data.datasources.forEach(file => {
                const fullPath = path ? `${path}/${file.name}` : file.name;
                html += `
                    <div class="file-item">
                        <i class="file-icon fas fa-file"></i>
                        <div class="file-info">
                            <div class="file-name">${file.name}</div>
                            <div class="file-details">${file.sizeFormatted || '文件'}</div>
                        </div>
                        <div class="file-actions">
                            <button class="btn btn-primary" onclick="selectPickedDatasourceFolder('${fullPath}', false)">选择</button>
                        </div>
                    </div>
                `;
            });
        }
        
        // 如果当前目录有内容，添加一个选择当前目录的选项（只在文件夹路径模式下显示）
        if (isFolderPathMode && path && data.directories?.length > 0) {
            html = `
                <div class="file-item" style="border: 2px solid #667eea; background: #f0f2ff;">
                    <i class="file-icon fas fa-folder-open" style="color: #667eea;"></i>
                    <div class="file-info">
                        <div class="file-name" style="color: #667eea; font-weight: bold;">选择当前目录</div>
                        <div class="file-details">${path}</div>
                    </div>
                    <div class="file-actions">
                        <button class="btn btn-primary" onclick="selectPickedDatasourceFolder('${path}', true)">选择此目录</button>
                    </div>
                </div>
            ` + html;
        }
        
        container.innerHTML = html || '<div class="message info">此目录为空</div>';
    } catch (error) {
        console.error('加载数据源失败:', error);
        container.innerHTML = '<div class="message error">加载失败</div>';
    }
}

// 更新数据源选择器面包屑
function updateDatasourcePickerBreadcrumb(path) {
    const breadcrumb = document.getElementById('datasourcePickerBreadcrumb');
    let html = '<span class="breadcrumb-item" onclick="loadDatasourcePickerFileList(\'\')">根目录</span>';
    
    if (path) {
        const parts = path.split('/');
        let currentPath = '';
        parts.forEach(part => {
            currentPath = currentPath ? `${currentPath}/${part}` : part;
            html += ` / <span class="breadcrumb-item" onclick="loadDatasourcePickerFileList('${currentPath}')">${part}</span>`;
        });
    }
    
    breadcrumb.innerHTML = html;
}

// 更新工作空间文件夹选择器面包屑
function updatePickerBreadcrumb(path) {
    const breadcrumb = document.getElementById('pickerBreadcrumb');
    let html = '<span class="breadcrumb-item" onclick="loadPickerFileList(\'\')">根目录</span>';
    
    if (path) {
        const parts = path.split('/');
        let currentPath = '';
        parts.forEach(part => {
            currentPath = currentPath ? `${currentPath}/${part}` : part;
            html += ` / <span class="breadcrumb-item" onclick="loadPickerFileList('${currentPath}')">${part}</span>`;
        });
    }
    
    breadcrumb.innerHTML = html;
}

// 选择数据源文件夹（从选择器中选择）
function selectPickedDatasourceFolder(path, isFolder = true) {
    if (filePickerCallback) {
        filePickerCallback(path, isFolder);
        // 不要立即关闭模态框，支持多选
        // closeDatasourcePickerModal();
    }
}

// 关闭数据源选择器模态框
function closeDatasourcePickerModal() {
    const modal = document.getElementById('datasourcePickerModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

// 存储当前正在操作的输入框ID
let currentDatasourceInputId = null;

// 清空数据源选择
function clearDatasourceSelection() {
    if (currentDatasourceInputId) {
        const element = document.getElementById(currentDatasourceInputId);
        if (element) {
            element.value = '';
            
            // 更新智能推荐按钮状态（只有文件匹配模式需要更新）
            if (!currentDatasourceInputId.includes('FolderPaths')) {
                if (currentDatasourceInputId === 'mapFilePatterns') {
                    updateRecommendButtonState('map');
                } else if (currentDatasourceInputId === 'terrainFilePatterns') {
                    updateRecommendButtonState('terrain');
                }
            }
            
            showMessage('选择已清空', 'info');
        }
    } else {
        showMessage('请先点击浏览按钮选择文件', 'warning');
    }
}

// 选择工作空间文件夹
function selectPickedFolder(path) {
    if (filePickerCallback) {
        filePickerCallback(path, true);
        closeModal();
    }
}

// 透明瓦片扫描
async function scanNodataTiles() {
    const folder = document.getElementById('nodataFolder').value;
    if (!folder) {
        showMessage('请先选择文件夹', 'warning');
        return;
    }
    try {
        await terraForgeAPI.scanNodataTiles({ path: folder });
        showMessage('透明瓦片扫描任务已提交', 'success');
    } catch (e) {
        showMessage('扫描失败: ' + e.message, 'error');
    }
}

// 透明瓦片删除
async function deleteNodataTiles() {
    const folder = document.getElementById('nodataFolder').value;
    if (!folder) {
        showMessage('请先选择文件夹', 'warning');
        return;
    }
    try {
        await terraForgeAPI.deleteNodataTiles({ path: folder });
        showMessage('透明瓦片删除任务已提交', 'success');
    } catch (e) {
        showMessage('删除失败: ' + e.message, 'error');
    }
}

// layer.json 生成
async function generateLayerJson() {
    const folder = document.getElementById('layerJsonFolder').value;
    if (!folder) {
        showMessage('请先选择文件夹', 'warning');
        return;
    }
    try {
        await terraForgeAPI.updateLayerJson({ folderPath: folder });
        showMessage('layer.json 生成任务已提交', 'success');
    } catch (e) {
        showMessage('生成失败: ' + e.message, 'error');
    }
}

// 添加必要的CSS类
const style = document.createElement('style');
style.textContent = `
.info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 15px;
    margin: 15px 0;
}

.info-item {
    padding: 10px;
    background: #f8f9fa;
    border-radius: 6px;
    border-left: 3px solid #667eea;
}

.info-label {
    font-size: 14px;
    color: #6c757d;
    margin-bottom: 5px;
}

.info-value {
    font-weight: 600;
    color: #333;
}

.task-stats, .workspace-stats {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 15px;
}

.stat-item {
    text-align: center;
    padding: 15px;
    background: #f8f9fa;
    border-radius: 6px;
}

.stat-number {
    font-size: 24px;
    font-weight: bold;
    color: #667eea;
}

.stat-label, .stat-value {
    font-size: 12px;
    color: #6c757d;
    margin-top: 5px;
}

.system-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
    gap: 20px;
}

.update-status, .config-info {
    display: grid;
    gap: 10px;
}

.status-item, .config-item {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    border-bottom: 1px solid #e9ecef;
}

.status-label, .config-label {
    font-weight: 500;
    color: #495057;
}

.status-value, .config-value {
    color: #333;
}

.routes-list {
    max-height: 300px;
    overflow-y: auto;
}

.route-item {
    display: flex;
    gap: 10px;
    padding: 8px 0;
    border-bottom: 1px solid #f1f3f4;
}

.route-method {
    background: #667eea;
    color: white;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    min-width: 50px;
    text-align: center;
}

.route-path {
    font-family: monospace;
    flex: 1;
}

.route-desc {
    color: #6c757d;
    font-size: 12px;
}

.routes-summary {
    margin-top: 10px;
    text-align: center;
    color: #6c757d;
    font-size: 14px;
}
`;
document.head.appendChild(style); 

// 更新系统状态和配置信息
async function updateSystemInfo() {
    try {
        // 显示刷新开始提示
        showRefreshIndicator(true);
        
        const [healthData, systemInfo, taskData] = await Promise.all([
            terraForgeAPI.getHealthStatus(),
            terraForgeAPI.getSystemInfo(),
            terraForgeAPI.getAllTasks()
        ]);

        console.log('系统信息数据:', systemInfo); // 调试用

        // 更新任务概览数据
        updateOverviewCards(taskData);

        // 更新系统状态
        const healthStatus = document.getElementById('healthStatus');
        if (healthStatus) {
            healthStatus.innerHTML = `
                <div class="dashboard-info-item">
                    <span class="dashboard-label">服务状态</span>
                    <span class="dashboard-value ${healthData.status === 'healthy' ? 'text-success' : 'text-danger'}">
                        ${healthData.status === 'healthy' ? '🟢 正常运行' : '🔴 服务异常'}
                    </span>
                </div>
                <div class="dashboard-info-item">
                    <span class="dashboard-label">API版本</span>
                    <span class="dashboard-value">${healthData.version || '未知'}</span>
                </div>
                <div class="dashboard-info-item">
                    <span class="dashboard-label">最后检查</span>
                    <span class="dashboard-value">${healthData.timestamp || '未知'}</span>
                </div>
            `;
        }

        // 更新系统配置 - 显示更丰富的信息
        const systemConfig = document.getElementById('systemConfig');
        if (systemConfig) {
            // 提取系统信息
            const cpuCount = systemInfo?.system?.cpuCount || systemInfo?.cpuCount || '未知';
            const memoryTotal = formatMemory(systemInfo?.system?.memoryTotal || systemInfo?.memoryTotal);
            const memoryAvailable = formatMemory(systemInfo?.system?.memoryAvailable || systemInfo?.memoryAvailable);
            const diskUsage = systemInfo?.system?.diskUsage ? `${systemInfo.system.diskUsage.toFixed(1)}%` : '未知';
            
            // 提取配置信息
            const maxThreads = systemInfo?.config?.maxThreads || '未知';
            
            // 计算内存使用率
            let memoryUsage = '未知';
            if (systemInfo?.system?.memoryTotal && systemInfo?.system?.memoryAvailable) {
                const used = systemInfo.system.memoryTotal - systemInfo.system.memoryAvailable;
                const usagePercent = (used / systemInfo.system.memoryTotal * 100).toFixed(1);
                memoryUsage = `${usagePercent}%`;
            }
            
            systemConfig.innerHTML = `
                <div class="dashboard-info-item">
                    <span class="dashboard-label">CPU核心数</span>
                    <span class="dashboard-value">🖥️ ${cpuCount} 核</span>
                </div>
                <div class="dashboard-info-item">
                    <span class="dashboard-label">内存总量</span>
                    <span class="dashboard-value">💾 ${memoryTotal}</span>
                </div>
                <div class="dashboard-info-item">
                    <span class="dashboard-label">可用内存</span>
                    <span class="dashboard-value">📊 ${memoryAvailable} (${memoryUsage})</span>
                </div>
                <div class="dashboard-info-item">
                    <span class="dashboard-label">磁盘使用率</span>
                    <span class="dashboard-value">💿 ${diskUsage}</span>
                </div>
                <div class="dashboard-info-item">
                    <span class="dashboard-label">最大线程数</span>
                    <span class="dashboard-value">⚙️ ${maxThreads}</span>
                </div>
            `;
        }
        

        
        // 显示刷新成功提示
        showRefreshIndicator(false, true);
        
    } catch (error) {
        console.error('更新系统信息失败:', error);
        showRefreshIndicator(false, false);
        showMessage('获取系统信息失败', 'error');
    }
}

// 更新任务概览卡片
function updateOverviewCards(taskData) {
    if (!taskData || !taskData.tasks) {
        return;
    }
    
    const tasks = Object.values(taskData.tasks);
    const totalTasks = tasks.length;
    const completedTasks = tasks.filter(task => task.status === 'completed').length;
    const runningTasks = tasks.filter(task => task.status === 'running').length;
    const failedTasks = tasks.filter(task => task.status === 'failed').length;
    
    // 更新卡片数据
    const cards = document.querySelectorAll('.overview-card .card-value');
    if (cards.length >= 4) {
        cards[0].textContent = totalTasks;
        cards[1].textContent = completedTasks;
        cards[2].textContent = runningTasks;
        cards[3].textContent = failedTasks;
    }
}

// 格式化内存显示
function formatMemory(bytes) {
    if (!bytes || bytes === 0) return '未知';
    
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let size = bytes;
    let unitIndex = 0;
    
    while (size >= 1024 && unitIndex < units.length - 1) {
        size /= 1024;
        unitIndex++;
    }
    
    return `${size.toFixed(1)} ${units[unitIndex]}`;
}

// 定时更新系统信息
setInterval(updateSystemInfo, 30000); // 每30秒更新一次 

// 显示刷新指示器
function showRefreshIndicator(isRefreshing, success = null) {
    const existingIndicator = document.getElementById('refreshIndicator');
    if (existingIndicator) {
        existingIndicator.remove();
    }
    
    if (isRefreshing) {
        // 显示刷新中指示器 - 使用统一的消息框样式
        const indicator = document.createElement('div');
        indicator.id = 'refreshIndicator';
        indicator.className = 'message info';
        indicator.innerHTML = `
            <i class="refresh-icon">🔄</i>
            <span>正在刷新系统信息...</span>
        `;
        indicator.style.top = '80px'; // 避免和其他消息重叠
        document.body.appendChild(indicator);
    } else if (success !== null) {
        // // 显示刷新结果 - 使用统一的消息框样式
        // const indicator = document.createElement('div');
        // indicator.id = 'refreshIndicator';
        // indicator.className = `message ${success ? 'success' : 'error'}`;
        // indicator.innerHTML = `
        //     <i class="fas ${success ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
        //     <span>${success ? '系统信息已更新' : '刷新失败'}</span>
        // `;
        // indicator.style.top = '60px'; // 避免和其他消息重叠
        // document.body.appendChild(indicator);
        //
        // // 2秒后自动消失
        // setTimeout(() => {
        //     if (indicator.parentElement) {
        //         indicator.style.animation = 'slideUp 0.3s ease-out forwards';
        //         setTimeout(() => indicator.remove(), 300);
        //     }
        // }, 2000);
    }
}

// 添加刷新指示器样式
function addRefreshIndicatorStyles() {
    if (document.getElementById('refreshIndicatorStyles')) return;
    
    const style = document.createElement('style');
    style.id = 'refreshIndicatorStyles';
    style.textContent = `
        .refresh-indicator .refresh-icon {
            animation: rotate 1s linear infinite;
        }
        
        @keyframes rotate {
            from { transform: rotate(0deg); }
            to { transform: rotate(360deg); }
        }
        
        @keyframes slideUp {
            from {
                top: 80px;
                opacity: 1;
            }
            to {
                top: -100px;
                opacity: 0;
            }
        }
    `;
    document.head.appendChild(style);
}

function closeModal() {
    // 关闭文件选择器模态框
    const filePickerModal = document.getElementById('filePickerModal');
    if (filePickerModal) {
        filePickerModal.style.display = 'none';
    }
    // 关闭通用模态框
    const modal = document.getElementById('modal');
    if (modal) {
        modal.style.display = 'none';
    }
    filePickerCallback = null;
}

 