package com.controller;

import com.dto.TerrainCutRequestDto;
import com.dto.terraforge.TerrainTileRequestDto;
import com.dto.terraforge.TerrainTileResponseDto;
import com.service.TerrainCutService;
import com.service.TerraForgeCommonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 地图服务控制器
 * @date 2025/4/21 03:28:22
 */
@Slf4j
@Api(tags = "地型切片服务")
@Controller
@RequestMapping("/terrain/cut")
public class TerrainCutController {

    @Value("${TILES_BASE_DIR}")
    private String tilesBaseDir;

    @Autowired
    private TerrainCutService terrainCutService;

    @Autowired
    private TerraForgeCommonService terraForgeCommonService;

    /**
     * @param file
     * @param workspace
     * @param minZoom
     * @param maxZoom
     * @return java.lang.String
     * @description 地形切片
     * @author xushi
     * @date 2025/4/21 04:21:31
     */
    @ResponseBody
    @ApiOperation("地形切片（文件）- java")
    @PostMapping("/terrainCutOfFile")
    public String tmsCutOfFile(@RequestParam("file") MultipartFile file,
                               @RequestParam("workspaceGroup") String workspaceGroup,
                               @RequestParam("workspace") String workspace,
                               @RequestParam(value = "minZoom", required = false) Integer minZoom,
                               @RequestParam(value = "maxZoom", required = false) Integer maxZoom,
                               @RequestParam(value = "intensity", required = false) Double intensity,
                               @RequestParam(value = "calculateNormals", required = false) boolean calculateNormals,
                               @RequestParam(value = "interpolationType", required = false) String interpolationType,
                               @RequestParam(value = "nodataValue", required = false) Integer nodataValue,
                               @RequestParam(value = "mosaicSize", required = false) Integer mosaicSize,
                               @RequestParam(value = "rasterMaxSize", required = false) Integer rasterMaxSize,
                               @RequestParam(value = "isContinue", required = false) boolean isContinue) {
        // 检查工作空间是否为空
        if (StringUtils.isEmpty(workspace)) {
            return "工作空间不能为空";
        }
        // 检查文件是否为空
        if (file.isEmpty()) {
            return "请选择要上传的文件";
        }
        // 检查文件类型是否为 TIF
        String fileName = file.getOriginalFilename();
        // 检查文件类型是否为 TIF
        if (StringUtils.isEmpty(fileName) || !fileName.toLowerCase().endsWith(".tif")) {
            return "仅支持上传 TIF 文件";
        }

        try {
            // 创建临时文件
            File tempFile = File.createTempFile("temp", ".tif");
            // 将 MultipartFile 内容传输到临时文件
            file.transferTo(tempFile);
            // 打印临时文件的路径
            log.info("临时文件路径：{}", tempFile.getAbsolutePath());
            String path = tempFile.getAbsolutePath();

            // 创建 TerrainCutRequestDto 对象并设置参数
            TerrainCutRequestDto terrainCutRequestDto = new TerrainCutRequestDto();
            // 设置文件路径
            terrainCutRequestDto.setFilePath(path);
            // 设置工作空间
            terrainCutRequestDto.setOutputDir(tilesBaseDir + workspaceGroup + "/" + workspace);
            // 设置最小层级
            terrainCutRequestDto.setMinZoom(minZoom);
            // 设置最大层级
            terrainCutRequestDto.setMaxZoom(maxZoom);
            // 设置网格细化强度
            terrainCutRequestDto.setIntensity(intensity);
            // 设置是否计算法线
            terrainCutRequestDto.setCalculateNormals(calculateNormals);
            // 设置插值类型
            terrainCutRequestDto.setInterpolationType(interpolationType);
            // 设置无数据值
            terrainCutRequestDto.setNodataValue(nodataValue);
            // 设置拼接大小
            terrainCutRequestDto.setMosaicSize(mosaicSize);
            // 设置最大栅格尺寸
            terrainCutRequestDto.setRasterMaxSize(rasterMaxSize);
            // 设置是否继续
            terrainCutRequestDto.setContinue(isContinue);
            // 调用地形切割服务进行地形切割
            String ofPath = terrainCutService.terrainCutOfPath(terrainCutRequestDto);

            // 删除临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }

            return ofPath;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * @param terrainCutRequestDto
     * @return java.lang.String
     * @description 地形切片
     * @author xushi
     * @date 2025/4/21 04:21:45
     */
    @ResponseBody
    @ApiOperation("地形切片（路径）-java")
    @PostMapping("/terrainCutOfPath")
    public String tmsCut(@RequestBody TerrainCutRequestDto terrainCutRequestDto) {
        // 获取 TIF 文件路径
        String tifDir = terrainCutRequestDto.getFilePath();
        // 获取工作空间
        String workspace = terrainCutRequestDto.getWorkspace();
        // 检查工作空间是否为空
        if (StringUtils.isEmpty(workspace)) {
            return "工作空间不能为空";
        }
        // 创建文件对象
        File file = new File(tifDir);
        // 检查文件是否为空
        if (!file.exists()) {
            return "文件不存在";
        }
        // 检查文件类型是否为 TIF
        String fileName = file.getName();
        // 检查文件类型是否为 TIF
        if (StringUtils.isEmpty(fileName) || !fileName.toLowerCase().endsWith(".tif")) {
            return "仅支持 TIF 文件";
        }
        terrainCutRequestDto.setOutputDir(tilesBaseDir + terrainCutRequestDto.getWorkspaceGroup() + "/" + workspace);
        return terrainCutService.terrainCutOfPath(terrainCutRequestDto);
    }

    /**
     * 创建地形瓦片 - TerraForge API
     * 使用CTB（Cesium Terrain Builder）创建地形瓦片
     * 
     * 核心功能：
     * 1. 支持批量处理多个TIF文件
     * 2. 智能缩放级别推荐
     * 3. 地形瓦片合并功能（NEW）
     * 4. 任务进度监控
     * 
     * 地形合并功能：
     * - 当 mergeTerrains=true 时，系统会在所有地形切片完成后
     * - 自动将多个地形文件夹合并成一个统一的地形目录
     * - 智能合并 layer.json 文件，包括边界信息
     * - 使用硬链接节省磁盘空间
     * 
     * API路径与Python服务保持一致: POST /api/tile/terrain
     */
    @ResponseBody
    @ApiOperation(value = "创建地形瓦片（TerraForge API）", 
                  notes = "支持批量处理和地形合并功能。设置 mergeTerrains=true 可自动合并多个地形文件夹")
    @PostMapping("/tile/terrain")
    public TerrainTileResponseDto createTerrainTiles(@RequestBody TerrainTileRequestDto request) {
        return terraForgeCommonService.createTerrainTiles(request);
    }

}