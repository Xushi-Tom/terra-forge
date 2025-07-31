package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 解压地形文件响应DTO
 */
@Data
@ApiModel("解压地形文件响应")
public class DecompressResponseDto {

    @ApiModelProperty(value = "操作结果消息", example = "地形文件解压完成")
    private String message;

    @ApiModelProperty(value = "地形目录路径", example = "terrain/taiwan/v1")
    private String terrainDir;

    @ApiModelProperty(value = "解压的文件数量", example = "1089")
    private Integer decompressedFiles;

    @ApiModelProperty(value = "操作状态", example = "success", allowableValues = "success,failed")
    private String status;

    @ApiModelProperty(value = "处理时间", example = "2分钟30秒")
    private String processingTime;

    @ApiModelProperty(value = "操作时间", example = "2025-01-01 16:45:00")
    private String timestamp;
} 