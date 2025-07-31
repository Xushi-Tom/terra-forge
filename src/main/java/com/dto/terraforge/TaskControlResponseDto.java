package com.dto.terraforge;

import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 任务控制响应DTO
 * @date 2025/7/1 10:00:00
 */
@Data
public class TaskControlResponseDto {
    
    /**
     * 操作结果消息
     */
    private String message;
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 操作状态
     */
    private String status;
    
    /**
     * 操作时间戳
     */
    private String timestamp;
}
