package com.config;

/**
 * CTB (Cesium Terrain Builder) 配置类
 * 专门用于优化CTB工具的参数配置
 * 
 * @author xushi
 * @version 1.0
 */
public class CtbConfig {
    
    /**
     * 内存限制（MB）
     */
    private int memoryLimitMB = 4096;
    
    /**
     * 线程数
     */
    private int threadCount = 4;
    
    /**
     * 误差阈值
     */
    private double errorThreshold = 1.0;
    
    /**
     * 最大三角形数量
     */
    private int maxTriangles = 32768;
    
    /**
     * 瓦片尺寸
     */
    private int tileSize = 256;
    
    /**
     * 缓冲区大小
     */
    private int bufferSize = 32;
    
    /**
     * 是否启用压缩
     */
    private boolean compressionEnabled = true;
    
    /**
     * 是否生成法线
     */
    private boolean generateNormals = true;
    
    /**
     * 输出格式
     */
    private String outputFormat = "Mesh";
    
    /**
     * 坐标系配置
     */
    private String profile = "geodetic";
    
    /**
     * 图层名称
     */
    private String layerName = "terrain";
    
    /**
     * 质量级别 (1-5, 5为最高质量)
     */
    private int qualityLevel = 3;
    
    /**
     * 根据系统资源和文件复杂度创建优化配置
     */
    public static CtbConfig createOptimizedConfig(long totalMemoryGB, int availableCpus, int fileComplexity) {
        CtbConfig config = new CtbConfig();
        
        // 内存配置
        if (totalMemoryGB >= 128) {
            config.memoryLimitMB = 65536; // 64GB
        } else if (totalMemoryGB >= 64) {
            config.memoryLimitMB = 32768; // 32GB
        } else if (totalMemoryGB >= 32) {
            config.memoryLimitMB = 16384; // 16GB
        } else if (totalMemoryGB >= 16) {
            config.memoryLimitMB = 8192;  // 8GB
        } else {
            config.memoryLimitMB = 4096;  // 4GB
        }
        
        // 线程配置
        if (availableCpus >= 64) {
            config.threadCount = Math.min(32, availableCpus / 2);
        } else if (availableCpus >= 32) {
            config.threadCount = Math.min(16, availableCpus / 2);
        } else if (availableCpus >= 16) {
            config.threadCount = Math.min(8, availableCpus / 2);
        } else {
            config.threadCount = Math.max(1, availableCpus / 2);
        }
        
        // 根据文件复杂度调整质量参数
        switch (fileComplexity) {
            case 4: // 超复杂文件
                config.qualityLevel = 5;
                config.errorThreshold = 0.5;
                config.maxTriangles = 65536;
                config.tileSize = 512;
                config.bufferSize = 64;
                // 减少线程数避免内存竞争
                config.threadCount = Math.max(1, config.threadCount / 2);
                break;
            case 3: // 复杂文件
                config.qualityLevel = 4;
                config.errorThreshold = 1.0;
                config.maxTriangles = 32768;
                config.tileSize = 256;
                config.bufferSize = 32;
                break;
            case 2: // 中等文件
                config.qualityLevel = 3;
                config.errorThreshold = 1.5;
                config.maxTriangles = 16384;
                config.tileSize = 256;
                config.bufferSize = 32;
                break;
            case 1: // 简单文件
                config.qualityLevel = 2;
                config.errorThreshold = 2.0;
                config.maxTriangles = 8192;
                config.tileSize = 256;
                config.bufferSize = 16;
                // 可以增加线程数
                config.threadCount = Math.min(availableCpus, (int)(config.threadCount * 1.5));
                break;
            default:
                // 保持默认值
                break;
        }
        
        return config;
    }
    
    /**
     * 转换为CTB命令行参数
     */
    public String[] toCommandLineArgs() {
        return new String[] {
            "-f", outputFormat,
            compressionEnabled ? "-C" : "",
            generateNormals ? "-N" : "",
            "-m", String.valueOf(memoryLimitMB),
            "-t", String.valueOf(threadCount),
            "--error-threshold", String.valueOf(errorThreshold),
            "--max-triangles", String.valueOf(maxTriangles),
            "--tile-size", String.valueOf(tileSize),
            "--buffer-size", String.valueOf(bufferSize),
            "--profile", profile,
            "--layer", layerName
        };
    }
    
    // Getters and Setters
    public int getMemoryLimitMB() { return memoryLimitMB; }
    public void setMemoryLimitMB(int memoryLimitMB) { this.memoryLimitMB = memoryLimitMB; }
    
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    
    public double getErrorThreshold() { return errorThreshold; }
    public void setErrorThreshold(double errorThreshold) { this.errorThreshold = errorThreshold; }
    
    public int getMaxTriangles() { return maxTriangles; }
    public void setMaxTriangles(int maxTriangles) { this.maxTriangles = maxTriangles; }
    
    public int getTileSize() { return tileSize; }
    public void setTileSize(int tileSize) { this.tileSize = tileSize; }
    
    public int getBufferSize() { return bufferSize; }
    public void setBufferSize(int bufferSize) { this.bufferSize = bufferSize; }
    
    public boolean isCompressionEnabled() { return compressionEnabled; }
    public void setCompressionEnabled(boolean compressionEnabled) { this.compressionEnabled = compressionEnabled; }
    
    public boolean isGenerateNormals() { return generateNormals; }
    public void setGenerateNormals(boolean generateNormals) { this.generateNormals = generateNormals; }
    
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
    
    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }
    
    public String getLayerName() { return layerName; }
    public void setLayerName(String layerName) { this.layerName = layerName; }
    
    public int getQualityLevel() { return qualityLevel; }
    public void setQualityLevel(int qualityLevel) { this.qualityLevel = qualityLevel; }
    
    @Override
    public String toString() {
        return String.format("CtbConfig{memory=%dMB, threads=%d, quality=%d, errorThreshold=%.2f, maxTriangles=%d}", 
            memoryLimitMB, threadCount, qualityLevel, errorThreshold, maxTriangles);
    }
}
