package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 工作空间创建文件夹响应DTO
 * @date 2025/1/2 20:00:00
 */
@Data
@ApiModel("工作空间创建文件夹响应")
public class WorkspaceCreateFolderResponseDto {

    @ApiModelProperty(value = "响应消息", example = "文件夹创建成功")
    private String message;

    @ApiModelProperty(value = "创建的路径", example = "workspace/projects/new-project")
    private String path;

    @ApiModelProperty(value = "文件夹名称", example = "new-project")
    private String name;
} 