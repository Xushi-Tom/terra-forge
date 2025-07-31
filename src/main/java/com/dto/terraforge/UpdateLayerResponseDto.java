package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 更新Layer.json响应DTO
 */
@Data
@ApiModel("更新Layer.json响应")
public class UpdateLayerResponseDto {

    @ApiModelProperty(value = "操作结果消息", example = "Layer.json更新完成")
    private String message;

    @ApiModelProperty(value = "地形目录路径", example = "terrain/taiwan/v1")
    private String terrainDir;

    @ApiModelProperty(value = "Layer.json文件路径", example = "/app/tiles/terrain/taiwan/v1/layer.json")
    private String layerJsonPath;

    @ApiModelProperty(value = "操作状态", example = "success", allowableValues = "success,failed")
    private String status;

    @ApiModelProperty(value = "操作时间", example = "2025-01-01 16:45:00")
    private String timestamp;
} 