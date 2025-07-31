package com.dto.terraforge;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 任务列表响应DTO - 与Python API对齐，前端忽略processLog
 * @date 2025/7/1 10:00:00
 */
@Data
public class TaskListResponseDto {
    
    /**
     * 任务列表 - 完整映射Python API结构，前端选择性使用
     */
    private Map<String, TaskInfo> tasks;
    
    /**
     * 任务总数
     */
    private Integer count;
    
    @Data
    public static class TaskInfo {
        /**
         * 任务ID
         */
        private String taskId;
        
        /**
         * 任务状态
         */
        private String status;
        
        /**
         * 进度百分比
         */
        private Integer progress;
        
        /**
         * 状态消息
         */
        private String message;
        
        /**
         * 开始时间
         */
        private String startTime;
        
        /**
         * 结束时间（如果已完成）
         */
        private String endTime;
        
        /**
         * 当前阶段
         */
        private String currentStage;
        
        /**
         * 文件处理信息
         */
        private FileInfo files;
        
        /**
         * 任务结果
         */
        private Object result;
        
        /**
         * 处理日志 - 前端任务列表忽略此字段
         */
        private List<ProcessLog> processLog;
        
        /**
         * 统计信息
         */
        private Stats stats;
    }
    
    @Data
    public static class FileInfo {
        private Integer completed;
        private String current;
        private Integer failed;
        private Integer total;
    }
    
    @Data
    public static class ProcessLog {
        private String message;
        private Integer progress;
        private String stage;
        private String status;
        private String timestamp;
        private Object fileInfo;
        private List<String> errors;
        private Object summary;
    }
    
    @Data
    public static class Stats {
        private Double averageSpeed;
        private Double currentSpeed;
        private Integer deletedNodataTiles;
        private String estimatedTimeRemaining;
        private Long estimatedTimeRemainingSeconds;
        private Integer failedTiles;
        private Integer processedTiles;
        private Integer remainingTiles;
        private String successRate;
        private String totalProcessingTime;
        private Integer totalTiles;
        private Integer batchesCompleted;
        private Integer totalBatches;
    }
}
