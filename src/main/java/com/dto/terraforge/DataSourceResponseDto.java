package com.dto.terraforge;

import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 数据源浏览响应DTO - 与Python API对齐
 * @date 2025/7/1 10:00:00
 */
@Data
public class DataSourceResponseDto {
    
    /**
     * 当前路径
     */
    private String currentPath;
    
    /**
     * 父级路径，根目录时为null
     */
    private String parentPath;
    
    /**
     * 子目录列表
     */
    private List<DirectoryInfo> directories;
    
    /**
     * 数据源文件列表
     */
    private List<DataSourceFile> datasources;
    
    /**
     * 子目录总数
     */
    private Integer totalDirectories;
    
    /**
     * 文件总数
     */
    private Integer totalFiles;
    
    /**
     * 文件总数（向后兼容）
     */
    private Integer count;

    /**
     * 筛选信息（使用地理筛选时）
     */
    private FilterInfo filterInfo;
    
    @Data
    public static class DirectoryInfo {
        /**
         * 目录名
         */
        private String name;
        
        /**
         * 相对路径
         */
        private String path;
        
        /**
         * 目录内文件数量（地理筛选时提供）
         */
        private Integer itemCount;
        
        /**
         * 固定值"directory"
         */
        private String type = "directory";
    }
    
    @Data
    public static class DataSourceFile {
        /**
         * 文件名
         */
        private String name;
        
        /**
         * 相对路径
         */
        private String path;
        
        /**
         * 完整文件路径
         */
        private String fullPath;
        
        /**
         * 文件大小（字节）
         */
        private Long size;
        
        /**
         * 格式化的文件大小
         */
        private String sizeFormatted;
        
        /**
         * 文件格式（如".tif"）
         */
        private String format;
        
        /**
         * 固定值"file"
         */
        private String type = "file";
        
        /**
         * 文件类型："geotiff"或"image"
         */
        private String fileType;
        
        /**
         * 最后修改时间
         */
        private String modified;
        
        /**
         * 地理边界信息（如果有）
         */
        private GeographicBounds geoBounds;
    }

    @Data
    public static class FilterInfo {
        /**
         * 筛选使用的地理范围
         */
        private Object boundsFilter;

        /**
         * 筛选后的文件数量
         */
        private Integer filteredCount;

        /**
         * 筛选说明
         */
        private String message;
    }

    @Data
    public static class GeographicBounds {
        private Double west;
        private Double south;
        private Double east;
        private Double north;
    }
}
