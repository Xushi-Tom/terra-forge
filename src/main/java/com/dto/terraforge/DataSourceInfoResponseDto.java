package com.dto.terraforge;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 数据源详情响应DTO - 直接映射Python API响应
 * @date 2025/7/1 10:00:00
 */
@Data
public class DataSourceInfoResponseDto {
    
    /**
     * 文件格式
     */
    private String format;
    
    /**
     * 地理边界
     */
    private GeoBounds geoBounds;
    
    /**
     * 最后修改时间
     */
    private String lastModified;
    
    /**
     * 元数据信息
     */
    private Metadata metadata;
    
    /**
     * 推荐配置
     */
    private Recommendations recommendations;
    
    /**
     * 文件大小（字节）
     */
    private Long size;
    
    /**
     * 文件类型
     */
    private String type;
    
    @Data
    public static class GeoBounds {
        private Double east;
        private Double heightDegrees;
        private Double lowerRightLat;
        private Double lowerRightLon;
        private Double north;
        private Double south;
        private Double upperLeftLat;
        private Double upperLeftLon;
        private Double west;
        private Double widthDegrees;
    }
    
    @Data
    public static class Metadata {
        /**
         * 波段数量
         */
        private Integer bandCount;
        
        /**
         * 地理边界（与geoBounds相同的结构）
         */
        private GeoBounds bounds;
        
        /**
         * 像素大小
         */
        private PixelSize pixelSize;
        
        /**
         * 栅格尺寸
         */
        private RasterSize rasterSize;
        
        /**
         * 空间参考系统
         */
        private String srs;
    }
    
    @Data
    public static class PixelSize {
        private Double x;
        private Double y;
    }
    
    @Data
    public static class RasterSize {
        private Integer height;
        private Integer width;
    }
    
    @Data
    public static class Recommendations {
        /**
         * 最大缩放级别
         */
        private Integer maxZoom;
        
        /**
         * 最小缩放级别
         */
        private Integer minZoom;
        
        /**
         * 处理进程数
         */
        private Integer processes;
        
        /**
         * 质量
         */
        private Integer quality;
        
        /**
         * 瓦片格式
         */
        private String tileFormat;
    }
}
