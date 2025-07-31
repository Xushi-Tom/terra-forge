package com.terrain.geometry;

import com.terrain.model.GaiaFace;
import com.terrain.model.GaiaPrimitive;
import com.terrain.model.GaiaScene;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector4d;

@Slf4j
@Getter
@Setter
public class GaiaFaceData {

    private GaiaScene sceneParent = null;
    private GaiaPrimitive primitiveParent = null;
    private GaiaFace face = null;
    private GaiaBoundingBox boundingBox = null;
    private Vector3d centerPoint = null;
    private Vector4d averageColor = null; // Average color of the face

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            if (primitiveParent != null) {
                boundingBox = face.getBoundingBox(primitiveParent.getVertices(), new GaiaBoundingBox());
            } else {
                log.error("[ERROR][getBoundingBox] : primitiveParent is null.");
            }
        }
        return boundingBox;
    }

    public Vector3d getCenterPoint() {
        if (centerPoint == null) {
            if (boundingBox == null) {
                getBoundingBox();
            }
            centerPoint = boundingBox.getCenter();
        }
        return centerPoint;
    }
}
