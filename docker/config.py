#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import threading
from datetime import datetime

try:
    import psutil
    psutilAvailable = True
except ImportError:
    psutilAvailable = False
    print("警告: psutil不可用，将使用默认设置")

# 全局配置
config = {
    "host": "0.0.0.0",
    "port": 8000,
    "debug": False,
    "dataSourceDir": "/app/dataSource",
    "tilesDir": "/app/tiles", 
    "logDir": "/app/log",
    "maxThreads": psutil.cpu_count() if psutilAvailable else 4,
    "defaultMemoryLimit": "8g",
    "supportedFormats": [".tif", ".tiff", ".png", ".jpg", ".jpeg", ".txt"],
    "taskCleanup": {
        "maxTasks": 100
    }
}

# 任务状态管理
taskStatus = {}
taskProcesses = {}
taskLock = threading.Lock()

def getConfig():
    """获取全局配置"""
    return config

def getTaskStatus():
    """获取任务状态"""
    return taskStatus

def getTaskProcesses():
    """获取任务进程"""
    return taskProcesses

def getTaskLock():
    """获取任务锁"""
    return taskLock 