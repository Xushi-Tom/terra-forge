package com.terrain.common;

import com.dto.TerrainCutRequestDto;
import com.terrain.enums.InterpolationType;
import com.terrain.enums.PriorityType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileExistsException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Global options for Gaia3D Tiler.
 */
@Setter
@Getter
@NoArgsConstructor
@Slf4j
public class GlobalOptions {

    /* singleton */
    private static final GlobalOptions instance = new GlobalOptions();
    // 插值类型，默认双线性
    private static final InterpolationType DEFAULT_INTERPOLATION_TYPE = InterpolationType.BILINEAR;
    // 最小层级，默认0，最小0
    private static final int DEFAULT_MINIMUM_TILE_DEPTH = 0;
    // 最大层级，默认14，最大22
    private static final int DEFAULT_MAXIMUM_TILE_DEPTH = 14;
    // 缓冲区大小，默认16
    private static final int DEFAULT_MOSAIC_SIZE = 16;
    // 最大栅格尺寸，默认-8612 / 16384
    private static final int DEFAULT_MAX_RASTER_SIZE = 16384;
    // 默认强度，默认4.0 强度越大，地形越平滑，范围为0.0到16.0
    private static final double DEFAULT_INTENSITY = 4.0;
    // 无数据值，默认-9999.0
    private static final double DEFAULT_NO_DATA_VALUE = -9999.0;

    /**
     * 地形数据的无数据值，用于表示地形数据中无效或缺失的值，默认值为 -9999.0。
     */
    private double noDataValue;
    /**
     * 是否继续生成的标志位，适用于中途中断后继续生成的场景，默认不继续。
     */
    private boolean isContinue = false;

    /**
     * 程序的基本信息，包含程序名称、版本号和供应商信息。
     */
    private String programInfo;
    /**
     * 是否生成图层 JSON 文件的标志位，默认不生成。
     */
    private boolean layerJsonGenerate = false;

    /**
     * 是否保留临时文件的标志位，默认不保留。
     */
    private boolean leaveTemp = false;

    /**
     * 程序的开始时间，单位为毫秒。
     */
    private long startTime = 0;
    /**
     * 程序的结束时间，单位为毫秒。
     */
    private long endTime = 0;

    /**
     * 每个瓦片的拼接缓冲区大小。
     */
    private int mosaicSize;
    /**
     * 最大栅格尺寸。
     */
    private int maxRasterSize;

    /**
     * 输入文件的路径。
     */
    private String inputPath;
    /**
     * 输出文件的路径。
     */
    private String outputPath;

    /**
     * 标准化处理时临时文件的存储路径。
     */
    private String standardizeTempPath;
    /**
     * 调整 TIFF 文件大小后临时文件的存储路径。
     */
    private String resizedTiffTempPath;
    /**
     * 分割 TIFF 文件后临时文件的存储路径。
     */
    private String splitTiffTempPath;
    /**
     * 瓦片处理时临时文件的存储路径。
     */
    private String tileTempPath;

    /**
     * 日志文件的存储路径。
     */
    private String logPath;
    /**
     * 最小瓦片层级，范围为 0 到 22。
     */
    private int minimumTileDepth;
    /**
     * 最大瓦片层级，范围为 0 到 22。
     */
    private int maximumTileDepth;
    /**
     * 插值类型，用于图像处理中对像素值进行估算。
     */
    private InterpolationType interpolationType;
    /**
     * 是否为地形添加顶点法线以支持光照效果的标志位，默认不添加。
     */
    private boolean calculateNormals;
    /**
     * 瓦片处理的优先级类型。
     */
    private PriorityType priorityType;

    /**
     * 网格细化强度，范围为 1.0 到 16.0，强度越大，地形越平滑。
     */
    private double intensity;
    /**
     * 目标坐标参考系统，默认为 WGS84 地理坐标系。
     */
    private CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;


    public static GlobalOptions getInstance() {
        return instance;
    }

    public static void init(TerrainCutRequestDto terrainCutRequestDto) throws IOException {

        // 获取文件路径
        String inputPath = terrainCutRequestDto.getFilePath();
        // 校验文件路径
        if (StringUtils.hasText(inputPath)) {
            // 校验文件路径
            validateInputPath(new File(inputPath).toPath());
            // 设置输入路径
            instance.setInputPath(inputPath);
        } else {
            throw new IllegalArgumentException("请输入文件路径");
        }

        // 获取输出路径
        String outputPath = terrainCutRequestDto.getOutputDir();
        // 设置输出路径
        if (StringUtils.hasText(outputPath)) {
            // 校验输出路径
            validateOutputPath(new File(outputPath).toPath());
            // 设置输出路径
            instance.setOutputPath(outputPath);
            // 设置调整 TIFF 文件大小后临时文件的存储路径，在输出路径下创建 resized 文件夹
            instance.setResizedTiffTempPath(outputPath + File.separator + "resized");
            // 设置瓦片处理时临时文件的存储路径，在输出路径下创建 temp 文件夹
            instance.setTileTempPath(outputPath + File.separator + "temp");
            // 设置分割 TIFF 文件后临时文件的存储路径，在输出路径下创建 split 文件夹
            instance.setSplitTiffTempPath(outputPath + File.separator + "split");
            // 设置标准化处理时临时文件的存储路径，在输出路径下创建 standardization 文件夹
            instance.setStandardizeTempPath(outputPath + File.separator + "standardization");

        } else {
            throw new IllegalArgumentException("请输入输出路径");
        }

        // 设置是否继续生成
        instance.setContinue(terrainCutRequestDto.isContinue());

        // 设置最大瓦片深度
        Integer maxZoom = terrainCutRequestDto.getMaxZoom();
        // 判断最大瓦片深度是否为空，如果为空则设置默认值, 最大瓦片深度必须在0到22之间
        if (Objects.nonNull(maxZoom) && maxZoom > 0 && maxZoom <= 22) {
            // 设置最大瓦片深度
            instance.setMaximumTileDepth(maxZoom);

        } else {
            log.warn("最大瓦片深度必须在0到22之间，当前值为{}，将设置为默认值14", maxZoom);
            // 设置最大瓦片深度
            instance.setMaximumTileDepth(DEFAULT_MAXIMUM_TILE_DEPTH);
        }

        // 获取最小瓦片深度
        Integer minZoom = terrainCutRequestDto.getMinZoom();
        // 判断最小瓦片深度是否为空，如果为空则设置默认值, 最小瓦片深度必须在0到22之间
        if (Objects.nonNull(minZoom) && minZoom >= 0 && minZoom <= 22) {
            // 设置最小瓦片深度
            instance.setMinimumTileDepth(minZoom);

        } else {
            log.warn("最小瓦片深度必须在0到22之间，当前值为{}，将设置为默认值0", maxZoom);
            instance.setMinimumTileDepth(DEFAULT_MINIMUM_TILE_DEPTH);
        }

        // 如果最小瓦片深度大于最大瓦片深度，则抛出异常
        if (instance.getMinimumTileDepth() > instance.getMaximumTileDepth()) {
            throw new IllegalArgumentException("最小瓦片深度必须小于或等于最大瓦片深度");
        }

        // 设置插值类型
        String interpolationType = terrainCutRequestDto.getInterpolationType();
        // 判断插值类型是否为空，如果为空则设置默认值
        if (StringUtils.hasText(interpolationType)) {
            // 设置插值类型
            InterpolationType type;
            try {
                // 获取插值类型
                type = InterpolationType.fromString(interpolationType);
            } catch (IllegalArgumentException e) {
                log.warn("插值类型无效。已设置为双线性");
                // 使用默认插值类型
                type = DEFAULT_INTERPOLATION_TYPE;
            }
            // 设置插值类型
            instance.setInterpolationType(type);
        } else {
            // 使用默认插值类型
            instance.setInterpolationType(DEFAULT_INTERPOLATION_TYPE);
        }

        // 设置优先级类型, 默认为分辨率
        instance.setPriorityType(PriorityType.RESOLUTION);

        // 获取缓冲区大小
        Integer mosaicSize = terrainCutRequestDto.getMosaicSize();
        // 判断缓冲区大小是否为空，如果为空则设置默认值
        instance.setMosaicSize(Objects.nonNull(mosaicSize) && mosaicSize > 0 ? mosaicSize : DEFAULT_MOSAIC_SIZE);

        // 获取最大栅格大小
        Integer maxRasterSize = terrainCutRequestDto.getRasterMaxSize();
        // 判断最大栅格大小是否为空，如果为空则设置默认值
        instance.setMaxRasterSize(Objects.nonNull(maxRasterSize) && maxRasterSize > 0 ? maxRasterSize : DEFAULT_MAX_RASTER_SIZE);

        // 获取细化强度
        Double intensity = terrainCutRequestDto.getIntensity();
        // 判断细化强度是否为空，如果为空则设置默认值
        instance.setIntensity(Objects.nonNull(intensity) && intensity >= 1.0 && intensity <= 16.0 ? intensity : DEFAULT_INTENSITY);

        // 获取无数据值
        Integer nodataValue = terrainCutRequestDto.getNodataValue();
        // 判断无数据值是否为空，如果为空则设置默认值
        instance.setNoDataValue(Objects.nonNull(nodataValue) ? nodataValue : DEFAULT_NO_DATA_VALUE);

        // 设置是否为地形添加顶点法线以支持光照效果
        instance.setCalculateNormals(terrainCutRequestDto.isCalculateNormals());

        // 打印全局选项
        printGlobalOptions();
    }

    protected static void printGlobalOptions() {
        log.info("输入路径: " + instance.getInputPath());
        log.info("输出路径: " + instance.getOutputPath());
        log.info("最小瓦片深度: " + instance.getMinimumTileDepth());
        log.info("最大瓦片深度: " + instance.getMaximumTileDepth());
        log.info("强度: " + instance.getIntensity());
        log.info("插值类型: " + instance.getInterpolationType());
        log.info("优先级类型: " + instance.getPriorityType());
        log.info("计算法线: " + instance.isCalculateNormals());
        log.info("----------------------------------------");
        log.info("平铺拼接大小: " + instance.getMosaicSize());
        log.info("平铺最大光栅大小: " + instance.getMaxRasterSize());
//        log.info("生成图层 Json: {}", instance.isLayerJsonGenerate());
//        log.info("调试模式: {}", instance.isDebugMode());
        log.info("----------------------------------------");
    }

    /**
     * @description 校验输入路径是否存在
     * @param path
     * @return void
     * @author xushi
     * @date 2025/5/6 14:38:54
     */
    protected static void validateInputPath(Path path) throws IOException {
        // 将传入的 Path 对象转换为 File 对象
        File output = path.toFile();
        // 检查文件或目录是否存在
        if (!output.exists()) {
            // 若不存在，抛出 FileExistsException 异常，提示路径不存在
            throw new FileExistsException(String.format("%s 路径不存在", path));
        } else if (!output.canWrite()) {
            // 若存在但不可写，抛出 IOException 异常，提示路径不可写
            throw new IOException(String.format("%s 路径不可写", path));
        }
    }

    /**
     * @description 校验输出路径是否存在
     * @param path
     * @return void
     * @author xushi
     * @date 2025/5/6 14:38:54
     */
    protected static void validateOutputPath(Path path) throws IOException {
        // 将传入的 Path 对象转换为 File 对象
        File output = path.toFile();
        // 检查目录是否存在
        if (!output.exists()) {
            // 若不存在，尝试递归创建目录
            boolean isSuccess = output.mkdirs();
            if (!isSuccess) {
                // 若创建失败，抛出 FileNotFoundException 异常，提示路径不存在
                throw new FileExistsException(String.format("%s 路径不存在，且创建失败。", path));
            } else {
                // 若创建成功，记录日志
                log.info("已创建新的输出目录: " + path);
            }
        } else if (!output.isDirectory()) {
            // 若存在但不是目录，抛出 NotDirectoryException 异常，提示路径不是目录
            throw new NotDirectoryException(String.format("%s 路径不是目录。", path));
        } else if (!output.canWrite()) {
            // 若存在且是目录但不可写，抛出 IOException 异常，提示路径不可写
            throw new IOException(String.format("%s 路径不可写。", path));
        }
    }

    public double getNoDataValue() {
        return noDataValue;
    }

    public void setNoDataValue(double noDataValue) {
        this.noDataValue = noDataValue;
    }

    public boolean isContinue() {
        return isContinue;
    }

    public void setContinue(boolean aContinue) {
        isContinue = aContinue;
    }

    public String getProgramInfo() {
        return programInfo;
    }

    public void setProgramInfo(String programInfo) {
        this.programInfo = programInfo;
    }

    public boolean isLayerJsonGenerate() {
        return layerJsonGenerate;
    }

    public void setLayerJsonGenerate(boolean layerJsonGenerate) {
        this.layerJsonGenerate = layerJsonGenerate;
    }

    public boolean isLeaveTemp() {
        return leaveTemp;
    }

    public void setLeaveTemp(boolean leaveTemp) {
        this.leaveTemp = leaveTemp;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getMosaicSize() {
        return mosaicSize;
    }

    public void setMosaicSize(int mosaicSize) {
        this.mosaicSize = mosaicSize;
    }

    public int getMaxRasterSize() {
        return maxRasterSize;
    }

    public void setMaxRasterSize(int maxRasterSize) {
        this.maxRasterSize = maxRasterSize;
    }

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getStandardizeTempPath() {
        return standardizeTempPath;
    }

    public void setStandardizeTempPath(String standardizeTempPath) {
        this.standardizeTempPath = standardizeTempPath;
    }

    public String getResizedTiffTempPath() {
        return resizedTiffTempPath;
    }

    public void setResizedTiffTempPath(String resizedTiffTempPath) {
        this.resizedTiffTempPath = resizedTiffTempPath;
    }

    public String getSplitTiffTempPath() {
        return splitTiffTempPath;
    }

    public void setSplitTiffTempPath(String splitTiffTempPath) {
        this.splitTiffTempPath = splitTiffTempPath;
    }

    public String getTileTempPath() {
        return tileTempPath;
    }

    public void setTileTempPath(String tileTempPath) {
        this.tileTempPath = tileTempPath;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public int getMinimumTileDepth() {
        return minimumTileDepth;
    }

    public void setMinimumTileDepth(int minimumTileDepth) {
        this.minimumTileDepth = minimumTileDepth;
    }

    public int getMaximumTileDepth() {
        return maximumTileDepth;
    }

    public void setMaximumTileDepth(int maximumTileDepth) {
        this.maximumTileDepth = maximumTileDepth;
    }

    public InterpolationType getInterpolationType() {
        return interpolationType;
    }

    public void setInterpolationType(InterpolationType interpolationType) {
        this.interpolationType = interpolationType;
    }

    public boolean isCalculateNormals() {
        return calculateNormals;
    }

    public void setCalculateNormals(boolean calculateNormals) {
        this.calculateNormals = calculateNormals;
    }

    public PriorityType getPriorityType() {
        return priorityType;
    }

    public void setPriorityType(PriorityType priorityType) {
        this.priorityType = priorityType;
    }

    public double getIntensity() {
        return intensity;
    }

    public void setIntensity(double intensity) {
        this.intensity = intensity;
    }

    public CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    public void setTargetCRS(CoordinateReferenceSystem targetCRS) {
        this.targetCRS = targetCRS;
    }
}
