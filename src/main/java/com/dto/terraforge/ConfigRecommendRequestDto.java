package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 配置推荐请求DTO
 * @date 2025/7/24 12:00:00
 */
@Data
@ApiModel("配置推荐请求")
public class ConfigRecommendRequestDto {
    
    @ApiModelProperty(value = "源文件名", required = true, example = "taiwan.tif")
    private String sourceFile;
    
    @ApiModelProperty(value = "瓦片类型", example = "terrain")
    private String tileType;
    
    @ApiModelProperty(value = "最小缩放级别", example = "0")
    private Integer minZoom;
    
    @ApiModelProperty(value = "最大缩放级别", example = "15")
    private Integer maxZoom;
} 