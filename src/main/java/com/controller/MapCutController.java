package com.controller;

import com.dto.MapCutRequestDto;
import com.dto.terraforge.IndexedTilesRequestDto;
import com.dto.terraforge.IndexedTilesResponseDto;
import com.dto.terraforge.MapTileRequestDto;
import com.dto.terraforge.MapTileResponseDto;
import com.service.MapCutService;
import com.service.TerraForgeCommonService;
import com.service.TerraForgeMapService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;
import java.util.HashMap;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description 地图服务控制器
 * @date 2025/4/21 03:28:22
 */
@Api(tags = "Java地图切片服务")
@Controller
@RequestMapping("/map/cut")
public class MapCutController {

    @Value("${TILES_BASE_DIR}")
    private String tilesBaseDir;

    @Value("${IMAGE_TYPE}")
    private String imageType;

    @Autowired
    private MapCutService mapCutService;

    @Autowired
    private TerraForgeMapService terraForgeMapService;

    @Autowired
    private TerraForgeCommonService terraForgeCommonService;

    /**
     * @param file
     * @param workspace
     * @param type      1:电子 2：遥感
     * @param minZoom
     * @param maxZoom
     * @return java.lang.String
     * @description tms切片
     * @author xushi
     * @date 2025/4/21 04:21:31
     */
    @ResponseBody
    @ApiOperation("地图切片（文件） - java")
    @PostMapping("/mapCutOfFile")
    public String tmsCutOfFile(@RequestParam("file") MultipartFile file,
                               @RequestParam("workspaceGroup") String workspaceGroup,
                               @RequestParam("type") String type,
                               @RequestParam("workspace") String workspace,
                               @RequestParam("minZoom") Integer minZoom,
                               @RequestParam("maxZoom") Integer maxZoom) {
        // 检查工作空间是否为空
        if (StringUtils.isEmpty(workspaceGroup)) {
            return "工作空间组不能为空";
        }

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

        // 检查最小层级层级是否为空，如果为空则设置默认值
        if (StringUtils.isEmpty(minZoom)) {
            minZoom = 1;
        }
        //  检查最最大层级是否为空，如果为空则设置默认值
        if (StringUtils.isEmpty(maxZoom)) {
            maxZoom = 10;
        }

        return mapCutService.tmsCutOfFile(file, workspaceGroup + "/" + workspace, type, minZoom, maxZoom);
    }

    /**
     * @param mapCutRequestDto
     * @return java.lang.String
     * @description tms切片
     * @author xushi
     * @date 2025/4/21 04:21:45
     */
    @ResponseBody
    @ApiOperation("地图切片（路径）- java")
    @PostMapping("/mapCutOfPath")
    public String tmsCut(@RequestBody MapCutRequestDto mapCutRequestDto) {
        // 获取 TIF 文件路径
        String tifDir = mapCutRequestDto.getTifDir();
        // 获取工作空间组
        String workspaceGroup = mapCutRequestDto.getWorkspaceGroup();
        // 获取工作空间
        String workspace = mapCutRequestDto.getWorkspace();
        // 获取最大层级
        Integer maxZoom = mapCutRequestDto.getMaxZoom();
        // 获取最小层级
        Integer minZoom = mapCutRequestDto.getMinZoom();

        // 检查工作空间组是否为空
        if (StringUtils.isEmpty(workspaceGroup)) {
            return "工作空间组不能为空";
        }

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

        // 检查最小层级层级是否为空，如果为空则设置默认值
        if (StringUtils.isEmpty(minZoom)) {
            mapCutRequestDto.setMinZoom(1);
        }
        //  检查最最大层级是否为空，如果为空则设置默认值
        if (StringUtils.isEmpty(maxZoom)) {
            mapCutRequestDto.setMaxZoom(1);
        }

        return mapCutService.tmsCut(mapCutRequestDto);
    }

    /**
     * 索引瓦片切片 - TerraForge API
     * 基于瓦片索引的精确切片，支持通配符和txt文件列表
     * 
     * 核心特性：
     * 1. 根据TIF文件生成SHP分幅矢量栅格
     * 2. 记录每个瓦片的详细元数据（位置、关联TIF文件、像素、nodata、透明度等）
     * 3. 使用GDAL精确切割每个瓦片（无VRT中间层）
     * 4. 支持通配符模式：*.tif、K50*.tiff
     * 5. 支持txt文件列表模式：filelist.txt
     * 6. 支持混合模式：通配符 + txt文件
     * 7. 自动清理元数据和索引文件
     * 
     * API路径与Python服务保持一致: POST /api/tile/indexed-tiles
     */
    @ResponseBody
    @ApiOperation("索引瓦片切片（TerraForge API）")
    @PostMapping("/tile/indexedTiles")
    public IndexedTilesResponseDto createIndexedTiles(@RequestBody IndexedTilesRequestDto request) {
        return terraForgeMapService.createIndexedTiles(request);
    }

    /**
     * 扫描透明瓦片 - TerraForge API
     * 扫描指定目录中包含透明（nodata）值的PNG瓦片
     * <p>
     * API路径与Python服务保持一致: POST /api/tiles/nodata/scan
     */
    @ResponseBody
    @ApiOperation("扫描透明瓦片（TerraForge API）")
    @PostMapping("/tiles/nodata/scan")
    public Map<String, Object> scanNodataTiles(@RequestParam("tilesPath") String tilesPath,
                                              @RequestParam(value = "includeDetails", defaultValue = "false") Boolean includeDetails) {
        return terraForgeCommonService.scanNodataTiles(tilesPath, includeDetails);
    }

    /**
     * 删除透明瓦片 - TerraForge API
     * 删除包含透明（nodata）值的PNG瓦片，并清理空目录
     * <p>
     * API路径与Python服务保持一致: POST /api/tiles/nodata/delete
     */
    @ResponseBody
    @ApiOperation("删除透明瓦片（TerraForge API）")
    @PostMapping("/tiles/nodata/delete")
    public Map<String, Object> deleteNodataTiles(@RequestParam("tilesPath") String tilesPath,
                                                @RequestParam(value = "includeDetails", defaultValue = "true") Boolean includeDetails) {
        return terraForgeCommonService.deleteNodataTiles(tilesPath, includeDetails);
    }

}