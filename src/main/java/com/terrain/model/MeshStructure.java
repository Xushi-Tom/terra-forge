package com.terrain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public abstract class MeshStructure implements Serializable {
    protected List<GaiaPrimitive> primitives = new ArrayList<>();
}
