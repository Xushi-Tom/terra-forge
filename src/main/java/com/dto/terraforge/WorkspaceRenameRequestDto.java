package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 工作空间重命名请求DTO
 * @date 2025/1/2 20:00:00
 */
@Data
@ApiModel("工作空间重命名请求")
public class WorkspaceRenameRequestDto {

    @ApiModelProperty(value = "新名称", required = true, example = "renamed-project")
    private String newName;
} 