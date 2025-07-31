package com.service;

import com.dto.terraforge.TerrainTileRequestDto;
import com.dto.terraforge.TerrainTileResponseDto;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description TerraForge地形切片服务接口
 * @date 2025/7/1 10:00:00
 */
public interface TerraForgeTerrainService {
    
    /**
     * 创建地形瓦片
     */
    TerrainTileResponseDto createTerrainTile(TerrainTileRequestDto request);
}
