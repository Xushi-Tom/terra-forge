package com.dto.terraforge;

import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 缓存信息响应DTO - 与Python API对齐
 * @date 2025/7/16 12:00:00
 */
@Data
public class CacheInfoResponseDto {
    
    /**
     * 缓存目录列表
     */
    private List<CacheDirectory> cacheDirectories;
    
    /**
     * 缓存目录总数
     */
    private Integer totalDirectories;
    
    /**
     * 瓦片基础目录
     */
    private String tilesBaseDir;
    
    @Data
    public static class CacheDirectory {
        /**
         * 目录名
         */
        private String directory;
        
        /**
         * 目录路径
         */
        private String path;
        
        /**
         * 是否有元数据
         */
        private Boolean hasMetadata;
        
        /**
         * 是否有SHP索引
         */
        private Boolean hasShpIndex;
        
        /**
         * 是否有GeoJSON索引
         */
        private Boolean hasGeoJsonIndex;
        
        /**
         * 生成时间
         */
        private String generatedAt;
        
        /**
         * 完成时间
         */
        private String completedAt;
        
        /**
         * 源文件列表
         */
        private List<String> sourceFiles;
        
        /**
         * 源文件总数
         */
        private Integer totalSourceFiles;
        
        /**
         * 缩放级别
         */
        private String zoomLevels;
        
        /**
         * 瓦片大小
         */
        private Integer tileSize;
        
        /**
         * 总瓦片数
         */
        private Integer totalTiles;
        
        /**
         * 已处理瓦片数
         */
        private Integer processedTiles;
        
        /**
         * 失败瓦片数
         */
        private Integer failedTiles;
        
        /**
         * 成功率
         */
        private String successRate;
        
        /**
         * 处理方法
         */
        private String method;
        
        /**
         * 重采样方法
         */
        private String resampling;
        
        /**
         * 透明度阈值
         */
        private Double transparencyThreshold;
        
        /**
         * 实际瓦片文件数
         */
        private Integer actualTileFiles;
        
        /**
         * 元数据错误信息
         */
        private String metadataError;
    }
} 