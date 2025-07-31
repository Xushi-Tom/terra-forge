package com.dto.terraforge;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 系统信息响应DTO - 与Python API对齐
 * @date 2025/7/1 10:00:00
 */
@Data
public class SystemInfoResponseDto {
    
    /**
     * 时间戳
     */
    private String timestamp;
    
    /**
     * 配置信息
     */
    private ConfigInfo config;
    
    /**
     * 系统资源信息
     */
    private SystemInfo system;
    
    /**
     * 任务统计信息
     */
    private TasksInfo tasks;
    
    @Data
    public static class ConfigInfo {
        /**
         * 数据源目录
         */
        private String dataSourceDir;
        
        /**
         * 瓦片目录
         */
        private String tilesDir;
        
        /**
         * 最大线程数
         */
        private Integer maxThreads;
        
        /**
         * 支持的格式
         */
        private List<String> supportedFormats;
    }
    
    @Data
    public static class SystemInfo {
        /**
         * CPU核心数
         */
        private Integer cpuCount;
        
        /**
         * 总内存（字节）
         */
        private Long memoryTotal;
        
        /**
         * 可用内存（字节）
         */
        private Long memoryAvailable;
    
        /**
         * 磁盘使用率百分比 (0-100)
         */
        private Double diskUsage;
    }
    
    @Data
    public static class TasksInfo {
        /**
         * 任务总数
         */
        private Integer total;
        
        /**
         * 运行中的任务数
         */
        private Integer running;
        
        /**
         * 已完成的任务数
         */
        private Integer completed;
        
        /**
         * 失败的任务数
         */
        private Integer failed;
    }
}
