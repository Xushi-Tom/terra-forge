package com.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dto.wmts.WmtsCapabilitiesDto;
import com.po.WorkspacePo;
import com.service.IWorkspaceService;
import com.service.WmtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * WMTS服务实现类
 * WMTS Service Implementation
 * 
 * @author xushi
 * @version 1.0
 * @project map
 * @description WMTS (Web Map Tile Service) 服务实现，提供标准的WMTS协议支持
 * @date 2025/7/1 14:00:00
 */
@Slf4j
@Service
public class WmtsServiceImpl implements WmtsService {
    
    @Value("${TILES_BASE_DIR}")
    private String tilesBaseDir;
    
    @Value("${IMAGE_TYPE}")
    private String imageType;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Autowired
    private IWorkspaceService workspaceService;
    
    /**
     * 生成WMTS Capabilities XML文档
     * Generate WMTS Capabilities XML document
     */
    @Override
    public String generateCapabilities() {
        WmtsCapabilitiesDto capabilities = getCapabilitiesData();
        return buildCapabilitiesXml(capabilities);
    }
    
    /**
     * 获取WMTS Capabilities数据对象
     * Get WMTS Capabilities data object
     */
    @Override
    public WmtsCapabilitiesDto getCapabilitiesData() {
        WmtsCapabilitiesDto capabilities = new WmtsCapabilitiesDto();
        
        // 设置服务标识信息
        WmtsCapabilitiesDto.ServiceIdentification serviceId = new WmtsCapabilitiesDto.ServiceIdentification();
        serviceId.setTitle("TerraForge WMTS Service");
        serviceId.setAbstractText("Web Map Tile Service for TerraForge generated tiles");
        capabilities.setServiceIdentification(serviceId);
        
        // 设置服务提供者信息
        WmtsCapabilitiesDto.ServiceProvider serviceProvider = new WmtsCapabilitiesDto.ServiceProvider();
        serviceProvider.setProviderName("TerraForge");
        serviceProvider.setProviderSite("http://localhost:" + serverPort);
        capabilities.setServiceProvider(serviceProvider);
        
        // 设置图层列表（这里可以从数据库动态获取）
        capabilities.setLayers(getAvailableLayers());
        
        // 设置瓦片矩阵集合
        capabilities.setTileMatrixSets(getTileMatrixSets());
        
        return capabilities;
    }
    
    /**
     * 获取瓦片资源
     * Get tile resource
     */
    @Override
    public Resource getTileResource(String workspaceGroup, String workspace, 
                                  String tileMatrix, Integer tileRow, Integer tileCol, String format) {
        try {
            // 解析缩放级别
            int z = Integer.parseInt(tileMatrix);
            
            // WMTS使用标准XYZ坐标系，不需要Y轴转换
            // 构建瓦片文件路径
            Path tilePath = Paths.get(tilesBaseDir)
                    .resolve(workspaceGroup)
                    .resolve(workspace)
                    .resolve(String.valueOf(z))
                    .resolve(String.valueOf(tileCol))
                    .resolve(tileRow + getFileExtension(format));
            
            log.debug("WMTS请求瓦片路径: {}", tilePath);
            
            // 检查文件是否存在
            if (!Files.exists(tilePath)) {
                log.warn("WMTS瓦片文件不存在: {}", tilePath);
                return null;
            }
            
            return new UrlResource(tilePath.toUri());
        } catch (MalformedURLException | NumberFormatException e) {
            log.error("WMTS获取瓦片资源失败", e);
            return null;
        }
    }
    
    /**
     * 检查图层是否存在
     * Check if layer exists
     */
    @Override
    public boolean layerExists(String workspaceGroup, String workspace) {
        try {
            // 创建条件查询对象
            QueryWrapper<WorkspacePo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", workspaceGroup);
            queryWrapper.eq("type", 1);
            WorkspacePo workspacePo = workspaceService.getOne(queryWrapper);
            
            if (Objects.isNull(workspacePo)) {
                return false;
            }
            
            // 查询工作空间
            QueryWrapper<WorkspacePo> workSpaceQueryWrapper = new QueryWrapper<>();
            workSpaceQueryWrapper.eq("name", workspace);
            workSpaceQueryWrapper.eq("type", 2);
            workSpaceQueryWrapper.eq("parent_id", workspacePo.getId());
            WorkspacePo workspaceServiceOne = workspaceService.getOne(workSpaceQueryWrapper);
            
            return !Objects.isNull(workspaceServiceOne) && workspaceServiceOne.getStatus() == 1;
        } catch (Exception e) {
            log.error("检查图层是否存在失败", e);
            return false;
        }
    }
    
    /**
     * 获取图层的边界框
     * Get layer bounding box
     */
    @Override
    public WmtsCapabilitiesDto.BoundingBox getLayerBoundingBox(String workspaceGroup, String workspace) {
        // 默认全球范围边界框（Web Mercator）
        WmtsCapabilitiesDto.BoundingBox boundingBox = new WmtsCapabilitiesDto.BoundingBox();
        boundingBox.setCrs("EPSG:3857");
        boundingBox.setMinX(-20037508.34);
        boundingBox.setMinY(-20037508.34);
        boundingBox.setMaxX(20037508.34);
        boundingBox.setMaxY(20037508.34);
        return boundingBox;
    }
    
    /**
     * 获取可用图层列表
     * Get available layers list
     */
    private List<WmtsCapabilitiesDto.Layer> getAvailableLayers() {
        List<WmtsCapabilitiesDto.Layer> layers = new ArrayList<>();
        
        // 这里可以从数据库动态获取图层信息
        // 暂时返回示例图层
        WmtsCapabilitiesDto.Layer layer = new WmtsCapabilitiesDto.Layer();
        layer.setIdentifier("default_layer");
        layer.setTitle("Default Map Layer");
        layer.setAbstractText("Default map layer from TerraForge");
        layer.setFormat("image/png");
        layer.setTileMatrixSetLinks(Arrays.asList("EPSG:3857"));
        layer.setBoundingBox(getLayerBoundingBox("", ""));
        
        layers.add(layer);
        return layers;
    }
    
    /**
     * 获取瓦片矩阵集合
     * Get tile matrix sets
     */
    private List<WmtsCapabilitiesDto.TileMatrixSet> getTileMatrixSets() {
        List<WmtsCapabilitiesDto.TileMatrixSet> tileMatrixSets = new ArrayList<>();
        
        // Web Mercator 瓦片矩阵集合
        WmtsCapabilitiesDto.TileMatrixSet tileMatrixSet = new WmtsCapabilitiesDto.TileMatrixSet();
        tileMatrixSet.setIdentifier("EPSG:3857");
        tileMatrixSet.setSupportedCRS("urn:ogc:def:crs:EPSG::3857");
        
        // 生成0-18级瓦片矩阵
        List<WmtsCapabilitiesDto.TileMatrix> tileMatrices = new ArrayList<>();
        for (int z = 0; z <= 18; z++) {
            WmtsCapabilitiesDto.TileMatrix tileMatrix = new WmtsCapabilitiesDto.TileMatrix();
            tileMatrix.setIdentifier(String.valueOf(z));
            tileMatrix.setScaleDenominator(559082264.029 / Math.pow(2, z));
            tileMatrix.setTopLeftCorner(new double[]{-20037508.34, 20037508.34});
            tileMatrix.setMatrixWidth((int) Math.pow(2, z));
            tileMatrix.setMatrixHeight((int) Math.pow(2, z));
            tileMatrices.add(tileMatrix);
        }
        
        tileMatrixSet.setTileMatrices(tileMatrices);
        tileMatrixSets.add(tileMatrixSet);
        
        return tileMatrixSets;
    }
    
    /**
     * 根据格式获取文件扩展名
     * Get file extension by format
     */
    private String getFileExtension(String format) {
        if ("image/jpeg".equals(format) || "image/jpg".equals(format)) {
            return ".jpg";
        }
        return imageType; // 默认使用配置的图片类型
    }
    
    /**
     * 构建Capabilities XML
     * Build Capabilities XML
     */
    private String buildCapabilitiesXml(WmtsCapabilitiesDto capabilities) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<WMTS_Capabilities xmlns=\"http://www.opengis.net/wmts/1.0\" ");
        xml.append("xmlns:ows=\"http://www.opengis.net/ows/1.1\" ");
        xml.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" ");
        xml.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        xml.append("xmlns:gml=\"http://www.opengis.net/gml\" ");
        xml.append("xsi:schemaLocation=\"http://www.opengis.net/wmts/1.0 http://schemas.opengis.net/wmts/1.0/wmtsGetCapabilities_response.xsd\" ");
        xml.append("version=\"1.0.0\">\n");
        
        // Service Identification
        xml.append("  <ows:ServiceIdentification>\n");
        xml.append("    <ows:Title>").append(capabilities.getServiceIdentification().getTitle()).append("</ows:Title>\n");
        xml.append("    <ows:Abstract>").append(capabilities.getServiceIdentification().getAbstractText()).append("</ows:Abstract>\n");
        xml.append("    <ows:ServiceType>").append(capabilities.getServiceIdentification().getServiceType()).append("</ows:ServiceType>\n");
        xml.append("    <ows:ServiceTypeVersion>").append(capabilities.getServiceIdentification().getServiceTypeVersion()).append("</ows:ServiceTypeVersion>\n");
        xml.append("  </ows:ServiceIdentification>\n");
        
        // Service Provider
        xml.append("  <ows:ServiceProvider>\n");
        xml.append("    <ows:ProviderName>").append(capabilities.getServiceProvider().getProviderName()).append("</ows:ProviderName>\n");
        xml.append("    <ows:ProviderSite xlink:href=\"").append(capabilities.getServiceProvider().getProviderSite()).append("\"/>\n");
        xml.append("  </ows:ServiceProvider>\n");
        
        // Operations Metadata
        xml.append("  <ows:OperationsMetadata>\n");
        xml.append("    <ows:Operation name=\"GetCapabilities\">\n");
        xml.append("      <ows:DCP>\n");
        xml.append("        <ows:HTTP>\n");
        xml.append("          <ows:Get xlink:href=\"http://localhost:").append(serverPort).append("/map/wmts/capabilities\">\n");
        xml.append("            <ows:Constraint name=\"GetEncoding\">\n");
        xml.append("              <ows:AllowedValues><ows:Value>KVP</ows:Value></ows:AllowedValues>\n");
        xml.append("            </ows:Constraint>\n");
        xml.append("          </ows:Get>\n");
        xml.append("        </ows:HTTP>\n");
        xml.append("      </ows:DCP>\n");
        xml.append("    </ows:Operation>\n");
        xml.append("    <ows:Operation name=\"GetTile\">\n");
        xml.append("      <ows:DCP>\n");
        xml.append("        <ows:HTTP>\n");
        xml.append("          <ows:Get xlink:href=\"http://localhost:").append(serverPort).append("/map/wmts/tile\">\n");
        xml.append("            <ows:Constraint name=\"GetEncoding\">\n");
        xml.append("              <ows:AllowedValues><ows:Value>KVP</ows:Value></ows:AllowedValues>\n");
        xml.append("            </ows:Constraint>\n");
        xml.append("          </ows:Get>\n");
        xml.append("        </ows:HTTP>\n");
        xml.append("      </ows:DCP>\n");
        xml.append("    </ows:Operation>\n");
        xml.append("  </ows:OperationsMetadata>\n");
        
        // Contents
        xml.append("  <Contents>\n");
        
        // Layers
        for (WmtsCapabilitiesDto.Layer layer : capabilities.getLayers()) {
            xml.append("    <Layer>\n");
            xml.append("      <ows:Title>").append(layer.getTitle()).append("</ows:Title>\n");
            xml.append("      <ows:Abstract>").append(layer.getAbstractText()).append("</ows:Abstract>\n");
            xml.append("      <ows:Identifier>").append(layer.getIdentifier()).append("</ows:Identifier>\n");
            xml.append("      <Style isDefault=\"true\">\n");
            xml.append("        <ows:Identifier>").append(layer.getStyle()).append("</ows:Identifier>\n");
            xml.append("      </Style>\n");
            xml.append("      <Format>").append(layer.getFormat()).append("</Format>\n");
            for (String tileMatrixSetLink : layer.getTileMatrixSetLinks()) {
                xml.append("      <TileMatrixSetLink>\n");
                xml.append("        <TileMatrixSet>").append(tileMatrixSetLink).append("</TileMatrixSet>\n");
                xml.append("      </TileMatrixSetLink>\n");
            }
            xml.append("    </Layer>\n");
        }
        
        // TileMatrixSets
        for (WmtsCapabilitiesDto.TileMatrixSet tileMatrixSet : capabilities.getTileMatrixSets()) {
            xml.append("    <TileMatrixSet>\n");
            xml.append("      <ows:Identifier>").append(tileMatrixSet.getIdentifier()).append("</ows:Identifier>\n");
            xml.append("      <ows:SupportedCRS>").append(tileMatrixSet.getSupportedCRS()).append("</ows:SupportedCRS>\n");
            
            for (WmtsCapabilitiesDto.TileMatrix tileMatrix : tileMatrixSet.getTileMatrices()) {
                xml.append("      <TileMatrix>\n");
                xml.append("        <ows:Identifier>").append(tileMatrix.getIdentifier()).append("</ows:Identifier>\n");
                xml.append("        <ScaleDenominator>").append(tileMatrix.getScaleDenominator()).append("</ScaleDenominator>\n");
                xml.append("        <TopLeftCorner>").append(tileMatrix.getTopLeftCorner()[0]).append(" ").append(tileMatrix.getTopLeftCorner()[1]).append("</TopLeftCorner>\n");
                xml.append("        <TileWidth>").append(tileMatrix.getTileWidth()).append("</TileWidth>\n");
                xml.append("        <TileHeight>").append(tileMatrix.getTileHeight()).append("</TileHeight>\n");
                xml.append("        <MatrixWidth>").append(tileMatrix.getMatrixWidth()).append("</MatrixWidth>\n");
                xml.append("        <MatrixHeight>").append(tileMatrix.getMatrixHeight()).append("</MatrixHeight>\n");
                xml.append("      </TileMatrix>\n");
            }
            
            xml.append("    </TileMatrixSet>\n");
        }
        
        xml.append("  </Contents>\n");
        xml.append("</WMTS_Capabilities>");
        
        return xml.toString();
    }
}
