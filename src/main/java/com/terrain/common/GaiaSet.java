package com.terrain.common;

import com.terrain.enums.AttributeType;
import com.terrain.enums.FormatType;
import com.terrain.enums.TextureType;
import com.terrain.geometry.GaiaBoundingBox;
import com.terrain.geometry.GaiaRectangle;
import com.terrain.model.*;
import com.utils.ImageResizer;
import com.utils.ImageUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joml.Vector2d;
import org.joml.Vector3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaSet implements Serializable {
    private List<GaiaBufferDataSet> bufferDataList;
    private List<GaiaMaterial> materials;
    private GaiaAttribute attribute;

    private String projectName;
    private String filePath;
    private String folderPath;
    private String projectFolderPath;
    private String outputDir;

    public GaiaBoundingBox getBoundingBox() {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (GaiaBufferDataSet bufferDataSet : bufferDataList) {
            boundingBox.addBoundingBox(bufferDataSet.getBoundingBox());
        }
        return boundingBox;
    }

    public void translate(Vector3d translation) {
        for (GaiaBufferDataSet bufferData : this.bufferDataList) {
            GaiaBuffer positionBuffer = bufferData.getBuffers().get(AttributeType.POSITION);

            if (positionBuffer == null) {
                log.error("[ERROR] Position buffer is null");
                return;
            }

            float[] positions = positionBuffer.getFloats();
            for (int i = 0; i < positions.length; i += 3) {
                positions[i] += (float) translation.x;
                positions[i + 1] += (float) translation.y;
                positions[i + 2] += (float) translation.z;
            }
        }
    }

    public GaiaSet clone() {
        GaiaSet gaiaSet = new GaiaSet();
        gaiaSet.setBufferDataList(new ArrayList<>());
        for (GaiaBufferDataSet bufferData : this.bufferDataList) {
            gaiaSet.getBufferDataList().add(bufferData.clone());
        }
        gaiaSet.setMaterials(new ArrayList<>());
        for (GaiaMaterial material : this.materials) {
            gaiaSet.getMaterials().add(material.clone());
        }
        gaiaSet.setProjectName(this.projectName);
        gaiaSet.setFilePath(this.filePath);
        gaiaSet.setFolderPath(this.folderPath);
        gaiaSet.setProjectFolderPath(this.projectFolderPath);
        gaiaSet.setOutputDir(this.outputDir);
        return gaiaSet;
    }

    public void clear() {
        this.bufferDataList.forEach(GaiaBufferDataSet::clear);
        this.bufferDataList.clear();
        for (GaiaMaterial material : this.materials) {
            material.clear();
        }
        this.materials.clear();
    }
}
