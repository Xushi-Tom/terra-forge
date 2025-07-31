package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 工作空间移动请求DTO
 * @date 2025/1/2 20:00:00
 */
@Data
@ApiModel("工作空间移动请求")
public class WorkspaceMoveRequestDto {

    @ApiModelProperty(value = "源路径", required = true, example = "workspace/old-location/project")
    private String sourcePath;

    @ApiModelProperty(value = "目标父路径", required = true, example = "workspace/new-location")
    private String targetParentPath;
} 