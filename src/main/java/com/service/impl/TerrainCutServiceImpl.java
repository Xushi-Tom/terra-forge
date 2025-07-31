package com.service.impl;

import com.po.MapRecordPo;
import com.service.IMapRecordService;
import com.terrain.common.Configurator;
import com.terrain.common.GlobalOptions;
import com.dto.TerrainCutRequestDto;
import com.terrain.geometry.TerrainLayer;
import com.terrain.manager.TerrainElevationDataManager;
import com.terrain.manager.TileWgs84Manager;
import com.service.TerrainCutService;
import com.utils.HttpUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description TMS地形切片接口实现类
 * @date 2025/4/21 03:44:30
 */
@Slf4j
@Service
public class TerrainCutServiceImpl implements TerrainCutService {

    @Autowired
    private IMapRecordService mapRecordService;

    /**
     * TerraForge 地形切片API URL
     */
    @Value("${terraforge.api.terrain-tile-url}")
    private String terrainTileUrl;

    @Override
    public String terrainCutOfPath(TerrainCutRequestDto terrainCutRequestDto) {
        // 获取成功回调地址
        String backSuccessUrl = terrainCutRequestDto.getBackSuccessUrl();
        // 获取失败回调地址
        String backFailUrl = terrainCutRequestDto.getBackFailUrl();
        try {
            // 开始时间
            LocalDateTime startDate = LocalDateTime.now();
            // 初始化控制台日志记录器，这里假设Configurator类有静态方法initConsoleLogger来进行初始化
            Configurator.initConsoleLogger();
            // 设置EPSG相关信息，这里假设Configurator类有静态方法setEpsg来进行设置
            Configurator.setEpsg();
            // 初始化全局选项，这里假设GlobalOptions类有静态方法init来初始化选项
            GlobalOptions.init(terrainCutRequestDto);
            // 获取全局选项实例
            GlobalOptions globalOptions = GlobalOptions.getInstance();

            // 如果设置了生成layer.json文件的选项
            if (GlobalOptions.getInstance().isLayerJsonGenerate()) {
                // 打印开始生成layer.json文件的日志信息
                log.info("[生成][layer.json] 开始生成 layer.json 文件。");
                // 执行生成layer.json文件的方法
                executeLayerJsonGenerate();
                // 打印生成layer.json文件完成的日志信息
                log.info("[生成][layer.json] 完成生成 layer.json 文件。");
                // 结束程序执行
                return "完成生成 layer.json 文件";
            } else {
                // 打印开始地形处理的日志信息
                log.info("[生成] 开始地形处理流程。");
                // 执行地形处理的方法
                execute();
                // 打印地形处理完成的日志信息
                log.info("[生成] 完成地形处理流程。");
                // 如果没有设置保留临时文件的选项
                if (!globalOptions.isLeaveTemp()) {
                    Thread.sleep(2000);
                    // 清理临时文件和目录
                    cleanTemp();
                }
            }

            // 创建文件对象
            File file = new File(globalOptions.getOutputPath());

            // 结束时间
            LocalDateTime endDate = LocalDateTime.now();
            // 创建地图记录对象
            MapRecordPo mapRecordPo = new MapRecordPo();
            // 设置 id
            mapRecordPo.setId(UUID.randomUUID().toString());
            // 设置开始时间
            mapRecordPo.setStartTime(startDate);
            // 设置结束时间
            mapRecordPo.setEndTime(endDate);
            // 设置文件路径
            mapRecordPo.setFilePath(terrainCutRequestDto.getFilePath());
            // 设置文件大小
            mapRecordPo.setFileSize(String.valueOf(file.length()));
            // 设置类型
            mapRecordPo.setType(1);
            // 设置空间组
            mapRecordPo.setWorkspaceGroup(terrainCutRequestDto.getWorkspaceGroup());
            // 设置工作空间
            mapRecordPo.setWorkspace(terrainCutRequestDto.getWorkspace());
            // 设置输出路径
            mapRecordPo.setOutputPath(terrainCutRequestDto.getOutputDir());
            // 设置文件名称
            mapRecordPo.setFileName(file.getName());
            // 设置最小层级
            mapRecordPo.setZoomMin(terrainCutRequestDto.getMinZoom());
            // 设置最大层级
            mapRecordPo.setZoomMax(terrainCutRequestDto.getMaxZoom());
            // 设置创建时间
            mapRecordPo.setCreateTime(LocalDateTime.now());
            // 保存
            mapRecordService.save(mapRecordPo);

        } catch (FactoryException e) {
            try {
                if (!StringUtils.isEmpty(backFailUrl)) {
                    HttpUtils.sendGet(backFailUrl + "/" + URLEncoder.encode("设置 EPSG 失败", "UTF-8"), null);
                }
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                unsupportedEncodingException.printStackTrace();
            }
            // 捕获工厂异常，记录错误日志并抛出运行时异常
            log.error("设置 EPSG 失败。", e);
            throw new RuntimeException(e);
        } catch (TransformException e) {
            try {
                if (!StringUtils.isEmpty(backFailUrl)) {
                    HttpUtils.sendGet(backFailUrl + "/" + URLEncoder.encode("坐标转换失败", "UTF-8"), null);
                }
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                unsupportedEncodingException.printStackTrace();
            }
            // 捕获坐标转换异常，记录错误日志并抛出运行时异常
            log.error("坐标转换失败。", e);
            throw new RuntimeException(e);
        } catch (ParseException e) {
            try {
                if (!StringUtils.isEmpty(backFailUrl)) {
                    HttpUtils.sendGet(backFailUrl + "/" + URLEncoder.encode("命令行选项解析失败，请检查参数", "UTF-8"), null);
                }
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                unsupportedEncodingException.printStackTrace();
            }
            // 捕获命令行参数解析异常，记录错误日志并抛出运行时异常
            log.error("命令行选项解析失败，请检查参数。", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            try {
                if (!StringUtils.isEmpty(backFailUrl)) {
                    HttpUtils.sendGet(backFailUrl + "/" + URLEncoder.encode("流程运行失败，请检查参数", "UTF-8"), null);
                }
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                unsupportedEncodingException.printStackTrace();
            }
            // 捕获I/O异常，记录错误日志并抛出运行时异常
            log.error("流程运行失败，请检查参数。", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            // 捕获其他异常，直接抛出运行时异常
            throw new RuntimeException(e);
        }

        // 销毁日志记录器，这里假设Configurator类有静态方法destroyLogger来进行销毁
        Configurator.destroyLogger();
        try {
            if (!StringUtils.isEmpty(backSuccessUrl)) {
                HttpUtils.sendGet(backSuccessUrl + "/" + URLEncoder.encode("切片完成", "UTF-8"), null);
            }
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            unsupportedEncodingException.printStackTrace();
        }
        return "切片完成";
    }

    /**
     * 执行地形处理的方法，包括标准化GeoTiff文件、调整文件大小、生成地形高程数据、生成瓦片网格等操作。
     *
     * @throws Exception 如果在执行过程中发生I/O异常、工厂异常或坐标转换异常等
     */
    private static void execute() throws Exception {
        // 获取全局选项实例
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        // 创建WGS84瓦片管理器实例
        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();

        // 打印开始标准化GeoTiff文件的日志信息
        log.info("[预处理][标准化] 开始标准化 GeoTiff 文件。");
        // 执行标准化GeoTiff文件的方法
        tileWgs84Manager.processStandardizeRasters();
        // 打印完成标准化GeoTiff文件的日志信息
        log.info("[预处理][标准化] 完成 GeoTiff 文件标准化。");

        // 打印开始调整GeoTiff文件大小的日志信息
        log.info("[预处理][调整大小] 开始调整 GeoTiff 文件大小。");
        // 执行调整GeoTiff文件大小的方法，传入输入路径和可能的其他参数（这里为null）
        tileWgs84Manager.processResizeRasters(globalOptions.getInputPath(), null);
        // 打印完成调整GeoTiff文件大小的日志信息
        log.info("[预处理][调整大小] 完成 GeoTiff 文件大小调整。");

        // 打印开始生成地形高程数据的日志信息
        log.info("[瓦片处理] 开始生成地形高程数据。");
        // 创建地形高程数据管理器实例并设置到瓦片管理器中
        tileWgs84Manager.setTerrainElevationDataManager(new TerrainElevationDataManager());
        // 将瓦片管理器设置到地形高程数据管理器中，建立关联
        tileWgs84Manager.getTerrainElevationDataManager().setTileWgs84Manager(tileWgs84Manager);
        // 设置地形高程数据文件夹路径
        tileWgs84Manager.getTerrainElevationDataManager().setTerrainElevationDataFolderPath(globalOptions.getResizedTiffTempPath() + File.separator + "0");

        // 设置初始深度为0
        int depth = 0;
        // 调用方法生成地形四叉树数据
        tileWgs84Manager.getTerrainElevationDataManager().makeTerrainQuadTree(depth);
        // 打印完成生成地形高程数据的日志信息
        log.info("[瓦片处理] 完成地形高程数据生成。");

        // 检查是否是从现有瓦片集继续生成瓦片网格，获取相关选项值
        boolean isContinue = globalOptions.isContinue();
        //isContinue = true; // test
        // 如果是继续生成瓦片网格
        if (isContinue) {
            // 打印继续生成瓦片网格的日志信息
            log.info("[瓦片处理] 继续生成瓦片网格。");
            // 执行继续生成瓦片网格的方法
            tileWgs84Manager.makeTileMeshesContinue();
            // 打印完成生成瓦片网格的日志信息
            log.info("[瓦片处理] 完成瓦片网格生成。");
        } else {
            // 打印开始生成瓦片网格的日志信息
            log.info("[瓦片处理] 开始生成瓦片网格。");
            // 执行生成瓦片网格的方法
            tileWgs84Manager.makeTileMeshes();
            // 打印完成生成瓦片网格的日志信息
            log.info("[瓦片处理] 完成瓦片网格生成。");
        }

        // 打印开始删除内存对象的日志信息
        log.info("[后处理][清理] 开始删除内存对象。");
        // 调用方法删除相关内存对象
        tileWgs84Manager.deleteObjects();
        // 打印完成删除内存对象的日志信息
        log.info("[后处理][清理] 完成内存对象删除。");

        System.gc();

        // 将报告信息写入到指定的报告文件中，传入报告文件对象
//        globalOptions.getReporter().writeReportFile(new File(globalOptions.getOutputPath()));
    }

    /**
     * 执行生成layer.json文件的方法，设置地形图层的默认属性，生成可用瓦片信息，并保存为layer.json文件。
     */
    private static void executeLayerJsonGenerate() {
        // 获取全局选项实例
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        // 创建地形图层实例
        TerrainLayer terrainLayer = new TerrainLayer();
        // 设置地形图层的默认属性
        terrainLayer.setDefault();
        // 设置地形图层的边界范围
        terrainLayer.setBounds(new double[]{-180.0, -90.0, 180.0, 90.0});
        // 根据输入路径生成可用瓦片信息
        terrainLayer.generateAvailableTiles(globalOptions.getInputPath());
        // 如果设置了计算法线的选项
        if (globalOptions.isCalculateNormals()) {
            // 为地形图层添加"octvertexnormals"扩展
            terrainLayer.addExtension("octvertexnormals");
        }
        // 将地形图层信息保存为layer.json文件，传入输入路径和文件名
        terrainLayer.saveJsonFile(globalOptions.getOutputPath(), "layer.json");
    }

    /**
     * 清理临时文件夹的方法，包括瓦片临时文件夹、分割后的GeoTiff临时文件夹和调整大小后的GeoTiff临时文件夹。
     */
    private static void cleanTemp() {
        // 获取全局选项实例
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        // 创建瓦片临时文件夹对象
        File tileTempFolder = new File(globalOptions.getTileTempPath());
        // 如果瓦片临时文件夹存在且是一个目录
        if (tileTempFolder.exists() && tileTempFolder.isDirectory()) {
            try {
                // 打印开始删除瓦片临时文件夹的日志信息
                log.info("[后处理] 开始删除瓦片临时文件夹");
                // 使用FileUtils删除瓦片临时文件夹及其内容
                FileUtils.deleteDirectory(tileTempFolder);
            } catch (IOException e) {
                // 捕获删除异常，记录错误日志
                log.error("[后处理] 删除瓦片临时文件夹失败。", e);
            }
        }

        // 创建分割后的GeoTiff临时文件夹对象
        File splitTempFolder = new File(globalOptions.getSplitTiffTempPath());
        // 如果分割后的GeoTiff临时文件夹存在且是一个目录
        if (splitTempFolder.exists() && splitTempFolder.isDirectory()) {
            try {
                // 打印开始删除分割后的GeoTiff临时文件夹的日志信息
                log.info("[后处理] 开始删除分割后的 GeoTiff 临时文件夹");
                // 使用FileUtils删除分割后的GeoTiff临时文件夹及其内容
                FileUtils.deleteDirectory(splitTempFolder);
            } catch (IOException e) {
                // 捕获删除异常，记录错误日志
                log.error("[后处理] 删除分割后的 GeoTiff 临时文件夹失败。", e);
            }
        }

        // 创建标准化后的 GeoTiff 临时文件夹对象
        File standardizeTempFolder = new File(globalOptions.getStandardizeTempPath());
        // 如果标准化后的 GeoTiff 临时文件夹存在且是一个目录
        if (standardizeTempFolder.exists() && standardizeTempFolder.isDirectory()) {
            try {
                // 打印开始删除标准化后的 GeoTiff 临时文件夹的日志信息
                log.info("[后处理] 开始删除标准化后的 GeoTiff 临时文件夹");
                // 使用FileUtils删除标准化后的 GeoTiff 临时文件夹及其内容
                FileUtils.deleteDirectory(standardizeTempFolder);
            } catch (IOException e) {
                // 捕获删除异常，记录错误日志
                log.error("[后处理] 删除标准化后的 GeoTiff 临时文件夹失败。", e);
            }
        }

        // 创建调整大小后的 GeoTiff 临时文件夹对象
        File resizedTempFolder = new File(globalOptions.getResizedTiffTempPath());
        // 如果调整大小后的 GeoTiff 临时文件夹存在且是一个目录
        if (resizedTempFolder.exists() && resizedTempFolder.isDirectory()) {
            try {
                // 打印开始删除调整大小后的 GeoTiff 临时文件夹的日志信息
                log.info("[后处理] 开始删除调整大小后的 GeoTiff 临时文件夹");
                // 使用FileUtils删除调整大小后的 GeoTiff 临时文件夹及其内容
                FileUtils.deleteDirectory(resizedTempFolder);
            } catch (IOException e) {
                // 捕获删除异常，记录错误日志
                log.error("[后处理] 删除调整大小后的 GeoTiff 临时文件夹失败。", e);
            }
        }
    }

    @Override
    public String terrainCutByPathOfTerraForge(TerrainCutRequestDto terrainCutRequestDto) {
        try {
            // 构建TerraForge API请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sourceFile", new File(terrainCutRequestDto.getFilePath()).getName());
            requestBody.put("outputPath", Arrays.asList("terrain", terrainCutRequestDto.getWorkspaceGroup(), terrainCutRequestDto.getWorkspace()));
            requestBody.put("startZoom", terrainCutRequestDto.getMinZoom() != null ? terrainCutRequestDto.getMinZoom() : 0);
            requestBody.put("endZoom", terrainCutRequestDto.getMaxZoom() != null ? terrainCutRequestDto.getMaxZoom() : 8);
            requestBody.put("autoZoom", false);
            requestBody.put("zoomStrategy", "conservative");
            requestBody.put("threads", 4);
            requestBody.put("maxMemory", "8g");
            requestBody.put("maxTriangles", 6291456);
            requestBody.put("compression", true);
            requestBody.put("decompress", true);

            // 调用TerraForge API（使用配置中的URL）
            String terraforgeUrl = terrainTileUrl;

            // 使用RestTemplate调用API
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(terraforgeUrl, entity, String.class);

            log.info("TerraForge地形切片API响应: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("调用TerraForge地形切片API失败", e);
            return "调用TerraForge地形切片API失败: " + e.getMessage();
        }
    }
}
