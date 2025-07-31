package com.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.time.LocalDateTime;

/**
 * @author xushi
 * @version 1.0
 * @project map-tms
 * @description 地图服务解析记录实体类
 * @date 2025/5/8 11:34:14
 */
@TableName("cim_bas_map_record")
public class MapRecordPo extends Model<MapRecordPo> {

    /**
     * sid
     */
    @TableId("id")
    private String id;

    /**
     * 文件路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 文件大小
     */
    @TableField("file_size")
    private String fileSize;

    /**
     * 文件类型
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 文件名称
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 解析的最小层级
     */
    @TableField("zoom_min")
    private Integer zoomMin;

    /**
     * 解析的最大层级
     */
    @TableField("zoom_max")
    private Integer zoomMax;

    /**
     * 解析类型 1：地图 2：地形
     */
    @TableField("type")
    private Integer type;

    /**
     * 输出路径
     */
    @TableField("output_path")
    private String outputPath;

    /**
     * 空间组
     */
    @TableField("workspace_group")
    private String workspaceGroup;

    /**
     * 工作空间
     */
    @TableField("workspace")
    private String workspace;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getZoomMin() {
        return zoomMin;
    }

    public void setZoomMin(Integer zoomMin) {
        this.zoomMin = zoomMin;
    }

    public Integer getZoomMax() {
        return zoomMax;
    }

    public void setZoomMax(Integer zoomMax) {
        this.zoomMax = zoomMax;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
