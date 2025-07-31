package com.terrain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class PrimitiveStructure implements Serializable {
    protected List<GaiaVertex> vertices = new ArrayList<>();
    protected List<GaiaSurface> surfaces = new ArrayList<>();
}
