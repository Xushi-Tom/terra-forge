package com.service.impl;

import com.dto.terraforge.IndexedTilesRequestDto;
import com.dto.terraforge.IndexedTilesResponseDto;
import com.dto.terraforge.MapTileRequestDto;
import com.dto.terraforge.MapTileResponseDto;
import com.service.TerraForgeMapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * TerraForge地图切片服务实现类
 * TerraForge Map Service Implementation
 *
 * @author xushi
 * @version 1.0
 * @project map
 * @description 实现TerraForge API的地图切片功能
 * @date 2025/7/1 10:00:00
 */
@Slf4j
@Service
public class TerraForgeMapServiceImpl implements TerraForgeMapService {

    @Value("${terraforge.api.indexed-tiles-url}")
    private String indexedTilesUrl;

    /**
     * REST 模板，用于HTTP请求
     * REST template for HTTP requests
     */
    private final RestTemplate restTemplate = new RestTemplate();


    // ========== 索引瓦片切片方法实现 ==========

    /**
     * 索引瓦片切片 - 基于瓦片索引的精确切片
     * Indexed tiles generation - Precise tiling based on tile index
     */
    @Override
    public IndexedTilesResponseDto createIndexedTiles(IndexedTilesRequestDto request) {
        try {
            // 处理outputPath：如果是字符串，转换为数组发送给Python API
            if (request.getOutputPath() != null) {
                // 将outputPath字符串分割为数组
                String outputPathStr = request.getOutputPath();
                java.util.List<String> outputPathArray = java.util.Arrays.asList(outputPathStr.split("/"));
                
                // 创建包含outputPath数组的Map发送给Python API
                java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
                requestBody.put("folderPaths", request.getFolderPaths());
                requestBody.put("filePatterns", request.getFilePatterns());
                requestBody.put("outputPath", outputPathArray);
                requestBody.put("minZoom", request.getMinZoom());
                requestBody.put("maxZoom", request.getMaxZoom());
                requestBody.put("tileSize", request.getTileSize());
                requestBody.put("processes", request.getProcesses());
                requestBody.put("maxMemory", request.getMaxMemory());
                requestBody.put("resampling", request.getResampling());
                requestBody.put("generateShpIndex", request.getGenerateShpIndex());
                requestBody.put("enableIncrementalUpdate", request.getEnableIncrementalUpdate());
                requestBody.put("skipNodataTiles", request.getSkipNodataTiles());
                
                // 设置请求头
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Content-Type", "application/json");
                org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);

                log.debug("调用TerraForge索引瓦片切片API: {}", indexedTilesUrl);
                log.debug("索引瓦片切片参数: {}", requestBody);

                org.springframework.http.ResponseEntity<IndexedTilesResponseDto> response = restTemplate.postForEntity(
                        indexedTilesUrl, entity, IndexedTilesResponseDto.class);
                return response.getBody();
            } else {
                // 如果outputPath为空，直接发送原请求
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Content-Type", "application/json");
                org.springframework.http.HttpEntity<IndexedTilesRequestDto> entity = new org.springframework.http.HttpEntity<>(request, headers);

                org.springframework.http.ResponseEntity<IndexedTilesResponseDto> response = restTemplate.postForEntity(
                        indexedTilesUrl, entity, IndexedTilesResponseDto.class);
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("调用TerraForge索引瓦片切片API失败", e);

            // 返回错误响应
            IndexedTilesResponseDto errorResponse = new IndexedTilesResponseDto();
            errorResponse.setMessage("索引瓦片切片失败: " + e.getMessage());
            return errorResponse;
        }
    }

}
