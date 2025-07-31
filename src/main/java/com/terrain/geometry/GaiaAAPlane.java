package com.terrain.geometry;

import com.terrain.enums.PlaneType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@Setter
@Getter
public class GaiaAAPlane {
    // Axis Aligned Plane
    private PlaneType planeType;
    private Vector3d point;

    public GaiaAAPlane() {
        this.planeType = PlaneType.XY;
        this.point = new Vector3d();
    }
}
