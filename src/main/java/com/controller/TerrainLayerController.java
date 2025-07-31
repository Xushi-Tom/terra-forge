package com.controller;

import com.config.LocalCacheService;
import com.service.IWorkspaceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author xushi
 * @version 1.0
 * @project terraforge-server
 * @description 图层控制器
 * @date 2025/7/1 10:24:59
 */
@Api(tags = "地形图层服务控制器")
@Controller
@RequestMapping("/terrain")
public class TerrainLayerController {

    @Value("${TILES_BASE_DIR}")
    private String tilesBaseDir;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private LocalCacheService localCacheService;

    @ApiOperation("获取地形，参数：工作空间、层级、X、Y")
    @GetMapping("/{workspaceGroup}/{workspace}/{z}/{x}/{y}.terrain")
    public ResponseEntity<Resource> getTerrain(@PathVariable String workspaceGroup,
                                               @PathVariable String workspace,
                                               @PathVariable int z,
                                               @PathVariable int x,
                                               @PathVariable int y) {
        try {
            // 构建地形的文件路径
            Path tilePath = Paths.get(tilesBaseDir)
                    .resolve(workspaceGroup)
                    .resolve(workspace)
                    .resolve(String.valueOf(z))
                    .resolve(String.valueOf(x))
                    .resolve(y + ".terrain");
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

    @ApiOperation("获取layer.json")
    @GetMapping("/{workspaceGroup}/{workspace}/layer.json")
    public ResponseEntity<Resource> getLayer(@PathVariable String workspaceGroup, @PathVariable String workspace) {
        try {
            // 存入redis
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
//                // 存入redis
//                // 存入本地缓存（缓存30分钟）
//                value = workspaceServiceOne.getStatus().toString();
//                localCacheService.setValue(cacheKey, value, 1800);
//            }
//
//            // 返回文件不存在
//            if (Integer.parseInt(value) == 0) {
//                return ResponseEntity.notFound().build();
//            }

            // 构建地形的文件路径
            Path tilePath = Paths.get(tilesBaseDir)
                    .resolve(workspaceGroup)
                    .resolve(workspace)
                    .resolve("layer.json");
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

}
