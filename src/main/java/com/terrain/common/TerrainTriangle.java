package com.terrain.common;

import com.terrain.enums.TerrainObjectStatus;
import com.terrain.geometry.GaiaBoundingBox;
import com.terrain.io.BigEndianDataInputStream;
import com.terrain.io.BigEndianDataOutputStream;
import com.terrain.manager.TileIndices;
import com.utils.GlobeUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class TerrainTriangle {

    private int id = -1;
    private int halfEdgeId = -1;
    private Vector3f normal = null;
    private TileIndices ownerTileIndices = new TileIndices();
    private TerrainObjectStatus objectStatus = TerrainObjectStatus.ACTIVE;
    private GaiaBoundingBox myBoundingBox = null;
    private TerrainPlane myPlane = null;
    public TerrainHalfEdge halfEdge = null;
    private int splitDepth = 0;
    private boolean refineChecked = false;

    public void deleteObjects() {
        halfEdge = null;
        ownerTileIndices = null;
        myBoundingBox = null;
        myPlane = null;
    }

    public void setHalfEdge(TerrainHalfEdge halfEdge) {
        this.halfEdge = halfEdge;
        halfEdge.setTriangleToHEdgesLoop(this);
    }

    public List<TerrainVertex> getVertices(List<TerrainVertex> resultVertices, List<TerrainHalfEdge> listHalfEdges) {
        listHalfEdges.clear();
        this.halfEdge.getHalfEdgesLoop(listHalfEdges);
        if (resultVertices == null) resultVertices = new ArrayList<>();
        for (TerrainHalfEdge halfEdge : listHalfEdges) {
            resultVertices.add(halfEdge.getStartVertex());
        }
        listHalfEdges.clear();
        return resultVertices;
    }

    public GaiaBoundingBox getBoundingBox(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        listHalfEdges.clear();
        if (this.myBoundingBox == null) {
            this.myBoundingBox = new GaiaBoundingBox();
            listVertices.clear();
            listVertices = this.getVertices(listVertices, listHalfEdges);
            for (TerrainVertex vertex : listVertices) {
                this.myBoundingBox.addPoint(vertex.getPosition());
            }
        }

        return this.myBoundingBox;
    }

    public TerrainPlane getPlane(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        if (this.myPlane == null) {
            this.myPlane = new TerrainPlane();
            listVertices.clear();
            listHalfEdges.clear();
            listVertices = this.getVertices(listVertices, listHalfEdges);
            TerrainVertex vertex0 = listVertices.get(0);
            TerrainVertex vertex1 = listVertices.get(1);
            TerrainVertex vertex2 = listVertices.get(2);
            this.myPlane.set3Points(vertex0.getPosition(), vertex1.getPosition(), vertex2.getPosition());
        }

        return this.myPlane;
    }

    public Vector3d getBarycenter(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        listVertices.clear();
        listHalfEdges.clear();
        listVertices = this.getVertices(listVertices, listHalfEdges);
        Vector3d barycenter = new Vector3d();
        for (TerrainVertex vertex : listVertices) {
            barycenter.add(vertex.getPosition());
        }
        barycenter.mul(1.0 / 3.0);
        return barycenter;
    }

    public TerrainHalfEdge getLongestHalfEdge(List<TerrainHalfEdge> listHalfEdges) {
        // Note : the length of the halfEdges meaning only the length of the XY plane
        listHalfEdges.clear();
        this.halfEdge.getHalfEdgesLoop(listHalfEdges);
        TerrainHalfEdge longestHalfEdge = null;
        double maxLength = 0.0;
        for (TerrainHalfEdge halfEdge : listHalfEdges) {
            double length = halfEdge.getSquaredLengthXY();
            if (length > maxLength) {
                maxLength = length;
                longestHalfEdge = halfEdge;
            }
        }
        listHalfEdges.clear();

        return longestHalfEdge;
    }

    public double getTriangleMaxSizeInMeters(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        listHalfEdges.clear();
        GaiaBoundingBox bboxTriangle = this.getBoundingBox(listVertices, listHalfEdges);
        double triangleMaxLengthDeg = Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY());
        double triangleMaxLengthRad = Math.toRadians(triangleMaxLengthDeg);
        return triangleMaxLengthRad * GlobeUtils.EQUATORIAL_RADIUS;
    }

    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) {
        try {
            // First, save id
            dataOutputStream.writeInt(id);

            // 2nd, save halfEdge
            if (halfEdge != null) {
                dataOutputStream.writeInt(halfEdge.getId());
            } else {
                dataOutputStream.writeInt(-1);
            }

            // 3rd, save ownerTile_tileIndices
            if (ownerTileIndices == null) {
                dataOutputStream.writeInt(-1);
                dataOutputStream.writeInt(-1);
                dataOutputStream.writeInt(-1);
            } else {
                ownerTileIndices.saveDataOutputStream(dataOutputStream);
            }

            // save splitDepth
            dataOutputStream.writeInt(splitDepth);

        } catch (Exception e) {
            log.error("Error:", e);
        }
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException {
        this.id = dataInputStream.readInt();
        this.halfEdgeId = dataInputStream.readInt();
        if (this.ownerTileIndices == null) {
            this.ownerTileIndices = new TileIndices();
        }
        this.ownerTileIndices.loadDataInputStream(dataInputStream);

        this.splitDepth = dataInputStream.readInt();
    }

    public void calculateNormal(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        listHalfEdges.clear();
        calculateNormalWC(listVertices, listHalfEdges);
    }

    public Vector3f getNormal() {
        if (this.normal == null) {
            List<TerrainVertex> listVertices = new ArrayList<>();
            List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();
            return this.getNormal(listVertices, listHalfEdges);
        }
        return this.normal;
    }

    public Vector3f getNormal(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        if (this.normal == null) {
            listHalfEdges.clear();
            calculateNormalWC(listVertices, listHalfEdges);
        }
        return this.normal;
    }

    public void calculateNormalWC(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        if (this.normal == null) {
            listHalfEdges.clear();
            listVertices = this.getVertices(listVertices, listHalfEdges);
            Vector3d p0 = listVertices.get(0).getPosition();
            Vector3d p1 = listVertices.get(1).getPosition();
            Vector3d p2 = listVertices.get(2).getPosition();

            Vector3d p0WC = GlobeUtils.geographicToCartesianWgs84(p0);
            Vector3d p1WC = GlobeUtils.geographicToCartesianWgs84(p1);
            Vector3d p2WC = GlobeUtils.geographicToCartesianWgs84(p2);

            Vector3d v1 = new Vector3d(p1WC).sub(p0WC);
            Vector3d v2 = new Vector3d(p2WC).sub(p0WC);
            Vector3d normalized = new Vector3d(v1).cross(v2).normalize();

            this.normal = new Vector3f((float) normalized.x, (float) normalized.y, (float) normalized.z);
        }
    }
}
