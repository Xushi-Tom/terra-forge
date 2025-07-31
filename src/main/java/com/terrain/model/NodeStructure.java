package com.terrain.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class NodeStructure implements Serializable {
    protected GaiaNode parent = null;
    protected List<GaiaMesh> meshes = new ArrayList<>();
    protected List<GaiaNode> children = new ArrayList<>();
}
