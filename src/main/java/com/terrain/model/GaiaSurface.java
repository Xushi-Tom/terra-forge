package com.terrain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that represents a face of a Gaia object.
 * It contains the indices and the face normal.
 * The face normal is calculated by the indices and the vertices.
 */
@Slf4j
@Getter
@Setter
public class GaiaSurface extends SurfaceStructure implements Serializable {

    public int[] getIndices() {
        int index = 0;
        int indicesCount = getIndicesCount();
        int[] resultIndices = new int[indicesCount];
        for (GaiaFace face : faces) {
            for (int indices : face.getIndices()) {
                resultIndices[index++] = indices;
            }
        }
        return resultIndices;
    }

    public int getIndicesCount() {
        int count = 0;
        for (GaiaFace face : faces) {
            count += face.getIndices().length;
        }
        return count;
    }

    public void clear() {
        for (GaiaFace face : faces) {
            if (face != null) {
                face.clear();
            }
        }
        faces.clear();
    }

    public GaiaSurface clone() {
        GaiaSurface clonedSurface = new GaiaSurface();
        for (GaiaFace face : faces) {
            if (face != null) {
                clonedSurface.getFaces().add(face.clone());
            }
        }
        return clonedSurface;
    }

    public int deleteDegeneratedFaces(List<GaiaVertex> vertices) {
        List<GaiaFace> facesToDelete = new ArrayList<>();
        for (GaiaFace face : faces) {
            if (face.isDegenerated(vertices)) {
                facesToDelete.add(face);
            }
        }
        int facesToDeleteCount = facesToDelete.size();
        faces.removeAll(facesToDelete);

        return facesToDeleteCount;
    }

    public void makeTriangleFaces() {
        List<GaiaFace> facesToAdd = new ArrayList<>();
        List<GaiaFace> triFaces = new ArrayList<>();
        for (GaiaFace face : faces) {
            triFaces.clear();
            triFaces = face.getTriangleFaces(triFaces);
            facesToAdd.addAll(triFaces);
        }

        faces.clear();
        faces.addAll(facesToAdd);
    }

    public void makeTriangularFaces(List<GaiaVertex> vertices) {
        List<GaiaFace> facesToAdd = new ArrayList<>();
        List<GaiaFace> triangularFaces = new ArrayList<>();
        for (GaiaFace face : faces) {
            triangularFaces.clear();
            facesToAdd.addAll(face.getTriangleFaces(triangularFaces));
        }
        faces.clear();
        faces.addAll(facesToAdd);
    }
}
