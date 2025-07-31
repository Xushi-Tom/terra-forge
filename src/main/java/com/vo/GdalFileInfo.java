package com.vo;

/**
 * GDAL文件信息类
 * 存储从GeoTools获取的文件详细信息
 */
public class GdalFileInfo {
    
    private int width;
    private int height;
    private int bands;
    private String dataType;
    private String projection;
    private double[] bounds;
    private double pixelSizeX;
    private double pixelSizeY;
    private long fileSize;
    private long pixelCount;
    private int complexity;
    
    public GdalFileInfo() {
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public int getBands() {
        return bands;
    }
    
    public void setBands(int bands) {
        this.bands = bands;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    public String getProjection() {
        return projection;
    }
    
    public void setProjection(String projection) {
        this.projection = projection;
    }
    
    public double[] getBounds() {
        return bounds;
    }
    
    public void setBounds(double[] bounds) {
        this.bounds = bounds;
    }
    
    public double getPixelSizeX() {
        return pixelSizeX;
    }
    
    public void setPixelSizeX(double pixelSizeX) {
        this.pixelSizeX = pixelSizeX;
    }
    
    public double getPixelSizeY() {
        return pixelSizeY;
    }
    
    public void setPixelSizeY(double pixelSizeY) {
        this.pixelSizeY = pixelSizeY;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public long getPixelCount() {
        return pixelCount;
    }
    
    public void setPixelCount(long pixelCount) {
        this.pixelCount = pixelCount;
    }
    
    public int getComplexity() {
        return complexity;
    }
    
    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }
    
    @Override
    public String toString() {
        return "GdalFileInfo{" +
                "width=" + width +
                ", height=" + height +
                ", bands=" + bands +
                ", dataType='" + dataType + '\'' +
                ", projection='" + projection + '\'' +
                ", pixelSizeX=" + pixelSizeX +
                ", pixelSizeY=" + pixelSizeY +
                ", fileSize=" + fileSize +
                ", pixelCount=" + pixelCount +
                ", complexity=" + complexity +
                '}';
    }
}