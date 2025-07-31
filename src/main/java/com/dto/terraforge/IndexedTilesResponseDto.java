package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 索引瓦片切片响应DTO - 与Python API对齐
 * @date 2025/7/9 10:30:00
 */
@Data
@ApiModel("索引瓦片切片响应")
public class IndexedTilesResponseDto {
    
    @ApiModelProperty(value = "任务ID，用于查询任务状态", example = "indexedTiles1752666673")
    private String taskId;
    
    @ApiModelProperty(value = "任务启动消息", example = "瓦片索引切片任务已启动，将处理 3 个文件")
    private String message;
    
    @ApiModelProperty(value = "任务状态查询URL", example = "/api/tasks/indexedTiles1752666673")
    private String statusUrl;
    
    @ApiModelProperty(value = "处理方法", example = "瓦片索引精确切片")
    private String method;
    
    @ApiModelProperty(value = "索引信息")
    private IndexInfo indexInfo;
    
    @ApiModelProperty(value = "处理信息")
    private ProcessingInfo processingInfo;

    /**
     * 索引信息
     */
    @Data
    @ApiModel("索引信息")
    public static class IndexInfo {
        @ApiModelProperty(value = "总文件数", example = "3")
        private Integer totalFiles;

        @ApiModelProperty(value = "缩放级别范围", example = "1-8")
        private String zoomLevels;

        @ApiModelProperty(value = "瓦片大小", example = "256")
        private Integer tileSize;

        @ApiModelProperty(value = "是否生成SHP索引", example = "true")
        private Boolean generateShpIndex;

        @ApiModelProperty(value = "是否启用增量更新", example = "false")
        private Boolean enableIncrementalUpdate;

        @ApiModelProperty(value = "是否跳过透明瓦片", example = "false")
        private Boolean skipNodataTiles;

        @ApiModelProperty(value = "透明度阈值", example = "0.1")
        private Double transparencyThreshold;
    }

    /**
     * 处理信息
     */
    @Data
    @ApiModel("处理信息")
    public static class ProcessingInfo {
        @ApiModelProperty(value = "输出路径数组", example = "[\"indexed_test_java\"]")
        private List<String> outputPathArray;

        @ApiModelProperty(value = "进程数", example = "4")
        private Integer processes;

        @ApiModelProperty(value = "最大内存", example = "8g")
        private String maxMemory;

        @ApiModelProperty(value = "重采样方法", example = "near")
        private String resampling;
    }
} 