package com.dto.terraforge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 地形瓦片创建请求DTO
 * @date 2025/7/1 10:00:00
 */
@Data
@ApiModel("地形切片请求")
public class TerrainTileRequestDto {
    
    @ApiModelProperty(value = "搜索文件夹路径列表（相对于数据源目录）", required = true, example = "[\"\", \"data/elevation\"]")
    private List<String> folderPaths;
    
    @ApiModelProperty(value = "文件匹配模式列表，支持通配符和txt文件列表", example = "[\"*.tif\", \"*.tiff\"]")
    private List<String> filePatterns;
    
    @ApiModelProperty(value = "输出路径（支持/分隔）", required = true, example = "terrain/taiwan/v1")
    private String outputPath;

    // ========== 基本参数 ==========
    @ApiModelProperty(value = "起始缩放级别（粗糙级别）", example = "0")
    private Integer startZoom = 0;

    @ApiModelProperty(value = "结束缩放级别（详细级别），最小值8", example = "8")
    private Integer endZoom = 8;

    @ApiModelProperty(value = "最大三角形数量", example = "6291456")
    private Integer maxTriangles = 6291456;

    @ApiModelProperty(value = "地理边界[west, south, east, north]")
    private List<Double> bounds;

    @ApiModelProperty(value = "是否启用压缩", example = "true")
    private Boolean compression = true;

    @ApiModelProperty(value = "是否自动解压输出文件", example = "true")
    private Boolean decompress = true;

    @ApiModelProperty(value = "线程数", example = "4")
    private Integer threads = 4;

    @ApiModelProperty(value = "最大内存限制", example = "8g")
    private String maxMemory = "8g";

    @ApiModelProperty(value = "是否启用智能分级", example = "false")
    private Boolean autoZoom = false;

    @ApiModelProperty(value = "智能分级策略", example = "conservative", allowableValues = "conservative,aggressive")
    private String zoomStrategy = "conservative";

    // ========== 地形合并参数 ==========
    @ApiModelProperty(value = "是否在切片完成后合并成一个文件夹", 
                      notes = "当设置为true时，系统会在所有地形切片完成后，自动将多个地形文件夹合并到outputPath指定的目录中。" +
                              "合并过程包括：1.合并所有.terrain瓦片文件 2.智能合并layer.json元数据 3.使用硬链接节省空间", 
                      example = "false")
    private Boolean mergeTerrains = false;
}
