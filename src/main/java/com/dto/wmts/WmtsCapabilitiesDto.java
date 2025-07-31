package com.dto.wmts;

import lombok.Data;
import java.util.List;

/**
 * WMTS Capabilities响应DTO
 * WMTS Capabilities Response DTO
 * 
 * @author xushi
 * @version 1.0
 * @project map
 * @description WMTS服务能力描述文档的数据传输对象
 * @date 2025/7/1 14:00:00
 */
@Data
public class WmtsCapabilitiesDto {
    
    /**
     * 服务标识信息
     * Service identification information
     */
    private ServiceIdentification serviceIdentification;
    
    /**
     * 服务提供者信息
     * Service provider information
     */
    private ServiceProvider serviceProvider;
    
    /**
     * 图层列表
     * Layer list
     */
    private List<Layer> layers;
    
    /**
     * 瓦片矩阵集合
     * Tile matrix sets
     */
    private List<TileMatrixSet> tileMatrixSets;
    
    /**
     * 服务标识信息类
     * Service identification information class
     */
    @Data
    public static class ServiceIdentification {
        private String title;
        private String abstractText;
        private String serviceType = "OGC WMTS";
        private String serviceTypeVersion = "1.0.0";
    }
    
    /**
     * 服务提供者信息类
     * Service provider information class
     */
    @Data
    public static class ServiceProvider {
        private String providerName;
        private String providerSite;
        private String contactPerson;
        private String contactEmail;
    }
    
    /**
     * 图层信息类
     * Layer information class
     */
    @Data
    public static class Layer {
        private String identifier;
        private String title;
        private String abstractText;
        private String format;
        private String style = "default";
        private List<String> tileMatrixSetLinks;
        private BoundingBox boundingBox;
    }
    
    /**
     * 边界框类
     * Bounding box class
     */
    @Data
    public static class BoundingBox {
        private String crs;
        private double minX;
        private double minY;
        private double maxX;
        private double maxY;
    }
    
    /**
     * 瓦片矩阵集合类
     * Tile matrix set class
     */
    @Data
    public static class TileMatrixSet {
        private String identifier;
        private String supportedCRS;
        private List<TileMatrix> tileMatrices;
    }
    
    /**
     * 瓦片矩阵类
     * Tile matrix class
     */
    @Data
    public static class TileMatrix {
        private String identifier;
        private double scaleDenominator;
        private double[] topLeftCorner;
        private int tileWidth = 256;
        private int tileHeight = 256;
        private int matrixWidth;
        private int matrixHeight;
    }
}
