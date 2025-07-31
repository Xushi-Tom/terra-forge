package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 任务删除响应DTO
 */
@Data
@ApiModel("任务删除响应")
public class TaskDeleteResponseDto {

    @ApiModelProperty(value = "操作结果消息", example = "任务删除成功")
    private String message;

    @ApiModelProperty(value = "任务ID", example = "terrain_1751266765")
    private String taskId;

    @ApiModelProperty(value = "被删除的任务信息")
    private DeletedTaskInfo deletedTask;

    /**
     * 被删除的任务信息
     */
    @Data
    @ApiModel("被删除的任务信息")
    public static class DeletedTaskInfo {
        @ApiModelProperty(value = "任务状态", example = "completed")
        private String status;

        @ApiModelProperty(value = "进度百分比", example = "100")
        private Integer progress;

        @ApiModelProperty(value = "状态消息", example = "地形瓦片创建完成")
        private String message;

        @ApiModelProperty(value = "开始时间", example = "2024-06-30 16:45:00")
        private String startTime;

        @ApiModelProperty(value = "结束时间", example = "2024-06-30 16:48:30")
        private String endTime;
    }
} 