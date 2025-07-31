package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 地图瓦片创建响应DTO
 * @date 2025/7/1 10:00:00
 */
@Data
@ApiModel("地图切片响应")
public class MapTileResponseDto {
    
    @ApiModelProperty(value = "任务ID，用于查询任务状态", example = "map_1751419460")
    private String taskId;
    
    @ApiModelProperty(value = "任务启动消息", example = "地图瓦片任务已启动")
    private String message;
    
    @ApiModelProperty(value = "任务状态查询URL", example = "/api/tasks/map_1751419460")
    private String statusUrl;
    
    @ApiModelProperty(value = "输出目录相对路径", example = "map/test")
    private String outputDir;
    
    @ApiModelProperty(value = "输出目录完整路径", example = "/app/tiles/map/test")
    private String outputFullPath;
    
    @ApiModelProperty(value = "输出路径数组", example = "[\"map\", \"test\"]")
    private List<String> outputPathArray;
    
    @ApiModelProperty(value = "级别范围说明", example = "0-10")
    private String zoomLevels;
    
    @ApiModelProperty(value = "使用的缩放策略", example = "conservative")
    private String zoomStrategy;
    
    @ApiModelProperty(value = "智能分级信息（如果启用）")
    private Object zoomDetection;
    
    @ApiModelProperty(value = "优化配置信息")
    private OptimizationConfig optimizations;

    /**
     * 优化配置信息
     */
    @Data
    @ApiModel("优化配置信息")
    public static class OptimizationConfig {
        @ApiModelProperty(value = "是否启用文件优化", example = "true")
        private Boolean fileOptimization;

        @ApiModelProperty(value = "是否启用概览创建", example = "true")
        private Boolean overviewCreation;

        @ApiModelProperty(value = "是否启用分级处理模式", example = "true")
        private Boolean optimizedMode;

        @ApiModelProperty(value = "使用的进程数", example = "2")
        private Integer processes;

        @ApiModelProperty(value = "使用的线程数", example = "2")
        private Integer threads;

        @ApiModelProperty(value = "使用的最大内存", example = "4g")
        private String maxMemory;
    }
}
