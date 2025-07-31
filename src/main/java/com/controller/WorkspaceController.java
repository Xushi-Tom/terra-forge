package com.controller;

import com.config.LocalCacheService;
import com.service.IWorkspaceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author xushi
 * @version 1.0
 * @project map-tms
 * @description 工作空间控制器
 * @date 2025/5/8 09:05:00
 */
@Api(tags = "工作空间控制器")
@Controller
@RequestMapping("/workspace")
public class WorkspaceController {

    @Value("${TILES_BASE_DIR}")
    private String tilesBaseDir;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private LocalCacheService localCacheService;

    /**
     * @param workspace
     * @return java.lang.Boolean
     * @description 创建工作空间
     * @author xushi
     * @date 2025/4/21 03:58:30
     */
    @ResponseBody
    @ApiOperation("创建工作空间")
    @PostMapping("/createWorkspace/{workspaceGroup}/{workspace}")
    public String createWorkspace(@PathVariable String workspaceGroup, @PathVariable String workspace) {
        try {
            // 构建工作空间的目录路径
            Path workspacePath = Paths.get(tilesBaseDir).resolve(workspaceGroup).resolve(workspace);
            // 判断工作空间是否存在
            if (Files.exists(workspacePath)) {
                return "工作空间已存在";
            }
            // 创建工作空间目录
            java.nio.file.Files.createDirectories(workspacePath);

        } catch (IOException e) {
            e.printStackTrace();
            return "创建失败";
        }
        return "创建成功";
    }

    /**
     * @param workspace
     * @return java.lang.Boolean
     * @description 删除工作空间
     * @author xushi
     * @date 2025/4/21 03:58:30
     */
    @ResponseBody
    @ApiOperation("删除工作空间")
    @DeleteMapping("/deleteWorkspace/{workspaceGroup}/{workspace}")
    public String deleteWorkspace(@PathVariable String workspaceGroup, @PathVariable String workspace) {
        try {
            // 构建工作空间的目录路径
            Path workspacePath = Paths.get(tilesBaseDir).resolve(workspaceGroup).resolve(workspace);
            // 调用递归删除方法
            deleteDirectoryRecursively(workspacePath);
            return "删除成功";
        } catch (IOException e) {
            e.printStackTrace();
            return "删除失败";
        }
    }

    /**
     * @param workspace
     * @return java.lang.Boolean
     * @description 修改工作空间状态
     * @author xushi
     * @date 2025/4/21 03:58:30
     */
    @ResponseBody
    @ApiOperation("修改工作空间状态")
    @GetMapping("/updateStatusWorkspace/{workspaceGroup}/{workspace}/{status}")
    public String updateStatusWorkspace(@PathVariable String workspaceGroup, @PathVariable String workspace, @PathVariable String status) {
        if (StringUtils.isEmpty(workspaceGroup) || StringUtils.isEmpty(workspace) || StringUtils.isEmpty(status)) {
            return "请检查参数值";
        }
        // 修改工作空间状态
        return workspaceService.updateStatusWorkspace(workspaceGroup, workspace, status);
    }

    /**
     * @param workspaceGroup
     * @return java.lang.Boolean
     * @description 创建工作空间组
     * @author xushi
     * @date 2025/4/21 03:58:30
     */
    @ResponseBody
    @ApiOperation("创建工作空间组")
    @PostMapping("/createWorkspaceGroup/{workspaceGroup}")
    public String createWorkspace(@PathVariable String workspaceGroup) {
        try {
            // 构建工作空间的目录路径
            Path workspacePath = Paths.get(tilesBaseDir).resolve(workspaceGroup);
            // 判断工作空间组是否存在
            if (Files.exists(workspacePath)) {
                return "工作空间组已存在";
            }
            // 创建工作空间目录
            java.nio.file.Files.createDirectories(workspacePath);

        } catch (IOException e) {
            e.printStackTrace();
            return "创建失败";
        }
        return "创建成功";
    }

    /**
     * @param workspaceGroup
     * @return java.lang.Boolean
     * @description 删除工作空间组
     * @author xushi
     * @date 2025/4/21 03:58:30
     */
    @ResponseBody
    @ApiOperation("删除工作空间组")
    @DeleteMapping("/deleteWorkspaceGroup/{workspaceGroup}")
    public String deleteWorkspace(@PathVariable String workspaceGroup) {
        try {
            // 构建工作空间的目录路径
            Path workspacePath = Paths.get(tilesBaseDir).resolve(workspaceGroup);
            // 调用递归删除方法
            deleteDirectoryRecursively(workspacePath);
            return "删除成功";
        } catch (IOException e) {
            e.printStackTrace();
            return "删除失败";
        }
    }

    // 递归删除目录的方法，接收一个 Path 对象作为要删除的目录路径
    private static void deleteDirectoryRecursively(Path path) throws IOException {
        // 检查指定的路径是否存在
        if (Files.exists(path)) {
            // 使用 try-with-resources 语句创建一个 DirectoryStream 对象，用于遍历目录下的所有文件和子目录
            // Files.newDirectoryStream(path) 方法会返回一个可迭代的对象，用于遍历指定目录下的所有条目
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                // 遍历 DirectoryStream 中的每个条目
                for (Path subPath : stream) {
                    // 检查当前条目是否为目录
                    if (Files.isDirectory(subPath)) {
                        // 如果是目录，则递归调用 deleteDirectoryRecursively 方法，继续删除该子目录
                        deleteDirectoryRecursively(subPath);
                    } else {
                        // 如果是文件，则直接使用 Files.delete 方法删除该文件
                        Files.delete(subPath);
                    }
                }
            }
            // 当目录下的所有文件和子目录都被删除后，当前目录变为空目录
            // 使用 Files.delete 方法删除这个空目录
            Files.delete(path);
        }
    }

}
