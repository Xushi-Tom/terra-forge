package com.dto.terraforge;

import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 健康检查响应DTO
 * @date 2025/7/1 10:00:00
 */
@Data
public class HealthResponseDto {
    /**
     * 服务状态，固定值"healthy"
     */
    private String status;
    
    /**
     * 时间戳，格式：yyyy-MM-dd HH:mm:ss
     */
    private String timestamp;
    
    /**
     * API版本号
     */
    private String version;
}
