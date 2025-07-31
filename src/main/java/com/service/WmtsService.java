package com.service;

import com.dto.wmts.WmtsCapabilitiesDto;
import org.springframework.core.io.Resource;

/**
 * WMTS服务接口
 * WMTS Service Interface
 * 
 * @author xushi
 * @version 1.0
 * @project map
 * @description WMTS (Web Map Tile Service) 服务接口，提供标准的WMTS协议支持
 * @date 2025/7/1 14:00:00
 */
public interface WmtsService {
    
    /**
     * 生成WMTS Capabilities XML文档
     * Generate WMTS Capabilities XML document
     * 
     * @return Capabilities XML字符串
     */
    String generateCapabilities();
    
    /**
     * 获取WMTS Capabilities数据对象
     * Get WMTS Capabilities data object
     * 
     * @return Capabilities数据对象
     */
    WmtsCapabilitiesDto getCapabilitiesData();
    
    /**
     * 获取瓦片资源
     * Get tile resource
     * 
     * @param workspaceGroup 工作空间组
     * @param workspace 工作空间
     * @param tileMatrix 瓦片矩阵（缩放级别）
     * @param tileRow 瓦片行号
     * @param tileCol 瓦片列号
     * @param format 图像格式
     * @return 瓦片资源
     */
    Resource getTileResource(String workspaceGroup, String workspace, 
                           String tileMatrix, Integer tileRow, Integer tileCol, String format);
    
    /**
     * 检查图层是否存在
     * Check if layer exists
     * 
     * @param workspaceGroup 工作空间组
     * @param workspace 工作空间
     * @return 是否存在
     */
    boolean layerExists(String workspaceGroup, String workspace);
    
    /**
     * 获取图层的边界框
     * Get layer bounding box
     * 
     * @param workspaceGroup 工作空间组
     * @param workspace 工作空间
     * @return 边界框
     */
    WmtsCapabilitiesDto.BoundingBox getLayerBoundingBox(String workspaceGroup, String workspace);
}
