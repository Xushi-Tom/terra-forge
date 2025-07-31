package com.utils;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * GeoTools 帮助工具类
 * 提供安全的文件读取和内存管理功能
 */
public class GeoToolsHelper {
    
    private static final Logger log = LoggerFactory.getLogger(GeoToolsHelper.class);
    
    /**
     * 安全地创建文件读取器
     * 
     * @param inputFile 输入文件
     * @return GridCoverage2DReader 或 null
     */
    public static GridCoverage2DReader createSafeReader(File inputFile) {
        try {
            AbstractGridFormat format = GridFormatFinder.findFormat(inputFile);
            if (format == null) {
                log.error("无法识别文件格式: {}", inputFile.getName());
                return null;
            }
            
            GridCoverage2DReader reader = format.getReader(inputFile);
            if (reader == null) {
                log.error("无法创建文件读取器: {}", inputFile.getName());
                return null;
            }
            
            return reader;
        } catch (Exception e) {
            log.error("创建文件读取器失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 安全地读取Coverage数据
     * 
     * @param reader 读取器
     * @return GridCoverage2D 或 null
     */
    public static GridCoverage2D readCoverageSafely(GridCoverage2DReader reader) {
        try {
            return reader.read(null);
        } catch (Exception e) {
            log.error("读取Coverage数据失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 估算内存使用量
     * 
     * @param width 宽度
     * @param height 高度
     * @param bands 波段数
     * @param dataType 数据类型
     * @return 估算的内存使用量（字节）
     */
    public static long estimateMemoryUsage(int width, int height, int bands, String dataType) {
        long pixels = (long) width * height * bands;
        
        int bytesPerPixel;
        switch (dataType) {
            case "Byte":
                bytesPerPixel = 1;
                break;
            case "UInt16":
            case "Int16":
                bytesPerPixel = 2;
                break;
            case "Int32":
            case "Float32":
                bytesPerPixel = 4;
                break;
            case "Float64":
                bytesPerPixel = 8;
                break;
            default:
                bytesPerPixel = 4; // 默认4字节
        }
        
        return pixels * bytesPerPixel;
    }
}