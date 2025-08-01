package com.terrain.geometry;

import org.joml.Vector3d;

public class GaiaPlane {
    private final double a;
    private final double b;
    private final double c;
    private final double d;

    public GaiaPlane() {
        this.a = 0;
        this.b = 0;
        this.c = 0;
        this.d = 0;
    }

    public GaiaPlane(Vector3d position, Vector3d normal) {
        this.a = normal.x;
        this.b = normal.y;
        this.c = normal.z;
        this.d = -position.dot(normal);
    }

    public double distanceToPoint(Vector3d point) {
        return a * point.x + b * point.y + c * point.z + d;
    }
}
