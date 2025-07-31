package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 任务停止响应DTO
 */
@Data
@ApiModel("任务停止响应")
public class TaskStopResponseDto {

    @ApiModelProperty(value = "操作结果消息", example = "任务已停止")
    private String message;

    @ApiModelProperty(value = "任务ID", example = "terrain_1751266765")
    private String taskId;

    @ApiModelProperty(value = "任务状态", example = "stopped")
    private String status;

    @ApiModelProperty(value = "停止时间", example = "2025-01-01 16:45:00")
    private String stoppedAt;
} 