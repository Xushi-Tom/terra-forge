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
 * @description 工作空间实体类
 * @date 2025/5/8 11:34:14
 */
@TableName("cim_bas_workspace")
public class WorkspacePo extends Model<WorkspacePo> {

    /**
     * sid
     */
    @TableId("id")
    private String id;

    /**
     * 父级 id
     */
    @TableField("parent_id")
    private String parentId;

    /**
     * 工作空间/组名称
     */
    @TableField("name")
    private String name;

    /**
     * 类型 1：空间组 2：空间
     */
    @TableField("type")
    private Integer type;

    /**
     * 状态 0：停用 1：启动
     */
    @TableField("status")
    private Integer status;

    /**
     * 工作空间/组完整路径
     */
    @TableField("path")
    private String path;

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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "WorkspacePo{" +
                "id='" + id + '\'' +
                ", parentId='" + parentId + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", path='" + path + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
