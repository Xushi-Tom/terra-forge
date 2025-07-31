package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 任务清理响应DTO
 */
@Data
@ApiModel("任务清理响应")
public class TaskCleanupResponseDto {

    @ApiModelProperty(value = "操作结果消息", example = "任务清理完成")
    private String message;

    @ApiModelProperty(value = "使用的清理策略", example = "completed", allowableValues = "count,all,completed,failed")
    private String strategy;

    @ApiModelProperty(value = "清理的任务数量", example = "15")
    private Integer cleanupCount;

    @ApiModelProperty(value = "剩余任务数量", example = "85")
    private Integer remainingTasks;

    @ApiModelProperty(value = "清理时间", example = "2025-01-01 16:45:00")
    private String cleanupTime;
} 