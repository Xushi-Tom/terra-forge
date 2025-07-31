package com.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author xushi
 * @version 1.0
 * @project map-tms
 * @description tms切片请求对象
 * @date 2025/4/21 04:03:02
 */
@ApiModel(value = "TMS地图切片请求对象")
public class MapCutRequestDto {

    @ApiModelProperty("工作空间组")
    private String workspaceGroup;

    @ApiModelProperty("工作空间")
    private String workspace;

    @ApiModelProperty("文件类型 1；电子 2：遥感")
    private String type;

    @ApiModelProperty("文件路径")
    private String tifDir;

    @ApiModelProperty("最小层级")
    private Integer minZoom;

    @ApiModelProperty("最大层级")
    private Integer maxZoom;

    @ApiModelProperty("成功回调地址(http://[ip]:[port]/{param1}/{param1}/......)")
    private String backSuccessUrl;

    @ApiModelProperty("失败回调地址(http://[ip]:[port]/{param1}/{param1}/......)")
    private String backFailUrl;

    public String getWorkspaceGroup() {
        return workspaceGroup;
    }

    public void setWorkspaceGroup(String workspaceGroup) {
        this.workspaceGroup = workspaceGroup;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getTifDir() {
        return tifDir;
    }

    public void setTifDir(String tifDir) {
        this.tifDir = tifDir;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

}
