package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 工作空间删除响应DTO
 * @date 2025/1/2 20:00:00
 */
@Data
@ApiModel("工作空间删除响应")
public class WorkspaceDeleteResponseDto {

    @ApiModelProperty(value = "响应消息", example = "文件夹删除成功")
    private String message;

    @ApiModelProperty(value = "删除的路径", example = "workspace/projects/old-project")
    private String path;
} 