package com.terrain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class SceneStructure implements Serializable {
    protected List<GaiaNode> nodes = new ArrayList<>();
    protected List<GaiaMaterial> materials = new ArrayList<>();
}
