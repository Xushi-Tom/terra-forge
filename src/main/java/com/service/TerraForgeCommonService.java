package com.service;

import com.dto.terraforge.*;
import java.util.Map;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description TerraForge通用功能服务接口
 * @date 2025/7/1 10:00:00
 */
public interface TerraForgeCommonService {
    
    /**
     * 获取健康状态
     */
    HealthResponseDto getHealth();
    
    /**
     * 获取系统信息
     */
    SystemInfoResponseDto getSystemInfo();
    
    /**
     * 浏览数据源目录
     */
    DataSourceResponseDto browseDatasources(String path);

    /**
     * 浏览数据源目录（支持地理范围筛选）
     */
    DataSourceResponseDto browseDatasources(String path, String bounds);
    
    /**
     * 获取数据源详情和智能配置推荐
     */
    DataSourceInfoResponseDto getDatasourceInfo(String filename, String tileType);
    
    /**
     * 浏览结果目录
     */
    ResultsResponseDto browseResults(String path);
    
    /**
     * 查询任务状态（详细信息，包含处理日志）
     */
    TaskStatusResponseDto getTaskStatus(String taskId);
    
    /**
     * 查询所有任务（简化信息，不包含处理日志）
     */
    TaskListResponseDto getAllTasks();

    /**
     * 停止任务
     */
    TaskActionResponseDto stopTask(String taskId);

    /**
     * 删除任务
     */
    TaskActionResponseDto deleteTask(String taskId);

    /**
     * 清理任务
     */
    String cleanupTasks();

    /**
     * 清理任务（带参数）
     */
    String cleanupTasks(TaskCleanupRequestDto request);

    /**
     * 更新Layer.json
     */
    String updateLayerJson(UpdateLayerRequestDto request);

    /**
     * 解压地形文件
     */
    String decompressTerrain(DecompressRequestDto request);

    /**
     * 创建地形瓦片
     */
    TerrainTileResponseDto createTerrainTiles(TerrainTileRequestDto request);

    // ========== 工作空间管理方法 ==========

    /**
     * 创建工作空间文件夹
     */
    WorkspaceCreateFolderResponseDto createWorkspaceFolder(WorkspaceCreateFolderRequestDto request);

    /**
     * 删除工作空间文件夹
     */
    WorkspaceDeleteResponseDto deleteWorkspaceFolder(String folderPath);

    /**
     * 重命名工作空间文件夹
     */
    WorkspaceRenameResponseDto renameWorkspaceFolder(String folderPath, WorkspaceRenameRequestDto request);

    /**
     * 删除工作空间文件
     */
    WorkspaceDeleteResponseDto deleteWorkspaceFile(String filePath);

    /**
     * 重命名工作空间文件
     */
    WorkspaceRenameResponseDto renameWorkspaceFile(String filePath, WorkspaceRenameRequestDto request);

    /**
     * 移动工作空间项目
     */
    WorkspaceMoveResponseDto moveWorkspaceItem(WorkspaceMoveRequestDto request);

    // 删除工作空间信息接口
    // WorkspaceInfoResponseDto getWorkspaceInfo();

    // ========== 瓦片格式转换方法 ==========

    /**
     * 瓦片格式转换
     * 支持 z/x_y.png（扁平格式）和 z/x/y.png（嵌套格式）两种格式的互转
     * 
     * @param sourcePath 源瓦片目录路径（相对于tiles目录）
     * @param targetPath 目标瓦片目录路径（相对于tiles目录）
     * @param sourceFormat 源格式："flat"（z/x_y.png）或"nested"（z/x/y.png）
     * @param targetFormat 目标格式："flat"（z/x_y.png）或"nested"（z/x/y.png）
     * @param overwrite 是否覆盖已存在的目标文件
     * @return 转换结果
     */
    Map<String, Object> convertTileFormat(String sourcePath, String targetPath, String sourceFormat, String targetFormat, Boolean overwrite);

    // ========== 透明瓦片处理方法 ==========

    /**
     * 扫描包含透明（nodata）值的PNG瓦片
     * 
     * @param tilesPath 瓦片目录路径（相对于tiles目录）
     * @param includeDetails 是否返回详细文件列表
     * @return 扫描结果
     */
    Map<String, Object> scanNodataTiles(String tilesPath, Boolean includeDetails);

    /**
     * 删除包含透明（nodata）值的PNG瓦片
     * 
     * @param tilesPath 瓦片目录路径（相对于tiles目录）
     * @param includeDetails 是否返回详细文件列表
     * @return 删除结果
     */
    Map<String, Object> deleteNodataTiles(String tilesPath, Boolean includeDetails);
}
