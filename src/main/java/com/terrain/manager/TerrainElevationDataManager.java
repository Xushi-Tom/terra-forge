package com.terrain.manager;

import com.terrain.common.GeographicExtension;
import com.terrain.common.GlobalOptions;
import com.terrain.common.TerrainTriangle;
import com.terrain.enums.PriorityType;
import com.terrain.geometry.GaiaGeoTiffManager;
import com.terrain.geometry.TerrainElevationDataQuadTree;
import com.terrain.geometry.TileRange;
import com.terrain.geometry.TileWgs84Raster;
import com.utils.FileUtils;
import com.utils.GaiaGeoTiffUtils;
import com.utils.TileWgs84Utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class TerrainElevationDataManager {
    private static GlobalOptions globalOptions = GlobalOptions.getInstance();

    private TileWgs84Manager tileWgs84Manager = null;
    private List<TerrainElevationData> terrainElevationDataArray = new ArrayList<>();
    private List<TerrainTriangle> trianglesArray = new ArrayList<>();
    private Map<String, TileWgs84Raster> mapIndicesTileRaster = new HashMap<>();
    private Map<String, Double> gridAreaMap = new HashMap<>();

    // Inside the folder, there are multiple geoTiff files
    private String terrainElevationDataFolderPath;
    private int geoTiffFilesCount = 0;

    // if there are multiple geoTiff files, use this
    private int quadtreeMaxDepth = 10;
    private TerrainElevationDataQuadTree rootTerrainElevationDataQuadTree = null;
    private GaiaGeoTiffManager myGaiaGeoTiffManager = null;
    private boolean[] intersects = {false};
    private List<String> geoTiffFileNames = new ArrayList<>();

    public void makeTerrainQuadTree(int depth) throws FactoryException, TransformException, IOException {
        List<File> standardizedGeoTiffFiles = tileWgs84Manager.getStandardizedGeoTiffFiles();

        // load all geoTiffFiles & make a quadTree
        loadAllGeoTiff(terrainElevationDataFolderPath, standardizedGeoTiffFiles);
        rootTerrainElevationDataQuadTree.makeQuadTree(quadtreeMaxDepth);
    }

    public GaiaGeoTiffManager getGaiaGeoTiffManager() {
        if (myGaiaGeoTiffManager == null) {
            myGaiaGeoTiffManager = new GaiaGeoTiffManager();
        }
        return myGaiaGeoTiffManager;
    }

    public TileWgs84Raster getTileWgs84Raster(TileIndices tileIndices, TileWgs84Manager tileWgs84Manager) {
        TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileIndices.getString());
        if (tileWgs84Raster == null) {
            tileWgs84Raster = new TileWgs84Raster(tileIndices, tileWgs84Manager);
            int tileRasterWidth = tileWgs84Manager.getRasterTileSize();
            int tileRasterHeight = tileWgs84Manager.getRasterTileSize();
            tileWgs84Raster.makeElevations(this, tileRasterWidth, tileRasterHeight);
            mapIndicesTileRaster.put(tileIndices.getString(), tileWgs84Raster);
        }
        return tileWgs84Raster;
    }

    public void makeAllTileWgs84Raster(TileRange tileRange, TileWgs84Manager tileWgs84Manager) {
        List<TileIndices> tileIndicesList = tileRange.getTileIndices(null);

        // 1rst, delete from the mapIndicesTileRaster the tiles that are not in the tileIndicesList.***
        List<String> tileIndicesStringList = new ArrayList<>(mapIndicesTileRaster.keySet());
        int initialSize = mapIndicesTileRaster.size();
        int reusedRasterTilesCount = 0;
        for (String tileIndicesString : tileIndicesStringList) {
            boolean isExist = false;
            for (TileIndices tileIndices : tileIndicesList) {
                if (tileIndicesString.equals(tileIndices.getString())) {
                    isExist = true;
                    reusedRasterTilesCount++;
                    break;
                }
            }

            if (!isExist) {
                TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileIndicesString);
                tileWgs84Raster.deleteObjects();
                mapIndicesTileRaster.remove(tileIndicesString);
            }
        }

        log.info("ReusedRasterTilesCount = " + reusedRasterTilesCount + " / " + initialSize);

        // now, delete TerrainElevationData's coverage that are not intersecting with the tileRange.***
        GeographicExtension geoExtensionTotal = null;
        for (TileIndices tileIndices : tileIndicesList) {
            String imageryType = tileWgs84Manager.getImaginaryType();
            boolean originIsLeftUp = tileWgs84Manager.isOriginIsLeftUp();
            GeographicExtension geoExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp);
            if (geoExtensionTotal == null) {
                geoExtensionTotal = new GeographicExtension();
                geoExtensionTotal.copyFrom(geoExtension);
            } else {
                geoExtensionTotal.union(geoExtension);
            }
        }
        if (geoExtensionTotal != null) {
            this.rootTerrainElevationDataQuadTree.deleteCoverageIfNoIntersectsGeoExtension(geoExtensionTotal);
        }

        for (TileIndices tileIndices : tileIndicesList) {
            TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileIndices.getString());
            if (tileWgs84Raster == null) {
                tileWgs84Raster = new TileWgs84Raster(tileIndices, tileWgs84Manager);
                int tileRasterWidth = tileWgs84Manager.getRasterTileSize();
                int tileRasterHeight = tileWgs84Manager.getRasterTileSize();
                tileWgs84Raster.makeElevations(this, tileRasterWidth, tileRasterHeight);
                mapIndicesTileRaster.put(tileIndices.getString(), tileWgs84Raster);
            }
        }
    }

    public void deleteTileRaster() {
        for (TileWgs84Raster tileWgs84Raster : mapIndicesTileRaster.values()) {
            tileWgs84Raster.deleteObjects();
        }

        mapIndicesTileRaster.clear();

    }

    public GeographicExtension getRootGeographicExtension() {
        if (rootTerrainElevationDataQuadTree == null) {
            return null;
        }

        return rootTerrainElevationDataQuadTree.getGeographicExtension();
    }

    public void deleteCoverage() {
        if (rootTerrainElevationDataQuadTree == null) {
            return;
        }
        rootTerrainElevationDataQuadTree.deleteCoverage();
    }

    public void deleteObjects() {
        this.deleteTileRaster();
        this.deleteCoverage();
        if (myGaiaGeoTiffManager != null) {
            myGaiaGeoTiffManager.deleteObjects();
            myGaiaGeoTiffManager = null;
        }

        if (rootTerrainElevationDataQuadTree == null) {
            return;
        }

        rootTerrainElevationDataQuadTree.deleteObjects();
        rootTerrainElevationDataQuadTree = null;

        terrainElevationDataArray.clear();
    }

    public double getElevationBilinearRasterTile(TileIndices tileIndices, TileWgs84Manager tileWgs84Manager, double lonDeg, double latDeg) {
        double resultElevation = 0.0;
        TileWgs84Raster tileWgs84Raster = null;
        tileWgs84Raster = this.getTileWgs84Raster(tileIndices, tileWgs84Manager);
        resultElevation = tileWgs84Raster.getElevationBilinear(lonDeg, latDeg);
        return resultElevation;
    }

    public Map<TerrainElevationData, TerrainElevationData> getTerrainElevationDataArray(GeographicExtension geoExtension,
                                                                                        Map<TerrainElevationData, TerrainElevationData> terrainElevDataMap) {
        if (rootTerrainElevationDataQuadTree == null) {
            return terrainElevDataMap;
        }

        if (terrainElevDataMap == null) {
            terrainElevDataMap = new HashMap<>();
        }

        rootTerrainElevationDataQuadTree.getTerrainElevationDataArray(geoExtension, terrainElevDataMap);
        return terrainElevDataMap;
    }

    public double getElevation(double lonDeg, double latDeg, List<TerrainElevationData> terrainElevDataArray) {
        double resultElevation = 0.0;

        if (rootTerrainElevationDataQuadTree == null) {
            return resultElevation;
        }

        double noDataValue = globalOptions.getNoDataValue();
        PriorityType priorityType = globalOptions.getPriorityType();

        intersects[0] = false;
        double pixelAreaAux = Double.MAX_VALUE;
        double candidateElevation = 0.0;
        for (TerrainElevationData terrainElevationData : terrainElevDataArray) {
            double elevation = terrainElevationData.getElevation(lonDeg, latDeg, intersects);
            if (!intersects[0]) {
                continue;
            }

            /* check if the priority is resolution */
            if (priorityType.equals(PriorityType.RESOLUTION)) {

                double pixelArea = putAndGetGridAreaMap(terrainElevationData.getGeotiffFileName(), terrainElevationData.getGeotiffFilePath());
                //double pixelArea = putAndGetGridAreaMap(terrainElevationData.getGeotiffFilePath());
                boolean isHigherResolution = pixelAreaAux > pixelArea; // smaller pixelArea is higher resolution
                if (isHigherResolution) {
                    if (noDataValue != 0.0) {
                        candidateElevation = elevation; // original.***
                        //candidateElevation = Math.max(candidateElevation, elevation); // new.***
                        pixelAreaAux = pixelArea;
                    }
                }
            } else {
                candidateElevation = Math.max(candidateElevation, elevation);
            }
        }

        resultElevation = candidateElevation;
        return resultElevation;
    }

    private void loadAllGeoTiff(String terrainElevationDataFolderPath, List<File> standardizedGeoTiffFiles) throws FactoryException, TransformException {
        // recursively load all geoTiff files
        geoTiffFileNames.clear();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

        if (myGaiaGeoTiffManager == null) {
            myGaiaGeoTiffManager = this.getGaiaGeoTiffManager();
        }
        GeometryFactory gf = new GeometryFactory();

        if (rootTerrainElevationDataQuadTree == null) {
            rootTerrainElevationDataQuadTree = new TerrainElevationDataQuadTree(null);
        }

        GridCoverage2D gridCoverage2D = null;
        String geoTiffFileName = null;
        String geoTiffFilePath = null;

        CoordinateReferenceSystem crsTarget = null;
        CoordinateReferenceSystem crsWgs84 = null;
        MathTransform targetToWgs = null;

        Map<String, String> mapNoUsableGeotiffPaths = this.tileWgs84Manager.getMapNoUsableGeotiffPaths();

        for (File geoTiffFile : standardizedGeoTiffFiles) {
            geoTiffFileName = geoTiffFile.getName();

            // check if "geoTiffFileName" exist in the "geoTiffFileNames" list
            if (!geoTiffFileNames.contains(geoTiffFileName)) {
                // if no exist, use the standardize file.***
                geoTiffFilePath = geoTiffFile.getAbsolutePath();
            } else {
                geoTiffFilePath = terrainElevationDataFolderPath + File.separator + geoTiffFileName;
            }

            // check if this geoTiff is usable
            if (mapNoUsableGeotiffPaths.containsKey(geoTiffFilePath)) {
                continue;
            }

            TerrainElevationData terrainElevationData = new TerrainElevationData(this);

            gridCoverage2D = myGaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);
            terrainElevationData.setGeotiffFilePath(geoTiffFilePath);
            terrainElevationData.setGeotiffFileName(geoTiffFileName);

            crsTarget = gridCoverage2D.getCoordinateReferenceSystem2D();
            crsWgs84 = CRS.decode("EPSG:4326", true);
            targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);

            GaiaGeoTiffUtils.getGeographicExtension(gridCoverage2D, gf, targetToWgs, terrainElevationData.getGeographicExtension());
            terrainElevationData.setPixelSizeMeters(GaiaGeoTiffUtils.getPixelSizeMeters(gridCoverage2D));

            rootTerrainElevationDataQuadTree.addTerrainElevationData(terrainElevationData);
            gridCoverage2D.dispose(true);

        }

        // now check if exist folders inside the terrainElevationDataFolderPath
        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        for (String folderName : folderNames) {
            String folderPath = terrainElevationDataFolderPath + File.separator + folderName;
            loadAllGeoTiff(folderPath, standardizedGeoTiffFiles);
        }
    }

    public Double putAndGetGridAreaMap(String fileName, String path) {
        if (gridAreaMap.containsKey(fileName)) {
            return gridAreaMap.get(fileName);
        }

        Double pixelArea = 0.0d;
        File file = new File(path);
        //String fileName = file.getName();

        File standardizationTempPath = new File(globalOptions.getStandardizeTempPath());
        File tempFile = new File(standardizationTempPath, fileName);

        if (tempFile.exists()) {
            try {
                GaiaGeoTiffManager gaiaGeoTiffManager = this.getGaiaGeoTiffManager();
                GridCoverage2D coverage = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(tempFile.getAbsolutePath());
                Vector2d originalArea = GaiaGeoTiffUtils.getPixelSizeMeters(coverage);
                pixelArea = originalArea.x * originalArea.y;
                coverage.dispose(true);
            } catch (FactoryException e) {
                log.error("[getPixelArea : FactoryException] Error in getPixelArea", e);
            }
        }
        gridAreaMap.put(fileName, pixelArea);
        return gridAreaMap.get(fileName);
    }

    public void deleteGeoTiffManager() {
        if (myGaiaGeoTiffManager != null) {
            myGaiaGeoTiffManager.deleteObjects();
            myGaiaGeoTiffManager = null;
        }
    }
}
