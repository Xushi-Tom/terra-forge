package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 工作空间移动响应DTO
 * @date 2025/1/2 20:00:00
 */
@Data
@ApiModel("工作空间移动响应")
public class WorkspaceMoveResponseDto {

    @ApiModelProperty(value = "响应消息", example = "移动成功")
    private String message;

    @ApiModelProperty(value = "源路径", example = "workspace/old-location/project")
    private String sourcePath;

    @ApiModelProperty(value = "目标路径", example = "workspace/new-location/project")
    private String targetPath;
} 