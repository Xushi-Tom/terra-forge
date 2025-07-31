package com.utils;

import com.terrain.common.*;
import com.terrain.enums.PlaneType;
import com.terrain.geometry.GaiaBoundingBox;
import com.terrain.geometry.GaiaOctreeVertices;
import com.terrain.geometry.GaiaPlane;
import com.terrain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HalfEdgeUtils {

    public static double calculateAngleBetweenNormals(Vector3d normalA, Vector3d normalB) {
        double dotProduct = normalA.dot(normalB);
        return Math.acos(dotProduct);
    }

    public static List<List<HalfEdgeFace>> getWeldedFacesGroups(List<HalfEdgeFace> facesList, List<List<HalfEdgeFace>> resultWeldedFacesGroups) {
        if (resultWeldedFacesGroups == null) {
            resultWeldedFacesGroups = new ArrayList<>();
        }

        Map<HalfEdgeVertex, List<HalfEdgeFace>> vertexFacesMap = new HashMap<>();
        for (HalfEdgeFace face : facesList) {
            List<HalfEdgeVertex> vertices = face.getVertices(null);
            for (HalfEdgeVertex vertex : vertices) {
                List<HalfEdgeFace> facesOfVertex = vertexFacesMap.computeIfAbsent(vertex, k -> new ArrayList<>());
                facesOfVertex.add(face);
            }
        }

        Map<HalfEdgeFace, HalfEdgeFace> mapVisitedFaces = new HashMap<>();
        int facesCount = facesList.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = facesList.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (mapVisitedFaces.containsKey(face)) {
                continue;
            }

            List<HalfEdgeFace> weldedFaces = new ArrayList<>();
            getWeldedFacesWithFace(face, weldedFaces, mapVisitedFaces);

            resultWeldedFacesGroups.add(weldedFaces);
        }

        return resultWeldedFacesGroups;
    }

    public static boolean getWeldedFacesWithFace(HalfEdgeFace face, List<HalfEdgeFace> resultWeldedFaces, Map<HalfEdgeFace, HalfEdgeFace> mapVisitedFaces) {
        List<HalfEdgeFace> weldedFacesAux = new ArrayList<>();
        List<HalfEdgeFace> faces = new ArrayList<>();
        faces.add(face);
        //mapVisitedFaces.put(face, face);
        boolean finished = false;
        int counter = 0;
        while (!finished)// && counter < 10000000)
        {
            List<HalfEdgeFace> newAddedfaces = new ArrayList<>();
            int facesCount = faces.size();
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace currFace = faces.get(i);
                if (currFace.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                if (mapVisitedFaces.containsKey(currFace)) {
                    continue;
                }

                resultWeldedFaces.add(currFace);
                mapVisitedFaces.put(currFace, currFace);
                weldedFacesAux.clear();
                currFace.getWeldedFaces(weldedFacesAux, mapVisitedFaces);
                newAddedfaces.addAll(weldedFacesAux);
            }

            if (newAddedfaces.isEmpty()) {
                finished = true;
            } else {
                faces.clear();
                faces.addAll(newAddedfaces);
            }

            counter++;
        }


        return true;
    }

    public static List<HalfEdgeVertex> getVerticesOfFaces(List<HalfEdgeFace> faces, List<HalfEdgeVertex> resultVertices) {
        Map<HalfEdgeVertex, HalfEdgeVertex> MapVertices = new HashMap<>();
        if (resultVertices == null) {
            resultVertices = new ArrayList<>();
        }
        for (HalfEdgeFace face : faces) {
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdgeVertex> faceVertices = face.getVertices(null);
            for (HalfEdgeVertex vertex : faceVertices) {
                if (MapVertices.containsKey(vertex)) {
                    continue;
                }
                resultVertices.add(vertex);
                MapVertices.put(vertex, vertex);
            }
        }

        //resultVertices.addAll(MapVertices.values());
        return resultVertices;
    }

    public static List<HalfEdge> getHalfEdgesOfFaces(List<HalfEdgeFace> faces, List<HalfEdge> resultHalfEdges) {
        Map<HalfEdge, HalfEdge> MapHalfEdges = new HashMap<>();
        if (resultHalfEdges == null) {
            resultHalfEdges = new ArrayList<>();
        }
        List<HalfEdge> faceHalfEdges = new ArrayList<>();
        for (HalfEdgeFace face : faces) {
            faceHalfEdges.clear();
            faceHalfEdges = face.getHalfEdgesLoop(faceHalfEdges);
            for (HalfEdge halfEdge : faceHalfEdges) {
                if (MapHalfEdges.containsKey(halfEdge)) {
                    continue;
                }
                resultHalfEdges.add(halfEdge);
                MapHalfEdges.put(halfEdge, halfEdge);
            }
        }

        //resultHalfEdges.addAll(MapHalfEdges.values());
        return resultHalfEdges;
    }

    public static HalfEdgeFace halfEdgeFaceFromGaiaFace(GaiaFace gaiaFace, List<GaiaVertex> gaiaVertices, HalfEdgeSurface halfEdgeSurfaceOwner, Map<GaiaVertex, HalfEdgeVertex> mapGaiaVertexToHalfEdgeVertex) {
        HalfEdgeFace halfEdgeFace = new HalfEdgeFace();

        // indices
        List<HalfEdge> currHalfEdges = new ArrayList<>();
        int[] indices = gaiaFace.getIndices();
        for (int index : indices) {
            if (index >= gaiaVertices.size()) {
                log.error("[ERROR] index >= gaiaVertices.size()");
            }
            GaiaVertex gaiaVertex = gaiaVertices.get(index);
            HalfEdgeVertex halfEdgeVertex = mapGaiaVertexToHalfEdgeVertex.get(gaiaVertex);
            if (halfEdgeVertex == null) {
                halfEdgeVertex = new HalfEdgeVertex();
                halfEdgeVertex.copyFromGaiaVertex(gaiaVertex);
                mapGaiaVertexToHalfEdgeVertex.put(gaiaVertex, halfEdgeVertex);
            }

            HalfEdge halfEdge = new HalfEdge();
            halfEdge.setStartVertex(halfEdgeVertex);
            halfEdge.setFace(halfEdgeFace);
            halfEdgeFace.setHalfEdge(halfEdge);

            currHalfEdges.add(halfEdge);
            halfEdgeSurfaceOwner.getHalfEdges().add(halfEdge);
        }

        // now set nextHalfEdges
        int currHalfEdgesCount = currHalfEdges.size();
        for (int i = 0; i < currHalfEdgesCount; i++) {
            HalfEdge currHalfEdge = currHalfEdges.get(i);
            HalfEdge nextHalfEdge = currHalfEdges.get((i + 1) % currHalfEdgesCount);
            currHalfEdge.setNext(nextHalfEdge);
        }

        return halfEdgeFace;
    }

    public static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    public static Vector3d calculateNormalAsConvex(List<HalfEdgeVertex> vertices, Vector3d resultNormal) {
        if (resultNormal == null) {
            resultNormal = new Vector3d();
        }
        int verticesCount = vertices.size();
        if (verticesCount < 3) {
            log.error("[ERROR] verticesCount < 3");
            return resultNormal;
        }
        HalfEdgeVertex vertex1 = vertices.get(0);
        HalfEdgeVertex vertex2 = vertices.get(1);
        HalfEdgeVertex vertex3 = vertices.get(2);
        Vector3d pos1 = vertex1.getPosition();
        Vector3d pos2 = vertex2.getPosition();
        Vector3d pos3 = vertex3.getPosition();
        Vector3d v1 = new Vector3d();
        Vector3d v2 = new Vector3d();
        v1.set(pos2.x - pos1.x, pos2.y - pos1.y, pos2.z - pos1.z);
        v2.set(pos3.x - pos1.x, pos3.y - pos1.y, pos3.z - pos1.z);
        v1.cross(v2, resultNormal);
        resultNormal.normalize();

        // check if x, y, z is NaN
        if (Double.isNaN(resultNormal.x) || Double.isNaN(resultNormal.y) || Double.isNaN(resultNormal.z)) {
            return null;
        }

        return resultNormal;
    }

    public static double calculateArea(HalfEdgeVertex a, HalfEdgeVertex b, HalfEdgeVertex c) {
        Vector3d posA = a.getPosition();
        Vector3d posB = b.getPosition();
        Vector3d posC = c.getPosition();

        double dist1 = posA.distance(posB);
        double dist2 = posB.distance(posC);
        double dist3 = posC.distance(posA);

        double s = (dist1 + dist2 + dist3) / 2.0;

        return Math.sqrt(s * (s - dist1) * (s - dist2) * (s - dist3));
    }

    public static double calculateAspectRatioAsTriangle(HalfEdgeVertex a, HalfEdgeVertex b, HalfEdgeVertex c) {
        Vector3d posA = a.getPosition();
        Vector3d posB = b.getPosition();
        Vector3d posC = c.getPosition();

        double dist1 = posA.distance(posB);
        double dist2 = posB.distance(posC);
        double dist3 = posC.distance(posA);

        double longest = Math.max(dist1, Math.max(dist2, dist3));
        double s = (dist1 + dist2 + dist3) / 2.0;
        double area = Math.sqrt(s * (s - dist1) * (s - dist2) * (s - dist3));

        double height = 2.0 * area / longest;

        return longest / height;
    }

    private static void getWeldableVertexMap(Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster, List<GaiaVertex> vertices, double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        Map<GaiaVertex, GaiaVertex> visitedMap = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            if (visitedMap.containsKey(vertex)) {
                continue;
            }
            mapVertexToVertexMaster.put(vertex, vertex);
            for (int j = i + 1; j < verticesCount; j++) {
                GaiaVertex vertex2 = vertices.get(j);
                if (visitedMap.containsKey(vertex2)) {
                    continue;
                }
                if (vertex.isWeldable(vertex2, error, checkTexCoord, checkNormal, checkColor, checkBatchId)) {
                    mapVertexToVertexMaster.put(vertex2, vertex);

                    visitedMap.put(vertex, vertex);
                    visitedMap.put(vertex2, vertex2);
                }
            }
        }
    }

    public static void weldVerticesGaiaSurface(GaiaSurface gaiaSurface, List<GaiaVertex> gaiaVertices, double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        // Weld the vertices
        GaiaOctreeVertices octreeVertices = new GaiaOctreeVertices(null);
        octreeVertices.getVertices().addAll(gaiaVertices);
        octreeVertices.calculateSize();
        octreeVertices.setAsCube();
        octreeVertices.setMaxDepth(10);
        octreeVertices.setMinBoxSize(1.0); // 1m

        octreeVertices.makeTreeByMinVertexCount(50);

        List<GaiaOctreeVertices> octreesWithContents = new ArrayList<>();
        octreeVertices.extractOctreesWithContents(octreesWithContents);

        Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster = new HashMap<>();

        for (GaiaOctreeVertices octree : octreesWithContents) {
            List<GaiaVertex> vertices = octree.getVertices();
            getWeldableVertexMap(mapVertexToVertexMaster, vertices, error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }

        Map<GaiaVertex, GaiaVertex> mapVertexMasters = new HashMap<>();
        for (GaiaVertex vertexMaster : mapVertexToVertexMaster.values()) {
            mapVertexMasters.put(vertexMaster, vertexMaster);
        }

        List<GaiaVertex> newVerticesArray = new ArrayList<>(mapVertexMasters.values());

        Map<GaiaVertex, Integer> vertexIdxMap = new HashMap<>();
        int verticesCount = newVerticesArray.size();
        for (int i = 0; i < verticesCount; i++) {
            vertexIdxMap.put(newVerticesArray.get(i), i);
        }

        // Now, update the indices of the faces
        Map<GaiaFace, GaiaFace> mapDeleteFaces = new HashMap<>();

        int facesCount = gaiaSurface.getFaces().size();
        for (int j = 0; j < facesCount; j++) {
            GaiaFace face = gaiaSurface.getFaces().get(j);
            int[] indices = face.getIndices();
            for (int k = 0; k < indices.length; k++) {
                GaiaVertex vertex = gaiaVertices.get(indices[k]);
                GaiaVertex vertexMaster = mapVertexToVertexMaster.get(vertex);
                int index = vertexIdxMap.get(vertexMaster);
                indices[k] = index;
            }

            // check indices
            for (int k = 0; k < indices.length; k++) {
                int index = indices[k];
                for (int m = k + 1; m < indices.length; m++) {
                    if (index == indices[m]) {
                        // must remove the face
                        mapDeleteFaces.put(face, face);
                    }
                }
            }
        }

        if (!mapDeleteFaces.isEmpty()) {
            List<GaiaFace> newFaces = new ArrayList<>();
            for (int j = 0; j < facesCount; j++) {
                GaiaFace face = gaiaSurface.getFaces().get(j);
                if (!mapDeleteFaces.containsKey(face)) {
                    newFaces.add(face);
                }
            }
            gaiaSurface.setFaces(newFaces);
        }

        // delete no used vertices
        for (GaiaVertex vertex : gaiaVertices) {
            if (!mapVertexMasters.containsKey(vertex)) {
                vertex.clear();
            }
        }
        gaiaVertices.clear();
        gaiaVertices.addAll(newVerticesArray);
    }

    public List<GaiaFace> getGaiaTriangleFacesFromGaiaFace(GaiaFace gaiaFace) {
        List<GaiaFace> gaiaFaces = new ArrayList<>();
        int[] indices = gaiaFace.getIndices();
        Vector3d normal = gaiaFace.getFaceNormal();
        int indicesCount = indices.length;

        for (int i = 0; i < indicesCount - 2; i += 3) {
            if (i + 2 >= indicesCount) {
                log.error("[ERROR] i + 2 >= indicesCount");
            }
            GaiaFace gaiaTriangleFace = new GaiaFace();
            gaiaTriangleFace.setIndices(new int[]{indices[i], indices[i + 1], indices[i + 2]});
            if (normal != null) {
                gaiaTriangleFace.setFaceNormal(new Vector3d(normal));
            }
            gaiaFaces.add(gaiaTriangleFace);
        }
        return gaiaFaces;
    }

}