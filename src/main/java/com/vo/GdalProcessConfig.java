package com.vo;

/**
 * GDAL处理配置类
 * 存储处理过程中的各种配置参数
 */
public class GdalProcessConfig {
    
    private int complexity;
    private int processes;
    private String memoryLimit;
    private int blockSize;
    private int estimatedDuration;
    private Integer minZoom;
    private Integer maxZoom;
    
    public GdalProcessConfig() {
    }
    
    public int getComplexity() {
        return complexity;
    }
    
    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }
    
    public int getProcesses() {
        return processes;
    }
    
    public void setProcesses(int processes) {
        this.processes = processes;
    }
    
    public String getMemoryLimit() {
        return memoryLimit;
    }
    
    public void setMemoryLimit(String memoryLimit) {
        this.memoryLimit = memoryLimit;
    }
    
    public int getBlockSize() {
        return blockSize;
    }
    
    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }
    
    public int getEstimatedDuration() {
        return estimatedDuration;
    }
    
    public void setEstimatedDuration(int estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
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
    
    @Override
    public String toString() {
        return "GdalProcessConfig{" +
                "complexity=" + complexity +
                ", processes=" + processes +
                ", memoryLimit='" + memoryLimit + '\'' +
                ", blockSize=" + blockSize +
                ", estimatedDuration=" + estimatedDuration +
                ", minZoom=" + minZoom +
                ", maxZoom=" + maxZoom +
                '}';
    }
}