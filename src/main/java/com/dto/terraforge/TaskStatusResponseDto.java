package com.dto.terraforge;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 任务状态响应DTO - 与Python API对齐
 * @date 2025/7/1 10:00:00
 */
@Data
public class TaskStatusResponseDto {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 任务状态："running"/"completed"/"failed"/"stopped"
     */
    private String status;
    
    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;
    
    /**
     * 任务状态消息
     */
    private String message;
    
    /**
     * 任务开始时间
     */
    private String startTime;
    
    /**
     * 任务结束时间（如果已完成）
     */
    private String endTime;
    
    /**
     * 当前所处的阶段
     */
    private String currentStage;
    
    /**
     * 过程记录列表
     */
    private List<ProcessLog> processLog;
    
    /**
     * 详细进度信息
     */
    private ProgressDetails progressDetails;
    
    /**
     * 任务结果（如果已完成）
     */
    private Map<String, Object> result;
    
    @Data
    public static class ProcessLog {
        /**
         * 阶段名称
         */
        private String stage;
        
        /**
         * 阶段状态："completed"/"failed"/"running"
         */
        private String status;
        
        /**
         * 阶段描述信息
         */
        private String message;
        
        /**
         * 阶段完成时间(ISO格式)
         */
        private String timestamp;
        
        /**
         * 阶段完成时的进度百分比
         */
        private Integer progress;
        
        /**
         * 文件相关信息（如果有）
         */
        private Map<String, Object> fileInfo;
    }
    
    @Data
    public static class ProgressDetails {
        /**
         * 当前已生成瓦片数
         */
        private Long currentTiles;
        
        /**
         * 预计总瓦片数
         */
        private Long estimatedTotal;
        
        /**
         * 瓦片生成速度（个/秒）
         */
        private Double tilesPerSecond;
        
        /**
         * 已用时间（秒）
         */
        private Long elapsedTime;
        
        /**
         * 预计剩余时间（秒）
         */
        private Long estimatedRemainingTime;
    }
}
