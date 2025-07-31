package com.dto.wmts;

import lombok.Data;

/**
 * WMTS GetTile请求参数DTO
 * WMTS GetTile Request Parameters DTO
 * 
 * @author xushi
 * @version 1.0
 * @project map
 * @description WMTS GetTile请求的参数封装
 * @date 2025/7/1 14:00:00
 */
@Data
public class WmtsGetTileRequestDto {
    
    /**
     * 服务类型，固定值"WMTS"
     * Service type, fixed value "WMTS"
     */
    private String service = "WMTS";
    
    /**
     * 请求类型，固定值"GetTile"
     * Request type, fixed value "GetTile"
     */
    private String request = "GetTile";
    
    /**
     * 版本号，默认"1.0.0"
     * Version number, default "1.0.0"
     */
    private String version = "1.0.0";
    
    /**
     * 图层标识符
     * Layer identifier
     */
    private String layer;
    
    /**
     * 样式，默认"default"
     * Style, default "default"
     */
    private String style = "default";
    
    /**
     * 图像格式
     * Image format
     */
    private String format = "image/png";
    
    /**
     * 瓦片矩阵集合标识符
     * Tile matrix set identifier
     */
    private String tileMatrixSet;
    
    /**
     * 瓦片矩阵标识符（缩放级别）
     * Tile matrix identifier (zoom level)
     */
    private String tileMatrix;
    
    /**
     * 瓦片行号
     * Tile row number
     */
    private Integer tileRow;
    
    /**
     * 瓦片列号
     * Tile column number
     */
    private Integer tileCol;
}
