package com.vo;

/**
 * CTB进程执行结果
 * 
 * @author xushi
 * @version 1.0
 */
public class CtbProcessResult {
    private final boolean success;
    private final String errorMessage;
    private final String output;

    public CtbProcessResult(boolean success, String errorMessage, String output) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.output = output;
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
}