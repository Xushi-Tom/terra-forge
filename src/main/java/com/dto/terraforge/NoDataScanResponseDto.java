package com.dto.terraforge;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 透明瓦片扫描响应DTO - 与Python API对齐
 * @date 2025/7/16 12:00:00
 */
@Data
public class NoDataScanResponseDto {
    
    /**
     * 扫描是否成功
     */
    private Boolean success;
    
    /**
     * 扫描摘要信息
     */
    private ScanSummary summary;
    
    /**
     * 按缩放级别统计
     */
    private Map<String, Object> zoomLevelStats;
    
    /**
     * 扫描结果消息
     */
    private String message;
    
    /**
     * 透明瓦片文件列表（includeDetails=true时）
     */
    private List<String> nodataFiles;
    
    /**
     * 说明信息
     */
    private String note;
    
    @Data
    public static class ScanSummary {
        /**
         * 检查的瓦片总数
         */
        private Integer totalChecked;
        
        /**
         * 包含透明值的瓦片数
         */
        private Integer nodataTiles;
        
        /**
         * 有效瓦片数
         */
        private Integer validTiles;
        
        /**
         * 检查错误数
         */
        private Integer errors;
        
        /**
         * 透明瓦片百分比
         */
        private Double nodataPercentage;
    }
} 