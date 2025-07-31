package com.terrain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector3d;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class FaceStructure implements Serializable {
    protected int[] indices;
    protected Vector3d faceNormal = new Vector3d();
}
