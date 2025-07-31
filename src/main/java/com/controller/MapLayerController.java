package com.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.config.LocalCacheService;
import com.po.WorkspacePo;
import com.service.IWorkspaceService;
import com.service.MapCutService;
import com.service.WmtsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 图层控制器
 * @date 2025/7/1 10:24:59
 */
@Api(tags = "地图图层服务控制器")
@Controller
@RequestMapping("/map")
public class MapLayerController {

    @Value("${TILES_BASE_DIR}")
    private String tilesBaseDir;

    @Value("${IMAGE_TYPE}")
    private String imageType;

    @Autowired
    private MapCutService mapCutService;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private LocalCacheService localCacheService;

    @Autowired
    private WmtsService wmtsService;

    /**
     * @param z 层级
     * @param x X坐标
     * @param y Y坐标
     * @return 瓦片图资源
     * @return org.springframework.http.ResponseEntity<org.springframework.core.io.Resource>
     * @description 获取瓦片图
     * @author xushi
     * @date 2025/4/21 03:58:56
     */
    @ApiOperation("获取瓦片图，参数：工作空间、层级Z、X、Y")
    @GetMapping("/{workspaceGroup}/{workspace}/{z}/{x}_{y}.png")
    public ResponseEntity<Resource> getTileOld(@PathVariable String workspaceGroup,
                                               @PathVariable String workspace,
                                               @PathVariable int z,
                                               @PathVariable int x,
                                               @PathVariable int y) {
        try {
            System.out.println("workspaceGroup:" + workspaceGroup);
            System.out.println("workspace:" + workspace);
            // 从本地缓存获取
            String cacheKey = workspaceGroup + "_" + workspace;
            String value = localCacheService.getValue(cacheKey);

            if (StringUtils.isEmpty(value)) {
                // 创建条件查询对象
                QueryWrapper<WorkspacePo> queryWrapper = new QueryWrapper<>();
                // 工作空间组
                queryWrapper.eq("name", workspaceGroup);
                // 类型
                queryWrapper.eq("type", 1);
                // 查询信息
                WorkspacePo workspacePo = workspaceService.getOne(queryWrapper);

                if (Objects.isNull(workspacePo)) {
                    return ResponseEntity.notFound().build();
                }

                // 创建条件查询对象
                QueryWrapper<WorkspacePo> workSpaceQueryWrapper = new QueryWrapper<>();
                // 工作空间
                workSpaceQueryWrapper.eq("name", workspace);
                // 类型
                workSpaceQueryWrapper.eq("type", 2);
                // 父id
                workSpaceQueryWrapper.eq("parent_id", workspacePo.getId());
                // 查询信息
                WorkspacePo workspaceServiceOne = workspaceService.getOne(workSpaceQueryWrapper);

                if (Objects.isNull(workspaceServiceOne)) {
                    return ResponseEntity.notFound().build();
                }

                // 存入本地缓存（缓存30分钟）
                value = workspaceServiceOne.getStatus().toString();
                localCacheService.setValue(cacheKey, value, 1800);
            }

            // 返回文件不存在
            if (Integer.parseInt(value) == 0) {
                return ResponseEntity.notFound().build();
            }

            int yTms = (1 << z) - y - 1;
            // 构建瓦片图的文件路径
            Path tilePath = Paths.get(tilesBaseDir)
                    .resolve(workspaceGroup)
                    .resolve(workspace)
                    .resolve(String.valueOf(z))
                    .resolve(x + "_" + yTms + imageType);
            // 检查文件是否存在
            if (!Files.exists(tilePath)) {
                return ResponseEntity.notFound().build();
            }
            // 返回
            return ResponseEntity.ok().body(new UrlResource(tilePath.toUri()));
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * @param z 层级
     * @param x X坐标
     * @param y Y坐标
     * @return 瓦片图资源
     * @return org.springframework.http.ResponseEntity<org.springframework.core.io.Resource>
     * @description 获取TMS格式瓦片图（Y轴偏移，从下到上）
     * @author xushi
     * @date 2025/4/21 03:58:56
     */
    @ApiOperation("TMS 获取瓦片图")
    @GetMapping("/{workspaceGroup}/{workspace}/{z}/{x}/{y}.png")
    public ResponseEntity<Resource> tms(@PathVariable String workspaceGroup,
                                            @PathVariable String workspace,
                                            @PathVariable int z,
                                            @PathVariable int x,
                                            @PathVariable int y) {
        try {
            System.out.println("workspaceGroup:" + workspaceGroup);
            System.out.println("workspace:" + workspace);
            // 从本地缓存获取
            String cacheKey = workspaceGroup + "_" + workspace;
            String value = localCacheService.getValue(cacheKey);

//            if (StringUtils.isEmpty(value)) {
//                // 创建条件查询对象
//                QueryWrapper<WorkspacePo> queryWrapper = new QueryWrapper<>();
//                // 工作空间组
//                queryWrapper.eq("name", workspaceGroup);
//                // 类型
//                queryWrapper.eq("type", 1);
//                // 查询信息
//                WorkspacePo workspacePo = workspaceService.getOne(queryWrapper);
//
//                if (Objects.isNull(workspacePo)) {
//                    return ResponseEntity.notFound().build();
//                }
//
//                // 创建条件查询对象
//                QueryWrapper<WorkspacePo> workSpaceQueryWrapper = new QueryWrapper<>();
//                // 工作空间
//                workSpaceQueryWrapper.eq("name", workspace);
//                // 类型
//                workSpaceQueryWrapper.eq("type", 2);
//                // 父id
//                workSpaceQueryWrapper.eq("parent_id", workspacePo.getId());
//                // 查询信息
//                WorkspacePo workspaceServiceOne = workspaceService.getOne(workSpaceQueryWrapper);
//
//                if (Objects.isNull(workspaceServiceOne)) {
//                    return ResponseEntity.notFound().build();
//                }
//
//                // 存入本地缓存（缓存30分钟）
//                value = workspaceServiceOne.getStatus().toString();
//                localCacheService.setValue(cacheKey, value, 1800);
//            }
//
//            // 返回文件不存在
//            if (Integer.parseInt(value) == 0) {
//                return ResponseEntity.notFound().build();
//            }

//            int yTms = (1 << z) - y - 1;
            // 构建瓦片图的文件路径
            Path tilePath = Paths.get(tilesBaseDir)
                    .resolve(workspaceGroup)
                    .resolve(workspace)
                    .resolve(String.valueOf(z))
                    .resolve(String.valueOf(x))
                    .resolve(y + imageType);
            // 检查文件是否存在
            if (!Files.exists(tilePath)) {
                return ResponseEntity.notFound().build();
            }
            // 返回
            return ResponseEntity.ok().body(new UrlResource(tilePath.toUri()));
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * @param z 层级
     * @param x X坐标
     * @param y Y坐标
     * @return 瓦片图资源
     * @return org.springframework.http.ResponseEntity<org.springframework.core.io.Resource>
     * @description 获取XYZ格式瓦片图（标准XYZ坐标系，Y轴从上到下）
     * @author xushi
     * @date 2025/4/21 03:58:56
     */
    @ApiOperation("XYZ 获取瓦片图")
    @GetMapping("/xyz/{workspaceGroup}/{workspace}/{z}/{x}/{y}.png")
    public ResponseEntity<Resource> xyzOsm(@PathVariable String workspaceGroup,
                                            @PathVariable String workspace,
                                            @PathVariable int z,
                                            @PathVariable int x,
                                            @PathVariable int y) {
        try {
            System.out.println("workspaceGroup:" + workspaceGroup);
            System.out.println("workspace:" + workspace);
            // 从本地缓存获取
            String cacheKey = workspaceGroup + "_" + workspace;
            String value = localCacheService.getValue(cacheKey);

//            if (StringUtils.isEmpty(value)) {
//                // 创建条件查询对象
//                QueryWrapper<WorkspacePo> queryWrapper = new QueryWrapper<>();
//                // 工作空间组
//                queryWrapper.eq("name", workspaceGroup);
//                // 类型
//                queryWrapper.eq("type", 1);
//                // 查询信息
//                WorkspacePo workspacePo = workspaceService.getOne(queryWrapper);
//
//                if (Objects.isNull(workspacePo)) {
//                    return ResponseEntity.notFound().build();
//                }
//
//                // 创建条件查询对象
//                QueryWrapper<WorkspacePo> workSpaceQueryWrapper = new QueryWrapper<>();
//                // 工作空间
//                workSpaceQueryWrapper.eq("name", workspace);
//                // 类型
//                workSpaceQueryWrapper.eq("type", 2);
//                // 父id
//                workSpaceQueryWrapper.eq("parent_id", workspacePo.getId());
//                // 查询信息
//                WorkspacePo workspaceServiceOne = workspaceService.getOne(workSpaceQueryWrapper);
//
//                if (Objects.isNull(workspaceServiceOne)) {
//                    return ResponseEntity.notFound().build();
//                }
//
//                // 存入本地缓存（缓存30分钟）
//                value = workspaceServiceOne.getStatus().toString();
//                localCacheService.setValue(cacheKey, value, 1800);
//            }
//
//            // 返回文件不存在
//            if (Integer.parseInt(value) == 0) {
//                return ResponseEntity.notFound().build();
//            }

            // 构建瓦片图的文件路径
            Path tilePath = Paths.get(tilesBaseDir)
                    .resolve(workspaceGroup)
                    .resolve(workspace)
                    .resolve(String.valueOf(z))
                    .resolve(String.valueOf(x))
                    .resolve(y + imageType);
            // 检查文件是否存在
            if (!Files.exists(tilePath)) {
                return ResponseEntity.notFound().build();
            }
            // 返回
            return ResponseEntity.ok().body(new UrlResource(tilePath.toUri()));
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取WMTS服务能力描述文档
     * Get WMTS service capabilities document
     *
     * @return WMTS Capabilities XML文档
     */
    @ApiOperation("获取WMTS服务能力描述文档")
    @GetMapping(value = "/wmts/capabilities", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public ResponseEntity<String> getWmtsCapabilities() {
        try {
            String capabilities = wmtsService.generateCapabilities();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("Cache-Control", "max-age=3600");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(capabilities);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * WMTS GetTile请求 - 获取瓦片
     * WMTS GetTile request - Get tile
     *
     * @param service 服务类型，固定值"WMTS"
     * @param request 请求类型，固定值"GetTile"
     * @param version 版本号，默认"1.0.0"
     * @param layer 图层标识符（格式：workspaceGroup/workspace）
     * @param style 样式，默认"default"
     * @param format 图像格式
     * @param tileMatrixSet 瓦片矩阵集合标识符
     * @param tileMatrix 瓦片矩阵标识符（缩放级别）
     * @param tileRow 瓦片行号
     * @param tileCol 瓦片列号
     * @return 瓦片图像资源
     */
    @ApiOperation("WMTS GetTile请求 - 获取瓦片")
    @GetMapping(value = "/wmts/tile", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE})
    public ResponseEntity<Resource> getWmtsTile(
            @RequestParam(value = "SERVICE", defaultValue = "WMTS") String service,
            @RequestParam(value = "REQUEST", defaultValue = "GetTile") String request,
            @RequestParam(value = "VERSION", defaultValue = "1.0.0") String version,
            @RequestParam(value = "LAYER") String layer,
            @RequestParam(value = "STYLE", defaultValue = "default") String style,
            @RequestParam(value = "FORMAT", defaultValue = "image/png") String format,
            @RequestParam(value = "TILEMATRIXSET") String tileMatrixSet,
            @RequestParam(value = "TILEMATRIX") String tileMatrix,
            @RequestParam(value = "TILEROW") Integer tileRow,
            @RequestParam(value = "TILECOL") Integer tileCol) {

        try {
            // 解析图层标识符（格式：workspaceGroup/workspace）
            String[] layerParts = layer.split("/");
            if (layerParts.length != 2) {
                return ResponseEntity.badRequest().build();
            }

            String workspaceGroup = layerParts[0];
            String workspace = layerParts[1];

            // 检查图层是否存在
            if (!wmtsService.layerExists(workspaceGroup, workspace)) {
                return ResponseEntity.notFound().build();
            }

            // 获取瓦片资源
            Resource tileResource = wmtsService.getTileResource(
                    workspaceGroup, workspace, tileMatrix, tileRow, tileCol, format);

            if (tileResource == null || !tileResource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaTypeFromFormat(format));
            headers.set("Cache-Control", "max-age=86400"); // 缓存24小时
            headers.set("Access-Control-Allow-Origin", "*"); // 允许跨域

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(tileResource);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * WMTS RESTful风格的GetTile请求
     * WMTS RESTful style GetTile request
     *
     * @param workspaceGroup 工作空间组
     * @param workspace 工作空间
     * @param tileMatrix 瓦片矩阵（缩放级别）
     * @param tileRow 瓦片行号
     * @param tileCol 瓦片列号
     * @param format 图像格式扩展名（如"png"、"jpg"）
     * @return 瓦片图像资源
     */
    @ApiOperation("WMTS RESTful风格的GetTile请求")
    @GetMapping(value = "/wmts/{workspaceGroup}/{workspace}/{tileMatrix}/{tileRow}/{tileCol}.{format}")
    public ResponseEntity<Resource> getWmtsTileRestful(
            @PathVariable String workspaceGroup,
            @PathVariable String workspace,
            @PathVariable String tileMatrix,
            @PathVariable Integer tileRow,
            @PathVariable Integer tileCol,
            @PathVariable String format) {

        try {
            // 检查图层是否存在
            if (!wmtsService.layerExists(workspaceGroup, workspace)) {
                return ResponseEntity.notFound().build();
            }

            // 转换格式
            String mimeType = "image/" + format;

            // 获取瓦片资源
            Resource tileResource = wmtsService.getTileResource(
                    workspaceGroup, workspace, tileMatrix, tileRow, tileCol, mimeType);

            if (tileResource == null || !tileResource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaTypeFromFormat(mimeType));
            headers.set("Cache-Control", "max-age=86400"); // 缓存24小时
            headers.set("Access-Control-Allow-Origin", "*"); // 允许跨域

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(tileResource);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 根据格式字符串获取MediaType
     * Get MediaType from format string
     *
     * @param format 格式字符串
     * @return MediaType对象
     */
    private MediaType getMediaTypeFromFormat(String format) {
        switch (format.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return MediaType.IMAGE_JPEG;
            case "image/png":
            default:
                return MediaType.IMAGE_PNG;
        }
    }

}
