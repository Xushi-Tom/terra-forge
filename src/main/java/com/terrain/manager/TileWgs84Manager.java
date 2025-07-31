package com.terrain.manager;

import com.terrain.common.GeographicExtension;
import com.terrain.common.GlobalOptions;
import com.terrain.common.TerrainTriangle;
import com.terrain.geometry.*;
import com.twelvemonkeys.io.LittleEndianDataInputStream;
import com.utils.DecimalUtils;
import com.utils.FileUtils;
import com.utils.GaiaGeoTiffUtils;
// 移除自定义StringUtils依赖，使用直接判断
import com.utils.TerrainMeshUtils;
import com.utils.TileWgs84Utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.TransformException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@Slf4j
public class TileWgs84Manager {
    private final static GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final int rasterTileSize = 256;
    private final String imaginaryType = "CRS84";

    private final Map<Integer, String> depthGeoTiffFolderPathMap = new HashMap<>();
    private final Map<Integer, Double> depthDesiredPixelSizeXinMetersMap = new HashMap<>();
    private final Map<Integer, Double> depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap = new HashMap<>();
    private final List<TileWgs84> tileWgs84List = new ArrayList<>();

    private TerrainElevationDataManager terrainElevationDataManager = null;
    private int geoTiffFilesCount = 0;

    private double vertexCoincidentError = 1e-11;
    private int triangleRefinementMaxIterations = 5;
    private TerrainLayer terrainLayer = null;
    private boolean originIsLeftUp = false;

    private List<Double> maxTriangleSizeForTileDepthList = new ArrayList<>();
    private List<Double> minTriangleSizeForTileDepthList = new ArrayList<>();

    private List<TerrainElevationData> terrainElevationDataList = new ArrayList<>();
    private List<TerrainTriangle> triangleList = new ArrayList<>();
    private Vector2d pixelSizeDegrees = new Vector2d();
    private Map<String, String> mapNoUsableGeotiffPaths = new HashMap<>();

    private GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();

    private List<File> standardizedGeoTiffFiles = new ArrayList<>();

    public List<File> getStandardizedGeoTiffFiles() {
        return standardizedGeoTiffFiles;
    }

    public void setStandardizedGeoTiffFiles(List<File> standardizedGeoTiffFiles) {
        this.standardizedGeoTiffFiles = standardizedGeoTiffFiles;
    }

    // constructor
    public TileWgs84Manager() {
        double intensity = globalOptions.getIntensity();

        for (int i = 0; i < 28; i++) {
            double tileSizeMeters = TileWgs84Utils.getTileSizeInMetersByDepth(i);
            double maxSize = tileSizeMeters / 2.5;
            if (i < 11) {
                maxSize *= 0.2;
            }

            maxTriangleSizeForTileDepthList.add(maxSize);

            double minSize = tileSizeMeters * 0.1 / (intensity);

            if (i > 17) {
                minSize *= 0.75;
            } else if (i > 15) {
                minSize *= 1.0;
            } else if (i > 14) {
                minSize *= 1.125;
            } else if (i > 13) {
                minSize *= 1.25;
            } else if (i > 12) {
                minSize *= 1.25;
            } else if (i > 10) {
                minSize *= 1.25;
            } else {
                minSize *= 1.0;
            }
            minTriangleSizeForTileDepthList.add(minSize);
        }

        for (int depth = 0; depth <= 28; depth++) {
            double tileSizeMeters = TileWgs84Utils.getTileSizeInMetersByDepth(depth);
            double desiredPixelSizeXinMeters = tileSizeMeters / 256.0;
            this.depthDesiredPixelSizeXinMetersMap.put(depth, desiredPixelSizeXinMeters);
        }
    }

    public void deleteObjects() {
        if (this.terrainElevationDataManager != null) {
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = null;
        }

        if (this.terrainLayer != null) {
            this.terrainLayer = null;
        }

        if (this.terrainElevationDataList != null) {
            this.terrainElevationDataList.clear();
        }

        if (this.triangleList != null) {
            this.triangleList.clear();
        }

        this.depthGeoTiffFolderPathMap.clear();
        this.depthDesiredPixelSizeXinMetersMap.clear();
        this.depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.clear();
        this.maxTriangleSizeForTileDepthList.clear();
        this.minTriangleSizeForTileDepthList.clear();
        this.mapNoUsableGeotiffPaths.clear();
    }

    public void makeTempFilesFromQuantizedMeshes(int depth) {
        // make temp folder.***
        String tempPath = globalOptions.getTileTempPath();
        String depthStr = "L" + depth;
        String depthTempPath = tempPath + File.separator + depthStr;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists()) {
            if (depthTempFolder.mkdirs()) {
                log.debug("Created temp folder: {}", depthTempFolder.getAbsolutePath());
            }
        }

        // find quantized mesh files.***
        String quantizedMeshPath = globalOptions.getOutputPath() + File.separator + depth;
        File quantizedMeshFolder = new File(quantizedMeshPath);
        if (!quantizedMeshFolder.exists()) {
            log.error("Quantized mesh folder does not exist: {}", quantizedMeshPath);
            return;
        }

        int L = depth;
        int X;
        int Y;

        TileIndices tileIndices = new TileIndices();
        QuantizedMeshManager quantizedMeshManager = new QuantizedMeshManager();
        List<String> quantizedMeshFolderNames = new ArrayList<>();
        FileUtils.getFolderNames(quantizedMeshPath, quantizedMeshFolderNames);

        for (String quantizedMeshFolderName : quantizedMeshFolderNames) {
            X = Integer.parseInt(quantizedMeshFolderName);

            String quantizedMeshFolderPath = quantizedMeshPath + File.separator + quantizedMeshFolderName;
            File quantizedMeshSubFolder = new File(quantizedMeshFolderPath);
            if (!quantizedMeshSubFolder.exists()) {
                log.error("Quantized mesh subfolder does not exist: {}", quantizedMeshSubFolder.getAbsolutePath());
                continue;
            }

            String tempXFolderName = "X" + quantizedMeshFolderName;

            List<String> quantizedMeshFileNames = new ArrayList<>();
            FileUtils.getFileNames(quantizedMeshFolderPath, ".terrain", quantizedMeshFileNames);
            for (String quantizedMeshFileName : quantizedMeshFileNames) {
                Y = Integer.parseInt(quantizedMeshFileName.substring(0, quantizedMeshFileName.indexOf(".")));

                String quantizedMeshFilePath = quantizedMeshFolderPath + File.separator + quantizedMeshFileName;

                // load the quantized mesh file
                File quantizedMeshFile = new File(quantizedMeshFilePath);
                if (!quantizedMeshFile.exists()) {
                    log.error("Quantized mesh file does not exist: {}", quantizedMeshFilePath);
                    continue;
                }

                String tempFileName = "L" + depth + "_" + tempXFolderName + "_Y" + Y + ".til";
                String tempFilePath = depthTempPath + File.separator + tempXFolderName + File.separator + tempFileName;

                // check if exist temp file
                File tempFile = new File(tempFilePath);
                if (tempFile.exists()) {
                    log.debug("Temp file already exists: {}", tempFilePath);
                    continue;
                }

                try {
                    LittleEndianDataInputStream inputStream = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(quantizedMeshFilePath)));
                    QuantizedMesh quantizedMesh = new QuantizedMesh();
                    quantizedMesh.loadDataInputStream(inputStream);

                    tileIndices.set(X, Y, L);
                    TileWgs84 tileWgs84 = quantizedMeshManager.getTileWgs84FromQuantizedMesh(quantizedMesh, tileIndices, this);
                    tileWgs84.saveFile(tileWgs84.getMesh(), tempFilePath);
                } catch (Exception e) {
                    log.error("Error loading quantized mesh file: {}", quantizedMeshFilePath, e);
                }
            }
        }
    }

    private void makeChildrenTempFiles(int depth) {
        int childrenDepth = depth + 1;

        // make temp folder.***
        String tempPath = globalOptions.getTileTempPath();
        String depthStr = "L" + depth;
        String depthTempPath = tempPath + File.separator + depthStr;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists()) {
            return;
        }

        // make the children temp folder.***
        String childrenTempPath = tempPath + File.separator + "L" + childrenDepth;
        File childrenTempFolder = new File(childrenTempPath);
        if (!childrenTempFolder.exists()) {
            if (childrenTempFolder.mkdirs()) {
                log.debug("Created children temp folder: {}", childrenTempFolder.getAbsolutePath());
            }
        }

        List<String> xFolders = new ArrayList<>();
        FileUtils.getFolderNames(depthTempPath, xFolders);
        for (String xFolderName : xFolders) {
            int X = Integer.parseInt(xFolderName.substring(1));
            String xFolderPath = depthTempPath + File.separator + xFolderName;
            File xFolder = new File(xFolderPath);
            if (!xFolder.exists()) {
                log.error("X folder does not exist: {}", xFolderPath);
                continue;
            }

            // make children XTemp folder.***
            String childrenXTempPath = childrenTempPath + File.separator + xFolderName;
            File childrenXTempFolder = new File(childrenXTempPath);
            if (!childrenXTempFolder.exists()) {
                if (childrenTempFolder.mkdirs()) {
                    log.debug("Created children temp folder: {}", childrenXTempFolder.getAbsolutePath());
                }
            }

            // load the TileWgs84 files.***
            List<String> tileWgs84FileNames = new ArrayList<>();
            FileUtils.getFileNames(xFolderPath, ".til", tileWgs84FileNames);
            for (String tileWgs84FileName : tileWgs84FileNames) {
                List<String> splitStrings = Arrays.asList(tileWgs84FileName.split("_"));
                String YFileName = splitStrings.get(2);
                int Y = Integer.parseInt(YFileName.substring(1, YFileName.indexOf(".")));
                String tileWgs84FilePath = xFolderPath + File.separator + tileWgs84FileName;

                // load the TileWgs84 file
                File tileWgs84File = new File(tileWgs84FilePath);
                if (!tileWgs84File.exists()) {
                    log.error("TileWgs84 file does not exist: {}", tileWgs84FilePath);
                    continue;
                }

                // load the TileWgs84
                try {
                    TileIndices tileIndices = new TileIndices();
                    tileIndices.set(X, Y, depth);
                    TileWgs84 tileWgs84 = loadTileWgs84(tileIndices);
                    if (tileWgs84 == null) {
                        log.error("TileWgs84 is null: {}", tileWgs84FilePath);
                        continue;
                    }

                    // save the TileWgs84 in the children temp folder.***
                    TerrainMeshUtils.save4ChildrenMeshes(tileWgs84.getMesh(), this, globalOptions);
                } catch (Exception e) {
                    log.error("Error loading TileWgs84 file: {}", tileWgs84FilePath, e);
                }
            }
        }
    }

    private int determineExistentTileSetMaxDepth(String tileSetDirectory) {
        int existentTileSetMaxDepth = -1;
        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(tileSetDirectory, folderNames);
        Map<Integer, Integer> depthFoldermap = new HashMap<>();
        for (String folderName : folderNames) {
            // 检查文件夹名是否为数字（zoom级别）
            try {
                int depth = Integer.parseInt(folderName);
                depthFoldermap.put(depth, depth);
            } catch (NumberFormatException e) {
                // 忽略非数字文件夹名
                continue;
            }
        }

        int foldersCount = depthFoldermap.size();
        for (int i = 0; i < foldersCount; i++) {
            int depth = depthFoldermap.get(i);
            if (depth > existentTileSetMaxDepth) {
                existentTileSetMaxDepth = depth;
            }
        }

        return existentTileSetMaxDepth;
    }

    private boolean existTempFiles(int depth) {
        String tempPath = globalOptions.getTileTempPath();
        String depthStr = "L" + depth;
        String depthTempPath = tempPath + File.separator + depthStr;
        File depthTempFolder = new File(depthTempPath);
        return depthTempFolder.exists();
    }

    public void makeTileMeshes() throws IOException, TransformException, FactoryException {
        GeographicExtension geographicExtension = this.terrainElevationDataManager.getRootGeographicExtension();

        double minLon = geographicExtension.getMinLongitudeDeg();
        double maxLon = geographicExtension.getMaxLongitudeDeg();
        double minLat = geographicExtension.getMinLatitudeDeg();
        double maxLat = geographicExtension.getMaxLatitudeDeg();

        // create the terrainLayer
        terrainLayer = new TerrainLayer();
        double[] bounds = terrainLayer.getBounds();
        bounds[0] = minLon;
        bounds[1] = minLat;
        bounds[2] = maxLon;
        bounds[3] = maxLat;

        if (globalOptions.isCalculateNormals()) {
            terrainLayer.addExtension("octvertexnormals");
        }

        log.info("----------------------------------------");
        int minTileDepth = globalOptions.getMinimumTileDepth();
        int maxTileDepth = globalOptions.getMaximumTileDepth();

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();
            //Date startDate = new Date(startTime);

            TileRange tilesRange = new TileRange();

            if (depth == 0) {
                // in this case, the tile is the world. L0X0Y0 & L0X1Y0
                tilesRange.setMinTileX(0);
                tilesRange.setMaxTileX(1);
                tilesRange.setMinTileY(0);
                tilesRange.setMaxTileY(0);
            } else {
                TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, tilesRange, originIsLeftUp);
            }

            // Set terrainLayer.available of tileSet json
            terrainLayer.getAvailable().add(tilesRange);
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
            this.terrainElevationDataManager.setTileWgs84Manager(this);
            this.terrainElevationDataManager.setTerrainElevationDataFolderPath(this.depthGeoTiffFolderPathMap.get(depth));
            this.terrainElevationDataManager.makeTerrainQuadTree(depth);

            int mosaicSize = globalOptions.getMosaicSize();
            List<TileRange> subDividedTilesRanges = TileWgs84Utils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);

            log.info("[瓦片][" + depth + "/" + maxTileDepth + "] 开始生成瓦片网格 - 分割后的瓦片数量: " + subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[瓦片][" + depth + "/" + maxTileDepth + "][" + progress + "/" + total + "] 生成所有瓦片的 WGS84 栅格数据...");

                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationDataManager.makeAllTileWgs84Raster(expandedTilesRange, this);

                log.info("[瓦片][" + depth + "/" + maxTileDepth + "][" + progress + "/" + total + "] 开始进行瓦片处理...");

                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == minTileDepth);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();
            }

            this.terrainElevationDataManager.deleteGeoTiffManager();
            this.terrainElevationDataManager.deleteTileRaster();
            this.terrainElevationDataManager.deleteCoverage();

            long endTime = System.currentTimeMillis();
            log.info("[瓦片][" + depth + "/" + maxTileDepth + "] - 瓦片网格生成结束 : 耗时: " + DecimalUtils.millisecondToDisplayTime(endTime - startTime));

            String javaHeapSize = System.getProperty("java.vm.name") + " " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB";
            // jvm heap size
            String maxMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory());
            // jvm total memory
            String totalMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory());
            // jvm free memory
            String freeMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory());
            // jvm used memory
            String usedMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            log.info("[瓦片][" + depth + "/" + maxTileDepth + "] Java 堆大小: " + javaHeapSize + " - 最大内存: " + maxMem + " / 总内存: " + totalMem + " / 空闲内存: " + freeMem + " / 已用内存: " + usedMem);

            log.info("----------------------------------------");
        }
        terrainLayer.saveJsonFile(globalOptions.getOutputPath(), "layer.json");
    }

    public void makeTileMeshesContinue() throws IOException, TransformException, FactoryException {
        String outputDirectory = globalOptions.getOutputPath();
        int existentMaxDepth = determineExistentTileSetMaxDepth(outputDirectory);
        log.info("现有最大深度: " + existentMaxDepth);

        GeographicExtension geographicExtension = this.terrainElevationDataManager.getRootGeographicExtension();

        double minLon = geographicExtension.getMinLongitudeDeg();
        double maxLon = geographicExtension.getMaxLongitudeDeg();
        double minLat = geographicExtension.getMinLatitudeDeg();
        double maxLat = geographicExtension.getMaxLatitudeDeg();

        // create the terrainLayer
        terrainLayer = new TerrainLayer();
        double[] bounds = terrainLayer.getBounds();
        bounds[0] = minLon;
        bounds[1] = minLat;
        bounds[2] = maxLon;
        bounds[3] = maxLat;

        if (globalOptions.isCalculateNormals()) {
            terrainLayer.addExtension("octvertexnormals");
        }

        log.info("----------------------------------------");
        int minTileDepth = globalOptions.getMinimumTileDepth();
        int maxTileDepth = globalOptions.getMaximumTileDepth();

        minTileDepth = Math.max(minTileDepth, existentMaxDepth + 1);

        for (int depth = 0; depth < minTileDepth; depth++) {
            TileRange tilesRange = new TileRange();
            if (depth == 0) {
                // in this case, the tile is the world. L0X0Y0 & L0X1Y0
                tilesRange.setMinTileX(0);
                tilesRange.setMaxTileX(1);
                tilesRange.setMinTileY(0);
                tilesRange.setMaxTileY(0);
            } else {
                TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, tilesRange, originIsLeftUp);
            }

            terrainLayer.getAvailable().add(tilesRange);
        }

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();
            TileRange tilesRange = new TileRange();

            if (depth == 0) {
                tilesRange.setMinTileX(0);
                tilesRange.setMaxTileX(1);
                tilesRange.setMinTileY(0);
                tilesRange.setMaxTileY(0);
            } else {
                TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, tilesRange, originIsLeftUp);
            }

            if (!existTempFiles(depth)) {
                makeTempFilesFromQuantizedMeshes(depth - 1);
                makeChildrenTempFiles(depth - 1);
            }

            terrainLayer.getAvailable().add(tilesRange);
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
            this.terrainElevationDataManager.setTileWgs84Manager(this);
            this.terrainElevationDataManager.setTerrainElevationDataFolderPath(this.depthGeoTiffFolderPathMap.get(depth));
            this.terrainElevationDataManager.makeTerrainQuadTree(depth);

            int mosaicSize = globalOptions.getMosaicSize();
            List<TileRange> subDividedTilesRanges = TileWgs84Utils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate wgs84 raster all tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationDataManager.makeAllTileWgs84Raster(expandedTilesRange, this);

                log.info("[Tile][{}/{}][{}/{}] process tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == 0);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();
            }

            this.terrainElevationDataManager.deleteGeoTiffManager();
            this.terrainElevationDataManager.deleteTileRaster();
            this.terrainElevationDataManager.deleteCoverage();

            long endTime = System.currentTimeMillis();
            log.info("[Tile][{}/{}] - End making tile meshes : Duration: {}", depth, maxTileDepth, DecimalUtils.millisecondToDisplayTime(endTime - startTime));

            String javaHeapSize = System.getProperty("java.vm.name") + " " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB";
            // jvm heap size
            String maxMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory());
            // jvm total memory
            String totalMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory());
            // jvm free memory
            String freeMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory());
            // jvm used memory
            String usedMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            log.info("[Tile][{}/{}] Java Heap Size: {} - MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB ({}%)", depth, maxTileDepth, javaHeapSize, maxMem, totalMem, freeMem, usedMem);
            log.info("----------------------------------------");
        }

        terrainLayer.saveJsonFile(globalOptions.getOutputPath(), "layer.json");
    }

    public double getMaxTriangleSizeForTileDepth(int depth) {
        return maxTriangleSizeForTileDepthList.get(depth);
    }

    public double getMinTriangleSizeForTileDepth(int depth) {
        return minTriangleSizeForTileDepthList.get(depth);
    }

    public double getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(int depth) {
        if (depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.containsKey(depth)) {
            return depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.get(depth);
        } else {
            double maxDiff = TileWgs84Utils.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(depth);
            depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.put(depth, maxDiff);
            return depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.get(depth);
        }
    }

    public String getTilePath(TileIndices tileIndices) {
        String tileTempDirectory = globalOptions.getTileTempPath();
        String neighborFilePath = TileWgs84Utils.getTileFilePath(tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
        return tileTempDirectory + File.separator + neighborFilePath;
    }

    public String getQuantizedMeshTileFolderPath(TileIndices tileIndices) {
        String outputDirectory = globalOptions.getOutputPath();
        String neighborFolderPath = tileIndices.getL() + File.separator + tileIndices.getX();
        return outputDirectory + File.separator + neighborFolderPath;
    }

    public String getQuantizedMeshTilePath(TileIndices tileIndices) {
        String outputDirectory = globalOptions.getOutputPath();
        String neighborFilePath = tileIndices.getL() + File.separator + tileIndices.getX() + File.separator + tileIndices.getY();
        return outputDirectory + File.separator + neighborFilePath + ".terrain";
    }

    public TileWgs84 loadOrCreateTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84
        // check if exist LDTileFile

        if (!tileIndices.isValid()) {
            return null;
        }

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = new TileWgs84(null, this);
        if (!FileUtils.isFileExists(neighborFullPath)) {
            log.debug("Creating tile: CREATE - * - CREATE : " + tileIndices.getX() + ", " + tileIndices.getY() + ", " + tileIndices.getL());
            neighborTile.setTileIndices(tileIndices);
            neighborTile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, this.imaginaryType, originIsLeftUp));
            neighborTile.createInitialMesh();
            if (neighborTile.getMesh() == null) {
                log.error("Error: neighborTile.mesh == null");
            }

            neighborTile.saveFile(neighborTile.getMesh(), neighborFullPath);
        } else {
            // load the Tile
            neighborTile.setTileIndices(tileIndices);
            neighborTile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imaginaryType, originIsLeftUp));
            neighborTile.loadFile(neighborFullPath);
        }

        return neighborTile;
    }

    public TileWgs84 loadTileWgs84(TileIndices tileIndices) throws IOException {
        // this function loads or creates a TileWgs84
        // check if exist LDTileFile

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = null;
        if (!FileUtils.isFileExists(neighborFullPath)) {
            //log.error("Error: neighborFullPath is not exist: " + neighborFullPath);
            return null;
        } else {
            log.debug("Loading tile: LOAD - * - LOAD : " + tileIndices.getX() + ", " + tileIndices.getY() + ", " + tileIndices.getL());
            // load the Tile
            neighborTile = new TileWgs84(null, this);
            neighborTile.setTileIndices(tileIndices);
            neighborTile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imaginaryType, originIsLeftUp));
            neighborTile.loadFile(neighborFullPath);
        }

        return neighborTile;
    }

    private void addNoUsableGeotiffPath(String noUsableGeotiffPath) {
        this.mapNoUsableGeotiffPaths.put(noUsableGeotiffPath, noUsableGeotiffPath);
    }

    /**
     * 对输入路径下的 GeoTIFF 文件进行标准化处理，并获取标准化后的文件列表。
     * 该方法会先检查输入路径，若路径有效则获取其中的 GeoTIFF 文件，
     * 接着对这些文件进行标准化处理，最后将标准化后的文件添加到列表中。
     */
    public void processStandardizeRasters() {
        // 从全局选项中获取输入路径
        String inputPath = globalOptions.getInputPath();
        // 创建表示输入路径的 File 对象
        File inputFolder = new File(inputPath);

        // 用于存储 GeoTIFF 文件路径的列表
        List<String> rasterFileNames = new ArrayList<>();
        // 检查输入路径是否存在且为目录
        if (inputFolder.exists() && inputFolder.isDirectory()) {
            // 若为目录，获取该目录下所有 .tif 扩展名的文件路径
            FileUtils.getFilePathsByExtension(inputPath, ".tif", rasterFileNames, true);
        } else if (inputFolder.exists() && inputFolder.isFile()) {
            // 若为文件，检查文件扩展名是否为 .tif
            if (inputPath.endsWith(".tif")) {
                // 若是 .tif 文件，将其路径添加到列表中
                rasterFileNames.add(inputPath);
            }
        } else {
            // 若输入路径不存在或不是目录，记录错误日志并抛出运行时异常
            log.error("输入路径不存在或不是一个目录: {}", inputPath);
            throw new RuntimeException("错误: 输入路径不存在或不是一个目录: " + inputPath);
        }

        // 检查是否找到 GeoTIFF 文件
        if (rasterFileNames.isEmpty()) {
            // 若未找到，记录错误日志并抛出运行时异常
            log.error("在输入路径中未找到 GeoTIFF 文件: {}", inputPath);
            throw new RuntimeException("错误: 在输入路径中未找到 GeoTIFF 文件: " + inputPath);
        }

        // 对获取到的 GeoTIFF 文件进行标准化处理
        standardizeRasters(rasterFileNames);

        // 现在创建标准化后的 GeoTIFF 文件列表 (20250311 jinho seongdo).***
        // 获取标准化处理的临时文件夹
        File tempFolder = new File(globalOptions.getStandardizeTempPath());
        // 获取临时文件夹下的所有文件和文件夹
        File[] children = tempFolder.listFiles();
        // 清空标准化后的 GeoTIFF 文件列表
        this.getStandardizedGeoTiffFiles().clear();
        // 检查 children 数组是否为 null
        if (children != null) {
            // 遍历临时文件夹下的所有文件和文件夹
            for (File child : children) {
                // 将文件添加到标准化后的 GeoTIFF 文件列表中
                this.getStandardizedGeoTiffFiles().add(child);
            }
        }
    }

    /**
     * 对指定的 GeoTIFF 文件列表进行标准化处理。
     * 该方法会将原始的 GeoTIFF 文件标准化后存储到临时文件夹中，并更新全局选项中的输入路径。
     *
     * @param geoTiffFileNames 待标准化的 GeoTIFF 文件路径列表
     */
    public void standardizeRasters(List<String> geoTiffFileNames) {
        // 获取标准化处理的临时文件夹路径
        String tempPath = globalOptions.getStandardizeTempPath();
        // 创建表示临时文件夹的 File 对象
        File tempFolder = new File(tempPath);
        // 检查临时文件夹是否存在，若不存在则尝试创建
        if (!tempFolder.exists() && tempFolder.mkdirs()) {
            // 若创建成功，记录创建标准化文件夹的日志信息
            log.debug("创建标准文件夹: {}", tempFolder.getAbsolutePath());
        }
        // 更新全局选项中的输入路径为临时文件夹的绝对路径
        globalOptions.setInputPath(tempFolder.getAbsolutePath());

        // 遍历 GeoTIFF 文件路径列表 TODO 1111
        geoTiffFileNames.forEach(geoTiffFileName -> {
            // 加载原始的 GeoTIFF 文件为 GridCoverage2D 对象
            GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFileName);
            // 创建 RasterStandardizer 对象，用于执行标准化操作
            RasterStandardizer rasterStandardizer = new RasterStandardizer();
            // 调用 RasterStandardizer 的 standardize 方法对原始的 GridCoverage2D 对象进行标准化处理，
            // 并将处理结果存储到临时文件夹中
            rasterStandardizer.standardize(originalGridCoverage2D, tempFolder);
//            gaiaGeoTiffManager.deleteObjects();
        });
    }


    public void processResizeRasters(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        File terrainElevationDataFolder = new File(terrainElevationDataFolderPath);
        if (!terrainElevationDataFolder.exists()) {
            log.error("terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
        } else if (!terrainElevationDataFolder.isDirectory()) {
            log.error("terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
        }

        // First check geoTiff files count
        List<String> geoTiffFilePaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(terrainElevationDataFolderPath, "tif", geoTiffFilePaths, true);

        int geotiffCount = geoTiffFilePaths.size();
        this.setGeoTiffFilesCount(geotiffCount);

        log.info("[Pre][Resize GeoTiff] resizing geoTiffs Count : {} ", geotiffCount);

        // TODO 111
        this.gaiaGeoTiffManager = new GaiaGeoTiffManager();
        resizeRasters(terrainElevationDataFolderPath, currentFolderPath);
        gaiaGeoTiffManager.deleteObjects();
    }

    public void resizeRasters(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        // load all geoTiffFiles
        List<String> geoTiffFileNames = new ArrayList<>();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857");

        if (currentFolderPath == null) {
            currentFolderPath = "";
        }

        // now load all geotiff and make geotiff geoExtension data
        int geoTiffFilesSize = geoTiffFileNames.size();
        int geoTiffFilesCount = 0;

        // TODO : Multi-threading
        for (String geoTiffFileName : geoTiffFileNames) {
            log.info("[Pre][Resize GeoTiff][{}/{}] resizing geoTiff : {} ", ++geoTiffFilesCount, geoTiffFilesSize, geoTiffFileName);
            String geoTiffFilePath = terrainElevationDataFolderPath + File.separator + geoTiffFileName;

            // check if the geotiffFileName is no usable
            if (this.mapNoUsableGeotiffPaths.containsKey(geoTiffFilePath)) {
                continue;
            }

            GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);

            CoordinateReferenceSystem crsTarget = originalGridCoverage2D.getCoordinateReferenceSystem2D();
            if (!(crsTarget instanceof ProjectedCRS || crsTarget instanceof GeographicCRS)) {
                log.error("The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems");
                throw new GeoTiffException(null, "The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems", null);
            }

            Vector2d pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(originalGridCoverage2D);

            int minTileDepth = globalOptions.getMinimumTileDepth();
            int maxTileDepth = globalOptions.getMaximumTileDepth();
            for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
                double desiredPixelSizeXinMeters = this.depthDesiredPixelSizeXinMetersMap.get(depth);
                double desiredPixelSizeYinMeters = desiredPixelSizeXinMeters;

                if (desiredPixelSizeXinMeters < pixelSizeMeters.x) {
                    // In this case just assign the originalGeoTiffFolderPath
                    this.depthGeoTiffFolderPathMap.put(depth, globalOptions.getInputPath());
                    continue;
                }

                String depthStr = String.valueOf(depth);
                String resizedGeoTiffFolderPath = globalOptions.getResizedTiffTempPath() + File.separator + depthStr + File.separator + currentFolderPath;
                String resizedGeoTiffFilePath = resizedGeoTiffFolderPath + File.separator + geoTiffFileName;

                // check if exist the file
                if (FileUtils.isFileExists(resizedGeoTiffFilePath)) {
                    // in this case, just assign the resizedGeoTiffFolderPath
                    String resizedGeoTiffSETFolderPath_forThisDepth = globalOptions.getResizedTiffTempPath() + File.separator + depthStr;
                    this.depthGeoTiffFolderPathMap.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
                    continue;
                }

                // in this case, resize the geotiff
                GridCoverage2D resizedGridCoverage2D = gaiaGeoTiffManager.getResizedCoverage2D(originalGridCoverage2D, desiredPixelSizeXinMeters, desiredPixelSizeYinMeters);
                FileUtils.createAllFoldersIfNoExist(resizedGeoTiffFolderPath);
                gaiaGeoTiffManager.saveGridCoverage2D(resizedGridCoverage2D, resizedGeoTiffFilePath);
                resizedGridCoverage2D.dispose(true);

                String resizedGeoTiffSETFolderPath_forThisDepth = globalOptions.getResizedTiffTempPath() + File.separator + depthStr;
                this.depthGeoTiffFolderPathMap.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
            }
            originalGridCoverage2D.dispose(true);
        }

        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        for (String folderName : folderNames) {
            String auxFolderPath = currentFolderPath + File.separator + folderName;
            String folderPath = terrainElevationDataFolderPath + File.separator + folderName;
            resizeRasters(folderPath, auxFolderPath);
        }

        System.gc();
    }

    public boolean originIsLeftUp() {
        return this.originIsLeftUp;
    }
}
