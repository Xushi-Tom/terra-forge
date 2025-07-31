package com.service.impl;

import com.dto.terraforge.TerrainTileRequestDto;
import com.dto.terraforge.TerrainTileResponseDto;
import com.service.TerraForgeTerrainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * TerraForge地形切片服务实现类
 * TerraForge Terrain Service Implementation
 *
 * @author xushi
 * @version 1.0
 * @project map
 * @description 实现TerraForge API的地形切片功能
 * @date 2025/7/1 10:00:00
 */
@Slf4j
@Service
public class TerraForgeTerrainServiceImpl implements TerraForgeTerrainService {

    /**
     * TerraForge 地形切片API URL
     */
    @Value("${terraforge.api.terrain-tile-url}")
    private String terrainTileUrl;

    /**
     * REST 模板，用于HTTP请求
     * REST template for HTTP requests
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 创建地形瓦片
     * Create terrain tiles
     */
    @Override
    public TerrainTileResponseDto createTerrainTile(TerrainTileRequestDto request) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<TerrainTileRequestDto> entity = new HttpEntity<>(request, headers);

            log.info("调用TerraForge地形切片API: {}", terrainTileUrl);
            log.info("请求参数: {}", request);

            ResponseEntity<TerrainTileResponseDto> response = restTemplate.postForEntity(terrainTileUrl, entity, TerrainTileResponseDto.class);

            log.info("TerraForge地形切片API响应: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge地形切片API失败", e);

            // 返回错误响应
            TerrainTileResponseDto errorResponse = new TerrainTileResponseDto();
            errorResponse.setMessage("创建地形瓦片失败: " + e.getMessage());
            return errorResponse;
        }
    }
}
