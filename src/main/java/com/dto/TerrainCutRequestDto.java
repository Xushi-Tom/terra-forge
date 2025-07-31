package com.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author xushi
 * @version 1.0
 * @project map-tms
 * @description 地形文件切片请求对象
 * @date 2025/5/6 10:49:58
 */
@ApiModel(value = "TMS地形切片请求对象")
public class TerrainCutRequestDto {

    @ApiModelProperty("工作空间组")
    private String workspaceGroup;

    @ApiModelProperty("工作空间")
    private String workspace;

    private String outputDir;

    @ApiModelProperty("文件路径")
    private String filePath;

    @ApiModelProperty("范围：0 ~ 22 默认值：0")
    private Integer minZoom;

    @ApiModelProperty("范围：0 ~ 22 默认值：14")
    private Integer maxZoom;

    @ApiModelProperty("网格细化强度（范围：1.0 - 16.0）默认值：4.0")
    private Double intensity;

    @ApiModelProperty("为地形添加顶点法线以支持光照效果，默认不添加")
    private boolean calculateNormals;

    @ApiModelProperty("插值类型（nearest-最近邻, bilinear-双线性）默认值：bilinear")
    private String interpolationType;

    @ApiModelProperty("地形数据的无数据值 默认值：-9999")
    private Integer nodataValue;

    @ApiModelProperty("每个瓦片的拼接缓冲区大小 默认值：16")
    private Integer mosaicSize;

    @ApiModelProperty("最大栅格尺寸 默认值：16384")
    private Integer rasterMaxSize;

    @ApiModelProperty("是否继续生成，适合与中途中断的场景，默认不继续")
    private boolean isContinue;

    @ApiModelProperty("成功回调地址(http://[ip]:[port]/{param1}/{param1}/......)")
    private String backSuccessUrl;

    @ApiModelProperty("失败回调地址(http://[ip]:[port]/{param1}/{param1}/......)")
    private String backFailUrl;

    public String getBackSuccessUrl() {
        return backSuccessUrl;
    }

    public void setBackSuccessUrl(String backSuccessUrl) {
        this.backSuccessUrl = backSuccessUrl;
    }

    public String getBackFailUrl() {
        return backFailUrl;
    }

    public void setBackFailUrl(String backFailUrl) {
        this.backFailUrl = backFailUrl;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public Integer getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(Integer minZoom) {
        this.minZoom = minZoom;
    }

    public Integer getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(Integer maxZoom) {
        this.maxZoom = maxZoom;
    }

    public Double getIntensity() {
        return intensity;
    }

    public void setIntensity(Double intensity) {
        this.intensity = intensity;
    }

    public boolean isCalculateNormals() {
        return calculateNormals;
    }

    public void setCalculateNormals(boolean calculateNormals) {
        this.calculateNormals = calculateNormals;
    }

    public String getInterpolationType() {
        return interpolationType;
    }

    public void setInterpolationType(String interpolationType) {
        this.interpolationType = interpolationType;
    }

    public Integer getNodataValue() {
        return nodataValue;
    }

    public void setNodataValue(Integer nodataValue) {
        this.nodataValue = nodataValue;
    }

    public Integer getMosaicSize() {
        return mosaicSize;
    }

    public void setMosaicSize(Integer mosaicSize) {
        this.mosaicSize = mosaicSize;
    }

    public Integer getRasterMaxSize() {
        return rasterMaxSize;
    }

    public void setRasterMaxSize(Integer rasterMaxSize) {
        this.rasterMaxSize = rasterMaxSize;
    }

    public boolean isContinue() {
        return isContinue;
    }

    public void setContinue(boolean aContinue) {
        isContinue = aContinue;
    }

    public String getWorkspaceGroup() {
        return workspaceGroup;
    }

    public void setWorkspaceGroup(String workspaceGroup) {
        this.workspaceGroup = workspaceGroup;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
}
