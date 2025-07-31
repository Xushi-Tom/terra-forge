package com.dto.terraforge;

import lombok.Data;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 解压地形文件请求DTO
 * @date 2025/7/1 10:00:00
 */
@Data
public class DecompressRequestDto {
    
    /**
     * 地形目录路径
     */
    private String terrainDir;
}
