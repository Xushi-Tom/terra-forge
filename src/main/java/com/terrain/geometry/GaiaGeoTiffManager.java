package com.terrain.geometry;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.joml.Vector2i;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;

import com.utils.GaiaGeoTiffUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class GaiaGeoTiffManager {
    /**
     * 定义投影坐标系，EPSG:3857 是 Web Mercator 投影，常用于网络地图服务。
     */
    private final String PROJECTION_CRS = "EPSG:3857";
    /**
     * 用于存储单个像素值的数组，长度为 1。
     */
    private int[] pixel = new int[1];
    /**
     * 用于存储原始栅格数据左上角坐标的数组，长度为 2，分别表示 x 和 y 坐标。
     */
    private double[] originalUpperLeftCorner = new double[2];
    /**
     * 存储文件路径与对应的栅格覆盖数据的映射关系，方便重复使用已加载的栅格数据。
     */
    private Map<String, GridCoverage2D> mapPathGridCoverage2d = new HashMap<>();
    /**
     * 存储文件路径与对应的栅格覆盖数据尺寸的映射关系，尺寸用 Vector2i 表示宽和高。
     */
    private Map<String, Vector2i> mapPathGridCoverage2dSize = new HashMap<>();
    /**
     * 存储 GeoTIFF 文件路径与对应的 EPSG:4326 坐标系 GeoTIFF 文件路径的映射关系。
     */
    private Map<String, String> mapGeoTiffToGeoTiff4326 = new HashMap<>();
    /**
     * 存储文件路径的列表，用于记录加载顺序，方便删除最旧的栅格覆盖数据。
     */
    private List<String> pathList = new ArrayList<>(); // used to delete the oldest coverage

    /**
     * 加载 GeoTIFF 文件并返回对应的栅格覆盖数据。
     * 如果数据已加载，则直接返回缓存中的数据；若缓存已满，会删除最旧的数据。
     *
     * @param geoTiffFilePath 要加载的 GeoTIFF 文件的路径
     * @return 加载后的栅格覆盖数据，如果加载失败则返回 null
     */
    public GridCoverage2D loadGeoTiffGridCoverage2D(String geoTiffFilePath) {
        // 检查缓存中是否已存在该文件的栅格覆盖数据
        if (mapPathGridCoverage2d.containsKey(geoTiffFilePath)) {
            log.info("复用 GeoTIFF 覆盖数据 : " + geoTiffFilePath);
            return mapPathGridCoverage2d.get(geoTiffFilePath);
        }

        // 检查是否存在对应的 EPSG:4326 坐标系的文件路径
        if (mapGeoTiffToGeoTiff4326.containsKey(geoTiffFilePath)) {
            String geoTiff4326FilePath = mapGeoTiffToGeoTiff4326.get(geoTiffFilePath);
            if (mapPathGridCoverage2d.containsKey(geoTiff4326FilePath)) {
                log.info("复用 EPSG:4326 坐标系的 GeoTIFF 覆盖数据: " + geoTiffFilePath);
                return mapPathGridCoverage2d.get(geoTiff4326FilePath);
            }
        }

        // 当缓存中的数据数量超过 4 个时，删除最旧的栅格覆盖数据
        while (mapPathGridCoverage2d.size() > 4) {
            // delete the old coverage. Check the pathList. the 1rst is the oldest
            String oldestPath = pathList.get(0);
            GridCoverage2D oldestCoverage = mapPathGridCoverage2d.get(oldestPath);
            oldestCoverage.dispose(true);
            mapPathGridCoverage2d.remove(oldestPath);
            mapPathGridCoverage2dSize.remove(oldestPath);
            pathList.remove(0);
        }

        log.info("[栅格][输入输出] 正在加载 GeoTIFF 文件: " + geoTiffFilePath);
        GridCoverage2D coverage = null;
        try {
            File file = new File(geoTiffFilePath);
            GeoTiffReader reader = new GeoTiffReader(file);
            coverage = reader.read(null);
            reader.dispose();
        } catch (Exception e) {
            log.error("Error:", e);
        }

        // 将加载的栅格覆盖数据存入缓存
        mapPathGridCoverage2d.put(geoTiffFilePath, coverage);
        pathList.add(geoTiffFilePath);

        // 保存栅格覆盖数据的宽度和高度
        GridGeometry gridGeometry = coverage.getGridGeometry();
        int width = gridGeometry.getGridRange().getSpan(0);
        int height = gridGeometry.getGridRange().getSpan(1);
        Vector2i size = new Vector2i(width, height);
        mapPathGridCoverage2dSize.put(geoTiffFilePath, size);

        log.debug("Loaded the geoTiff file ok");
        return coverage;
    }

    /**
     * 获取指定 GeoTIFF 文件对应的栅格覆盖数据的尺寸。
     * 如果尺寸信息未缓存，则先加载文件并获取尺寸，再释放资源。
     *
     * @param geoTiffFilePath 要获取尺寸的 GeoTIFF 文件的路径
     * @return 栅格覆盖数据的尺寸，用 Vector2i 表示宽和高
     */
    public Vector2i getGridCoverage2DSize(String geoTiffFilePath) {
        // 检查缓存中是否存在该文件的尺寸信息
        if (!mapPathGridCoverage2dSize.containsKey(geoTiffFilePath)) {
            GridCoverage2D coverage = loadGeoTiffGridCoverage2D(geoTiffFilePath);
            coverage.dispose(true);
        }
        return mapPathGridCoverage2dSize.get(geoTiffFilePath);
    }

    /**
     * 释放所有缓存的栅格覆盖数据并清空缓存。
     */
    public void deleteObjects() {
        // 遍历缓存中的所有栅格覆盖数据并释放资源
        for (GridCoverage2D coverage : mapPathGridCoverage2d.values()) {
            coverage.dispose(true);
        }
        mapPathGridCoverage2d.clear();
    }

    /**
     * 根据指定的像素尺寸对原始栅格覆盖数据进行缩放。
     *
     * @param originalCoverage          原始的栅格覆盖数据
     * @param desiredPixelSizeXinMeters 期望的 X 方向像素尺寸（米）
     * @param desiredPixelSizeYinMeters 期望的 Y 方向像素尺寸（米）
     * @return 缩放后的栅格覆盖数据
     * @throws FactoryException 当坐标参考系工厂创建失败时抛出异常
     */
    public GridCoverage2D getResizedCoverage2D(GridCoverage2D originalCoverage, double desiredPixelSizeXinMeters, double desiredPixelSizeYinMeters) throws FactoryException {
        GridCoverage2D resizedCoverage = null;

        // 获取原始栅格数据的几何信息和范围
        GridGeometry originalGridGeometry = originalCoverage.getGridGeometry();
        Envelope envelopeOriginal = originalCoverage.getEnvelope();

        // 获取原始栅格数据在 X 和 Y 方向的像素数量
        int gridSpanX = originalGridGeometry.getGridRange().getSpan(0);
        int gridSpanY = originalGridGeometry.getGridRange().getSpan(1);
        double[] envelopeSpanMeters = new double[2];
        // 获取原始栅格数据在 X 和 Y 方向的实际范围（米）
        GaiaGeoTiffUtils.getEnvelopeSpanInMetersOfGridCoverage2D(originalCoverage, envelopeSpanMeters);
        double envelopeSpanX = envelopeSpanMeters[0];
        double envelopeSpanY = envelopeSpanMeters[1];

        // 计算期望的 X 和 Y 方向的像素数量
        double desiredPixelsCountX = envelopeSpanX / desiredPixelSizeXinMeters;
        double desiredPixelsCountY = envelopeSpanY / desiredPixelSizeYinMeters;
        int minXSize = 24;
        int minYSize = 24;
        // 确保期望的图像宽度和高度不小于最小值
        int desiredImageWidth = Math.max((int) desiredPixelsCountX, minXSize);
        int desiredImageHeight = Math.max((int) desiredPixelsCountY, minYSize);

        // 计算 X 和 Y 方向的缩放比例
        double scaleX = (double) desiredImageWidth / (double) gridSpanX;
        double scaleY = (double) desiredImageHeight / (double) gridSpanY;

        Operations ops = new Operations(null);
        // 对原始栅格数据进行缩放
        resizedCoverage = (GridCoverage2D) ops.scale(originalCoverage, scaleX, scaleY, 0, 0);

        // 记录原始栅格数据的左上角坐标
        originalUpperLeftCorner[0] = envelopeOriginal.getMinimum(0);
        originalUpperLeftCorner[1] = envelopeOriginal.getMinimum(1);

        return resizedCoverage;
    }

    /**
     * 将栅格覆盖数据保存为 GeoTIFF 文件。
     *
     * @param coverage       要保存的栅格覆盖数据
     * @param outputFilePath 输出的 GeoTIFF 文件路径
     * @throws IOException 当文件操作出现异常时抛出异常
     */
    public void saveGridCoverage2D(GridCoverage2D coverage, String outputFilePath) throws IOException {
        // now save the newCoverage as geotiff
        File outputFile = new File(outputFilePath);
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        GeoTiffWriter writer = new GeoTiffWriter(bufferedOutputStream);
        writer.write(coverage, null);
        writer.dispose();
        outputStream.close();
        bufferedOutputStream.close();
    }

}
