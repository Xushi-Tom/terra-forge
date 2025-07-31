package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 工作空间信息响应DTO - 与Python API对齐
 * @date 2025/1/2 20:00:00
 */
@Data
@ApiModel("工作空间信息响应")
public class WorkspaceInfoResponseDto {

    @ApiModelProperty(value = "请求是否成功", example = "true")
    private Boolean success;

    @ApiModelProperty(value = "工作空间信息")
    private WorkspaceInfo workspaceInfo;

    @Data
    @ApiModel("工作空间信息")
    public static class WorkspaceInfo {
        @ApiModelProperty(value = "基础路径", example = "/app/tiles")
        private String basePath;

        @ApiModelProperty(value = "总大小（字节）", example = "50438742016")
    private Long totalSize;

        @ApiModelProperty(value = "总大小（格式化）", example = "46.98 GB")
    private String totalSizeFormatted;

        @ApiModelProperty(value = "总文件数", example = "1247")
        private Integer totalFiles;

        @ApiModelProperty(value = "总目录数", example = "23")
        private Integer totalDirectories;

        @ApiModelProperty(value = "最后更新时间", example = "2025-01-08T15:45:00")
        private String lastUpdated;
    }
} 