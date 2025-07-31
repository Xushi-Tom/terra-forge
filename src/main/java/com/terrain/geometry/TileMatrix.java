package com.terrain.geometry;

import com.terrain.common.*;
import com.terrain.enums.TerrainObjectStatus;
import com.terrain.manager.*;
import com.twelvemonkeys.io.LittleEndianDataOutputStream;
import com.utils.FileUtils;
import com.utils.GeometryUtils;
import com.utils.GlobeUtils;
import com.utils.TerrainMeshUtils;
import com.utils.TileWgs84Utils;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.opengis.referencing.operation.TransformException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.abs;

@Slf4j
public class TileMatrix {

    private static final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TileRange tilesRange;
    private final List<List<TileWgs84>> tilesMatrixRowCol = new ArrayList<>();
    public TileWgs84Manager manager;

    List<TerrainVertex> listVertices = new ArrayList<>();
    List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();

    public TileMatrix(TileRange tilesRange, TileWgs84Manager manager) {
        this.tilesRange = tilesRange;
        this.manager = manager;
    }

    public void deleteObjects() {
        for (List<TileWgs84> row : tilesMatrixRowCol) {
            for (TileWgs84 tile : row) {
                if (tile != null) tile.deleteObjects();
            }
        }

        listVertices.clear();
        listHalfEdges.clear();
    }

    private boolean setTwinsBetweenHalfEdgesInverseOrder(List<TerrainHalfEdge> listHEdges_A, List<TerrainHalfEdge> listHEdges_B) {
        if (listHEdges_A.size() != listHEdges_B.size()) {
            log.error("The size of the halfEdges lists are different.");
            return false;
        }

        int countA = listHEdges_A.size();
        for (int i = 0; i < countA; i++) {
            TerrainHalfEdge halfEdge = listHEdges_A.get(i);
            if (halfEdge.getTwin() != null) {
                continue;
            }

            int idxInverse = countA - i - 1;
            TerrainHalfEdge halfEdge2 = listHEdges_B.get(idxInverse);
            if (halfEdge2.getTwin() != null) {
                continue;
            }

            TerrainVertex startVertex = halfEdge.getStartVertex();
            TerrainVertex endVertex = halfEdge.getEndVertex();

            TerrainVertex startVertex2 = halfEdge2.getStartVertex();
            TerrainVertex endVertex2 = halfEdge2.getEndVertex();

            List<TerrainHalfEdge> outingHalfEdges_strVertex2 = startVertex2.getAllOutingHalfEdges();
            List<TerrainHalfEdge> outingHalfEdges_endVertex2 = endVertex2.getAllOutingHalfEdges();

            for (TerrainHalfEdge outingHalfEdge : outingHalfEdges_strVertex2) {

                outingHalfEdge.setStartVertex(endVertex);
            }

            for (TerrainHalfEdge outingHalfEdge : outingHalfEdges_endVertex2) {

                outingHalfEdge.setStartVertex(startVertex);
            }

            halfEdge.setTwin(halfEdge2);

            if (!startVertex2.equals(endVertex)) {
                startVertex2.setObjectStatus(TerrainObjectStatus.DELETED);
            }

            if (!endVertex2.equals(startVertex)) {
                endVertex2.setObjectStatus(TerrainObjectStatus.DELETED);
            }
        }

        return true;
    }

    public void makeMatrixMesh(boolean isFirstGeneration) throws TransformException, IOException {
        TileIndices tileIndices = new TileIndices();

        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        tilesMatrixRowCol.clear();
        int minTileX = tilesRange.getMinTileX() - 1;
        int maxTileX = tilesRange.getMaxTileX() + 1;
        int minTileY = tilesRange.getMinTileY() - 1;
        int maxTileY = tilesRange.getMaxTileY() + 1;

        int totalTiles = (maxTileX - minTileX + 1) * (maxTileY - minTileY + 1);

        int counter = 0;
        int counterAux = 0;
        for (int Y = minTileY; Y <= maxTileY; Y++) {
            List<TileWgs84> tilesListRow = new ArrayList<>();
            for (int X = minTileX; X <= maxTileX; X++) {
                tileIndices.set(X, Y, tilesRange.getTileDepth());
                TileWgs84 tile = null;
                if (isFirstGeneration) {
                    tile = this.manager.loadOrCreateTileWgs84(tileIndices);
                } else {
                    tile = this.manager.loadTileWgs84(tileIndices);
                }
                if (counter >= 100) {
                    counter = 0;
                    log.debug("Loading Tile Level : {}, i : {}/{}", tileIndices.getL(), counterAux, totalTiles);
                }

                tilesListRow.add(tile);

                counter++;
                counterAux++;
            }
            tilesMatrixRowCol.add(tilesListRow);
        }

        int rowsCount = tilesMatrixRowCol.size();
        int colsCount = tilesMatrixRowCol.get(0).size();
        log.debug("Making TileMatrix columns : {}, rows : {} ", colsCount, rowsCount);

        List<TerrainMesh> rowMeshesList = new ArrayList<>();
        for (int i = 0; i < rowsCount; i++) {
            List<TileWgs84> rowTilesArray = tilesMatrixRowCol.get(i);
            TerrainMesh rowMesh = null;

            for (int j = 0; j < colsCount; j++) {
                TileWgs84 tile = rowTilesArray.get(j);
                if (tile != null) {
                    TerrainMesh tileMesh = tile.getMesh();
                    if (rowMesh == null) {
                        rowMesh = tileMesh;
                    } else {

                        List<TerrainHalfEdge> rowMeshRightHalfEdges = rowMesh.getRightHalfEdgesSortedDownToUp();
                        List<TerrainHalfEdge> tileMeshLeftHalfEdges = tileMesh.getLeftHalfEdgesSortedUpToDown();

                        // the c_tile can be null
                        if (!rowMeshRightHalfEdges.isEmpty()) {
                            this.setTwinsBetweenHalfEdgesInverseOrder(rowMeshRightHalfEdges, tileMeshLeftHalfEdges);

                            // now, merge the left tile mesh to the result mesh.
                            rowMesh.removeDeletedObjects();
                            rowMesh.mergeMesh(tileMesh);
                        }
                    }
                }
            }
            rowMeshesList.add(rowMesh);
        }

        // now, join all the rowMeshes
        TerrainMesh resultMesh = null;
        for (TerrainMesh rowMesh : rowMeshesList) {
            if (rowMesh == null) {
                continue;
            }

            if (resultMesh == null) {
                resultMesh = rowMesh;
            } else {
                if (originIsLeftUp) {

                    List<TerrainHalfEdge> resultMeshDownHalfEdges = resultMesh.getDownHalfEdgesSortedLeftToRight();
                    List<TerrainHalfEdge> rowMeshUpHalfEdges = rowMesh.getUpHalfEdgesSortedRightToLeft();

                    // the c_tile can be null
                    if (!resultMeshDownHalfEdges.isEmpty()) {
                        // now, set twins of halfEdges
                        this.setTwinsBetweenHalfEdgesInverseOrder(resultMeshDownHalfEdges, rowMeshUpHalfEdges);
                        // now, merge the row mesh to the result mesh.
                        resultMesh.removeDeletedObjects();
                        resultMesh.mergeMesh(rowMesh);
                    }
                } else {

                    List<TerrainHalfEdge> resultMeshUpHalfEdges = resultMesh.getUpHalfEdgesSortedRightToLeft();
                    List<TerrainHalfEdge> rowMeshDownHalfEdges = rowMesh.getDownHalfEdgesSortedLeftToRight();

                    // the c_tile can be null
                    if (!resultMeshUpHalfEdges.isEmpty()) {
                        // now, set twins of halfEdges
                        this.setTwinsBetweenHalfEdgesInverseOrder(resultMeshUpHalfEdges, rowMeshDownHalfEdges);
                        // now, merge the row mesh to the result mesh.
                        resultMesh.removeDeletedObjects();
                        resultMesh.mergeMesh(rowMesh);
                    }
                }
            }
        }

        log.debug("End making TileMatrix");

        if (resultMesh != null) {
            resultMesh.setObjectsIdInList();

            this.recalculateElevation(resultMesh, tilesRange);
            this.refineMesh(resultMesh, tilesRange);

            // check if you must calculate normals
            if (globalOptions.isCalculateNormals()) {
                this.listVertices.clear();
                this.listHalfEdges.clear();
                resultMesh.calculateNormals(this.listVertices, this.listHalfEdges);
            }

            // now save the 9 tiles
            List<TerrainMesh> separatedMeshes = new ArrayList<>();
            TerrainMeshUtils.getSeparatedMeshes(resultMesh, separatedMeshes, originIsLeftUp);

            log.debug("正在保存分离后的瓦片...");

            saveSeparatedTiles(separatedMeshes);

            // now save quantizedMeshes
            log.debug("正在保存量化网格...");
            saveQuantizedMeshes(separatedMeshes);

            if (tilesRange.getTileDepth() < globalOptions.getMaximumTileDepth()) {
                log.debug("正在保存分离后的子瓦片...");
                saveSeparatedChildrenTiles(separatedMeshes);
            }
        } else {
            log.error("结果网格（ResultMesh）为空。");
        }
    }

    public void saveQuantizedMeshes(List<TerrainMesh> separatedMeshes) throws IOException {
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
        boolean calculateNormals = globalOptions.isCalculateNormals();

        for (TerrainMesh mesh : separatedMeshes) {
            TerrainTriangle triangle = mesh.triangles.get(0); // take the first triangle
            TileIndices tileIndices = triangle.getOwnerTileIndices();

            TileWgs84 tile = new TileWgs84(null, this.manager);
            tile.setTileIndices(tileIndices);
            String imageryType = this.manager.getImaginaryType();
            tile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp));
            tile.setMesh(mesh);

            QuantizedMeshManager quantizedMeshManager = new QuantizedMeshManager();
            QuantizedMesh quantizedMesh = quantizedMeshManager.getQuantizedMeshFromTile(tile, calculateNormals);
            String tileFullPath = this.manager.getQuantizedMeshTilePath(tileIndices);
            String tileFolderPath = this.manager.getQuantizedMeshTileFolderPath(tileIndices);
            FileUtils.createAllFoldersIfNoExist(tileFolderPath);

            LittleEndianDataOutputStream dataOutputStream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(tileFullPath)));

            // save the tile
            quantizedMesh.saveDataOutputStream(dataOutputStream, calculateNormals);
            dataOutputStream.close();
        }
    }

    public boolean saveSeparatedTiles(List<TerrainMesh> separatedMeshes) {
        int meshesCount = separatedMeshes.size();
        int counter = 0;
        for (int i = 0; i < meshesCount; i++) {
            TerrainMesh mesh = separatedMeshes.get(i);

            TerrainTriangle triangle = mesh.triangles.get(0);
            TileIndices tileIndices = triangle.getOwnerTileIndices();
            String tileTempDirectory = globalOptions.getTileTempPath();
            String tileFilePath = TileWgs84Utils.getTileFilePath(tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
            String tileFullPath = tileTempDirectory + File.separator + tileFilePath;

            if (counter >= 100) {
                counter = 0;
                log.debug("正在保存分离后的瓦片... 层级 : " + tileIndices.getL() + " 序号 : " + i + " / " + meshesCount);
            }

            try {
                mesh.saveFile(tileFullPath);
            } catch (IOException e) {
                log.error("Error:", e);
                return false;
            }
            counter++;
        }

        return true;
    }

    private void saveSeparatedChildrenTiles(List<TerrainMesh> separatedMeshes) {
        for (TerrainMesh mesh : separatedMeshes) {
            TerrainMeshUtils.save4ChildrenMeshes(mesh, this.manager, globalOptions);
        }
    }

    public void recalculateElevation(TerrainMesh terrainMesh, TileRange tilesRange) throws TransformException, IOException {
        List<TerrainTriangle> triangles = new ArrayList<>();
        terrainMesh.getTrianglesByTilesRange(tilesRange, triangles, null);

        HashMap<TerrainVertex, TerrainVertex> mapVertices = new HashMap<>();
        for (TerrainTriangle triangle : triangles) {
            this.listVertices.clear();
            this.listHalfEdges.clear();
            this.listVertices = triangle.getVertices(this.listVertices, this.listHalfEdges);
            for (TerrainVertex vertex : this.listVertices) {
                mapVertices.put(vertex, vertex);
            }
        }

        // now make vertices from the hashMap
        List<TerrainVertex> verticesOfCurrentTile = new ArrayList<>(mapVertices.values());
        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();

        int verticesCount = verticesOfCurrentTile.size();
        log.debug("recalculating elevations... vertices count : " + verticesCount);
        TileIndices tileIndicesAux = new TileIndices();
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
        int currDepth = tilesRange.getTileDepth();
        for (int i = 0; i < verticesCount; i++) {
            TerrainVertex vertex = verticesOfCurrentTile.get(i);
            TileWgs84Utils.selectTileIndices(currDepth, vertex.getPosition().x, vertex.getPosition().y, tileIndicesAux, originIsLeftUp);
            vertex.getPosition().z = terrainElevationDataManager.getElevationBilinearRasterTile(tileIndicesAux, this.manager, vertex.getPosition().x, vertex.getPosition().y);
        }
    }

    /**
     * 判断给定的三角形是否需要进行细分。
     * 该方法会从多个维度进行检查，包括三角形的尺寸、与地形数据的相交情况、
     * 三角形重心与平面的距离以及栅格瓦片的相关计算等。
     *
     * @param triangle 要判断是否需要细分的三角形
     * @return 如果需要细分则返回 true，否则返回 false
     */
    public boolean mustRefineTriangle(TerrainTriangle triangle) {
        // 检查该三角形是否已经检查过细分，若检查过则不再进行细分
        if (triangle.isRefineChecked()) {
            return false;
        }

        // 获取地形高程数据管理器
        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();
        // 获取三角形所属的瓦片索引
        TileIndices tileIndices = triangle.getOwnerTileIndices();

        // 检查三角形是否需要细分
        this.listVertices.clear();
        this.listHalfEdges.clear();
        // 获取三角形的边界框
        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox(this.listVertices, this.listHalfEdges);
        // 获取边界框在 XY 平面的最长距离
        double bboxMaxLength = bboxTriangle.getLongestDistanceXY();
        // 获取地球赤道半径
        double equatorialRadius = GlobeUtils.EQUATORIAL_RADIUS;
        // 将边界框的最长距离转换为米
        double bboxMaxLengthInMeters = Math.toRadians(bboxMaxLength) * equatorialRadius;

        // 获取当前瓦片层级
        int currL = triangle.getOwnerTileIndices().getL();

        // 获取当前层级瓦片的尺寸（米）
        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(currL);
        // 计算边界框最长距离与瓦片尺寸的比例
        double scale = bboxMaxLengthInMeters / tileSize;

        // 根据公式 Y = 0.8X + 0.2 调整比例
        scale = 0.8 * scale + 0.2;

        // 获取当前层级下 GeoTIFF 样本与三角形平面的最大允许差值
        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(triangle.getOwnerTileIndices().getL());
        // 根据比例调整最大允许差值
        maxDiff *= scale;

        // 获取当前瓦片索引对应的栅格瓦片
        TileWgs84Raster tileRaster = terrainElevationDataManager.getTileWgs84Raster(tileIndices, this.manager);

        // 如果三角形尺寸非常小，则不进行细分
        this.listVertices.clear();
        this.listHalfEdges.clear();
        // 获取三角形的最大边长（米）
        double triangleMaxLengthMeters = triangle.getTriangleMaxSizeInMeters(this.listVertices, this.listHalfEdges);
        // 获取当前层级下三角形的最小尺寸
        double minTriangleSizeForDepth = this.manager.getMinTriangleSizeForTileDepth(triangle.getOwnerTileIndices().getL());

        if (triangleMaxLengthMeters < minTriangleSizeForDepth) {
            // 标记该三角形已检查过细分
            triangle.setRefineChecked(true);
            log.debug("因三角形最小尺寸过滤 : 层级 : " + tileIndices.getL() + " # 三角形最大边长（米）: " + triangleMaxLengthMeters + " # 当前层级三角形最小尺寸: " + minTriangleSizeForDepth);
            return false;
        }

        // 获取当前层级下三角形的最大尺寸
        double maxTriangleSizeForDepth = this.manager.getMaxTriangleSizeForTileDepth(triangle.getOwnerTileIndices().getL());
        if (triangleMaxLengthMeters > maxTriangleSizeForDepth) {
            log.debug("因三角形最大尺寸过滤 : 层级 : " + tileIndices.getL() + " # 三角形最大边长（米）: " + triangleMaxLengthMeters + " # 当前层级三角形最大尺寸: " + maxTriangleSizeForDepth);
            return true;
        }

        // 检查三角形是否与地形数据相交
        GeographicExtension rootGeographicExtension = terrainElevationDataManager.getRootGeographicExtension();
        if (!rootGeographicExtension.intersectsBox(bboxTriangle.getMinX(), bboxTriangle.getMinY(), bboxTriangle.getMaxX(), bboxTriangle.getMaxY())) {
            // 仅检查三角形的三个顶点
            this.listVertices.clear();
            this.listHalfEdges.clear();
            this.listVertices = triangle.getVertices(this.listVertices, this.listHalfEdges);
            for (TerrainVertex vertex : this.listVertices) {
                if (vertex.getPosition().z > maxDiff) {
                    return true;
                }
            }

            return false;
        }

        // 检查栅格瓦片是否存在
        if (tileRaster == null) {
            return false;
        }

        // 计算三角形法线与瓦片中心笛卡尔坐标法线的夹角余弦值
        float cosAng = 1.0f;
        if (tileIndices.getL() > 10) {
            // 获取三角形的法线（世界坐标）
            Vector3f triangleNormalWC = triangle.getNormal();
            Vector3d triangleNormalDouble = new Vector3d(triangleNormalWC.x, triangleNormalWC.y, triangleNormalWC.z);
            // 获取栅格瓦片的地理范围
            GeographicExtension geographicExtension = tileRaster.getGeographicExtension();
            // 获取地理范围的中心点
            Vector3d centerGeoCoord = geographicExtension.getMidPoint();
            // 将地理坐标转换为笛卡尔坐标
            double[] centerCartesian = GlobeUtils.geographicToCartesianWgs84(centerGeoCoord.x, centerGeoCoord.y, centerGeoCoord.z);
            // 获取笛卡尔坐标点的法线
            Vector3d normalAtCartesian = GlobeUtils.normalAtCartesianPointWgs84(centerCartesian[0], centerCartesian[1], centerCartesian[2]);
            // 计算两个单位向量的夹角余弦值
            cosAng = (float) GeometryUtils.cosineBetweenUnitaryVectors(triangleNormalDouble.x, triangleNormalDouble.y, triangleNormalDouble.z, normalAtCartesian.x, normalAtCartesian.y, normalAtCartesian.z);
        }

        // 检查三角形的重心
        this.listVertices.clear();
        this.listHalfEdges.clear();
        // 获取三角形所在的平面
        TerrainPlane plane = triangle.getPlane(this.listVertices, this.listHalfEdges);
        this.listVertices.clear();
        this.listHalfEdges.clear();
        // 获取三角形的重心
        Vector3d barycenter = triangle.getBarycenter(this.listVertices, this.listHalfEdges);
        // 获取重心所在的列索引
        int colIdx = tileRaster.getColumn(barycenter.x);
        // 获取重心所在的行索引
        int rowIdx = tileRaster.getRow(barycenter.y);
        // 获取重心所在列的经度
        double barycenterLonDeg = tileRaster.getLonDeg(colIdx);
        // 获取重心所在行的纬度
        double barycenterLatDeg = tileRaster.getLatDeg(rowIdx);

        // 获取栅格瓦片在该位置的高程
        double elevation = tileRaster.getElevation(colIdx, rowIdx);
        // 计算平面在该位置的高程
        double planeElevation = plane.getValueZ(barycenterLonDeg, barycenterLatDeg);

        // 计算高程与平面高程的距离，并乘以夹角余弦值
        double distToPlane = abs(elevation - planeElevation) * cosAng;

        if (distToPlane > maxDiff) {
            // 因重心点距离平面过远，需要细分
            log.debug("因重心点过滤 : 层级 : " + tileIndices.getL() + " # 列号 : " + colIdx + " # 行号 : " + rowIdx + " # 到平面的距离 : " + distToPlane + " # 最大差值 : " + maxDiff);

            return true;
        }

        // 获取三角形在栅格瓦片中的边界框
        int startCol = tileRaster.getColumn(bboxTriangle.getMinX());
        int startRow = tileRaster.getRow(bboxTriangle.getMinY());
        int endCol = tileRaster.getColumn(bboxTriangle.getMaxX());
        int endRow = tileRaster.getRow(bboxTriangle.getMaxY());

        // 计算边界框的列数和行数
        int colsCount = endCol - startCol + 1;
        int rowsCount = endRow - startRow + 1;

        // 如果列数或行数小于 6，则不进行细分
        if (colsCount < 6 || rowsCount < 6) {
            triangle.setRefineChecked(true);
            return false;
        }

        // 获取三角形在栅格瓦片中的表示
        RasterTriangle rasterTriangle = tileRaster.getRasterTriangle(triangle);
        // 获取三角形在栅格瓦片中的三个顶点
        Vector2i rasterTriangleP1 = rasterTriangle.getP1();
        Vector2i rasterTriangleP2 = rasterTriangle.getP2();
        Vector2i rasterTriangleP3 = rasterTriangle.getP3();

        // 计算用于重心坐标的参数
        int deltaYBC = rasterTriangleP2.y - rasterTriangleP3.y;
        int deltaYCA = rasterTriangleP3.y - rasterTriangleP1.y;
        int deltaYAC = rasterTriangleP1.y - rasterTriangleP2.y;
        int deltaXCB = rasterTriangleP3.x - rasterTriangleP2.x;
        int deltaXAC = rasterTriangleP1.x - rasterTriangleP3.x;

        // 计算分母
        double denominator = deltaYBC * deltaXAC + deltaXCB * deltaYAC;

        // 获取起始列的经度
        double startLonDeg = tileRaster.getLonDeg(startCol);
        // 获取起始行的纬度
        double startLatDeg = tileRaster.getLatDeg(startRow);

        // 获取经度和纬度的增量
        double deltaLonDeg = tileRaster.getDeltaLonDeg();
        double deltaLatDeg = tileRaster.getDeltaLatDeg();

        double posX;
        double posY;

        int colAux = 0;
        int rowAux = 0;

        boolean intersects = false;
        // 遍历三角形边界框内的所有像素
        for (int col = startCol; col <= endCol; col++) {
            rowAux = 0;
            posX = startLonDeg + colAux * deltaLonDeg;
            for (int row = startRow; row <= endRow; row++) {

                // 跳过三角形边界框的四个角
                if (col == startCol && row == startRow) {
                    rowAux++;
                    continue;
                } else if (col == startCol && row == endRow) {
                    rowAux++;
                    continue;
                } else if (col == endCol && row == startRow) {
                    rowAux++;
                    continue;
                } else if (col == endCol && row == endRow) {
                    rowAux++;
                    continue;
                }

                // 检查像素是否在三角形内
                intersects = false;
                // 计算重心坐标的 alpha 值
                double alpha = (deltaYBC * (col - rasterTriangleP3.x) + deltaXCB * (row - rasterTriangleP3.y)) / denominator;
                if (alpha < 0 || alpha > 1) {
                    rowAux++;
                    continue;
                }
                // 计算重心坐标的 beta 值
                double beta = (deltaYCA * (col - rasterTriangleP3.x) + deltaXAC * (row - rasterTriangleP3.y)) / denominator;
                if (beta < 0 || beta > 1) {
                    rowAux++;
                    continue;
                }
                // 计算重心坐标的 gamma 值
                double gamma = 1.0 - alpha - beta;
                if (gamma < 0 || gamma > 1) {
                    rowAux++;
                    continue;
                }

                if (alpha >= 0 && beta >= 0 && gamma >= 0) {
                    intersects = true;
                }

                if (!intersects) {
                    rowAux++;
                    continue;
                }

                posY = startLatDeg + rowAux * deltaLatDeg;

                // 获取栅格瓦片在该像素位置的高程
                float elevationFloat = tileRaster.getElevation(col, row);

                // 计算平面在该像素位置的高程
                planeElevation = plane.getValueZ(posX, posY);

                // 计算高程与平面高程的距离，并乘以夹角余弦值
                distToPlane = abs(elevationFloat - planeElevation) * cosAng;
                if (distToPlane > maxDiff) {
                    // 因栅格瓦片内像素距离平面过远，需要细分
                    log.debug("因栅格瓦片过滤 : 层级 : " + tileIndices.getL() + " # 列号 : " + col + " / " + colsCount + " # 行号 : " + row + " / " + rowsCount + " # 夹角余弦值 : " + cosAng + " # 到平面的距离 : " + distToPlane + " # 最大差值 : " + maxDiff);

                    return true;
                }
                rowAux++;
            }
            colAux++;
        }
        // 标记该三角形已检查过细分
        triangle.setRefineChecked(true);
        log.debug("因栅格瓦片过滤 : 层级 : " + tileIndices.getL() + " # 列号 : " + colAux + " / " + colsCount + " # 行号 : " + rowAux + " / " + rowsCount + " # 夹角余弦值 : " + cosAng + " # 到平面的距离 : " + distToPlane + " # 最大差值 : " + maxDiff);
        return false;
    }


    private boolean refineMeshOneIteration(TerrainMesh mesh, TileRange tilesRange) throws TransformException, IOException {
        // Inside the mesh, there are triangles of 9 different tiles
        // Here refine only the triangles of the current tile

        // refine the mesh
        AtomicBoolean refined = new AtomicBoolean(false);
        AtomicInteger splitCount = new AtomicInteger();
        int trianglesCount = mesh.triangles.size();
        log.debug("[RefineMesh] Triangles count : {}", trianglesCount);
        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = mesh.triangles.get(i);

            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }

            if (!tilesRange.intersects(triangle.getOwnerTileIndices())) {
                continue;
            }
            if (mustRefineTriangle(triangle)) {
                this.manager.getTriangleList().clear();
                this.listHalfEdges.clear();
                mesh.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(), this.manager.getTriangleList(), this.listHalfEdges);
                this.listHalfEdges.clear();

                if (!this.manager.getTriangleList().isEmpty()) {
                    splitCount.getAndIncrement();
                    refined.set(true);
                }
                this.manager.getTriangleList().clear();
            }
        }

        if (refined.get()) {
            log.debug("Removing deleted Meshes : Splited count : {}", splitCount);
            mesh.removeDeletedObjects();
            mesh.setObjectsIdInList();
        }

        return refined.get();
    }

    public void refineMesh(TerrainMesh mesh, TileRange tilesRange) throws TransformException, IOException {
        // Inside the mesh, there are triangles of n different tiles
        // Here refine only the triangles of the tiles of TilesRange

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(tilesRange.getTileDepth());
        log.debug("[RefineMesh] Tile Level : {} # MaxDiff(m) : {}", tilesRange.getTileDepth(), maxDiff);

        // refine the mesh
        boolean finished = false;
        int splitCount = 0;
        int maxIterations = this.manager.getTriangleRefinementMaxIterations();
        while (!finished) {
            if (!this.refineMeshOneIteration(mesh, tilesRange)) {
                finished = true;
            }

            splitCount++;

            if (splitCount >= maxIterations) {
                finished = true;
            }
        }
    }
}
