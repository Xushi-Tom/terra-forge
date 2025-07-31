package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 任务操作响应DTO - 与Python API对齐
 * @date 2025/7/16 11:50:00
 */
@Data
@ApiModel("任务操作响应")
public class TaskActionResponseDto {
    
    @ApiModelProperty(value = "任务ID", example = "indexedTiles1752666673")
    private String taskId;
    
    @ApiModelProperty(value = "操作消息", example = "任务已停止")
    private String message;
    
    @ApiModelProperty(value = "操作是否成功", example = "true")
    private Boolean success;
} 