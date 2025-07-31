package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 工作空间重命名响应DTO
 * @date 2025/1/2 20:00:00
 */
@Data
@ApiModel("工作空间重命名响应")
public class WorkspaceRenameResponseDto {

    @ApiModelProperty(value = "响应消息", example = "文件夹重命名成功")
    private String message;

    @ApiModelProperty(value = "原路径", example = "workspace/projects/old-name")
    private String oldPath;

    @ApiModelProperty(value = "新路径", example = "workspace/projects/new-name")
    private String newPath;

    @ApiModelProperty(value = "新名称", example = "new-name")
    private String newName;
} 