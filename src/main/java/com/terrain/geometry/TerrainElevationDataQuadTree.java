package com.terrain.geometry;

import com.terrain.common.GeographicExtension;
import com.terrain.manager.TerrainElevationData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TerrainElevationDataQuadTree {
    private static final int CHILDREN_COUNT = 4;
    private int depth = 0;

    private TerrainElevationDataQuadTree parent;
    private GeographicExtension geographicExtension = new GeographicExtension();
    private TerrainElevationDataQuadTree[] children = null;
    private List<TerrainElevationData> terrainElevationDataList = new ArrayList<>();

    public TerrainElevationDataQuadTree(TerrainElevationDataQuadTree parent) {
        this.parent = parent;
        if (parent != null) {
            this.depth = parent.getDepth() + 1;
        }
    }

    public void deleteObjects() {
        if (terrainElevationDataList != null) {
            for (TerrainElevationData terrainElevationData : terrainElevationDataList) {
                terrainElevationData.deleteObjects();
            }
        }

        if (children != null) {
            for (int i = 0; i < CHILDREN_COUNT; i++) {
                children[i].deleteObjects();
            }
        }

        terrainElevationDataList = null;
        children = null;
        parent = null;
        geographicExtension.deleteObjects();
        geographicExtension = null;
    }

    public void addTerrainElevationData(TerrainElevationData terrainElevationData) {
        terrainElevationDataList.add(terrainElevationData);
    }

    public void calculateGeographicExtension() {
        int terrainElevationDataCount = terrainElevationDataList.size();
        for (int i = 0; i < terrainElevationDataCount; i++) {
            TerrainElevationData terrainElevationData = terrainElevationDataList.get(i);
            GeographicExtension geographicExtension = terrainElevationData.getGeographicExtension();

            if (i == 0) {
                this.geographicExtension.copyFrom(geographicExtension);
            } else {
                this.geographicExtension.union(geographicExtension);
            }
        }
    }

    private void makeTree(int maxDepth) {
        // if my data count is less than 2, then return
        if (terrainElevationDataList.size() < 2) {
            return;
        }

        if (children == null) {
            children = new TerrainElevationDataQuadTree[4];
            children[0] = new TerrainElevationDataQuadTree(this);
            children[1] = new TerrainElevationDataQuadTree(this);
            children[2] = new TerrainElevationDataQuadTree(this);
            children[3] = new TerrainElevationDataQuadTree(this);

            GeographicExtension geographicExtension = this.geographicExtension;
            double midLonDeg = geographicExtension.getMidLongitudeDeg();
            double midLatDeg = geographicExtension.getMidLatitudeDeg();

            double minLonDeg = geographicExtension.getMinLongitudeDeg();
            double minLatDeg = geographicExtension.getMinLatitudeDeg();
            double maxLonDeg = geographicExtension.getMaxLongitudeDeg();
            double maxLatDeg = geographicExtension.getMaxLatitudeDeg();

            GeographicExtension geographicExtension0 = children[0].geographicExtension;
            geographicExtension0.setDegrees(minLonDeg, minLatDeg, 0.0, midLonDeg, midLatDeg, 0.0);

            GeographicExtension geographicExtension1 = children[1].geographicExtension;
            geographicExtension1.setDegrees(midLonDeg, minLatDeg, 0.0, maxLonDeg, midLatDeg, 0.0);

            GeographicExtension geographicExtension2 = children[2].geographicExtension;
            geographicExtension2.setDegrees(midLonDeg, midLatDeg, 0.0, maxLonDeg, maxLatDeg, 0.0);

            GeographicExtension geographicExtension3 = children[3].geographicExtension;
            geographicExtension3.setDegrees(minLonDeg, midLatDeg, 0.0, midLonDeg, maxLatDeg, 0.0);
        }

        // 2nd distribute data to children
        for (TerrainElevationData terrainElevationData : terrainElevationDataList) {
            GeographicExtension geographicExtension = terrainElevationData.getGeographicExtension();

            for (int j = 0; j < CHILDREN_COUNT; j++) {
                if (children[j].geographicExtension.intersects(geographicExtension)) {
                    children[j].addTerrainElevationData(terrainElevationData);
                }
            }
        }

        // now remove all data from this node
        terrainElevationDataList.clear();

        if (this.depth < maxDepth) {
            // continue making children
            for (int j = 0; j < CHILDREN_COUNT; j++) {
                children[j].makeTree(maxDepth);
            }
        }
    }

    public void deleteCoverage() {
        if (terrainElevationDataList != null) {
            for (TerrainElevationData terrainElevationData : terrainElevationDataList) {
                terrainElevationData.deleteCoverage();
            }
        }

        if (children != null) {
            for (int i = 0; i < CHILDREN_COUNT; i++) {
                children[i].deleteCoverage();
            }
        }
    }

    public void deleteCoverageIfNoIntersectsGeoExtension(GeographicExtension geographicExtension) {
        if (terrainElevationDataList != null) {
            for (TerrainElevationData terrainElevationData : terrainElevationDataList) {
                if (!geographicExtension.intersects(terrainElevationData.getGeographicExtension())) {
                    terrainElevationData.deleteCoverage();
                }
            }
        }

        if (children != null) {
            for (int i = 0; i < CHILDREN_COUNT; i++) {
                children[i].deleteCoverageIfNoIntersectsGeoExtension(geographicExtension);
            }
        }
    }

    public void deleteCoverageIfNotIntersects(GeographicExtension geographicExtension) {
        if (terrainElevationDataList != null) {
            for (TerrainElevationData terrainElevationData : terrainElevationDataList) {
                if (!geographicExtension.intersects(terrainElevationData.getGeographicExtension())) {
                    terrainElevationData.deleteCoverage();
                }
            }
        }

        if (children != null) {
            for (int i = 0; i < CHILDREN_COUNT; i++) {
                if (!geographicExtension.intersects(children[i].geographicExtension)) {
                    children[i].deleteCoverage();
                } else {
                    children[i].deleteCoverageIfNotIntersects(geographicExtension);
                }
            }
        }
    }

    public void makeQuadTree(int maxDepth) {
        calculateGeographicExtension();
        makeTree(maxDepth);
    }

    public void getTerrainElevationDataArray(GeographicExtension geoExtension, Map<TerrainElevationData, TerrainElevationData> terrainElevDataMap) {
        for (TerrainElevationData terrainElevationData : terrainElevationDataList) {
            GeographicExtension geographicExtension = terrainElevationData.getGeographicExtension();
            if (geographicExtension.intersects(geoExtension)) {
                terrainElevDataMap.put(terrainElevationData, terrainElevationData);
            }
        }

        if (children != null) {
            for (int j = 0; j < CHILDREN_COUNT; j++) {
                if (children[j].geographicExtension.intersects(geoExtension)) {
                    children[j].getTerrainElevationDataArray(geoExtension, terrainElevDataMap);
                }
            }
        }
    }
}
