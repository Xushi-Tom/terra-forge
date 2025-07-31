package com.dto.terraforge;

import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 结果目录浏览响应DTO - 与Python API对齐
 * @date 2025/7/1 10:00:00
 */
@Data
public class ResultsResponseDto {
    
    /**
     * 当前路径
     */
    private String currentPath;
    
    /**
     * 父级路径，根目录时为null
     */
    private String parentPath;
    
    /**
     * 基础类型：'datasource' 或 'results'
     */
    private String baseType;
    
    /**
     * 子目录列表
     */
    private List<ResultDirectory> directories;
    
    /**
     * 文件列表
     */
    private List<ResultFile> files;
    
    /**
     * 子目录总数
     */
    private Integer totalDirectories;
    
    /**
     * 文件总数
     */
    private Integer totalFiles;
    
    @Data
    public static class ResultDirectory {
        /**
         * 目录名
         */
        private String name;
        
        /**
         * 相对路径
         */
        private String path;
        
        /**
         * 固定值"directory"
         */
        private String type = "directory";
        
        /**
         * 目录内文件数量
         */
        private Integer fileCount;
        
        /**
         * 目录内子目录数量
         */
        private Integer dirCount;
    }
    
    @Data
    public static class ResultFile {
        /**
         * 文件名
         */
        private String name;
        
        /**
         * 相对路径
         */
        private String path;
        
        /**
         * 文件大小（字节）
         */
        private Long size;
        
        /**
         * 格式化的文件大小
         */
        private String sizeFormatted;
        
        /**
         * 修改时间（时间戳）
         */
        private Double modifiedTime;
        
        /**
         * 文件扩展名
         */
        private String extension;
        
        /**
         * 固定值"file"
         */
        private String type = "file";
    }
}
