package com.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程执行结果
 * 
 * @author xushi
 * @version 1.0
 */
public class ProcessResult {
    private final boolean success;
    private final String errorMessage;
    private final String output;
    private boolean timedOut;
    private int exitCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;
    private int progressPercent;
    private int estimatedRemainingSeconds;
    private int currentZoomLevel;
    private String currentStage;
    private List<String> warnings;
    private List<String> detailedLog;
    
    public ProcessResult(boolean success, String errorMessage, String output) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.output = output;
        this.warnings = new ArrayList<>();
        this.detailedLog = new ArrayList<>();
        this.progressPercent = 0;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() { 
        return errorMessage; 
    }
    
    public String getOutput() { 
        return output; 
    }
    
    public boolean isTimedOut() {
        return timedOut;
    }
    
    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }
    
    public int getExitCode() {
        return exitCode;
    }
    
    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
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
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public int getProgressPercent() {
        return progressPercent;
    }
    
    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }
    
    public int getEstimatedRemainingSeconds() {
        return estimatedRemainingSeconds;
    }
    
    public void setEstimatedRemainingSeconds(int estimatedRemainingSeconds) {
        this.estimatedRemainingSeconds = estimatedRemainingSeconds;
    }
    
    public int getCurrentZoomLevel() {
        return currentZoomLevel;
    }
    
    public void setCurrentZoomLevel(int currentZoomLevel) {
        this.currentZoomLevel = currentZoomLevel;
    }
    
    public String getCurrentStage() {
        return currentStage;
    }
    
    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
    
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
    
    public List<String> getDetailedLog() {
        return detailedLog;
    }
    
    public void setDetailedLog(List<String> detailedLog) {
        this.detailedLog = detailedLog;
    }
    
    public void addDetailedLog(String logEntry) {
        this.detailedLog.add(logEntry);
    }
    
    @Override
    public String toString() {
        return "ProcessResult{" +
                "success=" + success +
                ", timedOut=" + timedOut +
                ", exitCode=" + exitCode +
                ", progressPercent=" + progressPercent +
                ", currentZoomLevel=" + currentZoomLevel +
                ", currentStage='" + currentStage + '\'' +
                ", durationMs=" + durationMs +
                '}';
    }
}