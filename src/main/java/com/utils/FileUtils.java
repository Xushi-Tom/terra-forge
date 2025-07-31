package com.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件系统操作工具类
 * 
 * <p>提供文件和目录操作的常用方法，主要用于地理数据文件的管理和处理。
 * 在地形切片和地图切片过程中，需要频繁进行文件系统操作，
 * 此工具类封装了常用的文件检查、目录创建、文件遍历等功能。</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>文件存在性检查</li>
 *   <li>目录结构创建</li>
 *   <li>文件路径解析</li>
 *   <li>文件列表获取（支持扩展名过滤）</li>
 *   <li>递归文件遍历</li>
 * </ul>
 * 
 * <p><b>注意</b>：所有方法都是线程安全的静态方法，可以在多线程环境中安全使用。</p>
 * 
 * @author TerraForge开发团队
 * @version 2.8.0
 * @since 2025-01-01
 */
@SuppressWarnings("ALL")
@Slf4j
public class FileUtils {

    /**
     * 检查文件或目录是否存在
     * 
     * <p>用于在文件操作前进行安全性检查，避免FileNotFoundException。
     * 在地理数据处理流程中，通常用于验证输入数据文件的有效性。</p>
     * 
     * @param filePath 文件或目录的完整路径
     * @return 如果文件或目录存在则返回true，否则返回false
     * 
     * @example
     * <pre>
     * if (FileUtils.isFileExists("/data/terrain.tif")) {
     *     // 处理地形文件
     * }
     * </pre>
     */
    public static boolean isFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    /**
     * 创建多级目录结构（如果不存在）
     * 
     * <p>递归创建指定路径的所有父目录。在瓦片切片过程中，
     * 需要创建复杂的目录结构来存储不同缩放级别的瓦片文件。</p>
     * 
     * <p><b>目录结构示例</b>：</p>
     * <pre>
     * tiles/
     * ├── terrain/
     * │   ├── 0/0/0.terrain
     * │   ├── 1/0/0.terrain
     * │   └── 1/1/1.terrain
     * └── layer.json
     * </pre>
     * 
     * @param filePath 要创建的目录路径
     * @throws RuntimeException 如果目录创建失败
     * 
     * @example
     * <pre>
     * FileUtils.createAllFoldersIfNoExist("/tiles/terrain/5/12/8/");
     * </pre>
     */
    public static void createAllFoldersIfNoExist(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isDirectory()) {
            return;
        } else {
            if (!file.mkdirs()) {
                throw new RuntimeException("Failed to create folder: " + filePath);
            }
        }
    }

    /**
     * 从完整文件路径中提取父目录路径
     * 
     * <p>用于获取文件所在的目录路径，去除文件名部分。
     * 在处理地理数据时，经常需要在同一目录下创建相关的输出文件。</p>
     * 
     * @param filePath 完整的文件路径
     * @return 文件的父目录路径，如果没有父目录则返回null
     * 
     * @example
     * <pre>
     * String parentDir = FileUtils.removeFileNameFromPath("/data/terrain.tif");
     * // 返回："/data"
     * </pre>
     */
    public static String removeFileNameFromPath(String filePath) {       
        File file = new File(filePath);
        return file.getParent();
    }

    /**
     * 获取指定目录下特定扩展名的文件名列表（不包含路径）
     * 
     * <p>扫描目录下的所有文件，筛选出指定扩展名的文件。
     * 在地理数据处理中常用于查找TIF、GeoTIFF等格式的数据文件。</p>
     * 
     * @param folderPath 要扫描的目录路径
     * @param extension 文件扩展名（如".tif"、".geotiff"）
     * @param fileNames 输出参数，用于收集找到的文件名列表
     * 
     * @example
     * <pre>
     * List&lt;String&gt; tifFiles = new ArrayList&lt;&gt;();
     * FileUtils.getFileNames("/data/source", ".tif", tifFiles);
     * // tifFiles可能包含：["terrain1.tif", "terrain2.tif"]
     * </pre>
     */
    public static void getFileNames(String folderPath, String extension, List<String> fileNames) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            log.warn("[WARN] No files in the folder: " + folderPath);
            return;
        }
        for (File file : listOfFiles) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.endsWith(extension)) {
                    fileNames.add(fileName);
                }
            }
        }
    }

    /**
     * 获取指定目录下的所有子目录名列表
     * 
     * <p>用于遍历瓦片目录结构，特别是在分析现有瓦片集合的缩放级别时。
     * 瓦片通常按照z/x/y的目录结构组织，此方法可以获取所有缩放级别。</p>
     * 
     * @param folderPath 要扫描的目录路径
     * @param folderNames 输出参数，用于收集找到的子目录名列表
     * 
     * @example
     * <pre>
     * List&lt;String&gt; zoomLevels = new ArrayList&lt;&gt;();
     * FileUtils.getFolderNames("/tiles/terrain", zoomLevels);
     * // zoomLevels可能包含：["0", "1", "2", "3", "4"]
     * </pre>
     */
    public static void getFolderNames(String folderPath, List<String> folderNames) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            log.warn("[WARN] No files in the folder: " + folderPath);
            return;
        }
        for (File file : listOfFiles) {
            if (file.isDirectory()) {
                String folderName = file.getName();
                folderNames.add(folderName);
            }
        }
    }

    /**
     * 递归获取指定目录及其子目录下特定扩展名文件的完整路径列表
     * 
     * <p>支持递归遍历子目录，返回文件的完整路径。
     * 在批量处理地理数据时特别有用，可以一次性获取整个目录树中的所有数据文件。</p>
     * 
     * <h4>算法描述：</h4>
     * <ol>
     *   <li>扫描当前目录下的指定扩展名文件</li>
     *   <li>将文件名转换为完整路径添加到结果列表</li>
     *   <li>如果启用递归，对每个子目录重复执行步骤1-2</li>
     * </ol>
     * 
     * @param folderPath 要扫描的根目录路径
     * @param extension 文件扩展名（如".tif"、".terrain"）
     * @param fileNames 输出参数，用于收集找到的文件完整路径列表
     * @param isRecursive 是否递归遍历子目录
     * 
     * @example
     * <pre>
     * List&lt;String&gt; allTerrainFiles = new ArrayList&lt;&gt;();
     * FileUtils.getFilePathsByExtension("/data", ".tif", allTerrainFiles, true);
     * // allTerrainFiles可能包含：
     * // ["/data/region1/terrain.tif", "/data/region2/dem.tif"]
     * </pre>
     */
    public static void getFilePathsByExtension(String folderPath, String extension, List<String> fileNames, boolean isRecursive) {
        // 获取当前目录下的指定扩展名文件
        List<String> currfileNames = new ArrayList<>();
        FileUtils.getFileNames(folderPath, extension, currfileNames);
        
        // 转换为完整路径并添加到结果列表
        for (String fileName : currfileNames) {
            fileNames.add(folderPath + File.separator + fileName);
        }

        // 递归处理子目录
        if (isRecursive) {
            List<String> folderNames = new ArrayList<>();
            FileUtils.getFolderNames(folderPath, folderNames);
            for (String folderName : folderNames) {
                String subFolderPath = folderPath + File.separator + folderName;
                FileUtils.getFilePathsByExtension(subFolderPath, extension, fileNames, isRecursive);
            }
        }
    }
}
