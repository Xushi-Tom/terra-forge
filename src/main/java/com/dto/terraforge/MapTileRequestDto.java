package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 地图瓦片创建请求DTO
 * @date 2025/7/1 10:00:00
 */
@Data
@ApiModel("地图切片请求")
public class MapTileRequestDto {
    
    @ApiModelProperty(value = "搜索文件夹路径列表（相对于数据源目录）", required = true, example = "[\"\", \"data/images\"]")
    private List<String> folderPaths;
    
    @ApiModelProperty(value = "文件匹配模式列表，支持通配符和txt文件列表", example = "[\"*.tif\", \"*.tiff\"]")
    private List<String> filePatterns;
    
    @ApiModelProperty(value = "输出路径（支持/分隔）", required = true, example = "map/test/v1")
    private String outputPath;

    // ========== 基本参数 ==========
    @ApiModelProperty(value = "最小缩放级别", example = "0")
    private Integer minZoom = 0;

    @ApiModelProperty(value = "最大缩放级别", example = "22")
    private Integer maxZoom;

    @ApiModelProperty(value = "是否启用智能分级", example = "true")
    private Boolean autoZoom = true;

    @ApiModelProperty(value = "智能分级策略", example = "conservative", allowableValues = "conservative,full")
    private String zoomStrategy = "conservative";

    // ========== ⭐ 优化参数 ==========
    @ApiModelProperty(value = "是否预先优化TIF文件结构（推荐开启）", example = "true")
    private Boolean optimizeFile = true;

    @ApiModelProperty(value = "是否创建概览金字塔（推荐开启）", example = "false")
    private Boolean createOverview = false;

    @ApiModelProperty(value = "是否使用分级处理模式（避免高级别卡死）", example = "true")
    private Boolean useOptimizedMode = true;

    // ========== 性能参数 ==========
    @ApiModelProperty(value = "进程数（自动根据CPU核心数推荐）", example = "2")
    private Integer processes;

    @ApiModelProperty(value = "线程数（用于文件优化和概览创建）", example = "2")
    private Integer threads = 2;

    @ApiModelProperty(value = "最大内存限制", example = "8g")
    private String maxMemory = "8g";

    @ApiModelProperty(value = "重采样方法", example = "near", allowableValues = "near,bilinear,cubic")
    private String resampling = "near";

    // ========== 传统参数 ==========
    @ApiModelProperty(value = "GDAL块大小（传统模式使用）", example = "1000000")
    private Integer swathSize = 1000000;

    @ApiModelProperty(value = "数据集池大小（传统模式使用）", example = "1000")
    private Integer maxDatasetPool = 1000;
}
