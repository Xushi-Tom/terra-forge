package com.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 瓦片清理工具类
 * 用于清理切图后不需要的文件
 */
@Slf4j
public class TileCleanupUtils {
    
    /**
     * 清理切图输出目录中不需要的文件
     * 
     * @param outputPath 输出目录路径
     */
    public static void cleanupUnnecessaryFiles(String outputPath) {
        try {
            log.info("🧹 开始清理不需要的文件: {}", outputPath);
            
            Path outputDir = Paths.get(outputPath);
            if (!Files.exists(outputDir)) {
                log.warn("输出目录不存在: {}", outputPath);
                return;
            }
            
            // 要删除的文件列表
            String[] filesToDelete = {
                "base_data.tif",
                "metadata.json"
            };
            
            int deletedCount = 0;
            for (String fileName : filesToDelete) {
                File file = new File(outputDir.toFile(), fileName);
                if (file.exists()) {
                    if (file.delete()) {
                        log.info("✅ 已删除文件: {}", fileName);
                        deletedCount++;
                    } else {
                        log.warn("⚠️ 删除文件失败: {}", fileName);
                    }
                }
            }
            
            if (deletedCount > 0) {
                log.info("🧹 清理完成，删除了 {} 个文件", deletedCount);
            } else {
                log.info("🧹 没有需要清理的文件");
            }
            
        } catch (Exception e) {
            log.error("清理文件时发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 验证瓦片结构并报告
     * 
     * @param outputPath 输出目录路径
     * @return 验证结果描述
     */
    public static String validateTileStructure(String outputPath) {
        try {
            Path outputDir = Paths.get(outputPath);
            if (!Files.exists(outputDir)) {
                return "输出目录不存在";
            }
            
            // 统计瓦片文件
            long tileCount = Files.walk(outputDir)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.endsWith(".terrain") || 
                           fileName.endsWith(".png") || 
                           fileName.endsWith(".jpg") ||
                           fileName.endsWith(".webp");
                })
                .count();
            
            // 统计层级目录
            long zoomDirCount = Files.list(outputDir)
                .filter(Files::isDirectory)
                .filter(path -> {
                    String dirName = path.getFileName().toString();
                    return dirName.matches("\\d+"); // 数字目录名
                })
                .count();
            
            if (tileCount == 0) {
                return String.format("⚠️ 未生成任何瓦片文件，但创建了 %d 个层级目录", zoomDirCount);
            } else {
                return String.format("✅ 生成了 %d 个瓦片文件，包含 %d 个层级", tileCount, zoomDirCount);
            }
            
        } catch (Exception e) {
            log.error("验证瓦片结构时发生异常: {}", e.getMessage(), e);
            return "验证失败: " + e.getMessage();
        }
    }
} 