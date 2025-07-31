package com.utils;

import com.terrain.model.GaiaFace;
import com.terrain.model.GaiaPrimitive;
import com.terrain.model.GaiaSurface;
import com.terrain.model.GaiaVertex;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GaiaPrimitiveUtils {

    public static void mergePrimitives(GaiaPrimitive primitiveMaster, GaiaPrimitive primitive) {
        int vertexCountMaster = primitiveMaster.getVertices().size();

        primitiveMaster.getVertices().addAll(primitive.getVertices());
        int surfacesCount = primitive.getSurfaces().size();
        for (int i = 0; i < surfacesCount; i++) {
            GaiaSurface surface = primitive.getSurfaces().get(i);
            GaiaSurface surfaceNew = new GaiaSurface();
            int facesCount = surface.getFaces().size();
            for (int j = 0; j < facesCount; j++) {
                GaiaFace face = surface.getFaces().get(j);
                GaiaFace faceNew = new GaiaFace();
                int[] indices = face.getIndices();
                int indicesCount = indices.length;
                int[] indicesNew = new int[indicesCount];
                for (int k = 0; k < indicesCount; k++) {
                    indicesNew[k] = indices[k] + vertexCountMaster;
                }
                faceNew.setIndices(indicesNew);
                surfaceNew.getFaces().add(faceNew);
            }

            primitiveMaster.getSurfaces().add(surfaceNew);
        }
    }

}
