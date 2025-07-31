package com.dto.terraforge;

import lombok.Data;
import java.util.List;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 更新Layer.json请求DTO
 * @date 2025/7/1 10:00:00
 */
@Data
public class UpdateLayerRequestDto {
    
    /**
     * 地形目录路径
     */
    private String terrainDir;
    
    /**
     * 地理边界[west, south, east, north]
     */
    private List<Double> bounds;
}
