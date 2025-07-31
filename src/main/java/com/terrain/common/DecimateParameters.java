package com.terrain.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecimateParameters {
    private double maxDiffAngDegrees = 15.0;
    private double hedgeMinLength = 0.5;
    private double frontierMaxDiffAngDeg = 4.0;
    private double maxAspectRatio = 6.0;
    private int maxCollapsesCount = 1000000;
    private int iterationsCount = 1;
    private double smallHedgeSize = 1.0;
    private int lod = -1;
}
