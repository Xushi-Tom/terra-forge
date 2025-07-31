package com.terrain.common;

import com.terrain.enums.PlaneType;
import com.terrain.enums.TextureType;
import com.terrain.geometry.GaiaBoundingBox;
import com.terrain.model.GaiaAttribute;
import com.terrain.model.GaiaMaterial;
import com.terrain.model.GaiaTexture;
import com.utils.ImageUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
public class HalfEdgeScene implements Serializable {
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox;
    private GaiaAttribute attribute;
    private List<HalfEdgeNode> nodes = new ArrayList<>();
    private List<GaiaMaterial> materials = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;

    public GaiaBoundingBox getGaiaBoundingBox() {
        if (gaiaBoundingBox == null) {
            gaiaBoundingBox = calculateBoundingBox(null);
        }
        return gaiaBoundingBox;
    }

    public void deleteObjects() {
        for (HalfEdgeNode node : nodes) {
            node.deleteObjects();
        }
        nodes.clear();

        for (GaiaMaterial material : this.materials) {
            material.clear();
        }
        materials.clear();
    }

    public void deleteFacesWithClassifyId(int classifyId) {
        for (HalfEdgeNode node : nodes) {
            node.deleteFacesWithClassifyId(classifyId);
        }
    }

    public List<HalfEdgeSurface> extractSurfaces(List<HalfEdgeSurface> resultHalfEdgeSurfaces) {
        if (resultHalfEdgeSurfaces == null) {
            resultHalfEdgeSurfaces = new ArrayList<>();
        }
        for (HalfEdgeNode node : nodes) {
            resultHalfEdgeSurfaces = node.extractSurfaces(resultHalfEdgeSurfaces);
        }
        return resultHalfEdgeSurfaces;
    }

    public void removeDeletedObjects() {
        for (HalfEdgeNode node : nodes) {
            node.removeDeletedObjects();
        }
    }

    public boolean cutByPlane(PlaneType planeType, Vector3d planePosition, double error) {
        // 1rst check if the plane intersects the bbox
        GaiaBoundingBox bbox = getBoundingBox();

        if (bbox == null) {
            return false;
        }

        if (planeType == PlaneType.XZ) {
            if (planePosition.y < bbox.getMinY() || planePosition.y > bbox.getMaxY()) {
                return false;
            }
        } else if (planeType == PlaneType.YZ) {
            if (planePosition.x < bbox.getMinX() || planePosition.x > bbox.getMaxX()) {
                return false;
            }
        } else if (planeType == PlaneType.XY) {
            if (planePosition.z < bbox.getMinZ() || planePosition.z > bbox.getMaxZ()) {
                return false;
            }
        }

        for (HalfEdgeNode node : nodes) {
            node.cutByPlane(planeType, planePosition, error);
        }

        removeDeletedObjects();

        return true;
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
        if (resultBBox == null) {
            resultBBox = new GaiaBoundingBox();
        }
        for (HalfEdgeNode node : nodes) {
            resultBBox = node.calculateBoundingBox(resultBBox);
        }
        return resultBBox;
    }

    public void translate(Vector3d translation) {
        for (HalfEdgeNode node : nodes) {
            node.translate(translation);
        }
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = calculateBoundingBox(null);
        }
        return boundingBox;
    }

    private void copyTextures(GaiaMaterial material, Path copyDirectory) throws IOException {
        Map<TextureType, List<GaiaTexture>> materialTextures = material.getTextures();
        List<GaiaTexture> diffuseTextures = materialTextures.get(TextureType.DIFFUSE);
        if (diffuseTextures != null && !diffuseTextures.isEmpty()) {
            GaiaTexture texture = materialTextures.get(TextureType.DIFFUSE).get(0);
            String parentPath = texture.getParentPath();
            File parentFile = new File(parentPath);
            String diffusePath = texture.getPath();
            File diffuseFile = new File(diffusePath);

            File imageFile = ImageUtils.correctPath(parentFile, diffuseFile);

            Path imagesFolderPath = copyDirectory.resolve("images");
            if (imagesFolderPath.toFile().mkdirs()) {
                log.debug("Images Directory created: {}", imagesFolderPath);
            }

            Path outputImagePath = imagesFolderPath.resolve(imageFile.getName());
            File outputImageFile = outputImagePath.toFile();

            texture.setPath(imageFile.getName());

            if (!imageFile.exists()) {
                log.error("[ERROR] Texture Input Image Path is not exists. {}", diffusePath);
            } else {
                FileUtils.copyFile(imageFile, outputImageFile);
            }
        }
    }

    public HalfEdgeScene clone() {
        HalfEdgeScene clonedScene = new HalfEdgeScene();
        clonedScene.originalPath = originalPath;
        clonedScene.gaiaBoundingBox = gaiaBoundingBox;
        clonedScene.attribute = attribute;
        for (HalfEdgeNode node : nodes) {
            clonedScene.nodes.add(node.clone());
        }
        for (GaiaMaterial material : materials) {
            clonedScene.materials.add(material.clone());
        }
        return clonedScene;
    }

    public HalfEdgeScene cloneByClassifyId(int classifyId) {
        HalfEdgeScene clonedScene = null;

        for (HalfEdgeNode node : nodes) {
            HalfEdgeNode clonedNode = node.cloneByClassifyId(classifyId);
            if (clonedNode != null) {
                if (clonedScene == null) {
                    clonedScene = new HalfEdgeScene();
                    clonedScene.originalPath = originalPath;
                    clonedScene.gaiaBoundingBox = gaiaBoundingBox;
                    clonedScene.attribute = attribute;
                }
                clonedScene.nodes.add(clonedNode);
            }
        }
        if (clonedScene != null) {
            for (GaiaMaterial material : materials) {
                clonedScene.materials.add(material.clone());
            }
        }

        return clonedScene;
    }

    public void scissorTexturesByMotherScene(List<GaiaMaterial> motherMaterials) {
        boolean hasTextures = false;
        for (GaiaMaterial material : materials) {
            if (material.hasTextures()) {
                hasTextures = true;
                break;
            }
        }

        if (!hasTextures) {
            return;
        }

        for (HalfEdgeNode node : nodes) {
            node.scissorTexturesByMotherScene(materials, motherMaterials);
        }
    }

    public int getTrianglesCount() {
        int trianglesCount = 0;
        for (HalfEdgeNode node : nodes) {
            trianglesCount += node.getTrianglesCount();
        }
        return trianglesCount;
    }

    public void setBoxTexCoordsXY(GaiaBoundingBox box) {
        for (HalfEdgeNode node : nodes) {
            node.setBoxTexCoordsXY(box);
        }
    }

    public List<Integer> getUsedMaterialsIds(List<Integer> resultMaterialsIds) {
        if (resultMaterialsIds == null) {
            resultMaterialsIds = new ArrayList<>();
        }
        for (HalfEdgeNode node : nodes) {
            node.getUsedMaterialsIds(resultMaterialsIds);
        }
        return resultMaterialsIds;
    }

    public void calculateNormals() {
        for (HalfEdgeNode node : nodes) {
            node.calculateNormals();
        }
    }

    public void makeSkirt() {
        GaiaBoundingBox bbox = getBoundingBox();
        if (bbox == null) {
            log.info("Making skirt : Error: bbox is null");
            return;
        }

        double error = 1e-3; // 0.001
        List<HalfEdgeVertex> westVertices = new ArrayList<>();
        List<HalfEdgeVertex> eastVertices = new ArrayList<>();
        List<HalfEdgeVertex> southVertices = new ArrayList<>();
        List<HalfEdgeVertex> northVertices = new ArrayList<>();
        for (HalfEdgeNode node : nodes) {
            node.getWestEastSouthNorthVertices(bbox, westVertices, eastVertices, southVertices, northVertices, error);
        }

        double bboxLengthX = bbox.getLengthX();
        double bboxLengthY = bbox.getLengthY();
        double bboxMaxSize = Math.max(bboxLengthX, bboxLengthY);
        double expandDistance = bboxMaxSize * 0.005;
        // provisionally, only expand the perimeter vertices
        double dotLimit = 0.35;
        if (westVertices.size() > 1) {
            for (HalfEdgeVertex vertex : westVertices) {
                Vector3d normal = vertex.calculateNormal();
                if (Math.abs(normal.dot(-1, 0, 0)) < dotLimit) {
                    Vector3d position = vertex.getPosition();
                    position.x -= expandDistance;
                }
            }
        }

        if (eastVertices.size() > 1) {
            for (HalfEdgeVertex vertex : eastVertices) {
                Vector3d normal = vertex.calculateNormal();
                if (Math.abs(normal.dot(1, 0, 0)) < dotLimit) {
                    Vector3d position = vertex.getPosition();
                    position.x += expandDistance;
                }
            }
        }

        if (southVertices.size() > 1) {
            for (HalfEdgeVertex vertex : southVertices) {
                Vector3d normal = vertex.calculateNormal();
                if (Math.abs(normal.dot(0, -1, 0)) < dotLimit) {
                    Vector3d position = vertex.getPosition();
                    position.y -= expandDistance;
                }
            }
        }

        if (northVertices.size() > 1) {
            for (HalfEdgeVertex vertex : northVertices) {
                Vector3d normal = vertex.calculateNormal();
                if (Math.abs(normal.dot(0, 1, 0)) < dotLimit) {
                    Vector3d position = vertex.getPosition();
                    position.y += expandDistance;
                }
            }
        }
    }

    public int getFacesCount() {
        int facesCount = 0;
        for (HalfEdgeNode node : nodes) {
            facesCount += node.getFacesCount();
        }
        return facesCount;
    }
}
