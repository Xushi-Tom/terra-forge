package com.terrain.model;

import com.terrain.enums.TextureType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public abstract class MaterialStructure implements Serializable {
    protected Map<TextureType, List<GaiaTexture>> textures = new HashMap<>();
}
