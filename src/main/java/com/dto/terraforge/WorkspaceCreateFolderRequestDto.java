package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 工作空间创建文件夹请求DTO
 * @date 2025/1/2 20:00:00
 */
@Data
@ApiModel("工作空间创建文件夹请求")
public class WorkspaceCreateFolderRequestDto {

    @ApiModelProperty(value = "文件夹路径", required = true, example = "workspace/projects/new-project")
    private String folderPath;
} 