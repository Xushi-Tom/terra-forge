package com.service.impl;

import com.service.IMapRecordService;
import com.service.MapCutService;
import com.dto.MapCutRequestDto;
import com.po.MapRecordPo;

import com.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description TMS切片接口实现类
 * @date 2025/4/21 03:44:30
 */
@Slf4j
@Service
public class MapCutServiceImpl implements MapCutService {
    /**
     * 瓦片图大小
     */
    private static final int TILE_SIZE = 256;
    /**
     * 世界范围
     */
    private static final double WORLD_SIZE = 20037508.342789244;
    /**
     * 默认坐标
     */
    private static final String DEFAULT_CRS = "EPSG:3857";

    @Value("${TILES_BASE_DIR}")
    private String tilesBaseDir;

    @Autowired
    private IMapRecordService mapRecordService;

    /**
     * 主TMS切割方法 - 将GeoTIFF图像切割为瓦片地图服务(TMS)格式的瓦片
     * 优化：解决瓦片间缝隙问题，采用精确的像素计算和边界扩展策略
     * 
     * @param reader GeoTIFF文件读取器，用于获取地理信息和坐标系统
     * @param sourceImage 源图像对象，需要被切割的完整图片
     * @param outPutPath 输出路径，瓦片保存的根目录
     * @param minZoom 最小缩放级别，开始切割的层级
     * @param maxZoom 最大缩放级别，结束切割的层级
     */
    private void mainTmsCut(GeoTiffReader reader, BufferedImage sourceImage, String outPutPath, Integer minZoom, Integer maxZoom) {
        try {
            log.info("开始TMS切片处理，层级范围: {}-{}", minZoom, maxZoom);
            
            // ================ 1. 获取和转换坐标系统 ================
            // 获取TIFF文件的原始坐标参考系统
            CoordinateReferenceSystem tiffCRS = reader.getCoordinateReferenceSystem();
            // 获取原始图像的地理范围边界
            Envelope originalEnvelope = reader.getOriginalEnvelope();
            
            // 创建带坐标系统的地理范围对象
            ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
                    originalEnvelope.getMinimum(0), // 最小X坐标（经度方向）
                    originalEnvelope.getMaximum(0), // 最大X坐标（经度方向）
                    originalEnvelope.getMinimum(1), // 最小Y坐标（纬度方向）
                    originalEnvelope.getMaximum(1), // 最大Y坐标（纬度方向）
                    tiffCRS                         // 原始坐标参考系统
            );

            // 解码Web墨卡托投影坐标系（EPSG:3857），这是TMS标准使用的坐标系
            CoordinateReferenceSystem webMercator = CRS.decode(DEFAULT_CRS);

            // 将地理范围从原始坐标系转换为Web墨卡托坐标系
            ReferencedEnvelope webMercatorEnvelope = referencedEnvelope.transform(webMercator, true);

            // 获取转换后的坐标边界（单位：米）
            double minX = webMercatorEnvelope.getMinX();  // 西边界
            double maxX = webMercatorEnvelope.getMaxX();  // 东边界  
            double minY = webMercatorEnvelope.getMinY();  // 南边界
            double maxY = webMercatorEnvelope.getMaxY();  // 北边界

            // 获取源图像的像素尺寸
            int imageWidth = sourceImage.getWidth();   // 图像宽度（像素）
            int imageHeight = sourceImage.getHeight(); // 图像高度（像素）
            
            log.info("图像尺寸: {}x{}, 地理范围: X[{}, {}], Y[{}, {}]", 
                    imageWidth, imageHeight, minX, maxX, minY, maxY);

            // ================ 2. 遍历缩放级别进行切片 ================
            for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
                log.info("处理缩放级别: {}", zoom);

                // 计算当前缩放级别的瓦片总数量（每个方向2^zoom个瓦片）
                int tilesPerAxis = (int) Math.pow(2, zoom);

                // 计算当前缩放级别下每个瓦片覆盖的地理范围大小（单位：米）
                double tileSize = WORLD_SIZE * 2 / tilesPerAxis;

                // ================ 3. 计算需要生成的瓦片范围 ================
                // 根据图像的地理边界计算需要生成的瓦片索引范围
                int xStart = (int) Math.floor((minX + WORLD_SIZE) / tileSize);
                int xEnd = (int) Math.ceil((maxX + WORLD_SIZE) / tileSize);
                // TMS坐标系：Y轴从下往上，所以需要特殊处理
                int yStart = (int) Math.floor((WORLD_SIZE - maxY) / tileSize);
                int yEnd = (int) Math.ceil((WORLD_SIZE - minY) / tileSize);

                // 确保瓦片索引在有效范围内（0 到 tilesPerAxis-1）
                xStart = Math.max(0, xStart);
                xEnd = Math.min(tilesPerAxis, xEnd);
                yStart = Math.max(0, yStart);
                yEnd = Math.min(tilesPerAxis, yEnd);

                log.debug("缩放级别 {} 瓦片范围: X[{}, {}), Y[{}, {})", zoom, xStart, xEnd, yStart, yEnd);

                // ================ 4. 生成每个瓦片 ================
                for (int x = xStart; x < xEnd; x++) {
                    for (int y = yStart; y < yEnd; y++) {
                        // 转换为TMS标准的Y坐标（TMS从下往上计数）
                        // int yTms = (1 << zoom) - y - 1;

                        // 生成单个瓦片
                        generateSingleTile(sourceImage, outPutPath, zoom, x, y, y, 
                                         tileSize, minX, maxX, minY, maxY, imageWidth, imageHeight);
                    }
                }
            }

            log.info("TMS切片任务完成，共处理 {} 个缩放级别", maxZoom - minZoom + 1);
            reader.dispose();

        } catch (IOException e) {
            log.error("TMS切片文件IO错误: {}", e.getMessage(), e);
            throw new RuntimeException("切片处理失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("TMS切片处理异常: {}", e.getMessage(), e);
            throw new RuntimeException("切片处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成单个瓦片 - 解决缝隙问题的核心方法
     * 采用精确计算和边界扩展策略，确保相邻瓦片无缝拼接
     * 
     * @param sourceImage 源图像
     * @param outPutPath 输出路径
     * @param zoom 当前缩放级别
     * @param x 瓦片X坐标
     * @param y 瓦片Y坐标（标准坐标系）
     * @param yTms 瓦片Y坐标（TMS坐标系）
     * @param tileSize 瓦片地理大小
     * @param minX 图像最小X坐标
     * @param maxX 图像最大X坐标
     * @param minY 图像最小Y坐标
     * @param maxY 图像最大Y坐标
     * @param imageWidth 图像宽度
     * @param imageHeight 图像高度
     */
    private void generateSingleTile(BufferedImage sourceImage, String outPutPath, int zoom, int x, int y, int yTms,
                                  double tileSize, double minX, double maxX, double minY, double maxY,
                                  int imageWidth, int imageHeight) throws IOException {
        
        // ================ 1. 创建瓦片画布 ================
        // 创建ARGB格式的透明瓦片图像（支持透明度）
        BufferedImage tileImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tileImage.createGraphics();
        
        // 设置透明背景
        g2d.setBackground(new Color(0, 0, 0, 0));
        g2d.clearRect(0, 0, TILE_SIZE, TILE_SIZE);
        
        // 设置高质量渲染参数，减少锯齿和失真
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            // ================ 2. 计算瓦片的地理边界 ================
            // 计算当前瓦片在Web墨卡托坐标系中的地理边界
            double tileMinX = x * tileSize - WORLD_SIZE;       // 瓦片西边界
            double tileMaxX = (x + 1) * tileSize - WORLD_SIZE; // 瓦片东边界
            double tileMinY = WORLD_SIZE - (y + 1) * tileSize; // 瓦片南边界（注意Y轴方向）
            double tileMaxY = WORLD_SIZE - y * tileSize;       // 瓦片北边界

            // ================ 3. 计算重叠区域 ================
            // 计算源图像与当前瓦片的重叠区域
            double overlapMinX = Math.max(tileMinX, minX);
            double overlapMaxX = Math.min(tileMaxX, maxX);
            double overlapMinY = Math.max(tileMinY, minY);
            double overlapMaxY = Math.min(tileMaxY, maxY);

            // 检查是否存在有效的重叠区域
            if (overlapMaxX <= overlapMinX || overlapMaxY <= overlapMinY) {
                // 无重叠区域，保存空白瓦片
                saveTile(outPutPath, tileImage, zoom, x, yTms);
                return;
            }

            // ================ 4. 计算源图像采样区域（防缝隙优化）================
            // 计算重叠区域在源图像中的比例坐标
            double xRatioMin = (overlapMinX - minX) / (maxX - minX);
            double xRatioMax = (overlapMaxX - minX) / (maxX - minX);
            // Y轴需要翻转（图像坐标系Y向下，地理坐标系Y向上）
            double yRatioMin = (maxY - overlapMaxY) / (maxY - minY);
            double yRatioMax = (maxY - overlapMinY) / (maxY - minY);

            // 关键优化：扩展采样区域以消除缝隙
            double pixelExpansion = 2.0 / Math.min(imageWidth, imageHeight); // 基于图像分辨率的扩展量
            xRatioMin = Math.max(0, xRatioMin - pixelExpansion);
            xRatioMax = Math.min(1, xRatioMax + pixelExpansion);
            yRatioMin = Math.max(0, yRatioMin - pixelExpansion);
            yRatioMax = Math.min(1, yRatioMax + pixelExpansion);

            // 转换为像素坐标（使用Math.round提高精度）
            int srcX = (int) Math.round(xRatioMin * imageWidth);
            int srcY = (int) Math.round(yRatioMin * imageHeight);
            int srcWidth = (int) Math.round((xRatioMax - xRatioMin) * imageWidth);
            int srcHeight = (int) Math.round((yRatioMax - yRatioMin) * imageHeight);

            // 确保采样区域在图像边界内
            srcX = Math.max(0, Math.min(srcX, imageWidth - 1));
            srcY = Math.max(0, Math.min(srcY, imageHeight - 1));
            srcWidth = Math.max(1, Math.min(srcWidth, imageWidth - srcX));
            srcHeight = Math.max(1, Math.min(srcHeight, imageHeight - srcY));

            // ================ 5. 计算瓦片内的绘制区域 ================
            // 计算重叠区域在瓦片内的位置比例
            double tileXRatioMin = (overlapMinX - tileMinX) / (tileMaxX - tileMinX);
            double tileXRatioMax = (overlapMaxX - tileMinX) / (tileMaxX - tileMinX);
            double tileYRatioMin = (tileMaxY - overlapMaxY) / (tileMaxY - tileMinY);
            double tileYRatioMax = (tileMaxY - overlapMinY) / (tileMaxY - tileMinY);

            // 转换为瓦片内的像素坐标（关键：扩展绘制区域）
            int destX = (int) Math.floor(tileXRatioMin * TILE_SIZE);
            int destY = (int) Math.floor(tileYRatioMin * TILE_SIZE);
            int destWidth = (int) Math.ceil((tileXRatioMax - tileXRatioMin) * TILE_SIZE);
            int destHeight = (int) Math.ceil((tileYRatioMax - tileYRatioMin) * TILE_SIZE);

            // 防缝隙关键优化：向外扩展1-2像素
            int expansion = 2;
            destX = Math.max(0, destX - expansion);
            destY = Math.max(0, destY - expansion);
            destWidth = Math.min(TILE_SIZE - destX, destWidth + expansion * 2);
            destHeight = Math.min(TILE_SIZE - destY, destHeight + expansion * 2);

            // 确保有效的绘制区域
            if (destWidth <= 0 || destHeight <= 0) {
                saveTile(outPutPath, tileImage, zoom, x, yTms);
                return;
            }

            // ================ 6. 执行图像绘制 ================
            // 获取源图像的子区域
            BufferedImage srcSubImage = sourceImage.getSubimage(srcX, srcY, srcWidth, srcHeight);

            // 将源图像绘制到瓦片上，使用高质量缩放
            g2d.drawImage(srcSubImage, destX, destY, destWidth, destHeight, null);

            // 保存生成的瓦片
            saveTile(outPutPath, tileImage, zoom, x, yTms);

        } finally {
            // 确保图形资源被正确释放
            g2d.dispose();
        }
    }

    /**
     * 保存瓦片图像到指定工作目录 - 优化版本（严格遵循TMS规范）
     * 采用标准的TMS（Tile Map Service）目录结构和命名规范
     * 
     * @param workspace 切片存储根目录路径
     * @param tileImage 要保存的瓦片图像对象（256x256像素）
     * @param zoom 当前缩放级别（0为最小级别）
     * @param x 瓦片的X坐标（西东方向，从左到右递增）
     * @param y 瓦片的Y坐标（TMS坐标系，从下往上递增）
     * @throws IOException 当文件系统操作失败时抛出
     */
    private void saveTile(String workspace, BufferedImage tileImage, int zoom, int x, int y) throws IOException {
        // ================ 1. 参数验证 ================
        if (workspace == null || workspace.trim().isEmpty()) {
            throw new IllegalArgumentException("工作空间路径不能为空");
        }
        
        if (tileImage == null) {
            throw new IllegalArgumentException("瓦片图像不能为空");
        }
        
        if (zoom < 0 || zoom > 30) {
            throw new IllegalArgumentException("缩放级别超出有效范围 [0, 30]: " + zoom);
        }
        
        // 计算当前缩放级别的最大瓦片坐标
        int maxTileCoord = (1 << zoom) - 1;
        if (x < 0 || x > maxTileCoord || y < 0 || y > maxTileCoord) {
            throw new IllegalArgumentException(String.format(
                "瓦片坐标超出范围，缩放级别 %d 的有效范围是 [0, %d]: x=%d, y=%d", 
                zoom, maxTileCoord, x, y));
        }

        // ================ 2. 构建TMS标准路径 ================
        // TMS路径结构: {workspace}/{zoom}/{x}/{y}.png
        // 使用正斜杠确保跨平台兼容性（File.separator在不同系统下不同）
        String tilePath = String.format("%s/%d/%d/%d.png", workspace, zoom, x, y);

        // 创建文件对象
        File tileFile = new File(tilePath);

        try {
            // ================ 3. 确保目录结构存在 ================
            // 获取父目录并确保其存在
            File parentDir = tileFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                // mkdirs() 会创建所有必需的父目录
                boolean dirCreated = parentDir.mkdirs();
                if (!dirCreated && !parentDir.exists()) {
                    throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
                }
            }

            // ================ 4. 验证瓦片图像质量 ================
            // 检查瓦片尺寸是否符合标准
            if (tileImage.getWidth() != TILE_SIZE || tileImage.getHeight() != TILE_SIZE) {
                log.warn("瓦片尺寸非标准: {}x{}, 应为 {}x{}", 
                        tileImage.getWidth(), tileImage.getHeight(), TILE_SIZE, TILE_SIZE);
            }

            // ================ 5. 保存瓦片图像 ================
            // 使用PNG格式保存，PNG特点：
            // - 无损压缩，保证图像质量
            // - 支持透明度（Alpha通道）
            // - 压缩效率好，适合地图瓦片
            boolean saveSuccess = ImageIO.write(tileImage, "png", tileFile);
            
            if (!saveSuccess) {
                throw new IOException("ImageIO.write 返回 false，可能没有找到合适的PNG编码器");
            }

            // ================ 6. 验证保存结果 ================
            // 检查文件是否真正创建且大小合理
            if (!tileFile.exists()) {
                throw new IOException("瓦片文件保存后不存在: " + tilePath);
            }
            
            long fileSize = tileFile.length();
            if (fileSize == 0) {
                // 删除空文件
                tileFile.delete();
                throw new IOException("保存的瓦片文件为空: " + tilePath);
            }
            
            // 记录详细的保存信息（调试时有用）
            log.debug("瓦片保存成功: {} ({}字节)", tilePath, fileSize);

        } catch (IOException e) {
            // 保存失败时的清理工作
            if (tileFile.exists()) {
                try {
                    tileFile.delete();
                    log.debug("删除失败的瓦片文件: {}", tilePath);
                } catch (Exception cleanupEx) {
                    log.warn("清理失败瓦片文件时出错: {}", cleanupEx.getMessage());
                }
            }
            
            // 重新抛出异常，包含更多上下文信息
            throw new IOException(String.format(
                "保存瓦片失败 [缩放级别=%d, 坐标=(%d,%d), 路径=%s]: %s", 
                zoom, x, y, tilePath, e.getMessage()), e);
        }
    }

    @Override
    public String tmsCutOfFile(MultipartFile file, String workspace, String type, Integer minZoom, Integer maxZoom) {
        try {
            // 开始时间
            LocalDateTime startDate = LocalDateTime.now();
            // 创建临时文件
            File tempFile = File.createTempFile("temp", null);
            // 将 MultipartFile 内容传输到临时文件
            file.transferTo(tempFile);
            // 创建 Tiff 读取器
            GeoTiffReader reader = new GeoTiffReader(tempFile);

            // 检查文件大小，决定是否使用分块处理
            long fileSize = tempFile.length();
            boolean useBlockProcessing = fileSize > 500 * 1024 * 1024; // 500MB以上使用分块处理

            log.info("处理文件: {}, 大小: {} MB, 使用分块处理: {}",
                    tempFile.getName(), fileSize / (1024 * 1024), useBlockProcessing);

            if (useBlockProcessing) {
                // 使用分块处理
                processLargeFileWithBlocks(reader, tempFile, tilesBaseDir + workspace, minZoom, maxZoom);
            } else {
                // 使用传统方式处理
                BufferedImage sourceImage = ImageIO.read(tempFile);
                if (Objects.isNull(sourceImage)) {
                    return "无法读取图像文件";
                }

                // 进行 TMS 切片
                mainTmsCut(reader, sourceImage, tilesBaseDir + workspace, minZoom, maxZoom);
            }

            // 删除临时文件
            if (tempFile.delete()) {
                log.info("临时文件已删除");
            }

            // 结束时间
            LocalDateTime endDate = LocalDateTime.now();
            
            // 创建地图记录对象
            MapRecordPo mapRecordPo = new MapRecordPo();
            mapRecordPo.setId(UUID.randomUUID().toString());
            mapRecordPo.setStartTime(startDate);
            mapRecordPo.setEndTime(endDate);
            mapRecordPo.setFilePath(tempFile.getPath());
            mapRecordPo.setFileSize(String.valueOf(file.getSize()));
            mapRecordPo.setFileType(type);
            mapRecordPo.setType(1);
            mapRecordPo.setWorkspaceGroup(workspace.split("/")[0]);
            mapRecordPo.setWorkspace(workspace.split("/")[1]);
            mapRecordPo.setOutputPath(tilesBaseDir + workspace);
            mapRecordPo.setFileName(file.getOriginalFilename());
            mapRecordPo.setZoomMin(minZoom);
            mapRecordPo.setZoomMax(maxZoom);
            mapRecordPo.setCreateTime(LocalDateTime.now());
            
            // 保存记录
            mapRecordService.save(mapRecordPo);
            
            log.info("分块切片处理完成");

        } catch (Exception e) {
            log.error("分块切片处理失败", e);
            return "切片失败: " + e.getMessage();
        }
        return "切片完成";
    }

    @Override
    public String tmsCut(MapCutRequestDto mapCutRequestDto) {
        // 开始时间
        LocalDateTime startDate = LocalDateTime.now();
        // 获取工作空间
        String workspace = tilesBaseDir + "/" + mapCutRequestDto.getWorkspaceGroup() + "/" + mapCutRequestDto.getWorkspace();
        // 获取文件路径
        String tifDir = mapCutRequestDto.getTifDir();
        // 获取最小层级
        Integer minZoom = mapCutRequestDto.getMinZoom();
        // 获取最大层级
        Integer maxZoom = mapCutRequestDto.getMaxZoom();

        // 获取成功回调地址
        String backSuccessUrl = mapCutRequestDto.getBackSuccessUrl();
        // 获取失败回调地址
        String backFailUrl = mapCutRequestDto.getBackFailUrl();

        try {
            // 创建文件
            File inputFile = new File(tifDir);
            // 创建 Tiff 读取器
            GeoTiffReader reader = new GeoTiffReader(inputFile);
            
            // 检查文件大小，决定是否使用分块处理
            long fileSize = inputFile.length();
            boolean useBlockProcessing = fileSize > 500 * 1024 * 1024; // 500MB以上使用分块处理
            
            log.info("处理文件: {}, 大小: {} MB, 使用分块处理: {}", 
                    inputFile.getName(), fileSize / (1024 * 1024), useBlockProcessing);

            if (useBlockProcessing) {
                // 使用分块处理
                processLargeFileWithBlocks(reader, inputFile, workspace, minZoom, maxZoom);
            } else {
                // 使用传统方式处理
                BufferedImage sourceImage = ImageIO.read(inputFile);
                if (Objects.isNull(sourceImage)) {
                    String errorMsg = "无法读取图像文件";
                    if (!StringUtils.isEmpty(backFailUrl)) {
                        HttpUtils.sendGet(backFailUrl + "/" + URLEncoder.encode(errorMsg, "UTF-8"), null);
                    }
                    return errorMsg;
                }
                
                // 进行 TMS 切片
                mainTmsCut(reader, sourceImage, workspace, minZoom, maxZoom);
            }

            // 结束时间
            LocalDateTime endDate = LocalDateTime.now();
            // 创建地图记录对象
            MapRecordPo mapRecordPo = new MapRecordPo();
            mapRecordPo.setId(UUID.randomUUID().toString());
            mapRecordPo.setStartTime(startDate);
            mapRecordPo.setEndTime(endDate);
            mapRecordPo.setFilePath(tifDir);
            mapRecordPo.setFileSize(String.valueOf(inputFile.length()));
            mapRecordPo.setFileType(mapCutRequestDto.getType());
            mapRecordPo.setType(1);
            mapRecordPo.setWorkspaceGroup(mapCutRequestDto.getWorkspaceGroup());
            mapRecordPo.setWorkspace(mapCutRequestDto.getWorkspace());
            mapRecordPo.setOutputPath(workspace);
            mapRecordPo.setFileName(inputFile.getName());
            mapRecordPo.setZoomMin(minZoom);
            mapRecordPo.setZoomMax(maxZoom);
            mapRecordPo.setCreateTime(LocalDateTime.now());
            // 保存
            mapRecordService.save(mapRecordPo);

            log.info("TMS切片处理完成: {}", inputFile.getName());

        } catch (Exception e) {
            try {
                if (!StringUtils.isEmpty(backFailUrl)) {
                    HttpUtils.sendGet(backFailUrl + "/" + URLEncoder.encode("切片失败", "UTF-8"), null);
                }
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                log.error("回调URL编码失败", unsupportedEncodingException);
            }
            log.error("TMS切片处理失败", e);
            return "切片失败: " + e.getMessage();
        }
        
        try {
            if (!StringUtils.isEmpty(backSuccessUrl)) {
                HttpUtils.sendGet(backSuccessUrl + "/" + URLEncoder.encode("切片完成", "UTF-8"), null);
            }
        } catch (UnsupportedEncodingException e) {
            log.error("成功回调URL编码失败", e);
        }
        return "切片完成";
    }

    /**
     * 处理大文件的分块方法 - 防缝隙优化版本
     * 采用瓦片级别的缓存合并机制，彻底解决分块间缝隙问题
     * 
     * @param reader GeoTIFF读取器，用于获取地理信息
     * @param inputFile 输入的大型GeoTIFF文件
     * @param workspace 工作空间路径，瓦片输出目录
     * @param minZoom 最小缩放级别
     * @param maxZoom 最大缩放级别
     * @throws IOException 当文件读取或处理失败时抛出
     */
    private void processLargeFileWithBlocks(GeoTiffReader reader, File inputFile, 
                                          String workspace, Integer minZoom, Integer maxZoom) 
            throws IOException {
        
        log.info("开始大文件分块处理（防缝隙版本）: {}", inputFile.getName());
        
        // ================ 1. 计算最优分块策略 ================
        long fileSizeInMB = inputFile.length() / (1024 * 1024);
        int blockCount = calculateOptimalBlockCount(fileSizeInMB);
        
        log.info("文件大小: {} MB, 使用分块数量: {}x{}", fileSizeInMB, blockCount, blockCount);
        
        // ================ 2. 创建瓦片合并管理器 ================
        // 关键改进：使用瓦片缓存来合并重叠区域的瓦片
        TileMergeManager tileMergeManager = new TileMergeManager(workspace, minZoom, maxZoom);
        
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputFile)) {
            if (imageInputStream == null) {
                throw new IOException("无法创建图像输入流，可能文件格式不支持");
            }
            
            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
            if (!imageReaders.hasNext()) {
                throw new IOException("没有找到合适的图像读取器，请检查文件格式");
            }
            
            ImageReader imageReader = imageReaders.next();
            imageReader.setInput(imageInputStream);
            
            try {
                // ================ 3. 获取原图信息和地理参数 ================
                int totalWidth = imageReader.getWidth(0);
                int totalHeight = imageReader.getHeight(0);
                
                if (totalWidth <= 0 || totalHeight <= 0) {
                    throw new IOException("无效的图像尺寸: " + totalWidth + "x" + totalHeight);
                }
                
                // 获取地理参数用于瓦片对齐计算
                CoordinateReferenceSystem tiffCRS = reader.getCoordinateReferenceSystem();
                Envelope originalEnvelope = reader.getOriginalEnvelope();
                ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
                        originalEnvelope.getMinimum(0), originalEnvelope.getMaximum(0),
                        originalEnvelope.getMinimum(1), originalEnvelope.getMaximum(1), tiffCRS);
                CoordinateReferenceSystem webMercator = CRS.decode(DEFAULT_CRS);
                ReferencedEnvelope webMercatorEnvelope = referencedEnvelope.transform(webMercator, true);
                
                // 计算分块策略 - 关键改进：确保分块边界与瓦片边界对齐
                BlockStrategy blockStrategy = calculateAlignedBlockStrategy(
                        blockCount, totalWidth, totalHeight, webMercatorEnvelope, minZoom, maxZoom);
                
                log.info("图像信息 - 总尺寸: {}x{}, 分块策略: {} 个分块", 
                        totalWidth, totalHeight, blockStrategy.getBlockCount());

                // ================ 4. 分块处理 ================
                int totalBlocks = blockStrategy.getBlockCount();
                int processedBlocks = 0;
                
                for (BlockInfo blockInfo : blockStrategy.getBlocks()) {
                    processedBlocks++;
                    log.info("处理分块 [{}/{}] 区域: ({},{}) {}x{}", 
                            processedBlocks, totalBlocks,
                            blockInfo.getX(), blockInfo.getY(), 
                            blockInfo.getWidth(), blockInfo.getHeight());
                    
                    try {
                        // 读取分块图像（包含重叠区域）
                        ImageReadParam readParam = imageReader.getDefaultReadParam();
                        readParam.setSourceRegion(new Rectangle(
                                blockInfo.getExpandedX(), blockInfo.getExpandedY(),
                                blockInfo.getExpandedWidth(), blockInfo.getExpandedHeight()));

                        BufferedImage blockImage = imageReader.read(0, readParam);
                        
                        if (Objects.isNull(blockImage) || blockImage.getWidth() <= 0 || blockImage.getHeight() <= 0) {
                            log.warn("分块图像无效，跳过分块 {}", processedBlocks);
                            continue;
                        }

                        // 处理分块切片并累积到瓦片管理器
                        processBlockWithTileMerging(reader, blockImage, tileMergeManager, 
                                minZoom, maxZoom, blockInfo, webMercatorEnvelope, 
                                totalWidth, totalHeight);
                        
                        // 释放内存
                        blockImage.flush();
                        
                        log.info("分块 {} 处理完成 - 进度: {}/{}", processedBlocks, processedBlocks, totalBlocks);
                        
                    } catch (Exception e) {
                        log.error("处理分块 {} 时发生错误: {}", processedBlocks, e.getMessage(), e);
                    }
                    
                    // 内存管理
                    if (processedBlocks % 5 == 0) {
                        System.gc();
                        log.debug("执行垃圾回收，当前进度: {}/{}", processedBlocks, totalBlocks);
                    }
                }
                
                // ================ 5. 合并并保存所有瓦片 ================
                log.info("开始合并和保存瓦片...");
                tileMergeManager.mergeAndSaveAllTiles();
                log.info("所有瓦片合并保存完成");
                
            } finally {
                imageReader.dispose();
            }
        } catch (IOException e) {
            log.error("大文件分块处理IO错误: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("大文件分块处理异常: {}", e.getMessage(), e);
            throw new IOException("分块处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算最优分块数量
     * 根据文件大小智能调整分块策略，平衡内存使用和处理效率
     * 
     * @param fileSizeInMB 文件大小（MB）
     * @return 最优的分块数量（每个方向）
     */
    private int calculateOptimalBlockCount(long fileSizeInMB) {
        if (fileSizeInMB < 500) {
            return 5;   // 小文件：5x5 = 25块
        } else if (fileSizeInMB < 1000) {
            return 8;   // 中等文件：8x8 = 64块
        } else if (fileSizeInMB < 2000) {
            return 10;  // 大文件：10x10 = 100块
        } else {
            return 12;  // 超大文件：12x12 = 144块
        }
    }

    /**
     * 计算分块重叠大小
     * 根据分块尺寸计算合适的重叠区域，用于消除分块边界缝隙
     * 
     * @param blockWidth 分块宽度
     * @param blockHeight 分块高度
     * @return 重叠区域大小（像素）
     */
    private int calculateOverlapSize(int blockWidth, int blockHeight) {
        // 基于分块尺寸的动态重叠计算
        int minDimension = Math.min(blockWidth, blockHeight);
        
        if (minDimension < 1000) {
            return 5;   // 小分块：5像素重叠
        } else if (minDimension < 2000) {
            return 10;  // 中等分块：10像素重叠
        } else {
            return 20;  // 大分块：20像素重叠
        }
    }

    /**
     * 分块TMS切割方法 - 优化版本，解决分块间和瓦片间的缝隙问题
     * 采用精确的地理坐标计算和边界扩展策略，确保分块处理的连续性
     * 
     * @param reader GeoTIFF文件读取器，用于获取地理信息和坐标系统
     * @param blockImage 分块图像对象，当前需要处理的图片分块
     * @param outPutPath 输出路径，瓦片保存的根目录
     * @param minZoom 最小缩放级别，开始切割的层级
     * @param maxZoom 最大缩放级别，结束切割的层级
     * @param blockOffsetX 分块在原图中的X偏移量（像素坐标）
     * @param blockOffsetY 分块在原图中的Y偏移量（像素坐标）
     * @param totalWidth 原图总宽度（像素）
     * @param totalHeight 原图总高度（像素）
     */
    private void mainTmsCutForBlock(GeoTiffReader reader, BufferedImage blockImage, String outPutPath, 
                                  Integer minZoom, Integer maxZoom, int blockOffsetX, int blockOffsetY, 
                                  int totalWidth, int totalHeight) {
        try {
            log.debug("开始处理分块 - 偏移: ({}, {}), 尺寸: {}x{}", 
                     blockOffsetX, blockOffsetY, blockImage.getWidth(), blockImage.getHeight());
            
            // ================ 1. 获取和转换坐标系统 ================
            // 获取TIFF文件的原始坐标参考系统
            CoordinateReferenceSystem tiffCRS = reader.getCoordinateReferenceSystem();
            // 获取原始图像的地理范围边界
            Envelope originalEnvelope = reader.getOriginalEnvelope();
            
            // 创建带坐标系统的地理范围对象
            ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(
                    originalEnvelope.getMinimum(0), // 原图最小X坐标
                    originalEnvelope.getMaximum(0), // 原图最大X坐标
                    originalEnvelope.getMinimum(1), // 原图最小Y坐标
                    originalEnvelope.getMaximum(1), // 原图最大Y坐标
                    tiffCRS                         // 原始坐标参考系统
            );

            // 解码Web墨卡托投影坐标系（EPSG:3857）
            CoordinateReferenceSystem webMercator = CRS.decode(DEFAULT_CRS);

            // 将地理范围从原始坐标系转换为Web墨卡托坐标系
            ReferencedEnvelope webMercatorEnvelope = referencedEnvelope.transform(webMercator, true);

            // 获取整个原图的地理范围（Web墨卡托坐标系，单位：米）
            double totalMinX = webMercatorEnvelope.getMinX();  // 原图西边界
            double totalMaxX = webMercatorEnvelope.getMaxX();  // 原图东边界
            double totalMinY = webMercatorEnvelope.getMinY();  // 原图南边界
            double totalMaxY = webMercatorEnvelope.getMaxY();  // 原图北边界

            // ================ 2. 计算当前分块的精确地理范围 ================
            // 计算每个像素对应的地理单位（米/像素）
            double pixelSizeX = (totalMaxX - totalMinX) / totalWidth;   // X方向像素分辨率
            double pixelSizeY = (totalMaxY - totalMinY) / totalHeight;  // Y方向像素分辨率
            
            // 计算当前分块的精确地理边界
            double blockMinX = totalMinX + blockOffsetX * pixelSizeX;
            double blockMaxX = totalMinX + (blockOffsetX + blockImage.getWidth()) * pixelSizeX;
            // 注意：图像坐标系Y向下，地理坐标系Y向上，需要进行坐标转换
            double blockMaxY = totalMaxY - blockOffsetY * pixelSizeY;
            double blockMinY = totalMaxY - (blockOffsetY + blockImage.getHeight()) * pixelSizeY;

            // 获取分块图像的像素尺寸
            int blockWidth = blockImage.getWidth();
            int blockHeight = blockImage.getHeight();

            log.debug("分块地理信息 - 范围: X[{:.2f}, {:.2f}], Y[{:.2f}, {:.2f}], 像素密度: {:.6f}m/px", 
                     blockMinX, blockMaxX, blockMinY, blockMaxY, pixelSizeX);

            // ================ 3. 遍历缩放级别进行分块切片 ================
            for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
                log.debug("处理分块缩放级别: {}", zoom);

                // 计算当前缩放级别的瓦片总数量（每个方向2^zoom个瓦片）
                int tilesPerAxis = (int) Math.pow(2, zoom);

                // 计算当前缩放级别下每个瓦片覆盖的地理范围大小（单位：米）
                double tileSize = WORLD_SIZE * 2 / tilesPerAxis;

                // ================ 4. 计算需要生成的瓦片范围 ================
                // 根据分块的地理边界计算需要生成的瓦片索引范围
                int xStart = (int) Math.floor((blockMinX + WORLD_SIZE) / tileSize);
                int xEnd = (int) Math.ceil((blockMaxX + WORLD_SIZE) / tileSize);
                // TMS坐标系：Y轴从下往上计数
                int yStart = (int) Math.floor((WORLD_SIZE - blockMaxY) / tileSize);
                int yEnd = (int) Math.ceil((WORLD_SIZE - blockMinY) / tileSize);

                // 确保瓦片索引在有效范围内
                xStart = Math.max(0, xStart);
                xEnd = Math.min(tilesPerAxis, xEnd);
                yStart = Math.max(0, yStart);
                yEnd = Math.min(tilesPerAxis, yEnd);

                // ================ 5. 生成每个瓦片 ================
                for (int x = xStart; x < xEnd; x++) {
                    for (int y = yStart; y < yEnd; y++) {
                        // 转换为TMS标准的Y坐标（TMS从下往上计数）
                        // int yTms = (1 << zoom) - y - 1;

                        // 生成单个瓦片（使用优化的方法）
                        generateSingleTileFromBlock(blockImage, outPutPath, zoom, x, y, y, 
                                                  tileSize, blockMinX, blockMaxX, blockMinY, blockMaxY, 
                                                  blockWidth, blockHeight);
                    }
                }
            }

            log.debug("分块切片处理完成 - 偏移: ({}, {})", blockOffsetX, blockOffsetY);

        } catch (IOException e) {
            log.error("分块切片IO错误 - 偏移: ({}, {}), 错误: {}", blockOffsetX, blockOffsetY, e.getMessage(), e);
            throw new RuntimeException("分块切片处理失败", e);
        } catch (Exception e) {
            log.error("分块切片处理异常 - 偏移: ({}, {}), 错误: {}", blockOffsetX, blockOffsetY, e.getMessage(), e);
            throw new RuntimeException("分块切片处理失败", e);
        }
    }

    /**
     * 从分块图像生成单个瓦片 - 防缝隙优化版本
     * 专门处理分块图像的瓦片生成，确保分块边界处理正确
     * 
     * @param blockImage 分块图像
     * @param outPutPath 输出路径
     * @param zoom 当前缩放级别
     * @param x 瓦片X坐标
     * @param y 瓦片Y坐标（标准坐标系）
     * @param yTms 瓦片Y坐标（TMS坐标系）
     * @param tileSize 瓦片地理大小
     * @param blockMinX 分块最小X坐标
     * @param blockMaxX 分块最大X坐标
     * @param blockMinY 分块最小Y坐标
     * @param blockMaxY 分块最大Y坐标
     * @param blockWidth 分块图像宽度
     * @param blockHeight 分块图像高度
     */
    private void generateSingleTileFromBlock(BufferedImage blockImage, String outPutPath, int zoom, int x, int y, int yTms,
                                           double tileSize, double blockMinX, double blockMaxX, double blockMinY, double blockMaxY,
                                           int blockWidth, int blockHeight) throws IOException {

        // ================ 1. 创建瓦片画布 ================
        // 创建ARGB格式的透明瓦片图像（支持透明度）
        BufferedImage tileImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tileImage.createGraphics();

        // 设置透明背景
        g2d.setBackground(new Color(0, 0, 0, 0));
        g2d.clearRect(0, 0, TILE_SIZE, TILE_SIZE);

        // 设置高质量渲染参数（比原版更高质量）
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        try {
            // ================ 2. 计算瓦片的地理边界 ================
            // 计算当前瓦片在Web墨卡托坐标系中的地理边界
            double tileMinX = x * tileSize - WORLD_SIZE;       // 瓦片西边界
            double tileMaxX = (x + 1) * tileSize - WORLD_SIZE; // 瓦片东边界
            double tileMinY = WORLD_SIZE - (y + 1) * tileSize; // 瓦片南边界
            double tileMaxY = WORLD_SIZE - y * tileSize;       // 瓦片北边界

            // ================ 3. 计算重叠区域 ================
            // 计算分块图像与当前瓦片的重叠区域
            double overlapMinX = Math.max(tileMinX, blockMinX);
            double overlapMaxX = Math.min(tileMaxX, blockMaxX);
            double overlapMinY = Math.max(tileMinY, blockMinY);
            double overlapMaxY = Math.min(tileMaxY, blockMaxY);

            // 检查是否存在有效的重叠区域
            if (overlapMaxX <= overlapMinX || overlapMaxY <= overlapMinY) {
                // 无重叠区域，保存空白瓦片
                saveTile(outPutPath, tileImage, zoom, x, yTms);
                return;
            }

            // ================ 4. 计算分块图像采样区域（防缝隙优化）================
            // 计算重叠区域在分块图像中的比例坐标
            double xRatioMin = (overlapMinX - blockMinX) / (blockMaxX - blockMinX);
            double xRatioMax = (overlapMaxX - blockMinX) / (blockMaxX - blockMinX);
            // Y轴需要翻转（图像坐标系Y向下，地理坐标系Y向上）
            double yRatioMin = (blockMaxY - overlapMaxY) / (blockMaxY - blockMinY);
            double yRatioMax = (blockMaxY - overlapMinY) / (blockMaxY - blockMinY);

            // 关键优化：扩展采样区域以消除分块边界缝隙
            double pixelExpansion = 3.0 / Math.min(blockWidth, blockHeight); // 基于分块分辨率的扩展量
            xRatioMin = Math.max(0, xRatioMin - pixelExpansion);
            xRatioMax = Math.min(1, xRatioMax + pixelExpansion);
            yRatioMin = Math.max(0, yRatioMin - pixelExpansion);
            yRatioMax = Math.min(1, yRatioMax + pixelExpansion);

            // 转换为像素坐标（使用Math.round提高精度）
            int srcX = (int) Math.round(xRatioMin * blockWidth);
            int srcY = (int) Math.round(yRatioMin * blockHeight);
            int srcWidth = (int) Math.round((xRatioMax - xRatioMin) * blockWidth);
            int srcHeight = (int) Math.round((yRatioMax - yRatioMin) * blockHeight);

            // 确保采样区域在分块图像边界内
            srcX = Math.max(0, Math.min(srcX, blockWidth - 1));
            srcY = Math.max(0, Math.min(srcY, blockHeight - 1));
            srcWidth = Math.max(1, Math.min(srcWidth, blockWidth - srcX));
            srcHeight = Math.max(1, Math.min(srcHeight, blockHeight - srcY));

            // ================ 5. 计算瓦片内的绘制区域 ================
            // 计算重叠区域在瓦片内的位置比例
            double tileXRatioMin = (overlapMinX - tileMinX) / (tileMaxX - tileMinX);
            double tileXRatioMax = (overlapMaxX - tileMinX) / (tileMaxX - tileMinX);
            double tileYRatioMin = (tileMaxY - overlapMaxY) / (tileMaxY - tileMinY);
            double tileYRatioMax = (tileMaxY - overlapMinY) / (tileMaxY - tileMinY);

            // 转换为瓦片内的像素坐标（关键：扩展绘制区域）
            int destX = (int) Math.floor(tileXRatioMin * TILE_SIZE);
            int destY = (int) Math.floor(tileYRatioMin * TILE_SIZE);
            int destWidth = (int) Math.ceil((tileXRatioMax - tileXRatioMin) * TILE_SIZE);
            int destHeight = (int) Math.ceil((tileYRatioMax - tileYRatioMin) * TILE_SIZE);

            // 防分块缝隙关键优化：向外扩展更多像素（分块处理需要更大的重叠）
            int expansion = 3;
            destX = Math.max(0, destX - expansion);
            destY = Math.max(0, destY - expansion);
            destWidth = Math.min(TILE_SIZE - destX, destWidth + expansion * 2);
            destHeight = Math.min(TILE_SIZE - destY, destHeight + expansion * 2);

            // 确保有效的绘制区域
            if (destWidth <= 0 || destHeight <= 0) {
                saveTile(outPutPath, tileImage, zoom, x, yTms);
                return;
            }

            // ================ 6. 执行图像绘制 ================
            // 获取分块图像的子区域
            BufferedImage srcSubImage = blockImage.getSubimage(srcX, srcY, srcWidth, srcHeight);

            // 将源图像绘制到瓦片上，使用高质量缩放
            g2d.drawImage(srcSubImage, destX, destY, destWidth, destHeight, null);

            // 保存生成的瓦片
            saveTile(outPutPath, tileImage, zoom, x, yTms);

        } finally {
            // 确保图形资源被正确释放
            g2d.dispose();
        }
    }

    // ================ 支持类和方法 - 解决分块缝隙问题 ================

    /**
     * 分块信息类 - 存储分块的位置和尺寸信息
     */
    private static class BlockInfo {
        private final int x, y, width, height;
        private final int expandedX, expandedY, expandedWidth, expandedHeight;
        
        public BlockInfo(int x, int y, int width, int height, 
                        int expandedX, int expandedY, int expandedWidth, int expandedHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.expandedX = expandedX;
            this.expandedY = expandedY;
            this.expandedWidth = expandedWidth;
            this.expandedHeight = expandedHeight;
        }
        
        // Getters
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getExpandedX() { return expandedX; }
        public int getExpandedY() { return expandedY; }
        public int getExpandedWidth() { return expandedWidth; }
        public int getExpandedHeight() { return expandedHeight; }
    }

    /**
     * 分块策略类 - 管理所有分块的信息
     */
    private static class BlockStrategy {
        private final List<BlockInfo> blocks;
        
        public BlockStrategy(List<BlockInfo> blocks) {
            this.blocks = blocks;
        }
        
        public List<BlockInfo> getBlocks() { return blocks; }
        public int getBlockCount() { return blocks.size(); }
    }

    /**
     * 瓦片合并管理器 - 关键类，解决分块间缝隙问题
     */
    private class TileMergeManager {
        private final String workspace;
        private final int minZoom, maxZoom;
        private final Map<String, List<BufferedImage>> tileBuffer = new ConcurrentHashMap<>();
        
        public TileMergeManager(String workspace, int minZoom, int maxZoom) {
            this.workspace = workspace;
            this.minZoom = minZoom;
            this.maxZoom = maxZoom;
        }
        
        /**
         * 添加瓦片到缓冲区
         */
        public synchronized void addTile(int zoom, int x, int y, BufferedImage tileImage) {
            String key = zoom + "_" + x + "_" + y;
            tileBuffer.computeIfAbsent(key, k -> new ArrayList<>()).add(deepCopyImage(tileImage));
        }
        
        /**
         * 合并并保存所有瓦片
         */
        public void mergeAndSaveAllTiles() throws IOException {
            for (String key : tileBuffer.keySet()) {
                String[] parts = key.split("_");
                int zoom = Integer.parseInt(parts[0]);
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                
                List<BufferedImage> tileParts = tileBuffer.get(key);
                BufferedImage mergedTile = mergeTileParts(tileParts);
                
                if (mergedTile != null) {
                    saveTile(workspace, mergedTile, zoom, x, y);
                }
            }
            
            // 清理缓冲区
            tileBuffer.clear();
        }
        
        /**
         * 合并多个瓦片部分
         */
        private BufferedImage mergeTileParts(List<BufferedImage> tileParts) {
            if (tileParts.isEmpty()) return null;
            if (tileParts.size() == 1) return tileParts.get(0);
            
            BufferedImage result = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
            
            // 设置透明背景
            g2d.setBackground(new Color(0, 0, 0, 0));
            g2d.clearRect(0, 0, TILE_SIZE, TILE_SIZE);
            
            // 设置合成模式为源覆盖，重叠区域使用平均值
            g2d.setComposite(AlphaComposite.SrcOver);
            
            try {
                // 绘制所有瓦片部分
                for (BufferedImage tilePart : tileParts) {
                    if (tilePart != null) {
                        g2d.drawImage(tilePart, 0, 0, null);
                    }
                }
            } finally {
                g2d.dispose();
            }
            
            return result;
        }
        
        /**
         * 深度复制图像
         */
        private BufferedImage deepCopyImage(BufferedImage original) {
            BufferedImage copy = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());
            Graphics2D g2d = copy.createGraphics();
            g2d.drawImage(original, 0, 0, null);
            g2d.dispose();
            return copy;
        }
    }

    /**
     * 计算对齐的分块策略 - 确保分块边界与瓦片边界对齐
     */
    private BlockStrategy calculateAlignedBlockStrategy(int blockCount, int totalWidth, int totalHeight, 
                                                       ReferencedEnvelope webMercatorEnvelope, 
                                                       int minZoom, int maxZoom) {
        List<BlockInfo> blocks = new ArrayList<>();
        
        // 计算基础分块尺寸
        int baseBlockWidth = totalWidth / blockCount;
        int baseBlockHeight = totalHeight / blockCount;
        
        // 计算重叠大小
        int overlap = Math.max(50, Math.min(baseBlockWidth, baseBlockHeight) / 10);
        
        for (int row = 0; row < blockCount; row++) {
            for (int col = 0; col < blockCount; col++) {
                // 计算基础分块区域
                int x = col * baseBlockWidth;
                int y = row * baseBlockHeight;
                int w = (col == blockCount - 1) ? totalWidth - x : baseBlockWidth;
                int h = (row == blockCount - 1) ? totalHeight - y : baseBlockHeight;
                
                // 计算扩展区域（包含重叠）
                int expandedX = Math.max(0, x - overlap);
                int expandedY = Math.max(0, y - overlap);
                int expandedW = Math.min(totalWidth - expandedX, w + (x > 0 ? overlap : 0) + (x + w < totalWidth ? overlap : 0));
                int expandedH = Math.min(totalHeight - expandedY, h + (y > 0 ? overlap : 0) + (y + h < totalHeight ? overlap : 0));
                
                blocks.add(new BlockInfo(x, y, w, h, expandedX, expandedY, expandedW, expandedH));
            }
        }
        
        return new BlockStrategy(blocks);
    }

    /**
     * 使用瓦片合并的分块处理方法
     */
    private void processBlockWithTileMerging(GeoTiffReader reader, BufferedImage blockImage, 
                                           TileMergeManager tileMergeManager, int minZoom, int maxZoom, 
                                           BlockInfo blockInfo, ReferencedEnvelope webMercatorEnvelope,
                                           int totalWidth, int totalHeight) throws Exception {
        
        // 计算分块的地理范围
        double totalMinX = webMercatorEnvelope.getMinX();
        double totalMaxX = webMercatorEnvelope.getMaxX();
        double totalMinY = webMercatorEnvelope.getMinY();
        double totalMaxY = webMercatorEnvelope.getMaxY();
        
        double pixelSizeX = (totalMaxX - totalMinX) / totalWidth;
        double pixelSizeY = (totalMaxY - totalMinY) / totalHeight;
        
        double blockMinX = totalMinX + blockInfo.getExpandedX() * pixelSizeX;
        double blockMaxX = totalMinX + (blockInfo.getExpandedX() + blockInfo.getExpandedWidth()) * pixelSizeX;
        double blockMaxY = totalMaxY - blockInfo.getExpandedY() * pixelSizeY;
        double blockMinY = totalMaxY - (blockInfo.getExpandedY() + blockInfo.getExpandedHeight()) * pixelSizeY;
        
        // 为每个缩放级别生成瓦片
        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
            int tilesPerAxis = (int) Math.pow(2, zoom);
            double tileSize = WORLD_SIZE * 2 / tilesPerAxis;
            
            // 计算瓦片范围
            int xStart = (int) Math.floor((blockMinX + WORLD_SIZE) / tileSize);
            int xEnd = (int) Math.ceil((blockMaxX + WORLD_SIZE) / tileSize);
            int yStart = (int) Math.floor((WORLD_SIZE - blockMaxY) / tileSize);
            int yEnd = (int) Math.ceil((WORLD_SIZE - blockMinY) / tileSize);
            
            xStart = Math.max(0, xStart);
            xEnd = Math.min(tilesPerAxis, xEnd);
            yStart = Math.max(0, yStart);
            yEnd = Math.min(tilesPerAxis, yEnd);
            
            // 生成瓦片并添加到合并管理器
            for (int x = xStart; x < xEnd; x++) {
                for (int y = yStart; y < yEnd; y++) {
                    // int yTms = (1 << zoom) - y - 1;
                    BufferedImage tileImage = generateTileFromBlock(blockImage, zoom, x, y,
                            tileSize, blockMinX, blockMaxX, blockMinY, blockMaxY);
                    
                    if (tileImage != null) {
                        tileMergeManager.addTile(zoom, x, y, tileImage);
                    }
                }
            }
        }
    }

    /**
     * 从分块生成单个瓦片（返回图像而不直接保存）
     */
    private BufferedImage generateTileFromBlock(BufferedImage blockImage, int zoom, int x, int y,
                                              double tileSize, double blockMinX, double blockMaxX, 
                                              double blockMinY, double blockMaxY) {
        
        BufferedImage tileImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tileImage.createGraphics();
        
        g2d.setBackground(new Color(0, 0, 0, 0));
        g2d.clearRect(0, 0, TILE_SIZE, TILE_SIZE);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        try {
            // 计算瓦片地理边界
            double tileMinX = x * tileSize - WORLD_SIZE;
            double tileMaxX = (x + 1) * tileSize - WORLD_SIZE;
            double tileMinY = WORLD_SIZE - (y + 1) * tileSize;
            double tileMaxY = WORLD_SIZE - y * tileSize;
            
            // 计算重叠区域
            double overlapMinX = Math.max(tileMinX, blockMinX);
            double overlapMaxX = Math.min(tileMaxX, blockMaxX);
            double overlapMinY = Math.max(tileMinY, blockMinY);
            double overlapMaxY = Math.min(tileMaxY, blockMaxY);
            
            if (overlapMaxX <= overlapMinX || overlapMaxY <= overlapMinY) {
                return null; // 无重叠区域
            }
            
            // 计算采样和绘制区域
            int blockWidth = blockImage.getWidth();
            int blockHeight = blockImage.getHeight();
            
            double xRatioMin = (overlapMinX - blockMinX) / (blockMaxX - blockMinX);
            double xRatioMax = (overlapMaxX - blockMinX) / (blockMaxX - blockMinX);
            double yRatioMin = (blockMaxY - overlapMaxY) / (blockMaxY - blockMinY);
            double yRatioMax = (blockMaxY - overlapMinY) / (blockMaxY - blockMinY);
            
            int srcX = Math.max(0, (int) Math.round(xRatioMin * blockWidth));
            int srcY = Math.max(0, (int) Math.round(yRatioMin * blockHeight));
            int srcWidth = Math.min(blockWidth - srcX, (int) Math.round((xRatioMax - xRatioMin) * blockWidth));
            int srcHeight = Math.min(blockHeight - srcY, (int) Math.round((yRatioMax - yRatioMin) * blockHeight));
            
            if (srcWidth <= 0 || srcHeight <= 0) {
                return null;
            }
            
            double tileXRatioMin = (overlapMinX - tileMinX) / (tileMaxX - tileMinX);
            double tileXRatioMax = (overlapMaxX - tileMinX) / (tileMaxX - tileMinX);
            double tileYRatioMin = (tileMaxY - overlapMaxY) / (tileMaxY - tileMinY);
            double tileYRatioMax = (tileMaxY - overlapMinY) / (tileMaxY - tileMinY);
            
            int destX = (int) Math.floor(tileXRatioMin * TILE_SIZE);
            int destY = (int) Math.floor(tileYRatioMin * TILE_SIZE);
            int destWidth = (int) Math.ceil((tileXRatioMax - tileXRatioMin) * TILE_SIZE);
            int destHeight = (int) Math.ceil((tileYRatioMax - tileYRatioMin) * TILE_SIZE);
            
            destX = Math.max(0, destX);
            destY = Math.max(0, destY);
            destWidth = Math.min(TILE_SIZE - destX, destWidth);
            destHeight = Math.min(TILE_SIZE - destY, destHeight);
            
            if (destWidth <= 0 || destHeight <= 0) {
                return null;
            }
            
            // 绘制图像
            BufferedImage srcSubImage = blockImage.getSubimage(srcX, srcY, srcWidth, srcHeight);
            g2d.drawImage(srcSubImage, destX, destY, destWidth, destHeight, null);
            
            return tileImage;
            
        } finally {
            g2d.dispose();
        }
    }

}
