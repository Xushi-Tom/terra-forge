package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 索引瓦片切片请求DTO
 * @date 2025/7/9 10:30:00
 */
@Data
@ApiModel("索引瓦片切片请求")
public class IndexedTilesRequestDto {
    
    @ApiModelProperty(value = "数据源文件夹路径列表", required = true, example = "[\"111\", \"satellite\"]")
    private List<String> folderPaths;
    
    @ApiModelProperty(value = "文件模式列表（支持通配符和txt文件）", required = true, example = "[\"*.tif\", \"xxx.txt\"]")
    private List<String> filePatterns;
    
    @ApiModelProperty(value = "输出路径（支持/分隔）", required = true, example = "indexed-tiles/test")
    private String outputPath;

    // ========== 基本参数 ==========
    @ApiModelProperty(value = "最小缩放级别", example = "0")
    private Integer minZoom = 0;

    @ApiModelProperty(value = "最大缩放级别", example = "12")
    private Integer maxZoom = 12;

    @ApiModelProperty(value = "瓦片大小", example = "256")
    private Integer tileSize = 256;

    // ========== 性能参数 ==========
    @ApiModelProperty(value = "进程数", example = "4")
    private Integer processes = 4;

    @ApiModelProperty(value = "最大内存限制", example = "8g")
    private String maxMemory = "8g";

    @ApiModelProperty(value = "重采样方法", example = "near", allowableValues = "near,bilinear,cubic,average")
    private String resampling = "near";

    // ========== 高级参数 ==========
    @ApiModelProperty(value = "是否生成SHP矢量索引", example = "true")
    private Boolean generateShpIndex = true;

    @ApiModelProperty(value = "是否启用增量更新", example = "false")
    private Boolean enableIncrementalUpdate = false;

    @ApiModelProperty(value = "是否跳过透明瓦片（自动删除透明瓦片）", example = "false")
    private Boolean skipNodataTiles = false;
} 