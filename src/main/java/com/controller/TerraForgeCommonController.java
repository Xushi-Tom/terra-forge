package com.controller;

import com.dto.terraforge.*;
import com.service.TerraForgeCommonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description TerraForge通用功能控制器
 * @date 2025/7/1 10:00:00
 */
@Slf4j
@Api(tags = "TerraForge通用功能")
@RestController
@RequestMapping("/api")
public class TerraForgeCommonController {

    @Autowired
    private TerraForgeCommonService terraForgeCommonService;

    @Value("${terraforge.api.config-recommend-url}")
    private String configRecommendUrl;

    @Value("${terraforge.api.routes-url}")
    private String routesUrl;

    @Value("${terraforge.api.container-update-url}")
    private String containerUpdateUrl;

    private RestTemplate restTemplate = new RestTemplate();

    /**
     * 健康检查
     */
    @ApiOperation("健康检查")
    @GetMapping("/health")
    public HealthResponseDto health() {
        return terraForgeCommonService.getHealth();
    }

    /**
     * 获取系统信息
     */
    @ApiOperation("获取系统信息")
    @GetMapping("/system/info")
    public SystemInfoResponseDto getSystemInfo() {
        return terraForgeCommonService.getSystemInfo();
    }

    /**
     * 浏览数据源目录
     * Browse data sources directory
     *
     * @param path   路径参数，可选，为空时浏览根目录
     * @param bounds 地理范围筛选参数，可选
     * @return 数据源响应
     */
    @ApiOperation("浏览数据源目录")
    @GetMapping("/datasources")
    public DataSourceResponseDto browseDatasources(
            @RequestParam(required = false, defaultValue = "") String path,
            @RequestParam(required = false) String bounds) {
        return terraForgeCommonService.browseDatasources(path, bounds);
    }

    /**
     * 获取数据源详情和智能配置推荐
     */
    @ApiOperation("获取数据源详情和智能配置推荐")
    @GetMapping("/datasources/info/**")
    public DataSourceInfoResponseDto getDatasourceInfo(
            HttpServletRequest request,
            @RequestParam(required = false) String tileType) {
        String filename = extractPathFromRequest(request, "/api/datasources/info/");
        log.info("收到文件信息请求: {}", filename);
        return terraForgeCommonService.getDatasourceInfo(filename, tileType);
    }

    /**
     * 浏览结果目录
     * Browse results directory
     *
     * @param path 路径参数，可选，为空时浏览根目录
     * @return 结果响应
     */
    @ApiOperation("浏览结果目录")
    @GetMapping("/results")
    public ResultsResponseDto browseResults(
            @RequestParam(required = false, defaultValue = "") String path) {
        return terraForgeCommonService.browseResults(path);
    }

    /**
     * 查询任务状态
     */
    @ApiOperation("查询任务状态")
    @GetMapping("/tasks/{taskId}")
    public TaskStatusResponseDto getTaskStatus(@PathVariable String taskId) {
        return terraForgeCommonService.getTaskStatus(taskId);
    }

    /**
     * 查询所有任务
     */
    @ApiOperation("查询所有任务")
    @GetMapping("/tasks")
    public TaskListResponseDto getAllTasks() {
        return terraForgeCommonService.getAllTasks();
    }

    /**
     * 停止任务
     */
    @ApiOperation("停止任务")
    @PostMapping("/tasks/{taskId}/stop")
    public TaskActionResponseDto stopTask(@PathVariable String taskId) {
        return terraForgeCommonService.stopTask(taskId);
    }

    /**
     * 删除任务
     */
    @ApiOperation("删除任务")
    @DeleteMapping("/tasks/{taskId}")
    public TaskActionResponseDto deleteTask(@PathVariable String taskId) {
        return terraForgeCommonService.deleteTask(taskId);
    }

    /**
     * 清理任务
     * Cleanup tasks
     *
     * @param request 清理参数，可选，为空时使用默认策略
     * @return 清理结果
     */
    @ApiOperation("清理任务")
    @PostMapping("/tasks/cleanup")
    public String cleanupTasks(@RequestBody(required = false) TaskCleanupRequestDto request) {
        if (request == null) {
            // 使用默认策略
            return terraForgeCommonService.cleanupTasks();
        } else {
            // 使用指定参数
            return terraForgeCommonService.cleanupTasks(request);
        }
    }

    /**
     * 更新Layer.json
     */
    @ApiOperation("更新Layer.json")
    @PostMapping("/terrain/layer")
    public String updateLayerJson(@RequestBody UpdateLayerRequestDto request) {
        return terraForgeCommonService.updateLayerJson(request);
    }

    /**
     * 解压地形文件
     */
    @ApiOperation("解压地形文件")
    @PostMapping("/terrain/decompress")
    public String decompressTerrain(@RequestBody DecompressRequestDto request) {
        return terraForgeCommonService.decompressTerrain(request);
    }

    // ========== 工作空间管理接口 ==========

    /**
     * 创建工作空间文件夹
     */
    @ApiOperation("创建工作空间文件夹")
    @PostMapping("/workspace/createFolder")
    public WorkspaceCreateFolderResponseDto createWorkspaceFolder(@RequestBody WorkspaceCreateFolderRequestDto request) {
        return terraForgeCommonService.createWorkspaceFolder(request);
    }

    /**
     * 删除工作空间文件
     */
    @ApiOperation("删除工作空间文件")
    @DeleteMapping("/workspace/file/**")
    public WorkspaceDeleteResponseDto deleteWorkspaceFile(HttpServletRequest request) {
        String filePath = extractPathFromRequest(request, "/workspace/file/");
        log.debug("删除文件路径: {}", filePath);
        return terraForgeCommonService.deleteWorkspaceFile(filePath);
    }

    /**
     * 删除工作空间文件夹
     */
    @ApiOperation("删除工作空间文件夹")
    @DeleteMapping("/workspace/folder/**")
    public WorkspaceDeleteResponseDto deleteWorkspaceFolder(HttpServletRequest request) {
        String folderPath = extractPathFromRequest(request, "/workspace/folder/");
        log.debug("删除文件夹路径: {}", folderPath);
        return terraForgeCommonService.deleteWorkspaceFolder(folderPath);
    }

    /**
     * 重命名工作空间文件夹
     */
    @ApiOperation("重命名工作空间文件夹")
    @PutMapping("/workspace/folder/**")
    public WorkspaceRenameResponseDto renameWorkspaceFolder(
            HttpServletRequest request, 
            @RequestBody WorkspaceRenameRequestDto renameRequest) {
        String folderPath = extractPathFromRequest(request, "/workspace/folder/");
        return terraForgeCommonService.renameWorkspaceFolder(folderPath, renameRequest);
    }

    /**
     * 重命名工作空间文件
     */
    @ApiOperation("重命名工作空间文件")
    @PutMapping("/workspace/file/**")
    public WorkspaceRenameResponseDto renameWorkspaceFile(
            HttpServletRequest request, 
            @RequestBody WorkspaceRenameRequestDto renameRequest) {
        String filePath = extractPathFromRequest(request, "/workspace/file/");
        return terraForgeCommonService.renameWorkspaceFile(filePath, renameRequest);
    }

    /**
     * 移动工作空间项目
     */
    @ApiOperation("移动工作空间项目")
    @PutMapping("/workspace/move")
    public WorkspaceMoveResponseDto moveWorkspaceItem(@RequestBody WorkspaceMoveRequestDto request) {
        return terraForgeCommonService.moveWorkspaceItem(request);
    }

    // 删除工作空间信息接口
    // @ApiOperation("获取工作空间信息")
    // @GetMapping("/workspace/info")
    // public WorkspaceInfoResponseDto getWorkspaceInfo() {
    //     return terraForgeCommonService.getWorkspaceInfo();
    // }

    // ========== 分析工具接口 ==========

    /**
     * 推荐配置
     */
    @ApiOperation("推荐配置")
    @PostMapping("/config/recommend")
    public ConfigRecommendResponseDto recommendConfig(@RequestBody ConfigRecommendRequestDto request) {
        try {
            log.info("调用TerraForge推荐配置API: {}", configRecommendUrl);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<ConfigRecommendRequestDto> entity = new HttpEntity<>(request, headers);

            // 发送POST请求
            ResponseEntity<ConfigRecommendResponseDto> response = restTemplate.postForEntity(
                configRecommendUrl, entity, ConfigRecommendResponseDto.class);
            
            log.info("TerraForge推荐配置API调用成功");
            return response.getBody();
            
        } catch (Exception e) {
            log.error("调用TerraForge推荐配置API失败", e);
            ConfigRecommendResponseDto errorResponse = new ConfigRecommendResponseDto();
            errorResponse.setSuccess(false);
            return errorResponse;
        }
    }

    /**
     * 获取API路由列表
     */
    @ApiOperation("获取API路由列表")
    @GetMapping("/routes")
    public String getRoutes() {
        try {
            log.info("调用TerraForge获取API路由列表API: {}", routesUrl);

            // 发送GET请求
            ResponseEntity<String> response = restTemplate.getForEntity(routesUrl, String.class);
            
            log.info("TerraForge获取API路由列表API调用成功");
            return response.getBody();
            
        } catch (Exception e) {
            log.error("调用TerraForge获取API路由列表API失败", e);
            return "获取API路由列表失败: " + e.getMessage();
        }
    }

    /**
     * 容器更新
     */
    @ApiOperation("容器更新")
    @PostMapping("/container/update")
    public String updateContainer(@RequestBody String request) {
        try {
            log.info("调用TerraForge容器更新API: {}", containerUpdateUrl);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(request, headers);

            // 发送POST请求
            ResponseEntity<String> response = restTemplate.postForEntity(containerUpdateUrl, entity, String.class);
            
            log.info("TerraForge容器更新API调用成功");
            return response.getBody();
            
        } catch (Exception e) {
            log.error("调用TerraForge容器更新API失败", e);
            return "容器更新失败: " + e.getMessage();
        }
    }

    /**
     * 从请求中提取路径的辅助方法
     */
    private String extractPathFromRequest(HttpServletRequest request, String prefix) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
        String servletPath = contextPath + "/api" + prefix;

        String path = "";
        if (requestURI.startsWith(servletPath)) {
            path = requestURI.substring(servletPath.length());
        } else {
            String[] parts = requestURI.split(prefix);
            if (parts.length > 1) {
                path = parts[1];
            }
        }
        // URL解码
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (Exception e) {
            log.warn("URL解码失败: {}", path, e);
        }
        log.info("extractPathFromRequest 提取到的 path: {}", path);
        return path;
    }
}
