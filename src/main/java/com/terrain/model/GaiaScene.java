package com.terrain.model;

import com.terrain.geometry.GaiaBoundingBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * A class that represents a scene of a Gaia object.
 * The largest unit of the 3D file.
 * It contains the nodes and materials.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaScene extends SceneStructure implements Serializable {
    private Path originalPath;
    private GaiaBoundingBox gaiaBoundingBox;
    private GaiaAttribute attribute;

    public GaiaBoundingBox getBoundingBox() {
        this.gaiaBoundingBox = new GaiaBoundingBox();
        for (GaiaNode node : this.getNodes()) {
            GaiaBoundingBox boundingBox = node.getBoundingBox(null);
            if (boundingBox != null) {
                gaiaBoundingBox.addBoundingBox(boundingBox);
            }
        }
        return this.gaiaBoundingBox;
    }

    public void clear() {
        this.nodes.forEach(GaiaNode::clear);
        this.materials.forEach(GaiaMaterial::clear);
        this.originalPath = null;
        this.gaiaBoundingBox = null;
        this.nodes.clear();

        for (GaiaMaterial material : this.materials) {
            material.clear();
        }
        this.materials.clear();
    }

    public GaiaScene clone() {
        GaiaScene clone = new GaiaScene();
        for (GaiaNode node : this.nodes) {
            clone.getNodes().add(node.clone());
        }
        for (GaiaMaterial material : this.materials) {
            clone.getMaterials().add(material.clone());
        }
        clone.setOriginalPath(this.originalPath);
        clone.setGaiaBoundingBox(this.gaiaBoundingBox);

        // attribute is a reference type.
        GaiaAttribute attribute = this.attribute.getCopy();
        clone.setAttribute(attribute);
        return clone;
    }

    public int getFacesCount() {
        int facesCount = 0;
        for (GaiaNode node : this.nodes) {
            facesCount += node.getFacesCount();
        }
        return facesCount;
    }
}
