package com.service;


import com.dto.TerrainCutRequestDto;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description TMS服务地形切片接口
 * @date 2025/4/21 03:43:54
 */
public interface TerrainCutService {

    /**
     * @description 地形切片（路径）
     * @param terrainCutRequestDto
     * @return java.lang.String
     * @author xushi
     * @date 2025/4/21 04:21:45
     */
    String terrainCutOfPath(TerrainCutRequestDto terrainCutRequestDto);

    /**
     * @description 地形切片 - TerraForge API
     * @param terrainCutRequestDto
     * @return java.lang.String
     * @author xushi
     * @date 2025/7/1 10:00:00
     */
    String terrainCutByPathOfTerraForge(TerrainCutRequestDto terrainCutRequestDto);

}
