package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 地形瓦片创建响应DTO - 与Python API对齐
 * @date 2025/7/1 10:00:00
 */
@Data
@ApiModel("地形切片响应")
public class TerrainTileResponseDto {
    
    @ApiModelProperty(value = "任务ID，用于查询任务状态", example = "terrain1752666579")
    private String taskId;
    
    @ApiModelProperty(value = "任务启动消息", example = "地形切片任务已启动，将处理 1 个文件")
    private String message;
    
    @ApiModelProperty(value = "任务状态查询URL", example = "/api/tasks/terrain1752666579")
    private String statusUrl;
    
    @ApiModelProperty(value = "请求是否成功", example = "true")
    private Boolean success;
    
    @ApiModelProperty(value = "任务参数信息")
    private Parameters parameters;

    /**
     * 任务参数信息
     */
    @Data
    @ApiModel("任务参数信息")
    public static class Parameters {
        @ApiModelProperty(value = "最大内存", example = "8m")
        private String maxMemory;

        @ApiModelProperty(value = "输出路径数组", example = "[\"terrain_test_java\"]")
        private List<String> outputPath;

        @ApiModelProperty(value = "线程数", example = "4")
        private Integer threads;

        @ApiModelProperty(value = "总文件数", example = "1")
        private Integer totalFiles;

        @ApiModelProperty(value = "任务类型", example = "terrain")
        private String type;

        @ApiModelProperty(value = "缩放级别范围", example = "0-8")
        private String zoomRange;
    }
}
