package com.terrain.model;

import com.terrain.common.HalfEdgeFace;
import com.terrain.geometry.GaiaRectangle;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GaiaTextureScissorData {
    private GaiaRectangle originBoundary;
    private GaiaRectangle currentBoundary;
    private GaiaRectangle batchedBoundary;
    private GaiaRectangle texCoordBoundary;
    private List<HalfEdgeFace> faces;

    public GaiaRectangle getOriginBoundary() {
        if (originBoundary == null) {
            double w = currentBoundary.getMaxX() - currentBoundary.getMinX();
            double h = currentBoundary.getMaxY() - currentBoundary.getMinY();
            originBoundary = new GaiaRectangle(0, 0, w, h);
        }
        return originBoundary;
    }

    public boolean mergeIfMergeable(GaiaTextureScissorData other) {
        if (!isMergeable(other)) {
            return false;
        }

        currentBoundary.addBoundingRectangle(other.currentBoundary);
        texCoordBoundary.addBoundingRectangle(other.texCoordBoundary);
        faces.addAll(other.faces);
        other.faces.clear(); // clear the faces of the other.

        return true;
    }

    public boolean isMergeable(GaiaTextureScissorData other) {
        if (currentBoundary == null || other.currentBoundary == null) {
            return false;
        }
        if (texCoordBoundary == null || other.texCoordBoundary == null) {
            return false;
        }
        if (faces == null || other.faces == null) {
            return false;
        }

        // check the current boundary.
        double thisArea = currentBoundary.getArea();
        double otherArea = other.currentBoundary.getArea();
        GaiaRectangle mergedBoundary = new GaiaRectangle(currentBoundary);
        mergedBoundary.addBoundingRectangle(other.currentBoundary);
        double mergedArea = mergedBoundary.getArea();

        return !(mergedArea > thisArea + otherArea);
    }
}
