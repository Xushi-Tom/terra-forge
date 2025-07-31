package com.dto.terraforge;

import lombok.Data;

/**
 * 任务清理请求DTO
 * Task Cleanup Request DTO
 * 
 * @author xushi
 * @version 1.0
 * @project map
 * @description 用于任务清理API的请求参数
 * @date 2025/7/1 12:00:00
 */
@Data
public class TaskCleanupRequestDto {
    
    /**
     * 清理策略
     * Cleanup strategy
     * 
     * 可选值：
     * - "completed": 清理已完成的任务
     * - "failed": 清理失败的任务
     * - "all": 清理所有任务
     * - "count": 按数量清理，保留最新的指定数量任务
     */
    private String strategy = "completed";
    
    /**
     * 保留任务数量（当strategy为"count"时使用）
     * Number of tasks to keep (used when strategy is "count")
     */
    private Integer keepCount;
    
    /**
     * 是否强制清理（删除正在运行的任务）
     * Force cleanup (delete running tasks)
     */
    private Boolean force = false;
}
