package com.terrain.common;

import com.terrain.enums.PlaneType;
import com.terrain.geometry.GaiaAAPlane;
import com.terrain.geometry.GaiaBoundingBox;
import com.terrain.geometry.HalfEdgeOctree;
import com.terrain.model.GaiaAttribute;
import com.terrain.model.GaiaMaterial;
import com.utils.HalfEdgeUtils;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HalfEdgeCutter {
    public static void getPlanesGridXYZForBox(GaiaBoundingBox bbox, double gridSpacing, List<GaiaAAPlane> resultPlanesYZ, List<GaiaAAPlane> resultPlanesXZ, List<GaiaAAPlane> resultPlanesXY, HalfEdgeOctree resultOctree) {
        // Note : the grid is regularly spaced in the 3 axis
        double maxSize = bbox.getMaxSize();
        int desiredDepth = (int) Math.ceil(HalfEdgeUtils.log2(maxSize / gridSpacing));
        double desiredDistanceRoot = gridSpacing * Math.pow(2, desiredDepth);

        GaiaBoundingBox cubeBBox = bbox.clone();
        cubeBBox.setMaxX(cubeBBox.getMinX() + desiredDistanceRoot);
        cubeBBox.setMaxY(cubeBBox.getMinY() + desiredDistanceRoot);
        cubeBBox.setMaxZ(cubeBBox.getMinZ() + desiredDistanceRoot);

        resultOctree.setSize(cubeBBox.getMinX(), cubeBBox.getMinY(), cubeBBox.getMinZ(), cubeBBox.getMaxX(), cubeBBox.getMaxY(), cubeBBox.getMaxZ());
        resultOctree.setMaxDepth(desiredDepth);


        // create GaiaAAPlanes
        int leafOctreesCountForAxis = (int) Math.pow(2, desiredDepth);
        for (int i = 1; i < leafOctreesCountForAxis; i++) // 'i' starts in 1 because the first plane is the bbox min
        {
            // planes_YZ
            GaiaAAPlane planeYZ = new GaiaAAPlane();
            planeYZ.setPlaneType(PlaneType.YZ);
            Vector3d point = new Vector3d();
            point.x = bbox.getMinX() + i * gridSpacing;
            point.y = bbox.getMinY();
            point.z = bbox.getMinZ();
            planeYZ.setPoint(point);
            resultPlanesYZ.add(planeYZ);

            // planes_XZ
            GaiaAAPlane planeXZ = new GaiaAAPlane();
            planeXZ.setPlaneType(PlaneType.XZ);
            point = new Vector3d();
            point.x = bbox.getMinX();
            point.y = bbox.getMinY() + i * gridSpacing;
            point.z = bbox.getMinZ();
            planeXZ.setPoint(point);
            resultPlanesXZ.add(planeXZ);

            // planes_XY
            GaiaAAPlane planeXY = new GaiaAAPlane();
            planeXY.setPlaneType(PlaneType.XY);
            point = new Vector3d();
            point.x = bbox.getMinX();
            point.y = bbox.getMinY();
            point.z = bbox.getMinZ() + i * gridSpacing;
            planeXY.setPoint(point);
            resultPlanesXY.add(planeXY);
        }
    }

    public static List<HalfEdgeScene> cutHalfEdgeSceneByGaiaAAPlanes(HalfEdgeScene halfEdgeScene, List<GaiaAAPlane> planes, HalfEdgeOctree resultOctree, boolean scissorTextures, boolean makeSkirt) {
        double error = 1e-5; //
        int planesCount = planes.size();
        for (GaiaAAPlane plane : planes) {
            halfEdgeScene.cutByPlane(plane.getPlaneType(), plane.getPoint(), error);
        }

        // now, distribute faces into octree
        resultOctree.getFaces().clear();
        List<HalfEdgeSurface> surfaces = halfEdgeScene.extractSurfaces(null);
        for (HalfEdgeSurface surface : surfaces) {
            List<HalfEdgeFace> faces = surface.getFaces();
            for (HalfEdgeFace face : faces) {
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }
                resultOctree.getFaces().add(face);
            }
        }

        resultOctree.distributeFacesToTargetDepth(resultOctree.getMaxDepth());
        List<HalfEdgeOctree> octreesWithContents = new ArrayList<>();
        resultOctree.extractOctreesWithFaces(octreesWithContents);

        // now, separate the surface by the octrees
        List<HalfEdgeScene> resultScenes = new ArrayList<>();

        // set the classifyId for each face
        int octreesCount = octreesWithContents.size();
//        for (int j = 0; j < octreesCount; j++) {
//            HalfEdgeOctree octree = octreesWithContents.get(j);
//            List<HalfEdgeFace> faces = octree.getFaces();
//            for (HalfEdgeFace face : faces) {
//                face.setClassifyId(j);
//            }
//        }

        for (int j = 0; j < octreesCount; j++) {
            HalfEdgeOctree octree = octreesWithContents.get(j);
            List<HalfEdgeFace> faces = octree.getFaces();
            for (HalfEdgeFace face : faces) {
                face.setClassifyId(j);
            }
            // create a new HalfEdgeScene
            HalfEdgeScene cuttedScene = halfEdgeScene.cloneByClassifyId(j);

            if (cuttedScene == null) {
                log.info("cuttedScene is null");
                continue;
            }

            if (scissorTextures) {
                cuttedScene.scissorTexturesByMotherScene(halfEdgeScene.getMaterials());
            }

            if (makeSkirt) {
                cuttedScene.makeSkirt();
            }


            resultScenes.add(cuttedScene);
        }
        return resultScenes;
    }

    public static HalfEdgeSurface createHalfEdgeSurfaceByFacesCopy(List<HalfEdgeFace> faces, boolean checkClassifyId, boolean checkBestCameraDirectionType) {
        Map<HalfEdgeVertex, HalfEdgeVertex> vertexToNewVertexMap = new HashMap<>();

        List<HalfEdgeVertex> facesVertices = HalfEdgeUtils.getVerticesOfFaces(faces, null);

        // copy vertices
        for (HalfEdgeVertex vertex : facesVertices) {
            HalfEdgeVertex copyVertex = new HalfEdgeVertex();
            copyVertex.copyFrom(vertex);
            vertexToNewVertexMap.put(vertex, copyVertex);
        }

        List<HalfEdge> newHalfEdges = new ArrayList<>();
        List<HalfEdgeFace> newFaces = new ArrayList<>();

        // copy faces
        for (HalfEdgeFace face : faces) {
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeFace copyFace = new HalfEdgeFace();
            copyFace.copyFrom(face);

            List<HalfEdgeVertex> faceVertices = face.getVertices(null);

            HalfEdgeVertex hVertex0 = faceVertices.get(0);
            HalfEdgeVertex hVertex1 = faceVertices.get(1);
            HalfEdgeVertex hVertex2 = faceVertices.get(2);

            HalfEdgeVertex copyVertex0 = vertexToNewVertexMap.get(hVertex0);
            HalfEdgeVertex copyVertex1 = vertexToNewVertexMap.get(hVertex1);
            HalfEdgeVertex copyVertex2 = vertexToNewVertexMap.get(hVertex2);

            HalfEdge copyEdge0 = new HalfEdge();
            copyEdge0.setStartVertex(copyVertex0);
            copyVertex0.setOutingHalfEdge(copyEdge0);
            copyEdge0.setFace(copyFace);

            HalfEdge copyEdge1 = new HalfEdge();
            copyEdge1.setStartVertex(copyVertex1);
            copyVertex1.setOutingHalfEdge(copyEdge1);
            copyEdge1.setFace(copyFace);

            HalfEdge copyEdge2 = new HalfEdge();
            copyEdge2.setStartVertex(copyVertex2);
            copyVertex2.setOutingHalfEdge(copyEdge2);
            copyEdge2.setFace(copyFace);

            copyEdge0.setNext(copyEdge1);
            copyEdge1.setNext(copyEdge2);
            copyEdge2.setNext(copyEdge0);

            copyFace.setHalfEdge(copyEdge0);

            newHalfEdges.add(copyEdge0);
            newHalfEdges.add(copyEdge1);
            newHalfEdges.add(copyEdge2);

            newFaces.add(copyFace);
        }

        List<HalfEdgeVertex> newVertices = new ArrayList<>(vertexToNewVertexMap.values());

        HalfEdgeSurface newSurface = new HalfEdgeSurface();
        newSurface.setVertices(newVertices);
        newSurface.setFaces(newFaces);
        newSurface.setHalfEdges(newHalfEdges);

        newSurface.setTwins();

        return newSurface;
    }

}
