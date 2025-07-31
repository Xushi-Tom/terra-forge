package com.dto.terraforge;

import lombok.Data;
import java.util.Map;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 配置推荐响应DTO - 与Python API对齐
 * @date 2025/7/16 12:00:00
 */
@Data
public class ConfigRecommendResponseDto {
    
    /**
     * 请求是否成功
     */
    private Boolean success;
    
    /**
     * 文件大小（GB）
     */
    private Double fileSize;
    
    /**
     * 系统信息
     */
    private SystemInfo systemInfo;
    
    /**
     * 推荐配置
     */
    private Map<String, Object> recommendations;
    
    @Data
    public static class SystemInfo {
        /**
         * CPU核心数
         */
        private Integer cpuCount;
        
        /**
         * 系统总内存（GB）
         */
        private Double memoryTotalGb;
    }
} 