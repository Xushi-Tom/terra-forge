package com.service.impl;

import com.dto.terraforge.*;
import com.service.TerraForgeCommonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * TerraForge通用功能服务实现类
 * TerraForge Common Service Implementation
 *
 * @author xushi
 * @version 1.0
 * @project map
 * @description 实现TerraForge API的通用功能调用，包括系统状态、数据源管理、任务管理等
 * @date 2025/7/1 10:00:00
 */
@Slf4j
@Service
public class TerraForgeCommonServiceImpl implements TerraForgeCommonService {

    // TerraForge API URL配置
    @Value("${terraforge.api.health-url}")
    private String healthUrl;

    @Value("${terraforge.api.system-info-url}")
    private String systemInfoUrl;

    @Value("${terraforge.api.datasources-url}")
    private String datasourcesUrl;

    @Value("${terraforge.api.datasources-info-url}")
    private String datasourcesInfoUrl;

    @Value("${terraforge.api.results-url}")
    private String resultsUrl;

    @Value("${terraforge.api.tasks-url}")
    private String tasksUrl;

    @Value("${terraforge.api.task-stop-url}")
    private String taskStopUrl;

    @Value("${terraforge.api.task-delete-url}")
    private String taskDeleteUrl;

    @Value("${terraforge.api.task-cleanup-url}")
    private String taskCleanupUrl;

    @Value("${terraforge.api.terrain-layer-url}")
    private String terrainLayerUrl;

    @Value("${terraforge.api.terrain-decompress-url}")
    private String terrainDecompressUrl;

    @Value("${terraforge.api.terrain-tile-url}")
    private String terrainTileUrl;

    @Value("${terraforge.api.workspace-create-folder-url}")
    private String workspaceCreateFolderUrl;

    @Value("${terraforge.api.workspace-folder-url}")
    private String workspaceFolderUrl;

    @Value("${terraforge.api.workspace-file-url}")
    private String workspaceFileUrl;

    @Value("${terraforge.api.workspace-move-url}")
    private String workspaceMoveUrl;

    @Value("${terraforge.api.workspace-info-url}")
    private String workspaceInfoUrl;

    @Value("${terraforge.api.tile-convert-url}")
    private String tileConvertUrl;

    @Value("${terraforge.api.tiles-nodata-scan-url}")
    private String tilesNodataScanUrl;

    @Value("${terraforge.api.tiles-nodata-delete-url}")
    private String tilesNodataDeleteUrl;



    /**
     * REST 模板，用于HTTP请求
     * REST template for HTTP requests
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 获取健康状态
     * Get health status
     */
    @Override
    public HealthResponseDto getHealth() {
        try {
            log.debug("调用TerraForge健康检查API: {}", healthUrl);

            ResponseEntity<HealthResponseDto> response = restTemplate.getForEntity(healthUrl, HealthResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge健康检查API失败", e);

            // 返回错误响应
            HealthResponseDto errorResponse = new HealthResponseDto();
            errorResponse.setStatus("error");
            errorResponse.setTimestamp(java.time.LocalDateTime.now().toString());
            errorResponse.setVersion("unknown");
            return errorResponse;
        }
    }

    /**
     * 获取系统信息
     * Get system information
     */
    @Override
    public SystemInfoResponseDto getSystemInfo() {
        try {
            log.debug("调用TerraForge系统信息API: {}", systemInfoUrl);

            ResponseEntity<SystemInfoResponseDto> response = restTemplate.getForEntity(systemInfoUrl, SystemInfoResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge系统信息API失败", e);
            return new SystemInfoResponseDto();
        }
    }

    @Override
    public DataSourceResponseDto browseDatasources(String path) {
        return browseDatasources(path, null);
    }

    /**
     * 浏览数据源目录（支持地理范围筛选）
     * Browse data sources directory with geographic bounds filtering
     */
    @Override
    public DataSourceResponseDto browseDatasources(String path, String bounds) {
        try {
            // 构建数据源浏览API URL
            String url = datasourcesUrl;

            // 添加路径参数
            if (path != null && !path.isEmpty()) {
                url += "/" + path;
            }

            // 添加地理范围筛选参数
            if (bounds != null && !bounds.isEmpty()) {
                url += "?bounds=" + java.net.URLEncoder.encode(bounds, "UTF-8");
            }

            log.debug("调用TerraForge数据源浏览API: {}", url);
            ResponseEntity<DataSourceResponseDto> response = restTemplate.getForEntity(url, DataSourceResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge数据源浏览API失败", e);
            return new DataSourceResponseDto();
        }
    }

    /**
     * 获取数据源详情和智能配置推荐
     * Get data source information and intelligent configuration recommendations
     */
    @Override
    public DataSourceInfoResponseDto getDatasourceInfo(String filename, String tileType) {
        try {
            String url = datasourcesInfoUrl + "/" + filename;
            if (tileType != null && !tileType.isEmpty()) {
                url += "?tileType=" + tileType;
            }
            log.info("下游API请求URL: {}", url);
            ResponseEntity<DataSourceInfoResponseDto> response = restTemplate.getForEntity(url, DataSourceInfoResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge数据源详情API失败", e);
            return new DataSourceInfoResponseDto();
        }
    }

    /**
     * 浏览结果目录
     * Browse results directory
     */
    @Override
    public ResultsResponseDto browseResults(String path) {
        try {
            // 构建结果浏览API URL，使用查询参数而不是路径参数
            String url = resultsUrl;

            // 添加路径查询参数，Python 接口只支持 ?path= 方式
            if (path != null && !path.isEmpty()) {
                url += "?path=" + path;
            }

            log.debug("调用TerraForge结果浏览API: {}", url);
            ResponseEntity<ResultsResponseDto> response = restTemplate.getForEntity(url, ResultsResponseDto.class);
            log.debug("TerraForge结果浏览API响应: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge结果浏览API失败", e);
            ResultsResponseDto errorResponse = new ResultsResponseDto();
            errorResponse.setCurrentPath(path);
            errorResponse.setParentPath(null);
            errorResponse.setBaseType("results");
            errorResponse.setDirectories(new java.util.ArrayList<>());
            errorResponse.setFiles(new java.util.ArrayList<>());
            errorResponse.setTotalDirectories(0);
            errorResponse.setTotalFiles(0);
            return errorResponse;
        }
    }

    /**
     * 查询任务状态（包含最后20条处理日志）
     * Get task status (with last 20 process logs)
     */
    @Override
    public TaskStatusResponseDto getTaskStatus(String taskId) {
        try {
            // 构建任务状态查询API URL
            String url = tasksUrl + "/" + taskId;

            log.debug("调用TerraForge任务状态API: {}", url);
            
            // 获取原始数据
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> rawData = response.getBody();
            
            if (rawData == null) {
                TaskStatusResponseDto errorResponse = new TaskStatusResponseDto();
                errorResponse.setTaskId(taskId);
                errorResponse.setStatus("error");
                errorResponse.setMessage("获取任务状态失败");
                return errorResponse;
            }
            
            // 手动构建TaskStatusResponseDto，限制processLog为最后20条
            TaskStatusResponseDto result = new TaskStatusResponseDto();
            result.setTaskId(taskId);
            result.setStatus((String) rawData.get("status"));
            result.setProgress((Integer) rawData.get("progress"));
            result.setMessage((String) rawData.get("message"));
            result.setStartTime((String) rawData.get("startTime"));
            result.setEndTime((String) rawData.get("endTime"));
            result.setCurrentStage((String) rawData.get("currentStage"));
            result.setResult((Map<String, Object>) rawData.get("result"));
            
            // 处理processLog，只保留最后20条
            List<Map<String, Object>> rawProcessLog = (List<Map<String, Object>>) rawData.get("processLog");
            if (rawProcessLog != null && !rawProcessLog.isEmpty()) {
                // 只取最后20条
                int startIndex = Math.max(0, rawProcessLog.size() - 20);
                List<Map<String, Object>> limitedLogs = rawProcessLog.subList(startIndex, rawProcessLog.size());
                
                List<TaskStatusResponseDto.ProcessLog> processLogs = new java.util.ArrayList<>();
                for (Map<String, Object> logData : limitedLogs) {
                    TaskStatusResponseDto.ProcessLog processLog = new TaskStatusResponseDto.ProcessLog();
                    processLog.setStage((String) logData.get("stage"));
                    processLog.setStatus((String) logData.get("status"));
                    processLog.setMessage((String) logData.get("message"));
                    processLog.setTimestamp((String) logData.get("timestamp"));
                    processLog.setProgress((Integer) logData.get("progress"));
                    processLog.setFileInfo((Map<String, Object>) logData.get("fileInfo"));
                    processLogs.add(processLog);
                }
                result.setProcessLog(processLogs);
            }
            
            return result;
        } catch (Exception e) {
            log.error("调用TerraForge任务状态API失败", e);
            TaskStatusResponseDto errorResponse = new TaskStatusResponseDto();
            errorResponse.setTaskId(taskId);
            errorResponse.setStatus("error");
            errorResponse.setMessage("获取任务状态失败");
            return errorResponse;
        }
    }

    /**
     * 查询所有任务（简化信息，不包含处理日志）
     * Get all tasks (simplified info, without process logs)
     */
    /**
     * 安全转换数字类型为Double，兼容Integer和Double
     */
    private Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * 从任务ID中提取时间戳
     */
    private long extractTimestamp(String taskId) {
        try {
            // 任务ID格式通常是: taskType + timestamp
            String timestampStr = taskId.replaceAll("^[a-zA-Z]+", "");
            return Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public TaskListResponseDto getAllTasks() {
        try {
            log.debug("调用TerraForge任务列表API: {}", tasksUrl);

            // 调用Python API获取原始数据
            ResponseEntity<Map> response = restTemplate.getForEntity(tasksUrl, Map.class);
            Map<String, Object> rawData = response.getBody();
            
            if (rawData == null) {
                return new TaskListResponseDto();
            }
            
            TaskListResponseDto result = new TaskListResponseDto();
            result.setCount((Integer) rawData.get("count"));
            
            // 处理任务数据，移除processLog，并限制为最近50条
            Map<String, Object> rawTasks = (Map<String, Object>) rawData.get("tasks");
            if (rawTasks != null) {
                // 先按时间戳排序，保留最近的50条任务
                List<Map.Entry<String, Object>> sortedEntries = rawTasks.entrySet().stream()
                    .sorted((e1, e2) -> {
                        // 提取任务ID中的时间戳进行比较
                        long timestamp1 = extractTimestamp(e1.getKey());
                        long timestamp2 = extractTimestamp(e2.getKey());
                        return Long.compare(timestamp2, timestamp1); // 降序，最新的在前
                    })
                    .limit(50) // 限制为最近50条
                    .collect(java.util.stream.Collectors.toList());
                
                Map<String, TaskListResponseDto.TaskInfo> cleanTasks = new java.util.HashMap<>();
                
                for (Map.Entry<String, Object> entry : sortedEntries) {
                    String taskId = entry.getKey();
                    Map<String, Object> taskData = (Map<String, Object>) entry.getValue();
                    
                    // 创建TaskInfo但不包含processLog
                    TaskListResponseDto.TaskInfo taskInfo = new TaskListResponseDto.TaskInfo();
                    taskInfo.setTaskId(taskId);
                    taskInfo.setStatus((String) taskData.get("status"));
                    taskInfo.setProgress((Integer) taskData.get("progress"));
                    taskInfo.setMessage((String) taskData.get("message"));
                    taskInfo.setStartTime((String) taskData.get("startTime"));
                    taskInfo.setEndTime((String) taskData.get("endTime"));
                    taskInfo.setCurrentStage((String) taskData.get("currentStage"));
                    taskInfo.setResult(taskData.get("result"));
                    
                    // 处理文件信息
                    Map<String, Object> filesData = (Map<String, Object>) taskData.get("files");
                    if (filesData != null) {
                        TaskListResponseDto.FileInfo fileInfo = new TaskListResponseDto.FileInfo();
                        fileInfo.setCompleted((Integer) filesData.get("completed"));
                        fileInfo.setCurrent((String) filesData.get("current"));
                        fileInfo.setFailed((Integer) filesData.get("failed"));
                        fileInfo.setTotal((Integer) filesData.get("total"));
                        taskInfo.setFiles(fileInfo);
                    }
                    
                    // 处理统计信息
                    Map<String, Object> statsData = (Map<String, Object>) taskData.get("stats");
                    if (statsData != null) {
                        TaskListResponseDto.Stats stats = new TaskListResponseDto.Stats();
                        // 安全转换数字类型，兼容Integer和Double
                        stats.setAverageSpeed(convertToDouble(statsData.get("averageSpeed")));
                        stats.setCurrentSpeed(convertToDouble(statsData.get("currentSpeed")));
                        stats.setProcessedTiles((Integer) statsData.get("processedTiles"));
                        stats.setTotalTiles((Integer) statsData.get("totalTiles"));
                        stats.setSuccessRate((String) statsData.get("successRate"));
                        taskInfo.setStats(stats);
                    }
                    
                    // 不设置processLog，保持为null，减少数据传输
                    taskInfo.setProcessLog(null);
                    
                    cleanTasks.put(taskId, taskInfo);
                }
                
                result.setTasks(cleanTasks);
            }
            
            return result;
        } catch (Exception e) {
            log.error("调用TerraForge任务列表API失败", e);
            TaskListResponseDto emptyResult = new TaskListResponseDto();
            emptyResult.setTasks(new java.util.HashMap<>());
            emptyResult.setCount(0);
            return emptyResult;
        }
    }

    /**
     * 更新Layer.json文件
     * Update Layer.json file
     */
    @Override
    public String updateLayerJson(UpdateLayerRequestDto request) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<UpdateLayerRequestDto> entity = new HttpEntity<>(request, headers);

            log.debug("调用TerraForge更新Layer.json API: {}", terrainLayerUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(terrainLayerUrl, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge更新Layer.json API失败", e);
            return "更新Layer.json失败: " + e.getMessage();
        }
    }

    /**
     * 解压地形文件
     * Decompress terrain files
     */
    @Override
    public String decompressTerrain(DecompressRequestDto request) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<DecompressRequestDto> entity = new HttpEntity<>(request, headers);

            log.debug("调用TerraForge解压地形文件API: {}", terrainDecompressUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(terrainDecompressUrl, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge解压地形文件API失败", e);
            return "解压地形文件失败: " + e.getMessage();
        }
    }

    /**
     * 停止任务
     * Stop task
     */
    @Override
    public TaskActionResponseDto stopTask(String taskId) {
        try {
            // 构建停止任务API URL
            String url = taskStopUrl.replace("{taskId}", taskId);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("调用TerraForge停止任务API: {}", url);
            ResponseEntity<TaskActionResponseDto> response = restTemplate.postForEntity(url, entity, TaskActionResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge停止任务API失败", e);
            TaskActionResponseDto errorResponse = new TaskActionResponseDto();
            errorResponse.setTaskId(taskId);
            errorResponse.setMessage("停止任务失败: " + e.getMessage());
            errorResponse.setSuccess(false);
            return errorResponse;
        }
    }

    /**
     * 删除任务
     * Delete task
     */
    @Override
    public TaskActionResponseDto deleteTask(String taskId) {
        try {
            // 构建删除任务API URL
            String url = taskDeleteUrl.replace("{taskId}", taskId);

            log.debug("调用TerraForge删除任务API: {}", url);
            ResponseEntity<TaskActionResponseDto> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.DELETE, null, TaskActionResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge删除任务API失败", e);
            TaskActionResponseDto errorResponse = new TaskActionResponseDto();
            errorResponse.setTaskId(taskId);
            errorResponse.setMessage("删除任务失败: " + e.getMessage());
            errorResponse.setSuccess(false);
            return errorResponse;
        }
    }

    /**
     * 清理任务（无参数，使用默认策略）
     * Cleanup tasks (no parameters, use default strategy)
     */
    @Override
    public String cleanupTasks() {
        // 使用默认清理策略：清理已完成的任务
        TaskCleanupRequestDto defaultRequest = new TaskCleanupRequestDto();
        defaultRequest.setStrategy("completed");
        return cleanupTasks(defaultRequest);
    }

    /**
     * 清理任务（带参数）
     * Cleanup tasks with parameters
     */
    @Override
    public String cleanupTasks(TaskCleanupRequestDto request) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<TaskCleanupRequestDto> entity = new HttpEntity<>(request, headers);

            log.debug("调用TerraForge清理任务API: {}", taskCleanupUrl);
            log.debug("清理参数: {}", request);

            ResponseEntity<String> response = restTemplate.postForEntity(taskCleanupUrl, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge清理任务API失败", e);
            return "清理任务失败: " + e.getMessage();
        }
    }



    /**
     * 创建地形瓦片
     * Create terrain tiles
     */
    @Override
    public TerrainTileResponseDto createTerrainTiles(TerrainTileRequestDto request) {
        try {
            // 处理outputPath：如果是字符串，转换为数组发送给Python API
            if (request.getOutputPath() != null) {
                // 创建一个新的请求对象，避免修改原对象
                TerrainTileRequestDto processedRequest = new TerrainTileRequestDto();
                processedRequest.setFolderPaths(request.getFolderPaths());
                processedRequest.setFilePatterns(request.getFilePatterns());
                processedRequest.setStartZoom(request.getStartZoom());
                processedRequest.setEndZoom(request.getEndZoom());
                processedRequest.setMaxTriangles(request.getMaxTriangles());
                processedRequest.setBounds(request.getBounds());
                processedRequest.setCompression(request.getCompression());
                processedRequest.setDecompress(request.getDecompress());
                processedRequest.setThreads(request.getThreads());
                processedRequest.setMaxMemory(request.getMaxMemory());
                processedRequest.setAutoZoom(request.getAutoZoom());
                processedRequest.setZoomStrategy(request.getZoomStrategy());
                processedRequest.setMergeTerrains(request.getMergeTerrains());
                
                // 将outputPath字符串分割为数组
                String outputPathStr = request.getOutputPath();
                java.util.List<String> outputPathArray = java.util.Arrays.asList(outputPathStr.split("/"));
                
                // 创建包含outputPath数组的Map发送给Python API
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("folderPaths", processedRequest.getFolderPaths());
                requestBody.put("filePatterns", processedRequest.getFilePatterns());
                requestBody.put("outputPath", outputPathArray);
                requestBody.put("startZoom", processedRequest.getStartZoom());
                requestBody.put("endZoom", processedRequest.getEndZoom());
                requestBody.put("maxTriangles", processedRequest.getMaxTriangles());
                requestBody.put("bounds", processedRequest.getBounds());
                requestBody.put("compression", processedRequest.getCompression());
                requestBody.put("decompress", processedRequest.getDecompress());
                requestBody.put("threads", processedRequest.getThreads());
                requestBody.put("maxMemory", processedRequest.getMaxMemory());
                requestBody.put("autoZoom", processedRequest.getAutoZoom());
                requestBody.put("zoomStrategy", processedRequest.getZoomStrategy());
                requestBody.put("mergeTerrains", processedRequest.getMergeTerrains());
                
                // 设置请求头
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                log.debug("调用TerraForge创建地形瓦片API: {}", terrainTileUrl);
                log.debug("地形切片参数: {}", requestBody);

                ResponseEntity<TerrainTileResponseDto> response = restTemplate.postForEntity(terrainTileUrl, entity, TerrainTileResponseDto.class);
                return response.getBody();
            } else {
                // 如果outputPath为空，直接发送原请求
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                HttpEntity<TerrainTileRequestDto> entity = new HttpEntity<>(request, headers);

                ResponseEntity<TerrainTileResponseDto> response = restTemplate.postForEntity(terrainTileUrl, entity, TerrainTileResponseDto.class);
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("调用TerraForge创建地形瓦片API失败", e);

            // 返回错误响应
            TerrainTileResponseDto errorResponse = new TerrainTileResponseDto();
            errorResponse.setMessage("创建地形瓦片失败: " + e.getMessage());
            return errorResponse;
        }
    }


    /**
     * 创建工作空间文件夹
     * Create workspace folder
     */
    @Override
    public WorkspaceCreateFolderResponseDto createWorkspaceFolder(WorkspaceCreateFolderRequestDto request) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<WorkspaceCreateFolderRequestDto> entity = new HttpEntity<>(request, headers);

            log.debug("调用TerraForge创建工作空间文件夹API: {}", workspaceCreateFolderUrl);
            log.debug("创建文件夹参数: {}", request);

            ResponseEntity<WorkspaceCreateFolderResponseDto> response = restTemplate.postForEntity(
                    workspaceCreateFolderUrl, entity, WorkspaceCreateFolderResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge创建工作空间文件夹API失败", e);

            // 返回错误响应
            WorkspaceCreateFolderResponseDto errorResponse = new WorkspaceCreateFolderResponseDto();
            errorResponse.setMessage("创建文件夹失败: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 删除工作空间文件
     * Delete workspace file
     */
    @Override
    public WorkspaceDeleteResponseDto deleteWorkspaceFile(String filePath) {
        try {
            // 处理文件名中的空格
            String encodedPath = filePath.replace(" ", "%20");
            String url = workspaceFileUrl + "/" + encodedPath;

            log.debug("调用TerraForge删除工作空间文件API: {}", url);
            
            // 使用 exchange 方法来获取响应
            ResponseEntity<WorkspaceDeleteResponseDto> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.DELETE,
                null,
                WorkspaceDeleteResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                WorkspaceDeleteResponseDto result = response.getBody();
                if (result == null) {
                    result = new WorkspaceDeleteResponseDto();
                    result.setMessage("文件删除成功");
                    result.setPath(filePath);
                }
                return result;
            } else {
                throw new RuntimeException("删除文件失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("调用TerraForge删除工作空间文件API失败: {}", e.getMessage());

            // 返回错误响应
            WorkspaceDeleteResponseDto errorResponse = new WorkspaceDeleteResponseDto();
            errorResponse.setMessage("删除文件失败: " + e.getMessage());
            errorResponse.setPath(filePath);
            return errorResponse;
        }
    }

    /**
     * 删除工作空间文件夹
     * Delete workspace folder
     */
    @Override
    public WorkspaceDeleteResponseDto deleteWorkspaceFolder(String folderPath) {
        try {
            // 处理文件夹名中的空格
            String encodedPath = folderPath.replace(" ", "%20");
            String url = workspaceFolderUrl + "/" + encodedPath;

            log.debug("调用TerraForge删除工作空间文件夹API: {}", url);
            
            // 使用 exchange 方法来获取响应
            ResponseEntity<WorkspaceDeleteResponseDto> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.DELETE,
                null,
                WorkspaceDeleteResponseDto.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                WorkspaceDeleteResponseDto result = response.getBody();
                if (result == null) {
                    result = new WorkspaceDeleteResponseDto();
                    result.setMessage("文件夹删除成功");
                    result.setPath(folderPath);
                }
                return result;
            } else {
                throw new RuntimeException("删除文件夹失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("调用TerraForge删除工作空间文件夹API失败: {}", e.getMessage());

            // 返回错误响应
            WorkspaceDeleteResponseDto errorResponse = new WorkspaceDeleteResponseDto();
            errorResponse.setMessage("删除文件夹失败: " + e.getMessage());
            errorResponse.setPath(folderPath);
            return errorResponse;
        }
    }

    /**
     * 重命名工作空间文件
     * Rename workspace file
     */
    @Override
    public WorkspaceRenameResponseDto renameWorkspaceFile(String filePath, WorkspaceRenameRequestDto request) {
        try {
            String url = workspaceFileUrl + "/" + filePath + "/rename";

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<WorkspaceRenameRequestDto> entity = new HttpEntity<>(request, headers);

            log.debug("调用TerraForge重命名工作空间文件API: {}", url);
            log.debug("重命名参数: {}", request);

            ResponseEntity<WorkspaceRenameResponseDto> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.PUT, entity, WorkspaceRenameResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge重命名工作空间文件API失败", e);

            // 返回错误响应
            WorkspaceRenameResponseDto errorResponse = new WorkspaceRenameResponseDto();
            errorResponse.setMessage("重命名文件失败: " + e.getMessage());
            errorResponse.setOldPath(filePath);
            return errorResponse;
        }
    }

    /**
     * 重命名工作空间文件夹
     * Rename workspace folder
     */
    @Override
    public WorkspaceRenameResponseDto renameWorkspaceFolder(String folderPath, WorkspaceRenameRequestDto request) {
        try {
            String url = workspaceFolderUrl + "/" + folderPath + "/rename";

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<WorkspaceRenameRequestDto> entity = new HttpEntity<>(request, headers);

            log.debug("调用TerraForge重命名工作空间文件夹API: {}", url);
            log.debug("重命名参数: {}", request);

            ResponseEntity<WorkspaceRenameResponseDto> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.PUT, entity, WorkspaceRenameResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge重命名工作空间文件夹API失败", e);

            // 返回错误响应
            WorkspaceRenameResponseDto errorResponse = new WorkspaceRenameResponseDto();
            errorResponse.setMessage("重命名文件夹失败: " + e.getMessage());
            errorResponse.setOldPath(folderPath);
            return errorResponse;
        }
    }

    /**
     * 移动工作空间项目
     * Move workspace item
     */
    @Override
    public WorkspaceMoveResponseDto moveWorkspaceItem(WorkspaceMoveRequestDto request) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<WorkspaceMoveRequestDto> entity = new HttpEntity<>(request, headers);

            log.debug("调用TerraForge移动工作空间项目API: {}", workspaceMoveUrl);
            log.debug("移动参数: {}", request);

            ResponseEntity<WorkspaceMoveResponseDto> response = restTemplate.exchange(
                    workspaceMoveUrl, org.springframework.http.HttpMethod.PUT, entity, WorkspaceMoveResponseDto.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge移动工作空间项目API失败", e);

            // 返回错误响应
            WorkspaceMoveResponseDto errorResponse = new WorkspaceMoveResponseDto();
            errorResponse.setMessage("移动项目失败: " + e.getMessage());
            errorResponse.setSourcePath(request.getSourcePath());
            return errorResponse;
        }
    }

    // 删除工作空间信息接口
    // @Override
    // public WorkspaceInfoResponseDto getWorkspaceInfo() {
    //     try {
    //         log.debug("调用TerraForge获取工作空间信息API: {}", workspaceInfoUrl);
    //         ResponseEntity<WorkspaceInfoResponseDto> response = restTemplate.getForEntity(workspaceInfoUrl, WorkspaceInfoResponseDto.class);
    //         return response.getBody();
    //     } catch (Exception e) {
    //         log.error("调用TerraForge获取工作空间信息API失败", e);
    //         return new WorkspaceInfoResponseDto();
    //     }
    // }

    // ========== 透明瓦片处理方法实现 ==========

    /**
     * 扫描包含透明（nodata）值的PNG瓦片
     * 调用Python服务的 /api/tiles/nodata/scan 接口
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> scanNodataTiles(String tilesPath, Boolean includeDetails) {
        try {
            log.info("调用TerraForge扫描透明瓦片API: {}", tilesNodataScanUrl);

            // 构建请求体
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("tilesPath", tilesPath);
            if (includeDetails != null) {
                requestBody.put("includeDetails", includeDetails);
            }

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送POST请求
            ResponseEntity<Map> response = restTemplate.postForEntity(tilesNodataScanUrl, entity, Map.class);
            
            Map<String, Object> result = response.getBody();
            log.info("TerraForge扫描透明瓦片API调用成功");
            return result;
            
        } catch (Exception e) {
            log.error("调用TerraForge扫描透明瓦片API失败", e);
            
            // 返回错误响应
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "扫描透明瓦片失败: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 删除包含透明（nodata）值的PNG瓦片
     * 调用Python服务的 /api/tiles/nodata/delete 接口
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> deleteNodataTiles(String tilesPath, Boolean includeDetails) {
        try {
            log.info("调用TerraForge删除透明瓦片API: {}", tilesNodataDeleteUrl);

            // 构建请求体
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("tilesPath", tilesPath);
            if (includeDetails != null) {
                requestBody.put("includeDetails", includeDetails);
            }

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送POST请求
            ResponseEntity<Map> response = restTemplate.postForEntity(tilesNodataDeleteUrl, entity, Map.class);
            
            Map<String, Object> result = response.getBody();
            log.info("TerraForge删除透明瓦片API调用成功");
            return result;
            
        } catch (Exception e) {
            log.error("调用TerraForge删除透明瓦片API失败", e);
            
            // 返回错误响应
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "删除透明瓦片失败: " + e.getMessage());
            return errorResponse;
        }
    }

    // ========== 瓦片格式转换方法实现 ==========

    /**
     * 瓦片格式转换
     * 调用Python服务的 /api/tile/convert 接口
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> convertTileFormat(String sourcePath, String targetPath, String sourceFormat, String targetFormat, Boolean overwrite) {
        try {
            log.info("调用TerraForge瓦片格式转换API: {}", tileConvertUrl);

            // 构建请求体
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("sourcePath", sourcePath);
            requestBody.put("targetPath", targetPath);
            requestBody.put("sourceFormat", sourceFormat);
            requestBody.put("targetFormat", targetFormat);
            if (overwrite != null) {
                requestBody.put("overwrite", overwrite);
            }

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送POST请求
            ResponseEntity<Map> response = restTemplate.postForEntity(tileConvertUrl, entity, Map.class);
            
            Map<String, Object> result = response.getBody();
            log.info("TerraForge瓦片格式转换API调用成功");
            return result;
            
        } catch (Exception e) {
            log.error("调用TerraForge瓦片格式转换API失败", e);
            
            // 返回错误响应
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "瓦片格式转换失败: " + e.getMessage());
            return errorResponse;
        }
    }

}
