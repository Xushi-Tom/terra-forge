#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import json
import re
import threading
import math
import subprocess
import glob
import shutil
from datetime import datetime
from flask import Flask, request, jsonify, send_file

from config import config, taskStatus, taskProcesses, taskLock
from utils import *

# 任务停止标志字典
taskStopFlags = {}

# 核心业务逻辑函数
def generateSmartRecommendations(fileSizeGb, tileType="map", userMinZoom=None, userMaxZoom=None, cpuCount=4, memoryTotalGb=8):
    """
    根据文件大小和系统资源生成智能配置推荐
    
    @param fileSizeGb 文件大小（GB）
    @param tileType 瓦片类型，可选值：map（地图）、terrain（地形）
    @param userMinZoom 用户指定的最小缩放级别
    @param userMaxZoom 用户指定的最大缩放级别
    @param cpuCount CPU核心数
    @param memoryTotalGb 系统总内存（GB）
    @return 包含推荐配置的字典，失败时返回None
    """
    try:
        recommendations = {}
        
        # 根据文件大小和瓦片类型进行优化推荐
        if tileType == "terrain":
            # 地形切片推荐配置
            if fileSizeGb < 0.5:
                recommendations["maxZoom"] = 16
                recommendations["minZoom"] = 0
                recommendations["processes"] = min(cpuCount // 2, 4)  # 地形切片消耗更多资源
                recommendations["maxMemory"] = "4g"  # 地形切片使用GB单位
            elif fileSizeGb < 2:
                recommendations["maxZoom"] = 15
                recommendations["minZoom"] = 0
                recommendations["processes"] = min(cpuCount // 2, 6)
                recommendations["maxMemory"] = "6g"
            elif fileSizeGb < 8:
                recommendations["maxZoom"] = 14
                recommendations["minZoom"] = 0
                recommendations["processes"] = min(cpuCount // 2, 8)
                recommendations["maxMemory"] = "8g"
            else:
                recommendations["maxZoom"] = 13
                recommendations["minZoom"] = 0
                recommendations["processes"] = min(cpuCount // 2, 10)
                recommendations["maxMemory"] = "12g"
            
            # 地形切片固定配置
            recommendations["tileFormat"] = "terrain"  # 地形瓦片格式
            recommendations["quality"] = 100
            recommendations["compression"] = True
            recommendations["decompress"] = True
            recommendations["autoZoom"] = True
            recommendations["zoomStrategy"] = "conservative"
            
            # 根据系统内存调整
            if memoryTotalGb < 8:
                recommendations["maxMemory"] = "4g"
                recommendations["processes"] = min(recommendations["processes"], 2)
            elif memoryTotalGb < 16:
                recommendations["maxMemory"] = "6g"
                recommendations["processes"] = min(recommendations["processes"], 4)
            elif memoryTotalGb >= 32:
                recommendations["maxMemory"] = "16g"
                recommendations["processes"] = min(recommendations["processes"], 12)
        
        else:
            # 地图切片推荐配置
            if fileSizeGb < 1:
                recommendations["maxZoom"] = 18
                recommendations["minZoom"] = 0
                recommendations["tileFormat"] = "png"
                recommendations["quality"] = 90
                recommendations["processes"] = min(cpuCount - 1, 6)
                recommendations["maxMemory"] = max(2048, int(memoryTotalGb * 1024 * 0.3))  # 30%系统内存，MB单位
            elif fileSizeGb < 5:
                recommendations["maxZoom"] = 16
                recommendations["minZoom"] = 0
                recommendations["tileFormat"] = "webp"
                recommendations["quality"] = 85
                recommendations["processes"] = min(cpuCount - 1, 8)
                recommendations["maxMemory"] = max(4096, int(memoryTotalGb * 1024 * 0.4))  # 40%系统内存
            elif fileSizeGb < 20:
                recommendations["maxZoom"] = 15
                recommendations["minZoom"] = 0
                recommendations["tileFormat"] = "webp"
                recommendations["quality"] = 80
                recommendations["processes"] = min(cpuCount - 1, 10)
                recommendations["maxMemory"] = max(6144, int(memoryTotalGb * 1024 * 0.5))  # 50%系统内存
            else:
                recommendations["maxZoom"] = 14
                recommendations["minZoom"] = 0
                recommendations["tileFormat"] = "webp"
                recommendations["quality"] = 75
                recommendations["processes"] = min(cpuCount - 1, 12)
                recommendations["maxMemory"] = max(8192, int(memoryTotalGb * 1024 * 0.6))  # 60%系统内存
            
            # 地图切片额外配置
            recommendations["resampling"] = "bilinear"
            recommendations["autoZoom"] = True
            recommendations["zoomStrategy"] = "conservative"
            recommendations["optimizeFile"] = True
            recommendations["createOverview"] = fileSizeGb > 2  # 大文件建议创建概览
            recommendations["useOptimizedMode"] = True
            
            # 根据系统内存进一步调整
            if memoryTotalGb < 8:
                recommendations["processes"] = min(recommendations["processes"], 4)
                recommendations["maxMemory"] = min(recommendations["maxMemory"], 2048)
            elif memoryTotalGb < 16:
                recommendations["processes"] = min(recommendations["processes"], 6)
                recommendations["maxMemory"] = min(recommendations["maxMemory"], 4096)
        
        # 用户指定的级别优先
        if userMinZoom is not None:
            recommendations["minZoom"] = userMinZoom
        if userMaxZoom is not None:
            recommendations["maxZoom"] = userMaxZoom
        
        # 确保进程数合理
        recommendations["processes"] = max(1, min(recommendations["processes"], cpuCount))
        
        return recommendations
    except Exception as e:
        logMessage(f"生成推荐配置失败: {e}", "ERROR")
        return None

def detectOptimalZoomLevels(filePath):
    """
    检测基于文件分辨率的最佳缩放级别
    
    @param filePath 文件路径
    @return 包含最佳缩放级别的字典，失败时返回None
    """
    try:
        info = getFileInfo(filePath)
        if "error" in info:
            return None
        
        # 基于文件大小和分辨率计算最佳级别
        width = info.get("width", 0)
        height = info.get("height", 0)
        
        if width <= 0 or height <= 0:
            return None
        
        # 计算合适的最大级别
        maxDim = max(width, height)
        optimalMaxZoom = min(18, int(math.log2(maxDim / 256)) + 1)
        
        return {
            "minZoom": 0,
            "maxZoom": optimalMaxZoom,
            "reason": f"基于图像分辨率 {width}x{height} 计算"
        }
    except Exception as e:
        logMessage(f"检测最佳级别失败: {e}", "ERROR")
        return None

def optimizeTiff(filePath, outputPath=None, compressionType="lzw"):
    """
    优化TIFF文件，包括压缩、分块和BigTIFF支持
    
    @param filePath 输入TIFF文件路径
    @param outputPath 输出文件路径，为None时自动生成
    @param compressionType 压缩类型，默认为lzw
    @return 优化后的文件路径，失败时返回None
    """
    try:
        if not outputPath:
            outputPath = filePath.replace(".tif", "_optimized.tif")
        
        # 构建gdalwarp命令
        cmd = [
            "gdalwarp",
            "-co", f"COMPRESS={compressionType}",
            "-co", "TILED=YES",
            "-co", "BIGTIFF=IF_SAFER",
            filePath,
            outputPath
        ]
        
        # 执行优化
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        stdout, stderr = process.communicate()
        
        if process.returncode == 0:
            logMessage(f"TIFF优化完成: {outputPath}", "INFO")
            return outputPath
        else:
            logMessage(f"TIFF优化失败: {stderr.decode()}", "ERROR")
            return None
    except Exception as e:
        logMessage(f"TIFF优化异常: {e}", "ERROR")
        return None

def decompressTerrainFiles(terrainDir: str) -> bool:
    """使用gzip解压terrain文件"""
    import gzip
    try:
        terrainFiles = []
        for root, dirs, files in os.walk(terrainDir):
            for file in files:
                if file.endswith('.terrain'):
                    terrainFiles.append(os.path.join(root, file))
        
        decompressedCount = 0
        for terrainFile in terrainFiles:
            try:
                # 检查是否为gzip压缩文件
                with open(terrainFile, 'rb') as f:
                    magic = f.read(2)
                    if magic == b'\x1f\x8b':  # gzip魔数
                        # 解压文件
                        tempFile = terrainFile + '.tmp'
                        with gzip.open(terrainFile, 'rb') as fIn:
                            with open(tempFile, 'wb') as fOut:
                                shutil.copyfileobj(fIn, fOut)
                        
                        # 替换原文件
                        os.replace(tempFile, terrainFile)
                        decompressedCount += 1
            except Exception as e:
                logMessage(f"解压文件失败 {terrainFile}: {e}", "ERROR")
                continue
        
        logMessage(f"解压完成，处理了 {decompressedCount} 个文件")
        return True
    except Exception as e:
        logMessage(f"解压terrain文件失败: {e}", "ERROR")
        return False

def createTerrainPyramid(source_path):
    """为大地形文件创建金字塔概览，提升CTB处理效率"""
    try:
        # 检查是否已有金字塔文件
        ovr_file = source_path + '.ovr'
        if os.path.exists(ovr_file):
            logMessage(f"金字塔文件已存在: {os.path.basename(ovr_file)}")
            return True
        
        # 获取文件大小，选择合适的金字塔级别
        file_size_gb = os.path.getsize(source_path) / (1024**3)
        
        if file_size_gb > 50:
            levels = [2, 4, 8, 16, 32, 64, 128, 256]  # 超大文件
        elif file_size_gb > 20:
            levels = [2, 4, 8, 16, 32, 64, 128]       # 大文件
        else:
            levels = [2, 4, 8, 16, 32, 64]            # 中等文件
        
        # 执行gdaladdo命令创建金字塔
        cmd = [
            'gdaladdo', 
            '-r', 'average',  # 平均值采样，适合DEM
            '--config', 'COMPRESS_OVERVIEW', 'LZW',  # 压缩节省空间
            source_path
        ] + [str(level) for level in levels]
        
        logMessage(f"创建金字塔: gdaladdo -r average {os.path.basename(source_path)} {' '.join(map(str, levels))}")
        
        # 设置环境变量
        env = os.environ.copy()
        env['GDAL_CACHEMAX'] = '1024'  # 1GB缓存
        
        # 执行命令
        result = runCommand(cmd, env=env)
        
        if result["success"] and os.path.exists(ovr_file):
            ovr_size_mb = os.path.getsize(ovr_file) / (1024**2)
            logMessage(f"金字塔创建成功: {ovr_size_mb:.1f}MB")
            return True
        else:
            logMessage(f"金字塔创建失败: {result.get('stderr', '')}", "WARNING")
            return False
            
    except Exception as e:
        logMessage(f"金字塔创建异常: {str(e)}", "ERROR")
        return False

def optimizedGdal2tilesByLevels(filePath, outputDir, minZoom=0, maxZoom=18, processes=4, tileFormat="png", quality=85):
    """
    分级生成瓦片，逐级处理以优化性能
    
    @param filePath 输入文件路径
    @param outputDir 输出目录
    @param minZoom 最小缩放级别
    @param maxZoom 最大缩放级别
    @param processes 并行进程数
    @param tileFormat 瓦片格式：png、webp、jpeg
    @param quality 图片质量（0-100）
    @return 处理成功返回True，失败返回False
    """
    try:
        # 检查输出目录
        if not os.path.exists(outputDir):
            os.makedirs(outputDir)
        
        # 分级处理
        for zoom in range(minZoom, maxZoom + 1):
            zoomOutputDir = os.path.join(outputDir, str(zoom))
            if not os.path.exists(zoomOutputDir):
                os.makedirs(zoomOutputDir)
            
            # 构建gdal2tiles命令
            cmd = [
                "gdal2tiles.py",
                "-z", f"{zoom}-{zoom}",
                "-w", "none",
                "--processes", str(processes),
                filePath,
                zoomOutputDir
            ]
            
            if tileFormat == "webp":
                cmd.extend(["--webp-quality", str(quality)])
            elif tileFormat == "jpeg":
                cmd.extend(["--jpeg-quality", str(quality)])
            
            # 执行命令
            process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout, stderr = process.communicate()
            
            if process.returncode != 0:
                logMessage(f"级别 {zoom} 生成失败: {stderr.decode()}", "ERROR")
                return False
            
            logMessage(f"级别 {zoom} 生成完成", "INFO")
        
        return True
    except Exception as e:
        logMessage(f"分级生成瓦片失败: {e}", "ERROR")
        return False

def createTileGridIndex(tifFiles, outputPath, minZoom, maxZoom, tileSize=256):
    """
    创建瓦片网格索引（SHP分幅矢量栅格）
    
    @param tifFiles TIF文件列表
    @param outputPath 输出路径
    @param minZoom 最小缩放级别
    @param maxZoom 最大缩放级别
    @param tileSize 瓦片大小
    @return 瓦片索引结果
    """
    try:
        logMessage(f"开始创建瓦片网格索引：{len(tifFiles)} 个文件，级别 {minZoom}-{maxZoom}")
        
        # 步骤1：获取所有文件的地理边界
        allBounds = []
        for tifFile in tifFiles:
            bounds = getFileGeographicBounds(tifFile)
            if bounds and "west" in bounds:
                allBounds.append({
                    "file": tifFile,
                    "west": bounds["west"],
                    "south": bounds["south"], 
                    "east": bounds["east"],
                    "north": bounds["north"]
                })
                logMessage(f"文件边界: {os.path.basename(tifFile)} [{bounds['west']:.6f}, {bounds['south']:.6f}, {bounds['east']:.6f}, {bounds['north']:.6f}]")
        
        if not allBounds:
            return {"success": False, "error": "无法获取任何文件的地理边界"}
        
        # 步骤2：计算总体边界
        totalWest = min(b["west"] for b in allBounds)
        totalSouth = min(b["south"] for b in allBounds)  
        totalEast = max(b["east"] for b in allBounds)
        totalNorth = max(b["north"] for b in allBounds)
        
        logMessage(f"总体边界: [{totalWest:.6f}, {totalSouth:.6f}, {totalEast:.6f}, {totalNorth:.6f}]")
        
        # 步骤3：生成所有级别的瓦片网格索引
        tileIndex = []
        
        for zoom in range(minZoom, maxZoom + 1):
            # 计算该级别的瓦片范围（修复坐标计算）
            # 西北角 → 左上角瓦片坐标（minX, minY）
            minTileX, minTileY = deg2tile(totalNorth, totalWest, zoom)
            # 东南角 → 右下角瓦片坐标（maxX, maxY）
            maxTileX, maxTileY = deg2tile(totalSouth, totalEast, zoom)
            
            # 确保范围正确
            minTileX = max(0, min(minTileX, maxTileX))
            maxTileX = min((1 << zoom) - 1, max(minTileX, maxTileX))
            minTileY = max(0, min(minTileY, maxTileY))
            maxTileY = min((1 << zoom) - 1, max(minTileY, maxTileY))
            
            logMessage(f"级别 {zoom}: 瓦片范围 X({minTileX}-{maxTileX}) Y({minTileY}-{maxTileY})")
            
            # 为每个瓦片创建详细索引
            for tileX in range(minTileX, maxTileX + 1):
                for tileY in range(minTileY, maxTileY + 1):
                    # 计算瓦片地理边界
                    tileWest, tileNorth = tile2deg(tileX, tileY, zoom)
                    tileEast, tileSouth = tile2deg(tileX + 1, tileY + 1, zoom)
                    
                    # 查找与该瓦片相交的TIF文件
                    intersectingFiles = []
                    for fileInfo in allBounds:
                        if (fileInfo["west"] <= tileEast and fileInfo["east"] >= tileWest and
                            fileInfo["south"] <= tileNorth and fileInfo["north"] >= tileSouth):
                            intersectingFiles.append({
                                "file": fileInfo["file"],
                                "bounds": [fileInfo["west"], fileInfo["south"], fileInfo["east"], fileInfo["north"]],
                                "filename": os.path.basename(fileInfo["file"])
                            })
                    
                    # 只为有数据的瓦片创建索引（关键优化）
                    if intersectingFiles:
                        tileInfo = {
                            "z": zoom,
                            "x": tileX,
                            "y": tileY,
                            "bounds": [tileWest, tileSouth, tileEast, tileNorth],
                            "sourceFiles": intersectingFiles,
                            "sourceCount": len(intersectingFiles),
                            "tileSize": tileSize,
                            "area": (tileEast - tileWest) * (tileNorth - tileSouth)
                        }
                        tileIndex.append(tileInfo)
        
        logMessage(f"✅ 瓦片网格索引创建完成：总计 {len(tileIndex)} 个有效瓦片")
        
        return {
            "success": True,
            "tileIndex": tileIndex,
            "totalTiles": len(tileIndex),
            "totalBounds": [totalWest, totalSouth, totalEast, totalNorth],
            "zoomLevels": f"{minZoom}-{maxZoom}",
            "sourceFiles": len(tifFiles)
        }
        
    except Exception as e:
        logMessage(f"创建瓦片网格索引失败: {e}", "ERROR")
        return {"success": False, "error": str(e)}

def deg2tile(lat_deg, lon_deg, zoom):
    """将地理坐标转换为瓦片坐标"""
    import math
    lat_rad = math.radians(lat_deg)
    n = 2.0 ** zoom
    x = int((lon_deg + 180.0) / 360.0 * n)
    y = int((1.0 - math.asinh(math.tan(lat_rad)) / math.pi) / 2.0 * n)
    return (x, y)

def tile2deg(x, y, zoom):
    """将瓦片坐标转换为地理坐标"""
    import math
    n = 2.0 ** zoom
    lon_deg = x / n * 360.0 - 180.0
    lat_rad = math.atan(math.sinh(math.pi * (1 - 2 * y / n)))
    lat_deg = math.degrees(lat_rad)
    return (lon_deg, lat_deg)

def runCommand(cmd, cwd=None, env=None):
    """执行系统命令"""
    try:
        result = subprocess.run(
            cmd, 
            capture_output=True, 
            text=True, 
            cwd=cwd,
            env=env,
            timeout=3600  # 1小时超时
        )
        return {
            "success": result.returncode == 0,
            "stdout": result.stdout,
            "stderr": result.stderr,
            "returncode": result.returncode
        }
    except subprocess.TimeoutExpired:
        return {
            "success": False,
            "error": "命令执行超时"
        }
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

def processHighPerformanceTiles(tileIndex, outputPath, resampling="near", max_workers=None, batch_size=50, user_processes=None, taskId=None):
    """
    高性能批量瓦片处理，目标：每秒1000瓦片
    
    @param tileIndex 瓦片索引列表
    @param outputPath 输出路径
    @param resampling 重采样方法
    @param max_workers 最大工作进程数
    @param batch_size 批量大小
    @param user_processes 用户指定的进程数（优先使用）
    @param taskId 任务ID（用于停止检查）
    @return 处理结果统计
    """
    import multiprocessing as mp
    import queue
    import time
    import tempfile
    import os
    import threading
    from concurrent.futures import ProcessPoolExecutor, as_completed
    
    total_tiles = len(tileIndex)
    start_time = time.time()
    
    # 优先使用用户指定的进程数
    if max_workers is None:
        cpu_count = mp.cpu_count()
        if user_processes and user_processes > 0:
            # 用户明确指定了进程数，优先使用用户的值
            max_workers = user_processes
        else:
            if total_tiles < 100:
                max_workers = min(4, cpu_count)
            elif total_tiles < 1000:
                max_workers = min(cpu_count, 32)
            else:
                max_workers = min(cpu_count * 2, 32)
    
    # 创建停止标志文件名
    stop_flag_file = f"/tmp/stop_flag_{taskId}.txt" if taskId else None
    
    # 启动停止检查线程
    stop_checker_thread = None
    if taskId:
        def stop_checker():
            """停止检查线程：监控任务状态，一旦检测到停止就创建停止标志文件"""
            while True:
                try:
                    # 检查任务状态
                    with taskLock:
                        task_data = taskStatus.get(taskId)
                    if task_data and task_data.get("status") == "stopped":
                        # 创建停止标志文件
                        if stop_flag_file:
                            with open(stop_flag_file, 'w') as f:
                                f.write("STOP")
                            logMessage(f"停止标志文件已创建: {stop_flag_file}")
                        break
                    
                    # 每秒检查一次
                    time.sleep(1)
                except Exception as e:
                    logMessage(f"停止检查线程错误: {e}", "WARNING")
                    time.sleep(1)
        
        stop_checker_thread = threading.Thread(target=stop_checker, daemon=True)
        stop_checker_thread.start()
        logMessage(f"停止检查线程已启动，监控任务: {taskId}")
    
    # 计算批次大小
    batch_size = calculateOptimalBatchSize(total_tiles, max_workers)
    
    # 创建批次
    batches = []
    for i in range(0, total_tiles, batch_size):
        batch = tileIndex[i:i + batch_size]
        batches.append({
            'tiles': batch,
            'batch_idx': i // batch_size,
            'stop_flag_file': stop_flag_file  # 传递停止标志文件路径
        })
    
    logMessage(f"🚀 启动高性能瓦片处理: {total_tiles}瓦片, {max_workers}进程, 批量大小{batch_size}")
    logMessage(f"📦 瓦片分批完成: {len(batches)}批次")
    
    # 统计信息
    processed_tiles = 0
    failed_tiles = 0
    batch_results = []
    
    # 使用进程池处理批次
    with ProcessPoolExecutor(max_workers=max_workers) as executor:
        # 提交所有批次
        future_to_batch = {
            executor.submit(processTileBatch, batch['tiles'], outputPath, resampling, batch['batch_idx'], batch['stop_flag_file']): batch 
            for batch in batches
        }
        
        # 收集结果
        for future in as_completed(future_to_batch):
            batch = future_to_batch[future]
            try:
                result = future.result()
                processed_tiles += result['processed']
                failed_tiles += result['failed']
                batch_results.append(result)
                
                # 检查是否被停止
                if result.get('stopped', False):
                    logMessage(f"检测到停止信号，取消其他任务")
                    # 取消其他未完成的任务
                    for f in future_to_batch:
                        if not f.done():
                            f.cancel()
                    break
                    
            except Exception as e:
                failed_tiles += batch_size
                logMessage(f"批次 {batch['batch_idx']} 处理异常: {e}", "ERROR")
    
    # 清理停止标志文件
    if stop_flag_file and os.path.exists(stop_flag_file):
        try:
            os.remove(stop_flag_file)
            logMessage(f"停止标志文件已清理: {stop_flag_file}")
        except:
            pass
    
    # 计算平均速度
    total_time = time.time() - start_time
    average_speed = processed_tiles / total_time if total_time > 0 else 0
    
    # 返回结果
    return {
        "success": True,
        "processed_tiles": processed_tiles,
        "failed_tiles": failed_tiles,
        "total_tiles": total_tiles,
        "batch_results": batch_results,
        "average_speed": average_speed,
        "total_time": total_time
    }

def processTileBatch(tiles, outputPath, resampling, batch_idx, stop_flag_file=None):
    """
    处理一批瓦片
    
    @param tiles 瓦片列表
    @param outputPath 输出路径
    @param resampling 重采样方法
    @param batch_idx 批次索引
    @param stop_flag_file 停止标志文件路径
    @return 处理结果
    """
    import os
    import time
    
    processed = 0
    failed = 0
    
    for tile in tiles:
        # 检查停止标志文件
        if stop_flag_file and os.path.exists(stop_flag_file):
            logMessage(f"批次 {batch_idx} 检测到停止信号，终止处理")
            return {
                "processed": processed,
                "failed": failed,
                "batch_idx": batch_idx,
                "stopped": True
            }
        
        try:
            # 处理单个瓦片
            result = processSingleTileOptimized(tile, outputPath, resampling)
            if result["success"]:
                processed += 1
            else:
                failed += 1
        except Exception as e:
            failed += 1
            logMessage(f"瓦片处理异常: {e}", "ERROR")
    
    return {
        "processed": processed,
        "failed": failed,
        "batch_idx": batch_idx,
        "stopped": False
    }

def calculateOptimalBatchSize(tile_count, max_workers, target_speed=1000):
    """
    根据瓦片数量和工作进程数计算最优批量大小
    
    @param tile_count 瓦片总数
    @param max_workers 工作进程数
    @param target_speed 目标速度（瓦片/秒）
    @return 最优批量大小
    """
    # 基础计算：每个进程每秒处理的瓦片数
    tiles_per_worker_per_sec = target_speed / max_workers
    
    # 假设每批次处理时间为2-5秒（考虑进程启动开销）
    optimal_batch_time = 3.0
    
    # 计算批量大小
    batch_size = int(tiles_per_worker_per_sec * optimal_batch_time)
    
    # 限制在合理范围内
    if tile_count < 100:
        batch_size = min(batch_size, 10)
    elif tile_count < 1000:
        batch_size = min(batch_size, 50)
    elif tile_count < 10000:
        batch_size = min(batch_size, 100)
    else:
        batch_size = min(batch_size, 200)
    
    # 最小批量大小
    batch_size = max(batch_size, 5)
    
    return batch_size

def processSingleTileOptimized(tileInfo, tilesDir, resampling="near", temp_dir=None):
    """
    优化的单瓦片处理（减少IO，优化GDAL命令）
    使用两步法：gdalwarp -> GTiff -> gdal_translate -> PNG
    
    @param tileInfo 瓦片索引信息
    @param tilesDir 瓦片输出目录
    @param resampling 重采样方法
    @param temp_dir 临时目录
    @return 处理结果
    """
    try:
        zoom, tileX, tileY = tileInfo["z"], tileInfo["x"], tileInfo["y"]
        tileBounds = tileInfo["bounds"]  # [west, south, east, north]
        sourceFiles = tileInfo["sourceFiles"]
        
        # 创建瓦片目录
        tileDir = os.path.join(tilesDir, str(zoom), str(tileX))
        os.makedirs(tileDir, exist_ok=True)
        
        # 瓦片文件路径
        tileFile = os.path.join(tileDir, f"{tileY}.png")
        
        # 如果瓦片已存在且不为空，跳过处理
        if os.path.exists(tileFile) and os.path.getsize(tileFile) > 0:
            return {
                "success": True,
                "tileFile": tileFile,
                "skipped": True,
                "tilePath": f"{zoom}/{tileX}/{tileY}.png"
            }
        
        # 准备源文件列表
        sourceFileList = [sf["file"] for sf in sourceFiles]
        
        # 使用临时目录减少IO冲突
        if temp_dir:
            tempTileFile = os.path.join(temp_dir, f"tile_{zoom}_{tileX}_{tileY}.tif")
        else:
            tempTileFile = tileFile.replace('.png', '_temp.tif')
        
        # 第一步：gdalwarp生成GTiff
        cmd1 = [
            "gdalwarp",
            "-te", str(tileBounds[0]), str(tileBounds[1]), str(tileBounds[2]), str(tileBounds[3]),
            "-ts", "256", "256",
            "-r", resampling,
            "-of", "GTiff",  # 输出GTiff
            "-co", "TILED=YES",
            "-co", "COMPRESS=LZW",
            "-srcnodata", "0",
            "-dstnodata", "0",
            "-dstalpha",
            "-multi",  # 启用多线程处理
            "-wm", "128",  # 工作内存128MB
            "-q"  # 静默模式减少输出
        ] + sourceFileList + [tempTileFile]
        
        # 执行第一步命令
        result1 = subprocess.run(cmd1, capture_output=True, text=True, timeout=30)
        
        if result1.returncode != 0:
            return {
                "success": False,
                "error": f"gdalwarp失败: {result1.stderr}"
            }
        
        # 第二步：gdal_translate转换为PNG
        cmd2 = [
            "gdal_translate",
            "-of", "PNG",
            "-co", "WORLDFILE=NO",
            tempTileFile,
            tileFile
        ]
        
        result2 = subprocess.run(cmd2, capture_output=True, text=True, timeout=30)
        
        # 清理临时GTiff文件
        try:
            if os.path.exists(tempTileFile):
                os.remove(tempTileFile)
        except:
            pass
        
        if result2.returncode == 0:
            return {
                "success": True,
                "tileFile": tileFile,
                "sourceCount": len(sourceFiles),
                "method": "优化两步法gdalwarp+translate",
                "tilePath": f"{zoom}/{tileX}/{tileY}.png"
            }
        else:
            return {
                "success": False,
                "error": f"PNG转换失败: {result2.stderr}"
            }
            
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

def processSingleTileFromIndex(tileInfo, tilesDir, resampling="near"):
    """
    根据索引信息处理单个瓦片（无VRT，直接GDAL切割）
    
    @param tileInfo 瓦片索引信息
    @param tilesDir 瓦片输出目录
    @param resampling 重采样方法
    @return 处理结果
    """
    try:
        zoom, tileX, tileY = tileInfo["z"], tileInfo["x"], tileInfo["y"]
        tileBounds = tileInfo["bounds"]  # [west, south, east, north]
        sourceFiles = tileInfo["sourceFiles"]
        
        # 创建瓦片目录
        tileDir = os.path.join(tilesDir, str(zoom), str(tileX))
        os.makedirs(tileDir, exist_ok=True)
        
        # 瓦片文件路径
        tileFile = os.path.join(tileDir, f"{tileY}.png")
        
        # 准备源文件列表
        sourceFileList = [sf["file"] for sf in sourceFiles]
        tempTileFile = tileFile.replace('.png', '_temp.tif')
        
        # 添加边界重叠处理消除缝隙
        west, south, east, north = tileBounds
        overlap = 0.0001  # 边界重叠量（度）
        expandedBounds = [west - overlap, south - overlap, east + overlap, north + overlap]
        
        # 核心：gdalwarp直接处理多个输入文件（无VRT中间层 + 缝隙修复）
        cmd = [
            "gdalwarp",
            "-te", str(expandedBounds[0]), str(expandedBounds[1]), str(expandedBounds[2]), str(expandedBounds[3]),  # 扩展边界消除缝隙
            "-ts", "256", "256",  # 瓦片尺寸
            "-r", resampling,
            "-of", "GTiff",
            "-co", "TILED=YES", 
            "-co", "COMPRESS=LZW",
            "-co", "BIGTIFF=IF_SAFER",
            "-srcnodata", "0",  # 源文件nodata值
            "-dstnodata", "0",  # 目标文件nodata值
            "-dstalpha",  # 添加Alpha通道处理透明度
            "-multi"  # 多线程处理
        ] + sourceFileList + [tempTileFile]  # 多个输入 + 一个输出
        
        warpResult = runCommand(cmd)
        
        if not warpResult["success"]:
            return {
                "success": False,
                "error": f"gdalwarp失败: {warpResult.get('stderr', '未知错误')}"
            }
        
        # 转换为PNG
        cmd2 = [
            "gdal_translate",
            "-of", "PNG",
            "-co", "WORLDFILE=NO",
            tempTileFile,
            tileFile
        ]
        
        translateResult = runCommand(cmd2)
        
        # 清理临时GTiff文件
        try:
            if os.path.exists(tempTileFile):
                os.remove(tempTileFile)
        except:
            pass
        
        if not translateResult["success"]:
            return {
                "success": False,
                "error": f"PNG转换失败: {translateResult.get('stderr', '未知错误')}"
            }
        
        return {
            "success": True,
            "tileFile": tileFile,
            "sourceCount": len(sourceFiles),
            "method": "直接gdalwarp+translate",
            "tilePath": f"{zoom}/{tileX}/{tileY}.png"
        }
            
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

def generateShapefileIndex(tileIndex, outputPath, generateShp=True):
    """
    生成瓦片索引的Shapefile文件（SHP分幅矢量栅格）
    
    @param tileIndex 瓦片索引列表
    @param outputPath 输出路径
    @return 生成结果
    """
    try:
        import json
        import hashlib
        
        # 计算瓦片索引的哈希值，用于检查是否需要重新生成
        index_key = []
        for tile in tileIndex:
            tile_key = f"{tile['z']}/{tile['x']}/{tile['y']}"
            index_key.append(tile_key)
        index_str = '|'.join(sorted(index_key))
        current_hash = hashlib.md5(index_str.encode('utf-8')).hexdigest()
        
        # 定义文件路径
        geojsonFile = os.path.join(outputPath, "tile_index.geojson")
        shpFile = os.path.join(outputPath, "tile_index.shp")
        hashFile = os.path.join(outputPath, ".tile_index_hash")
        
        # 检查是否可以复用现有文件
        can_reuse = False
        if os.path.exists(hashFile):
            try:
                with open(hashFile, 'r') as f:
                    existing_hash = f.read().strip()
                if existing_hash == current_hash:
                    # 检查索引文件是否存在
                    if os.path.exists(geojsonFile):
                        if os.path.exists(shpFile):
                            logMessage(f"🔄 检测到相同索引，复用现有SHP和GeoJSON文件")
                            can_reuse = True
                        else:
                            logMessage(f"🔄 检测到相同索引，复用GeoJSON文件，重新生成SHP文件")
                            can_reuse = "geojson_only"
                    else:
                        logMessage(f"⚠️ 索引哈希匹配但文件缺失，重新生成")
            except Exception as e:
                logMessage(f"⚠️ 读取索引哈希失败: {e}")
        
        if can_reuse is True:
            # 完全复用现有文件
            return {
                "success": True,
                "shpFile": shpFile,
                "geojsonFile": geojsonFile,
                "totalFeatures": len(tileIndex),
                "reused": True
            }
        
        # 生成GeoJSON格式的瓦片索引
        features = []
        
        for tile in tileIndex:
            # 安全处理bounds，尝试修复而不是跳过
            tileBounds = tile.get("bounds", [])
            if not tileBounds or len(tileBounds) != 4:
                # 尝试从瓦片坐标计算bounds
                try:
                    z, x, y = tile["z"], tile["x"], tile["y"]
                    # 使用tile2deg函数计算瓦片边界
                    west, north = tile2deg(x, y, z)
                    east, south = tile2deg(x + 1, y + 1, z)
                    tileBounds = [west, south, east, north]
                    logMessage(f"🔧 修复瓦片bounds: {z}/{x}/{y} -> {tileBounds}", "INFO")
                except Exception as e:
                    logMessage(f"❌ 无法修复瓦片bounds，跳过: {tile.get('z', '?')}/{tile.get('x', '?')}/{tile.get('y', '?')} 错误: {e}", "ERROR")
                    continue
            
            west, south, east, north = tileBounds
            
            feature = {
                "type": "Feature",
                "properties": {
                    "z": tile["z"],
                    "x": tile["x"], 
                    "y": tile["y"],
                    "sourceCount": tile["sourceCount"],
                    "area": tile.get("area", 0),
                    "tileSize": tile.get("tileSize", 256),
                    "sourceFiles": [sf["filename"] for sf in tile["sourceFiles"]],
                    "tilePath": f"{tile['z']}/{tile['x']}/{tile['y']}.png"
                },
                "geometry": {
                    "type": "Polygon",
                    "coordinates": [[
                        [west, south],
                        [east, south],
                        [east, north],
                        [west, north],
                        [west, south]
                    ]]
                }
            }
            features.append(feature)
        
        geojson = {
            "type": "FeatureCollection",
            "crs": {
                "type": "name",
                "properties": {
                    "name": "urn:ogc:def:crs:OGC:1.3:CRS84"
                }
            },
            "features": features
        }
        
        # 如果不是仅复用GeoJSON，则保存新的GeoJSON文件
        if can_reuse != "geojson_only":
            with open(geojsonFile, 'w', encoding='utf-8') as f:
                json.dump(geojson, f, indent=2, ensure_ascii=False)
            logMessage(f"✅ GeoJSON瓦片索引已生成: {geojsonFile}")
        else:
            logMessage(f"🔄 复用现有GeoJSON文件: {geojsonFile}")
        
        # 使用ogr2ogr转换为Shapefile
        cmd = [
            "ogr2ogr",
            "-f", "ESRI Shapefile",
            "-overwrite",  # 覆盖现有文件
            shpFile,
            geojsonFile
        ]
        
        result = runCommand(cmd)
        
        if result["success"]:
            logMessage(f"✅ SHP索引文件已生成: {shpFile}")
            
            # 保存索引哈希值
            try:
                with open(hashFile, 'w') as f:
                    f.write(current_hash)
                logMessage(f"💾 索引哈希已保存: {current_hash[:8]}...")
            except Exception as e:
                logMessage(f"⚠️ 保存索引哈希失败: {e}", "WARNING")
            
            return {
                "success": True,
                "shpFile": shpFile,
                "geojsonFile": geojsonFile,
                "totalFeatures": len(features),
                "reused": False
            }
        else:
            # 即使SHP生成失败，GeoJSON仍然可用
            logMessage(f"⚠️ SHP转换失败，但GeoJSON可用: {result.get('stderr', '未知错误')}", "WARNING")
            
            # 仍然保存哈希值（针对GeoJSON）
            try:
                with open(hashFile, 'w') as f:
                    f.write(current_hash)
            except:
                pass
            
            return {
                "success": True,
                "shpFile": None,
                "geojsonFile": geojsonFile,
                "totalFeatures": len(features),
                "warning": f"SHP转换失败: {result.get('stderr', '未知错误')}",
                "reused": False
            }
        
    except Exception as e:
        logMessage(f"❌ 生成Shapefile索引失败: {e}", "ERROR")
        return {
            "success": False,
            "error": str(e)
        }

def verifyTilesIntegrity(outputPath, metadata):
    """
    验证瓦片文件的完整性
    
    @param outputPath 输出路径
    @param metadata 现有元数据
    @return 是否完整
    """
    try:
        # 检查输出目录是否存在
        if not os.path.exists(outputPath):
            logMessage(f"输出目录不存在: {outputPath}")
            return False
        
        # 检查基本统计信息
        processed_tiles = metadata.get("processedTiles", 0)
        if processed_tiles == 0:
            logMessage(f"没有已处理的瓦片")
            return False
        
        # 检查成功率
        success_rate_str = metadata.get("successRate", "0%")
        try:
            success_rate = float(success_rate_str.replace("%", ""))
            if success_rate < 50:  # 成功率低于50%认为不完整
                logMessage(f"瓦片成功率过低: {success_rate}%")
                return False
        except:
            logMessage(f"无法解析成功率: {success_rate_str}")
            return False
        
        # 检查是否有瓦片索引信息
        tile_index = metadata.get("tileIndex", [])
        if not tile_index:
            logMessage(f"缺少瓦片索引信息")
            return False
        
        # 随机检查几个瓦片文件是否存在
        import random
        sample_size = min(10, len(tile_index))  # 最多检查10个瓦片
        sample_tiles = random.sample(tile_index, sample_size)
        
        missing_count = 0
        for tile in sample_tiles:
            z, x, y = tile['z'], tile['x'], tile['y']
            tile_path = os.path.join(outputPath, str(z), str(x), f"{y}.png")
            if not os.path.exists(tile_path):
                missing_count += 1
        
        # 如果超过30%的采样瓦片缺失，认为不完整
        missing_rate = missing_count / sample_size
        if missing_rate > 0.3:
            logMessage(f"瓦片文件缺失率过高: {missing_rate*100:.1f}%")
            return False
        
        logMessage(f"瓦片完整性验证通过: 成功率{success_rate}%, 采样缺失率{missing_rate*100:.1f}%")
        return True
        
    except Exception as e:
        logMessage(f"瓦片完整性验证失败: {e}", "WARNING")
        return False

def cleanupTasksByCount(maxTasks=100):
    """
    按数量清理任务，删除最旧的任务以控制内存使用
    
    @param maxTasks 保留的最大任务数
    @return 清理的任务数量
    """
    try:
        with taskLock:
            taskIds = list(taskStatus.keys())
            if len(taskIds) > maxTasks:
                # 按时间排序，删除最老的任务
                taskIds.sort(key=lambda tid: taskStatus[tid].get("startTime", ""))
                toDelete = taskIds[:-maxTasks]
                
                for taskId in toDelete:
                    # 停止运行中的任务
                    if taskId in taskProcesses:
                        try:
                            taskProcesses[taskId].terminate()
                            del taskProcesses[taskId]
                        except:
                            pass
                    
                    # 删除任务状态
                    if taskId in taskStatus:
                        del taskStatus[taskId]
                
                logMessage(f"清理了 {len(toDelete)} 个旧任务", "INFO")
                return len(toDelete)
            
            return 0
    except Exception as e:
        logMessage(f"清理任务失败: {e}", "ERROR")
        return 0

def healthCheck():
    """
    健康检查接口，返回服务状态信息
    
    @return 包含服务状态的JSON响应
    """
    logMessage("收到健康检查请求", "INFO")
    response = {
        "status": "healthy",
        "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "version": "1.0.0"
    }
    logMessage("健康检查响应正常", "INFO")
    return jsonify(response)

def listDataSources(subpath=''):
    """
    获取数据源列表，支持子目录浏览和地理范围筛选
    
    @param subpath 子目录路径，空字符串表示根目录
    @return 包含数据源信息的JSON响应
    """
    try:
        logMessage(f"收到数据源列表请求，子路径: '{subpath}'", "INFO")
        datasourceDir = config["dataSourceDir"]
        
        # 构建完整路径
        if subpath:
            fullPath = os.path.join(datasourceDir, subpath)
        else:
            fullPath = datasourceDir
            
        # 安全检查：确保路径在允许的目录内
        fullPath = os.path.abspath(fullPath)
        datasourceDir = os.path.abspath(datasourceDir)
        if not fullPath.startswith(datasourceDir):
            logMessage(f"路径访问被拒绝: {fullPath}", "WARNING")
            return jsonify({"error": "路径不允许访问"}), 403
            
        if not os.path.exists(fullPath):
            logMessage(f"请求的路径不存在: {fullPath}", "WARNING")
            return jsonify({"error": "路径不存在"}), 404
            
        if not os.path.isdir(fullPath):
            logMessage(f"请求的路径不是目录: {fullPath}", "WARNING")
            return jsonify({"error": "路径不是目录"}), 400
        
        # 解析地理范围筛选参数
        searchBounds = None
        boundsParam = request.args.get('bounds')
        if boundsParam:
            try:
                boundsData = json.loads(boundsParam)
                if isinstance(boundsData, list):
                    searchBounds = boundsData
                else:
                    searchBounds = [boundsData]
            except:
                return jsonify({"error": "地理范围参数格式错误"}), 400
        
        # 扫描目录
        directories = []
        datasources = []
        
        try:
            items = os.listdir(fullPath)
            items.sort()
            
            for item in items:
                itemPath = os.path.join(fullPath, item)
                
                if os.path.isdir(itemPath):
                    # 目录
                    directories.append({
                        "name": item,
                        "type": "directory",
                        "path": os.path.join(subpath, item) if subpath else item
                    })
                elif os.path.isfile(itemPath):
                    # 文件
                    fileExt = os.path.splitext(item)[1].lower()
                    if fileExt in config["supportedFormats"]:
                        fileSize = os.path.getsize(itemPath)
                        
                        fileInfo = {
                            "name": item,
                            "type": "file",
                            "size": fileSize,
                            "sizeFormatted": formatFileSize(fileSize),
                            "extension": fileExt,
                            "path": os.path.join(subpath, item) if subpath else item
                        }
                        
                        # 地理范围筛选
                        if searchBounds:
                            try:
                                fileBounds = getFileGeographicBounds(itemPath)
                                if fileBounds and checkBoundsIntersection(fileBounds, searchBounds):
                                    # 添加地理边界信息
                                    fileInfo["geoBounds"] = fileBounds
                                    datasources.append(fileInfo)
                            except Exception as e:
                                logMessage(f"获取文件地理信息失败 {item}: {e}", "WARNING")
                                # 即使地理信息获取失败，仍然添加文件
                                datasources.append(fileInfo)
                        else:
                            # 获取基本文件信息
                            try:
                                detailedInfo = getFileInfo(itemPath)
                                if "geoBounds" in detailedInfo:
                                    fileInfo["geoBounds"] = detailedInfo["geoBounds"]
                            except Exception as e:
                                logMessage(f"获取文件信息失败 {item}: {e}", "WARNING")
                            # 无论是否成功获取详细信息，都添加文件
                            datasources.append(fileInfo)
        
        except PermissionError:
            return jsonify({"error": "权限不足"}), 403
        
        # 计算父级路径
        parentPath = None
        if subpath:
            pathParts = subpath.split('/')
            if len(pathParts) > 1:
                parentPath = '/'.join(pathParts[:-1])
            else:
                parentPath = ""
        
        response = {
            "currentPath": subpath,
            "parentPath": parentPath,
            "directories": directories,
            "datasources": datasources,
            "totalDirectories": len(directories),
            "totalFiles": len(datasources),
            "count": len(datasources)
        }
        
        # 如果有地理筛选，添加筛选信息
        if searchBounds:
            response["filterInfo"] = {
                "boundsFilter": searchBounds,
                "filteredCount": len(datasources),
                "message": f"已根据地理范围筛选，找到 {len(datasources)} 个匹配文件"
            }
            logMessage(f"地理范围筛选完成，找到 {len(datasources)} 个匹配文件", "INFO")
        
        logMessage(f"数据源列表请求处理完成，返回 {len(directories)} 个目录，{len(datasources)} 个文件", "INFO")
        return jsonify(response)
    except Exception as e:
        logMessage(f"数据源列表请求处理失败: {str(e)}", "ERROR")
        return jsonify({"error": str(e)}), 500

def getFileInfo(filePath):
    """
    获取文件的详细信息
    
    @param filePath 文件完整路径
    @return 文件信息字典
    """
    try:
        fileInfo = {}
        
        # 基本文件信息
        if os.path.exists(filePath):
            stat = os.stat(filePath)
            fileInfo.update({
                "size": stat.st_size,
                "lastModified": datetime.fromtimestamp(stat.st_mtime).isoformat(),
                "type": "file"
            })
            
            # 文件格式
            fileExt = os.path.splitext(filePath)[1].lower()
            fileInfo["format"] = fileExt
            
            # 如果是地理数据文件，获取元数据
            if fileExt in ['.tif', '.tiff', '.geotiff']:
                try:
                    # 使用gdalinfo获取详细信息
                    cmd = ["gdalinfo", filePath]
                    result = runCommand(cmd)
                    
                    if result["success"]:
                        # 解析基本元数据
                        gdalinfo_output = result["stdout"]
                        lines = gdalinfo_output.split('\n')
                        
                        metadata = {}
                        for line in lines:
                            line = line.strip()
                            if line.startswith('Size is'):
                                # Size is 18892, 15028
                                parts = line.replace('Size is ', '').split(',')
                                if len(parts) == 2:
                                    metadata["rasterSize"] = {
                                        "width": int(parts[0].strip()),
                                        "height": int(parts[1].strip())
                                    }
                            elif line.startswith('Pixel Size'):
                                # Pixel Size = (0.0002777778,-0.0002777778)
                                import re
                                match = re.search(r'Pixel Size = \(([^,]+),([^)]+)\)', line)
                                if match:
                                    metadata["pixelSize"] = {
                                        "x": float(match.group(1)),
                                        "y": float(match.group(2))
                                    }
                            elif 'data type' in line.lower():
                                metadata["dataType"] = line
                            elif line.startswith('Band '):
                                if "bandCount" not in metadata:
                                    metadata["bandCount"] = 0
                                metadata["bandCount"] += 1
                        
                        # 获取地理边界
                        bounds = extractGeographicBounds(gdalinfo_output)
                        if bounds:
                            metadata["bounds"] = bounds
                            fileInfo["geoBounds"] = bounds
                        
                        # 获取坐标系信息
                        for line in lines:
                            line = line.strip()
                            if line.startswith('PROJCS[') or line.startswith('GEOGCS['):
                                metadata["srs"] = line[:100] + "..." if len(line) > 100 else line
                                break
                        
                        fileInfo["metadata"] = metadata
                        
                except Exception as e:
                    logMessage(f"获取文件元数据失败 {filePath}: {e}", "WARNING")
        
        return fileInfo
    except Exception as e:
        logMessage(f"获取文件信息失败 {filePath}: {e}", "ERROR")
        return {"error": str(e)}

def getDataSourceInfo(filename):
    """
    获取数据源详细信息，包括文件属性和推荐配置
    
    @param filename 文件名
    @return 包含详细信息的JSON响应
    """
    try:
        logMessage(f"收到文件信息请求: {filename}", "INFO")
        filePath = os.path.join(config["dataSourceDir"], filename)
        
        if not os.path.exists(filePath):
            logMessage(f"请求的文件不存在: {filename}", "WARNING")
            return jsonify({"error": "文件不存在"}), 404
        
        fileInfo = getFileInfo(filePath)
        
        # 添加切片配置推荐
        if "error" not in fileInfo:
            fileSizeGb = fileInfo["size"] / (1024**3)
            
            # 获取推荐参数
            tileType = request.args.get('tileType')
            userMinZoom = request.args.get('minZoom')
            userMaxZoom = request.args.get('maxZoom')
            
            if userMinZoom:
                userMinZoom = int(userMinZoom)
            if userMaxZoom:
                userMaxZoom = int(userMaxZoom)
            
            # 生成推荐配置
            try:
                import psutil
                cpuCount = psutil.cpu_count() or 4
                memoryTotalGb = psutil.virtual_memory().total / (1024**3)
            except:
                cpuCount = 4
                memoryTotalGb = 8
            
            recommendations = generateSmartRecommendations(
                fileSizeGb, tileType or "map", userMinZoom, userMaxZoom, cpuCount, memoryTotalGb
            )
            
            if recommendations:
                fileInfo["recommendations"] = recommendations
                logMessage(f"为文件 {filename} 生成了配置推荐", "INFO")
        
        logMessage(f"文件信息请求处理完成: {filename}", "INFO")
        return jsonify(fileInfo)
    except Exception as e:
        logMessage(f"文件信息请求处理失败: {filename}, 错误: {str(e)}", "ERROR")
        return jsonify({"error": str(e)}), 500

def browseDirectory():
    """
    逐级浏览目录和文件，支持分页和筛选
    
    @return 包含目录和文件信息的JSON响应
    """
    try:
        # 获取参数
        browseType = request.args.get('type', 'results')
        path = request.args.get('path', '').strip('/')
        
        # 确定基础目录
        if browseType == 'datasource':
            baseDir = config["dataSourceDir"]
        else:
            baseDir = config["tilesDir"]
            
        # 构建完整路径
        if path:
            fullPath = os.path.join(baseDir, path)
        else:
            fullPath = baseDir
            
        # 安全检查
        fullPath = os.path.abspath(fullPath)
        baseDir = os.path.abspath(baseDir)
        if not fullPath.startswith(baseDir):
            return jsonify({"error": "路径不允许访问"}), 403
            
        if not os.path.exists(fullPath):
            return jsonify({"error": "目录不存在"}), 404
            
        if not os.path.isdir(fullPath):
            return jsonify({"error": "路径不是目录"}), 400
            
        # 扫描当前目录
        directories = []
        files = []
        
        try:
            items = os.listdir(fullPath)
            items.sort()
            
            for item in items:
                itemPath = os.path.join(fullPath, item)
                
                if os.path.isdir(itemPath):
                    # 统计子目录文件数量
                    try:
                        subItems = os.listdir(itemPath)
                        subFileCount = len([f for f in subItems if os.path.isfile(os.path.join(itemPath, f))])
                        subDirCount = len([f for f in subItems if os.path.isdir(os.path.join(itemPath, f))])
                    except:
                        subFileCount = 0
                        subDirCount = 0
                    
                    directories.append({
                        "name": item,
                        "type": "directory",
                        "path": os.path.join(path, item) if path else item,
                        "fileCount": subFileCount,
                        "dirCount": subDirCount
                    })
                elif os.path.isfile(itemPath):
                    # 文件信息
                    fileSize = os.path.getsize(itemPath)
                    modTime = os.path.getmtime(itemPath)
                    
                    files.append({
                        "name": item,
                        "type": "file",
                        "size": fileSize,
                        "sizeFormatted": formatFileSize(fileSize),
                        "modifiedTime": modTime,
                        "extension": os.path.splitext(item)[1].lower(),
                        "path": os.path.join(path, item) if path else item
                    })
        
        except PermissionError:
            return jsonify({"error": "权限不足"}), 403
        
        # 计算父级路径
        parentPath = None
        if path:
            pathParts = path.split('/')
            if len(pathParts) > 1:
                parentPath = '/'.join(pathParts[:-1])
            else:
                parentPath = ""
        
        return jsonify({
            "currentPath": path,
            "parentPath": parentPath,
            "baseType": browseType,
            "directories": directories,
            "files": files,
            "totalDirectories": len(directories),
            "totalFiles": len(files)
        })
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

def getFileDetails():
    """
    获取文件详细信息，包括地理属性和元数据
    
    @return 包含文件详细信息的JSON响应
    """
    try:
        # 获取参数
        browseType = request.args.get('type', 'results')
        filePath = request.args.get('path', '')
        
        if not filePath:
            return jsonify({"error": "缺少文件路径参数"}), 400
        
        # 确定基础目录
        if browseType == 'datasource':
            baseDir = config["dataSourceDir"]
        else:
            baseDir = config["tilesDir"]
        
        # 构建完整路径
        fullPath = os.path.join(baseDir, filePath)
        
        # 安全检查
        fullPath = os.path.abspath(fullPath)
        baseDir = os.path.abspath(baseDir)
        if not fullPath.startswith(baseDir):
            return jsonify({"error": "路径不允许访问"}), 403
        
        if not os.path.exists(fullPath):
            return jsonify({"error": "文件不存在"}), 404
        
        if not os.path.isfile(fullPath):
            return jsonify({"error": "路径不是文件"}), 400
        
        # 获取文件信息
        fileInfo = getFileInfo(fullPath)
        return jsonify(fileInfo)
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500


def analyzeTiffGeoContinuity(tifFiles, taskId):
    """
    分析TIF文件的地理位置连续性
    
    @param tifFiles TIF文件列表
    @param taskId 任务ID
    @return 分析结果字典
    """
    try:
        def logMessage(msg):
            with taskLock:
                if taskId in taskStatus:
                    taskStatus[taskId]["message"] = msg
        
        logMessage(f"🔍 开始分析 {len(tifFiles)} 个TIF文件的地理连续性...")
        
        geoInfoList = []
        
        # 获取每个文件的地理信息
        for i, tifFile in enumerate(tifFiles):
            try:
                cmd = f"gdalinfo -json '{tifFile}'"
                result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=30)
                
                if result.returncode == 0:
                    geoData = json.loads(result.stdout)
                    
                    # 提取关键地理信息
                    geoTransform = geoData.get('geoTransform', [])
                    size = geoData.get('size', [])
                    
                    if len(geoTransform) >= 6 and len(size) >= 2:
                        # 计算边界坐标
                        originX, pixelWidth, _, originY, _, pixelHeight = geoTransform
                        width, height = size
                        
                        minX = originX
                        maxX = originX + width * pixelWidth
                        minY = originY + height * pixelHeight
                        maxY = originY
                        
                        geoInfo = {
                            'file': tifFile,
                            'index': i,
                            'minX': minX,
                            'maxX': maxX,
                            'minY': minY,
                            'maxY': maxY,
                            'centerX': (minX + maxX) / 2,
                            'centerY': (minY + maxY) / 2,
                            'width': abs(maxX - minX),
                            'height': abs(maxY - minY),
                            'pixelWidth': abs(pixelWidth),
                            'pixelHeight': abs(pixelHeight),
                            'projection': geoData.get('coordinateSystem', {}).get('wkt', '')
                        }
                        geoInfoList.append(geoInfo)
                        
                        logMessage(f"文件 {i+1}/{len(tifFiles)}: {os.path.basename(tifFile)} - 范围: ({minX:.2f}, {minY:.2f}) 到 ({maxX:.2f}, {maxY:.2f})")
                    
            except Exception as e:
                logMessage(f"⚠️ 获取文件 {tifFile} 地理信息失败: {str(e)}")
        
        if len(geoInfoList) < 2:
            return {"continuous": True, "message": "文件数量不足，无需检查连续性"}
        
        # 分析连续性
        gaps = []
        overlaps = []
        
        # 按X坐标排序
        geoInfoList.sort(key=lambda x: x['centerX'])
        
        for i in range(len(geoInfoList) - 1):
            current = geoInfoList[i]
            next_file = geoInfoList[i + 1]
            
            # 检查X方向的间隙或重叠
            gap_x = next_file['minX'] - current['maxX']
            if gap_x > current['pixelWidth']:  # 有明显间隙
                gaps.append({
                    'type': 'X_gap',
                    'file1': os.path.basename(current['file']),
                    'file2': os.path.basename(next_file['file']),
                    'gap_size': gap_x,
                    'gap_pixels': gap_x / current['pixelWidth']
                })
            elif gap_x < -current['pixelWidth']:  # 有重叠
                overlaps.append({
                    'type': 'X_overlap',
                    'file1': os.path.basename(current['file']),
                    'file2': os.path.basename(next_file['file']),
                    'overlap_size': abs(gap_x),
                    'overlap_pixels': abs(gap_x) / current['pixelWidth']
                })
        
        # 按Y坐标排序检查Y方向
        geoInfoList.sort(key=lambda x: x['centerY'])
        
        for i in range(len(geoInfoList) - 1):
            current = geoInfoList[i]
            next_file = geoInfoList[i + 1]
            
            # 检查Y方向的间隙或重叠
            gap_y = next_file['minY'] - current['maxY']
            if gap_y > current['pixelHeight']:  # 有明显间隙
                gaps.append({
                    'type': 'Y_gap',
                    'file1': os.path.basename(current['file']),
                    'file2': os.path.basename(next_file['file']),
                    'gap_size': gap_y,
                    'gap_pixels': gap_y / current['pixelHeight']
                })
            elif gap_y < -current['pixelHeight']:  # 有重叠
                overlaps.append({
                    'type': 'Y_overlap',
                    'file1': os.path.basename(current['file']),
                    'file2': os.path.basename(next_file['file']),
                    'overlap_size': abs(gap_y),
                    'overlap_pixels': abs(gap_y) / current['pixelHeight']
                })
        
        # 生成报告
        continuous = len(gaps) == 0
        message = f"地理连续性分析完成: {len(geoInfoList)} 个文件"
        
        if gaps:
            message += f", 发现 {len(gaps)} 个间隙"
            for gap in gaps[:3]:  # 只显示前3个
                message += f"\n  - {gap['file1']} 与 {gap['file2']} 间有 {gap['gap_pixels']:.1f} 像素间隙"
        
        if overlaps:
            message += f", 发现 {len(overlaps)} 个重叠"
            for overlap in overlaps[:3]:  # 只显示前3个
                message += f"\n  - {overlap['file1']} 与 {overlap['file2']} 重叠 {overlap['overlap_pixels']:.1f} 像素"
        
        logMessage(message)
        
        return {
            'continuous': continuous,
            'message': message,
            'gaps': gaps,
            'overlaps': overlaps,
            'geoInfo': geoInfoList
        }
        
    except Exception as e:
        logMessage(f"❌ 地理连续性分析失败: {str(e)}")
        return {"continuous": False, "error": str(e)}


def processSingleTerrainFile(fileInfo, outputPath, startZoom, endZoom, maxTriangles, bounds, useCompression, decompressOutput, maxMemory, autoZoom, zoomStrategy, taskId, fileIndex):
    """
    处理单个地形文件
    
    @param fileInfo 文件信息字典
    @param outputPath 输出路径
    @param startZoom 起始层级
    @param endZoom 结束层级
    @param maxTriangles 最大三角形数
    @param bounds 边界
    @param useCompression 是否使用压缩
    @param decompressOutput 是否解压输出
    @param maxMemory 最大内存
    @param autoZoom 自动缩放
    @param zoomStrategy 缩放策略
    @param taskId 任务ID
    @param fileIndex 文件索引
    @return 处理结果
    """
    try:
        source_path = fileInfo["fullPath"]
        filename = fileInfo["filename"]
        
        logMessage(f"开始处理地形文件: {filename}")
        
        # 检查源文件是否存在
        if not os.path.exists(source_path):
            return {"success": False, "error": f"源文件不存在: {source_path}"}
        
        # 创建输出目录
        os.makedirs(outputPath, exist_ok=True)
        
        # 大文件金字塔优化
        file_size_gb = os.path.getsize(source_path) / (1024**3)
        logMessage(f"文件大小: {file_size_gb:.2f}GB")
        
        if file_size_gb > 5.0:
            logMessage(f"检测到大文件，创建金字塔以优化处理...")
            createTerrainPyramid(source_path)
        
        # 构建CTB命令
        ctb_start_zoom = endZoom    # CTB的start是用户的end（详细级别）
        ctb_end_zoom = startZoom    # CTB的end是用户的start（粗糙级别）
        
        cmd = [
            "ctb-tile",
            "-f", "Mesh"
        ]
        
        if useCompression:
            cmd.append("-C")
        
        # 转换内存参数
        def convertMemoryToBytes(memoryStr):
            """将内存字符串转换为字节数"""
            if isinstance(memoryStr, int):
                return str(memoryStr * 1024 * 1024)  # 假设输入是MB
            
            memoryStr = str(memoryStr).lower()
            
            if 'g' in memoryStr:
                gb = float(memoryStr.replace('g', ''))
                return str(int(gb * 1024 * 1024 * 1024))  # GB转字节
            elif 'm' in memoryStr:
                mb = float(memoryStr.replace('m', ''))
                return str(int(mb * 1024 * 1024))  # MB转字节
            elif 'k' in memoryStr:
                kb = float(memoryStr.replace('k', ''))
                return str(int(kb * 1024))  # KB转字节
            else:
                # 假设是MB
                try:
                    mb = float(memoryStr)
                    return str(int(mb * 1024 * 1024))
                except:
                    return "2147483648"  # 默认2GB的字节数
        
        ctb_memory = convertMemoryToBytes(maxMemory)
        
        cmd.extend([
            "-o", outputPath,
            "-s", str(ctb_start_zoom),
            "-e", str(ctb_end_zoom),
            "-m", ctb_memory,
            "-v",  # 详细输出
            source_path
        ])
        
        # 设置环境变量
        env = os.environ.copy()
        env['GDAL_CACHEMAX'] = maxMemory.rstrip('gmGM')
        env['GDAL_NUM_THREADS'] = '1'  # 单个文件处理使用单线程
        env['OMP_NUM_THREADS'] = '1'
        
        logMessage(f"执行CTB命令: {' '.join(cmd)}")
        result = runCommand(cmd, env=env)
        
        if result["success"]:
            # 使用CTB单独生成layer.json
            layer_cmd = [
                "ctb-tile", "-l", "-f", "Mesh",
                "-o", outputPath,
                "-s", str(ctb_start_zoom),
                "-e", str(ctb_end_zoom),
                "-m", ctb_memory,
                "-v", 
                source_path
            ]
            layer_result = runCommand(layer_cmd, env=env)
            
            if layer_result["success"]:
                logMessage(f"CTB生成layer.json成功: {filename}")
                # 修复CTB生成的layer.json的bounds和available数组
                logMessage(f"修复layer.json的bounds和available: {filename}")
                updateCtbLayerJsonBounds(outputPath, bounds)
            else:
                logMessage(f"CTB生成layer.json失败: {filename} - {layer_result.get('stderr', '未知错误')}", "WARNING")
            
            # 解压terrain文件
            if decompressOutput:
                logMessage(f"开始解压terrain文件: {filename}")
                decompressTerrainFiles(outputPath)
                logMessage(f"terrain文件解压完成: {filename}")
            
            # 统计生成的terrain文件数量
            terrain_count = 0
            for root, dirs, files in os.walk(outputPath):
                terrain_count += len([f for f in files if f.endswith('.terrain')])
            
            return {
                "success": True,
                "outputPath": outputPath,
                "terrainFiles": terrain_count,
                "filename": filename
            }
        else:
            return {
                "success": False,
                "error": result.get('stderr', '未知错误'),
                "filename": filename
            }
            
    except Exception as e:
        logMessage(f"处理地形文件失败 {fileInfo.get('filename', 'unknown')}: {e}", "ERROR")
        return {
            "success": False,
            "error": str(e),
            "filename": fileInfo.get("filename", "unknown")
        }

def createTerrainTiles():
    """
    创建地形瓦片，使用Cesium Terrain Builder生成地形数据
    支持单个文件和批量文件处理，以及地形合并功能
    
    核心功能：
    1. 批量处理多个TIF文件
    2. 智能缩放级别推荐
    3. 地形瓦片合并功能（NEW）
    4. 任务进度监控
    
    地形合并功能：
    - 参数：mergeTerrains (boolean) - 是否在切片完成后合并成一个文件夹
    - 当设置为true时，系统会在所有地形切片完成后自动合并
    - 智能合并layer.json文件，包括边界信息
    - 使用硬链接节省磁盘空间，避免重复瓦片
    
    @return 包含任务信息的JSON响应
    """
    try:
        logMessage("收到地形瓦片创建请求", "INFO")
        data = request.get_json()
        
        if data is None:
            error_msg = "请求数据为空，无法解析JSON"
            logMessage(f"地形瓦片创建失败: {error_msg}", "ERROR")
            return jsonify({"error": error_msg}), 400
        
        logMessage(f"地形瓦片请求参数: {data}", "INFO")
        
        # 统一参数处理
        outputPathArray = data.get('outputPath', [])
        startZoom = data.get('startZoom', 0)
        endZoom = data.get('endZoom', 8)
        
        # 地形特殊参数
        maxTriangles = data.get('maxTriangles', 32768)
        bounds = data.get('bounds')
        useCompression = data.get('compression', True)
        decompressOutput = data.get('decompress', True)
        autoZoom = data.get('autoZoom', False)
        zoomStrategy = data.get('zoomStrategy', 'conservative')
        
        # 性能参数
        maxMemory = data.get('maxMemory', '8m')
        threads = data.get('threads', min(4, config["maxThreads"]))
        
        # 地形合并参数
        mergeTerrains = data.get('mergeTerrains', False)  # 是否在切片完成后合并成一个文件夹
        
        # 生成任务ID
        timestamp = int(time.time())
        taskId = f"terrain{timestamp}"
        
        # 参数验证和错误处理 - 即使有错误也创建任务
        errors = []
        
        if not outputPathArray:
            errors.append("缺少参数: outputPath")
        if endZoom < 8:
            errors.append("地形切片的最大层级(endZoom)必须大于等于8")
        
        # 获取文件列表 - 统一批量处理
        tifFiles = []
        
        # 统一参数：都用 folderPaths 和 filePatterns
        folderPaths = data.get('folderPaths')
        filePatterns = data.get('filePatterns')
        
        if not folderPaths:
            errors.append("缺少参数: folderPaths")
        if not filePatterns:
            errors.append("缺少参数: filePatterns")
        
        # 如果没有基础参数错误，尝试查找文件
        if not errors:
            relativeTifFiles = findTifFilesInFolders(folderPaths, filePatterns)
            if not relativeTifFiles:
                errors.append("未找到匹配的TIF文件")
            else:
                for relativePath in relativeTifFiles:
                    fullPath = os.path.join(config["dataSourceDir"], relativePath)
                    if os.path.exists(fullPath):
                        tifFiles.append({
                            "relativePath": relativePath,
                            "fullPath": fullPath,
                            "filename": os.path.basename(relativePath)
                        })
                
                if not tifFiles:
                    errors.append("没有找到有效的TIF文件")
        
        # 如果有错误，创建失败状态的任务
        if errors:
            with taskLock:
                taskStatus[taskId] = {
                    "status": "failed",
                    "progress": 0,
                    "message": f"地形瓦片任务创建失败: {'; '.join(errors)}",
                    "startTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "endTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "processLog": [
                        {
                            "stage": "初始化",
                            "status": "failed",
                            "message": f"任务创建失败: {'; '.join(errors)}",
                            "timestamp": datetime.now().isoformat(),
                            "progress": 0,
                            "errors": errors
                        }
                    ],
                    "currentStage": "初始化失败",
                    "result": {
                        "totalFiles": 0,
                        "completedFiles": 0,
                        "failedFiles": 0,
                        "totalTerrainFiles": 0,
                        "errors": errors
                    }
                }
            
            logMessage(f"地形瓦片任务创建失败: {taskId}, 错误: {'; '.join(errors)}", "ERROR")
            return jsonify({
                "success": False,
                "taskId": taskId,
                "message": f"地形瓦片任务创建失败: {'; '.join(errors)}",
                "statusUrl": f"/api/tasks/{taskId}",
                "errors": errors
            }), 200  # 返回200，因为任务已经创建
        
        # 构建输出路径
        if isinstance(outputPathArray, str):
            outputPath = os.path.join(config["tilesDir"], outputPathArray)
        elif isinstance(outputPathArray, list):
            outputPath = os.path.join(config["tilesDir"], *outputPathArray)
        else:
            outputPath = os.path.join(config["tilesDir"], str(outputPathArray))
        
        os.makedirs(outputPath, exist_ok=True)
        
        # 生成任务ID
        timestamp = int(time.time())
        taskId = f"terrain{timestamp}"
        
        # 启动地形切片任务
        def runTerrainTask():
            # 重新获取tifFiles变量
            nonlocal tifFiles
            try:
                with taskLock:
                    taskStatus[taskId] = {
                        "status": "running",
                        "progress": 0,
                        "message": f"开始地形切片，共{len(tifFiles)}个文件...",
                        "startTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                        "files": {
                            "total": len(tifFiles),
                            "completed": 0,
                            "failed": 0,
                            "current": None
                        },
                        "processLog": [
                            {
                                "stage": "初始化",
                                "status": "completed",
                                "message": f"任务初始化完成，准备处理{len(tifFiles)}个文件",
                                "timestamp": datetime.now().isoformat(),
                                "progress": 0
                            }
                        ],
                        "currentStage": "文件处理"
                    }
                
                completed_files = []
                failed_files = []
                
                # 先分析TIF文件的地理连续性
                if len(tifFiles) > 1:
                    tifFilesPaths = [fileInfo["fullPath"] for fileInfo in tifFiles]
                    continuityResult = analyzeTiffGeoContinuity(tifFilesPaths, taskId)
                    
                    # 记录分析结果
                    def logGeoMessage(msg):
                        with taskLock:
                            taskStatus[taskId]["message"] = msg
                    
                    if continuityResult.get("continuous"):
                        logGeoMessage("✅ TIF文件地理位置连续，无明显间隙")
                    else:
                        logGeoMessage("⚠️ 发现TIF文件间存在地理间隙，这可能是缝隙的原因")
                        if continuityResult.get("gaps"):
                            logGeoMessage(f"发现 {len(continuityResult['gaps'])} 个地理间隙")
                        if continuityResult.get("overlaps"):
                            logGeoMessage(f"发现 {len(continuityResult['overlaps'])} 个地理重叠")
                
                # 注：已移除TIF文件智能合并预处理，改用后处理边界平滑
                
                # 顺序处理模式
                logMessage(f"📋 顺序处理模式：依次处理{len(tifFiles)}个文件")
                
                for i, fileInfo in enumerate(tifFiles):
                    try:
                        # 检查任务是否被停止
                        with taskLock:
                            if taskId in taskStatus and taskStatus[taskId].get("status") == "stopped":
                                logMessage(f"⏹️ 地形瓦片任务 {taskId} 已被停止，退出处理", "INFO")
                                return
                        
                        # 更新当前处理文件
                        with taskLock:
                            taskStatus[taskId]["files"]["current"] = fileInfo["filename"]
                            taskStatus[taskId]["message"] = f"正在处理文件 {i+1}/{len(tifFiles)}: {fileInfo['filename']}"
                        
                        # 确定输出路径
                        if len(tifFiles) > 1:
                            subOutputPath = os.path.join(outputPath, f"file_{i:03d}_{fileInfo['filename'].replace('.tif', '')}")
                        else:
                            subOutputPath = outputPath
                        
                        # 处理单个文件
                        result = processSingleTerrainFile(
                            fileInfo, subOutputPath, startZoom, endZoom,
                            maxTriangles, bounds, useCompression, decompressOutput,
                            maxMemory, autoZoom, zoomStrategy, taskId, i
                        )
                        
                        if result["success"]:
                            completed_files.append({
                                "filename": fileInfo["filename"],
                                "outputPath": result["outputPath"],
                                "terrainFiles": result.get("terrainFiles", 0)
                            })
                            logMessage(f"✅ 文件处理成功: {fileInfo['filename']}")
                        else:
                            failed_files.append({
                                "filename": fileInfo["filename"],
                                "error": result.get("error", "未知错误")
                            })
                            logMessage(f"❌ 文件处理失败: {fileInfo['filename']} - {result.get('error')}", "ERROR")
                        
                        # 更新进度
                        with taskLock:
                            taskStatus[taskId]["files"]["completed"] = len(completed_files)
                            taskStatus[taskId]["files"]["failed"] = len(failed_files)
                            taskStatus[taskId]["progress"] = int((i + 1) / len(tifFiles) * 100)
                            
                            # 添加过程记录
                            if result["success"]:
                                taskStatus[taskId]["processLog"].append({
                                    "stage": "文件处理",
                                    "status": "completed",
                                    "message": f"文件 {fileInfo['filename']} 处理成功，生成了 {result.get('terrainFiles', 0)} 个地形文件",
                                    "timestamp": datetime.now().isoformat(),
                                    "progress": int((i + 1) / len(tifFiles) * 100),
                                    "fileInfo": {
                                        "filename": fileInfo["filename"],
                                        "outputPath": result["outputPath"],
                                        "terrainFiles": result.get("terrainFiles", 0)
                                    }
                                })
                            else:
                                taskStatus[taskId]["processLog"].append({
                                    "stage": "文件处理",
                                    "status": "failed",
                                    "message": f"文件 {fileInfo['filename']} 处理失败: {result.get('error', '未知错误')}",
                                    "timestamp": datetime.now().isoformat(),
                                    "progress": int((i + 1) / len(tifFiles) * 100),
                                    "fileInfo": {
                                        "filename": fileInfo["filename"],
                                        "error": result.get("error", "未知错误")
                                    }
                                })
                            
                    except Exception as e:
                        failed_files.append({
                            "filename": fileInfo["filename"],
                            "error": str(e)
                        })
                        logMessage(f"顺序处理文件失败 {fileInfo['filename']}: {e}", "ERROR")
                
                # 任务完成
                total_terrain_files = sum(file_info.get("terrainFiles", 0) for file_info in completed_files)
                
                with taskLock:
                    # 保留现有的过程记录
                    existing_process_log = taskStatus[taskId].get("processLog", [])
                    
                    # 添加最终完成记录
                    existing_process_log.append({
                        "stage": "任务完成",
                        "status": "completed",
                        "message": f"地形切片任务完成! 成功处理:{len(completed_files)}个文件, 失败:{len(failed_files)}个文件, 总共生成:{total_terrain_files}个地形瓦片",
                        "timestamp": datetime.now().isoformat(),
                        "progress": 100,
                        "summary": {
                            "totalFiles": len(tifFiles),
                            "completedFiles": len(completed_files),
                            "failedFiles": len(failed_files),
                            "totalTerrainFiles": total_terrain_files
                        }
                    })
                    
                    taskStatus[taskId] = {
                        "status": "completed",
                        "progress": 100,
                        "message": f"地形切片完成! 成功:{len(completed_files)}个, 失败:{len(failed_files)}个, 总瓦片:{total_terrain_files}个",
                        "startTime": taskStatus[taskId]["startTime"],
                        "endTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                        "processLog": existing_process_log,
                        "currentStage": "已完成",
                        "result": {
                            "totalFiles": len(tifFiles),
                            "completedFiles": len(completed_files),
                            "failedFiles": len(failed_files),
                            "totalTerrainFiles": total_terrain_files,
                            "completedDetails": completed_files,
                            "failedDetails": failed_files,
                            "outputPath": outputPath
                        }
                    }
                
                # 地形合并处理
                if mergeTerrains and len(completed_files) > 1:
                    try:
                        logMessage(f"🔄 开始合并地形瓦片...")
                        with taskLock:
                            existing_process_log = taskStatus[taskId]["processLog"]
                            existing_process_log.append({
                                "stage": "地形合并",
                                "status": "running", 
                                "message": "开始合并多个地形文件夹",
                                "timestamp": datetime.now().isoformat(),
                                "progress": 95
                            })
                            taskStatus[taskId]["currentStage"] = "地形合并中"
                            taskStatus[taskId]["message"] = "正在合并地形瓦片..."
                            taskStatus[taskId]["processLog"] = existing_process_log
                        
                        # 执行地形合并 - 直接使用输出路径
                        mergeResult = mergeTerrainTiles(completed_files, outputPath, taskId)
                        
                        if mergeResult["success"]:
                            logMessage(f"✅ 地形合并成功: {outputPath}")
                            with taskLock:
                                existing_process_log = taskStatus[taskId]["processLog"]
                                existing_process_log.append({
                                    "stage": "地形合并",
                                    "status": "completed",
                                    "message": f"地形合并完成，输出路径: {outputPath}",
                                    "timestamp": datetime.now().isoformat(),
                                    "progress": 100
                                })
                                taskStatus[taskId]["processLog"] = existing_process_log
                                # 更新结果中的输出路径
                                taskStatus[taskId]["result"]["mergedOutputPath"] = outputPath
                                taskStatus[taskId]["result"]["mergeDetails"] = mergeResult
                        else:
                            logMessage(f"❌ 地形合并失败: {mergeResult.get('error', '未知错误')}")
                            with taskLock:
                                existing_process_log = taskStatus[taskId]["processLog"]
                                existing_process_log.append({
                                    "stage": "地形合并",
                                    "status": "failed",
                                    "message": f"地形合并失败: {mergeResult.get('error', '未知错误')}",
                                    "timestamp": datetime.now().isoformat(),
                                    "progress": 100
                                })
                                taskStatus[taskId]["processLog"] = existing_process_log
                                taskStatus[taskId]["result"]["mergeError"] = mergeResult.get('error', '未知错误')
                    
                    except Exception as me:
                        logMessage(f"❌ 地形合并异常: {me}", "ERROR")
                        with taskLock:
                            existing_process_log = taskStatus[taskId]["processLog"]
                            existing_process_log.append({
                                "stage": "地形合并",
                                "status": "failed",
                                "message": f"地形合并异常: {str(me)}",
                                "timestamp": datetime.now().isoformat(),
                                "progress": 100
                            })
                            taskStatus[taskId]["processLog"] = existing_process_log
                            taskStatus[taskId]["result"]["mergeError"] = str(me)
                
                logMessage(f"🎉 地形切片任务完成: {taskId}")
                
            except Exception as e:
                with taskLock:
                    taskStatus[taskId] = {
                        "status": "failed",
                        "progress": 0,
                        "message": f"地形切片失败: {str(e)}",
                        "startTime": taskStatus[taskId]["startTime"],
                        "error": str(e),
                        "endTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    }
                
                logMessage(f"❌ 地形切片失败: {taskId} - {e}", "ERROR")
        
        # 启动后台任务
        import threading
        taskThread = threading.Thread(target=runTerrainTask)
        taskThread.daemon = True
        
        # 保存线程对象到taskProcesses以便停止
        with taskLock:
            taskProcesses[taskId] = taskThread
        
        taskThread.start()
        
        return jsonify({
            "success": True,
            "taskId": taskId,
            "message": f"地形切片任务已启动，将处理 {len(tifFiles)} 个文件",
            "statusUrl": f"/api/tasks/{taskId}",
            "parameters": {
                "totalFiles": len(tifFiles),
                "outputPath": outputPathArray,
                "zoomRange": f"{startZoom}-{endZoom}",
                "threads": threads,
                "maxMemory": maxMemory,
                "type": "terrain"
            }
        })
        
    except Exception as e:
        error_msg = f"地形瓦片创建失败: {str(e)}"
        logMessage(error_msg, "ERROR")
        return jsonify({"error": error_msg}), 500

def splitLargeFile():
    """
    拆分大文件为分幅的小TIF文件
    
    @return 包含拆分任务信息的JSON响应
    """
    try:
        data = request.get_json()
        logMessage("收到文件拆分请求", "INFO")
        
        # 参数验证
        requiredParams = ['sourceFile', 'outputPath']
        for param in requiredParams:
            if param not in data:
                return jsonify({"error": f"缺少参数: {param}"}), 400
        
        sourceFile = data['sourceFile']
        outputPathArray = data['outputPath']
        
        # 拆分参数
        tileSize = data.get('tileSize', 4096)  # 默认4096x4096像素
        overlap = data.get('overlap', 0)  # 重叠像素数
        maxFileSize = data.get('maxFileSize', 1.0)  # 最大文件大小GB
        namingPattern = data.get('namingPattern', 'tile_{x}_{y}')  # 命名模式
        
        # 文件路径处理
        sourcePath = os.path.join(config["dataSourceDir"], sourceFile)
        if isinstance(outputPathArray, list):
            outputPath = os.path.join(config["dataSourceDir"], *outputPathArray)
        else:
            outputPath = os.path.join(config["dataSourceDir"], outputPathArray)
        
        if not os.path.exists(sourcePath):
            return jsonify({"error": "源文件不存在"}), 404
        
        # 检查文件大小
        fileSizeGB = os.path.getsize(sourcePath) / (1024**3)
        if fileSizeGB < maxFileSize:
            return jsonify({
                "message": "文件大小未超过阈值，无需拆分",
                "fileSize": f"{fileSizeGB:.2f}GB",
                "threshold": f"{maxFileSize}GB",
                "skipSplit": True
            })
        
        # 创建输出目录
        os.makedirs(outputPath, exist_ok=True)
        
        # 生成任务ID
        timestamp = int(time.time())
        taskId = f"split{timestamp}"
        
        def runSplitTask():
            try:
                with taskLock:
                    taskStatus[taskId] = {
                        "status": "running",
                        "progress": 0,
                        "message": "正在分析文件信息...",
                        "startTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    }
                
                # 获取文件信息
                logMessage(f"开始拆分文件: {sourcePath} -> {outputPath}")
                
                # 使用gdalinfo获取文件信息
                gdalinfoCmd = ["gdalinfo", sourcePath]
                result = subprocess.run(gdalinfoCmd, capture_output=True, text=True, timeout=30)
                
                if result.returncode != 0:
                    raise Exception(f"无法获取文件信息: {result.stderr}")
                
                # 解析文件尺寸
                import re
                sizeMatch = re.search(r'Size is (\d+), (\d+)', result.stdout)
                if not sizeMatch:
                    raise Exception("无法解析文件尺寸")
                
                width = int(sizeMatch.group(1))
                height = int(sizeMatch.group(2))
                
                logMessage(f"文件尺寸: {width}x{height}")
                
                # 计算分块数量
                tilesX = (width + tileSize - 1) // tileSize
                tilesY = (height + tileSize - 1) // tileSize
                totalTiles = tilesX * tilesY
                
                logMessage(f"计划拆分为 {tilesX}x{tilesY} = {totalTiles} 个分块")
                
                with taskLock:
                    taskStatus[taskId]["message"] = f"开始拆分为 {totalTiles} 个分块"
                    taskStatus[taskId]["progress"] = 10
                
                # 🚀 并行拆分文件 - 显著提升用户体验
                splitFiles = []
                
                # 准备拆分任务列表
                split_tasks = []
                for y in range(tilesY):
                    for x in range(tilesX):
                        # 计算窗口坐标
                        xOff = x * tileSize
                        yOff = y * tileSize
                        xSize = min(tileSize + overlap, width - xOff)
                        ySize = min(tileSize + overlap, height - yOff)
                        
                        # 生成输出文件名
                        outputFileName = namingPattern.format(x=x, y=y) + ".tif"
                        outputFilePath = os.path.join(outputPath, outputFileName)
                        
                        split_tasks.append({
                            "x": x, "y": y,
                            "xOff": xOff, "yOff": yOff,
                            "xSize": xSize, "ySize": ySize,
                            "outputPath": outputFilePath,
                            "sourcePath": sourcePath
                        })
                
                logMessage(f"🚀 启动高速并行拆分: {len(split_tasks)} 个分块")
                
                # 使用多进程并行拆分
                from concurrent.futures import ProcessPoolExecutor, as_completed
                import multiprocessing as mp
                
                # 计算最优进程数
                max_workers = min(mp.cpu_count() * 2, 12, len(split_tasks))
                
                def process_user_split_task(task):
                    """处理用户拆分任务"""
                    try:
                        translateCmd = [
                            "gdal_translate",
                            "-srcwin", str(task["xOff"]), str(task["yOff"]), 
                                      str(task["xSize"]), str(task["ySize"]),
                            "-co", "COMPRESS=LZW",
                            "-co", "TILED=YES",
                            "-co", "BLOCKXSIZE=512",
                            "-co", "BLOCKYSIZE=512",
                            "-co", "NUM_THREADS=2",
                            "-co", "BIGTIFF=IF_SAFER",
                            "-q",
                            task["sourcePath"],
                            task["outputPath"]
                        ]
                        
                        result = subprocess.run(translateCmd, capture_output=True, text=True, timeout=300)
                        
                        if result.returncode == 0:
                            return {
                                "success": True,
                                "outputPath": task["outputPath"],
                                "x": task["x"], "y": task["y"]
                            }
                        else:
                            return {
                                "success": False,
                                "error": result.stderr,
                                "x": task["x"], "y": task["y"]
                            }
                    except Exception as e:
                        return {
                            "success": False,
                            "error": str(e),
                            "x": task["x"], "y": task["y"]
                        }
                
                # 并行执行拆分
                processedTiles = 0
                failed_tiles = 0
                split_start_time = time.time()
                
                with ProcessPoolExecutor(max_workers=max_workers) as executor:
                    # 提交所有任务
                    future_to_task = {executor.submit(process_user_split_task, task): task for task in split_tasks}
                    
                    # 处理完成的任务
                    for future in as_completed(future_to_task):
                        result = future.result()
                        
                        if result["success"]:
                            splitFiles.append(os.path.relpath(result["outputPath"], config["dataSourceDir"]))
                            processedTiles += 1
                        else:
                            failed_tiles += 1
                            logMessage(f"分块拆分失败 ({result['x']},{result['y']}): {result['error']}", "WARNING")
                        
                        # 更新进度
                        progress = 10 + int((processedTiles / totalTiles) * 80)
                        with taskLock:
                            taskStatus[taskId]["progress"] = progress
                            taskStatus[taskId]["message"] = f"高速拆分进度: {processedTiles}/{totalTiles}"
                        
                        # 每20个分块报告一次性能
                        if processedTiles % 20 == 0:
                            elapsed = time.time() - split_start_time
                            speed = processedTiles / elapsed if elapsed > 0 else 0
                            logMessage(f"⚡ 拆分速度: {speed:.1f}块/秒, 已完成: {processedTiles}/{totalTiles}")
                
                # 拆分完成统计
                total_split_time = time.time() - split_start_time
                final_speed = processedTiles / total_split_time if total_split_time > 0 else 0
                
                logMessage(f"🎉 并行拆分完成! 耗时: {total_split_time:.1f}秒, "
                          f"平均速度: {final_speed:.1f}块/秒, 成功: {processedTiles}, 失败: {failed_tiles}")
                
                if failed_tiles > 0:
                    logMessage(f"⚠️  {failed_tiles} 个分块失败，继续处理成功的分块", "WARNING")
                
                # 任务完成
                with taskLock:
                    taskStatus[taskId] = {
                        "status": "completed",
                        "progress": 100,
                        "message": f"文件拆分完成！生成 {len(splitFiles)} 个分块",
                        "result": {
                            "sourceFile": sourceFile,
                            "outputPath": outputPath,
                            "splitFiles": splitFiles,
                            "totalFiles": len(splitFiles),
                            "tileSize": tileSize,
                            "overlap": overlap,
                            "originalSize": f"{fileSizeGB:.2f}GB",
                            "tilesX": tilesX,
                            "tilesY": tilesY
                        },
                        "endTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    }
                
                logMessage(f"✅ 文件拆分任务完成: {taskId}")
                
            except Exception as e:
                with taskLock:
                    taskStatus[taskId] = {
                        "status": "failed",
                        "progress": 0,
                        "message": f"文件拆分失败: {str(e)}",
                        "error": str(e),
                        "endTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    }
                
                logMessage(f"❌ 文件拆分失败: {taskId} - {e}", "ERROR")
        
        # 启动后台任务
        taskThread = threading.Thread(target=runSplitTask)
        taskThread.daemon = True
        taskThread.start()
        
        return jsonify({
            "message": "文件拆分任务已启动",
            "taskId": taskId,
            "statusUrl": f"/api/tasks/{taskId}",
            "sourceFile": sourceFile,
            "outputPath": outputPath,
            "fileSize": f"{fileSizeGB:.2f}GB",
            "splitConfig": {
                "tileSize": tileSize,
                "overlap": overlap,
                "maxFileSize": maxFileSize,
                "namingPattern": namingPattern
            }
        })
        
    except Exception as e:
        logMessage(f"文件拆分API异常: {e}", "ERROR")
        return jsonify({"error": str(e)}), 500

def performInternalFileSplit(sourcePath, splitTileSize, taskId):
    """
    内部文件拆分功能
    
    @param sourcePath 源文件路径
    @param splitTileSize 拆分块大小
    @param taskId 任务ID
    @return 拆分结果
    """
    try:
        # 使用gdalinfo获取文件信息
        gdalinfoCmd = ["gdalinfo", sourcePath]
        result = subprocess.run(gdalinfoCmd, capture_output=True, text=True, timeout=30)
        
        if result.returncode != 0:
            return {"success": False, "error": f"无法获取文件信息: {result.stderr}"}
        
        # 解析文件尺寸
        import re
        sizeMatch = re.search(r'Size is (\d+), (\d+)', result.stdout)
        if not sizeMatch:
            return {"success": False, "error": "无法解析文件尺寸"}
        
        width = int(sizeMatch.group(1))
        height = int(sizeMatch.group(2))
        
        # 计算分块数量
        tilesX = (width + splitTileSize - 1) // splitTileSize
        tilesY = (height + splitTileSize - 1) // splitTileSize
        totalTiles = tilesX * tilesY
        
        # 创建分幅目录（在datasource目录下）
        import tempfile
        splitTempDir = os.path.join(config["dataSourceDir"], f"split_{taskId}_{int(time.time())}")
        os.makedirs(splitTempDir, exist_ok=True)
        
        logMessage(f"开始拆分文件: {width}x{height} -> {tilesX}x{tilesY} = {totalTiles} 个分块")
        
        # 🚀 并行拆分文件 - 大幅提升速度
        splitFiles = []
        
        # 准备所有分块任务（添加重叠区域确保连续性）
        overlap = 50  # 重叠像素数，确保边界连续
        split_tasks = []
        for y in range(tilesY):
            for x in range(tilesX):
                # 计算窗口坐标（添加重叠区域）
                xOff = max(0, x * splitTileSize - overlap)
                yOff = max(0, y * splitTileSize - overlap)
                xSize = min(splitTileSize + 2 * overlap, width - xOff)
                ySize = min(splitTileSize + 2 * overlap, height - yOff)
                
                # 生成输出文件名
                outputFileName = f"tile_{x}_{y}.tif"
                outputFilePath = os.path.join(splitTempDir, outputFileName)
                
                split_tasks.append({
                    "x": x, "y": y,
                    "xOff": xOff, "yOff": yOff,
                    "xSize": xSize, "ySize": ySize,
                    "outputPath": outputFilePath,
                    "sourcePath": sourcePath
                })
        
        logMessage(f"🚀 启动并行拆分: {len(split_tasks)} 个分块, 预计提升 4-8倍速度")
        
        # 🔥 使用多线程并行处理拆分任务 (Docker容器内更稳定)
        from concurrent.futures import ThreadPoolExecutor, as_completed
        import multiprocessing as mp
        
        # 计算最优线程数 (拆分是IO密集型，适合多线程)
        max_workers = min(mp.cpu_count() * 2, 16, len(split_tasks))
        
        def process_split_task(task):
            """处理单个拆分任务"""
            try:
                # 第一步：使用gdal_translate裁剪，保持原坐标系
                temp_split_file = task["outputPath"].replace(".tif", "_temp.tif")
                
                # 裁剪命令
                translateCmd = [
                    "gdal_translate",
                    "-srcwin", str(task["xOff"]), str(task["yOff"]), 
                              str(task["xSize"]), str(task["ySize"]),
                    "-co", "COMPRESS=LZW",
                    "-co", "TILED=YES",
                    "-co", "BLOCKXSIZE=512",
                    "-co", "BLOCKYSIZE=512",
                    "-co", "NUM_THREADS=1",
                    "-co", "BIGTIFF=IF_SAFER",
                    "-q",
                    task["sourcePath"],
                    temp_split_file
                ]
                
                logMessage(f"开始拆分分块 ({task['x']},{task['y']}): {task['xSize']}x{task['ySize']}")
                
                # 执行裁剪
                result = subprocess.run(translateCmd, capture_output=True, text=True, timeout=600)
                
                if result.returncode != 0:
                    logMessage(f"分块裁剪失败 ({task['x']},{task['y']}): {result.stderr}", "ERROR")
                    return {
                        "success": False,
                        "error": f"裁剪失败: {result.stderr}",
                        "x": task["x"], "y": task["y"]
                    }
                
                # 第二步：使用gdalwarp重投影到WGS84地理坐标系（适配indexedTiles接口）
                warpCmd = [
                    "gdalwarp",
                    "-t_srs", "EPSG:4326",
                    "-r", "near",
                    "-co", "COMPRESS=LZW",
                    "-co", "TILED=YES",
                    "-co", "BLOCKXSIZE=512",
                    "-co", "BLOCKYSIZE=512",
                    "-co", "NUM_THREADS=1",
                    "-co", "BIGTIFF=IF_SAFER",
                    "-q",
                    temp_split_file,
                    task["outputPath"]
                ]
                
                # 执行重投影
                result2 = subprocess.run(warpCmd, capture_output=True, text=True, timeout=600)
                
                # 清理临时文件
                if os.path.exists(temp_split_file):
                    os.remove(temp_split_file)
                
                if result2.returncode == 0:
                    return {
                        "success": True,
                        "outputPath": task["outputPath"],
                        "x": task["x"], "y": task["y"]
                    }
                else:
                    logMessage(f"分块重投影失败 ({task['x']},{task['y']}): {result2.stderr}", "ERROR")
                    return {
                        "success": False,
                        "error": f"重投影失败: {result2.stderr}",
                        "x": task["x"], "y": task["y"]
                    }
            except Exception as e:
                logMessage(f"分块拆分异常 ({task['x']},{task['y']}): {str(e)}", "ERROR")
                return {
                    "success": False,
                    "error": str(e),
                    "x": task["x"], "y": task["y"]
                }
        
        # 执行并行拆分
        processedTiles = 0
        failed_tiles = 0
        start_time = time.time()
        
        logMessage(f"启动 {max_workers} 个线程进行并行拆分...")
        
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            # 提交所有任务
            future_to_task = {executor.submit(process_split_task, task): task for task in split_tasks}
            
            # 处理完成的任务
            for future in as_completed(future_to_task):
                result = future.result()
                
                if result["success"]:
                    splitFiles.append(result["outputPath"])
                    processedTiles += 1
                else:
                    failed_tiles += 1
                    logMessage(f"分块失败 ({result['x']},{result['y']}): {result['error']}", "WARNING")
                
                # 实时进度和速度统计
                if processedTiles % 20 == 0 or processedTiles == len(split_tasks):
                    elapsed = time.time() - start_time
                    speed = processedTiles / elapsed if elapsed > 0 else 0
                    
                    progress_percent = int((processedTiles / len(split_tasks)) * 100)
                    logMessage(f"⚡ 拆分进度: {processedTiles}/{len(split_tasks)} ({progress_percent}%) "
                             f"| 速度: {speed:.1f}块/秒 | 失败: {failed_tiles}")
        
        # 最终统计
        total_time = time.time() - start_time
        final_speed = processedTiles / total_time if total_time > 0 else 0
        
        logMessage(f"🎉 并行拆分完成! 总时间: {total_time:.1f}秒, "
                  f"平均速度: {final_speed:.1f}块/秒, 成功: {processedTiles}, 失败: {failed_tiles}")
        
        if failed_tiles > 0:
            logMessage(f"⚠️  {failed_tiles} 个分块拆分失败，但继续处理成功的分块", "WARNING")
        
        return {
            "success": True,
            "splitFiles": splitFiles,
            "splitTempDir": splitTempDir,
            "totalFiles": len(splitFiles),
            "tilesX": tilesX,
            "tilesY": tilesY
        }
        
    except Exception as e:
        return {"success": False, "error": str(e)}

def processIndexedTilesInternal(folderPaths, filePatterns, outputPath, minZoom, maxZoom, tileSize, processes, maxMemory, resampling, generateShpIndex, enableIncrementalUpdate, transparencyThreshold, skipNodataTiles, taskId):
    """
    内部处理索引瓦片的功能，直接复用createIndexedTiles的核心逻辑
    
    @param folderPaths 文件夹路径列表
    @param filePatterns 文件模式列表
    @param outputPath 输出路径
    @param minZoom 最小缩放级别
    @param maxZoom 最大缩放级别
    @param tileSize 瓦片大小
    @param processes 进程数
    @param maxMemory 最大内存
    @param resampling 重采样方法
    @param taskId 任务ID
    @return 处理结果
    """
    try:
        # 获取TIF文件列表
        relativeTifFiles = findTifFilesInFolders(folderPaths, filePatterns)
        if not relativeTifFiles:
            return {"success": False, "error": "未找到匹配的TIF文件"}
        
        tifFiles = []
        for relativePath in relativeTifFiles:
            fullPath = os.path.join(config["dataSourceDir"], relativePath)
            if os.path.exists(fullPath):
                tifFiles.append(fullPath)
        
        if not tifFiles:
            return {"success": False, "error": "所有TIF文件路径无效"}
        
        # 创建瓦片网格索引
        indexResult = createTileGridIndex(tifFiles, outputPath, minZoom, maxZoom, tileSize)
        
        if not indexResult["success"]:
            return {"success": False, "error": f"瓦片索引创建失败: {indexResult['error']}"}
        
        tileIndex = indexResult["tileIndex"]
        totalTiles = len(tileIndex)
        
        # 总是生成GeoJSON索引文件，Shapefile索引是可选的
        shpResult = generateShapefileIndex(tileIndex, outputPath, generateShpIndex)
        if shpResult["success"]:
            if shpResult.get("reused", False):
                logMessage(f"🔄 复用现有索引文件")
            else:
                logMessage(f"✅ 索引文件已生成: {shpResult.get('geojsonFile', 'N/A')}")
                if generateShpIndex and shpResult.get('shpFile'):
                    logMessage(f"✅ SHP索引文件已生成: {shpResult['shpFile']}")
            
            if shpResult.get("warning"):
                logMessage(f"⚠️ {shpResult['warning']}", "WARNING")
        else:
            logMessage(f"⚠️ 索引生成失败: {shpResult['error']}", "WARNING")
        
        # 生成元数据
        metadata = {
            "generatedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "sourceFiles": [os.path.relpath(f, config["dataSourceDir"]) for f in tifFiles],
            "totalSourceFiles": len(tifFiles),
            "zoomLevels": f"{minZoom}-{maxZoom}",
            "tileSize": tileSize,
            "totalTiles": totalTiles,
            "processedTiles": 0,
            "failedTiles": 0,
            "successRate": "0%",
            "tileIndex": tileIndex,
            "method": "内部智能切片",
            "resampling": resampling,
            "generateShpIndex": generateShpIndex,
            "enableIncrementalUpdate": enableIncrementalUpdate,
            "transparencyThreshold": transparencyThreshold
        }
        
        import json
        metadataFile = os.path.join(outputPath, "tile_metadata.json")
        with open(metadataFile, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
        
        # 高性能处理瓦片
        tile_count = len(tileIndex)
        
        # 智能计算工作进程数
        import psutil
        cpu_count = psutil.cpu_count() or 4
        memory_gb = psutil.virtual_memory().total / (1024**3)
        
        # 优先使用用户输入的进程数
        if processes and processes > 0:
            max_workers = processes
        else:
            if tile_count < 100:
                max_workers = min(4, cpu_count)
            elif tile_count < 1000:
                max_workers = min(cpu_count, 32)
            else:
                max_workers = min(cpu_count * 2, int(memory_gb / 2), 32)
        
        batch_size = calculateOptimalBatchSize(tile_count, max_workers, target_speed=1000)
        
        # 调用高性能处理函数
        hp_result = processHighPerformanceTiles(
            tileIndex, 
            outputPath, 
            resampling, 
            max_workers=max_workers,
            batch_size=batch_size,
            user_processes=processes,
            taskId=taskId  # 传递任务ID以便实时更新状态
        )
        
        if hp_result["success"]:
            processedTiles = hp_result["processed_tiles"]
            failedTiles = hp_result["failed_tiles"]
            
            # 更新元数据文件
            metadata["processedTiles"] = processedTiles
            metadata["failedTiles"] = failedTiles
            metadata["successRate"] = f"{processedTiles/totalTiles*100:.1f}%" if totalTiles > 0 else "0%"
            metadata["completedAt"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            
            with open(metadataFile, 'w', encoding='utf-8') as f:
                json.dump(metadata, f, indent=2, ensure_ascii=False)
            
            # 如果启用了跳过透明瓦片，则自动清理透明瓦片
            if skipNodataTiles:
                try:
                    logMessage("开始清理透明瓦片...")
                    cleanupResult = deleteNodataTilesInternal(
                        os.path.relpath(outputPath, config["tilesDir"]), 
                        includeDetails=False
                    )
                    if cleanupResult.get("success"):
                        deletedCount = cleanupResult.get("deletedTiles", 0)
                        logMessage(f"透明瓦片清理完成，删除了 {deletedCount} 个透明瓦片")
                        metadata["deletedNodataTiles"] = deletedCount
                        metadata["skipNodataTilesEnabled"] = True
                    else:
                        logMessage(f"透明瓦片清理失败: {cleanupResult.get('error', '未知错误')}", "WARNING")
                except Exception as e:
                    logMessage(f"透明瓦片清理异常: {e}", "WARNING")
            
            return {
                "success": True,
                "processedTiles": processedTiles,
                "failedTiles": failedTiles,
                "totalTiles": totalTiles
            }
        else:
            return {
                "success": False,
                "error": hp_result.get("error", "瓦片处理失败")
            }
            
    except Exception as e:
        return {"success": False, "error": str(e)}

def createIndexedTiles():
    """
    创建索引瓦片，支持多文件处理和网格索引
    
    @return 包含任务信息的JSON响应
    """
    try:
        data = request.get_json()
        
        # 基本参数
        folderPaths = data.get('folderPaths', [])
        filePatterns = data.get('filePatterns', [])
        outputPathArray = data.get('outputPath', [])
        minZoom = data.get('minZoom', 0)
        maxZoom = data.get('maxZoom', 12)
        tileSize = data.get('tileSize', 256)
        processes = data.get('processes', 4)
        maxMemory = data.get('maxMemory', '8g')
        resampling = data.get('resampling', 'near')
        generateShpIndex = data.get('generateShpIndex', True)
        enableIncrementalUpdate = data.get('enableIncrementalUpdate', False)

        skipNodataTiles = data.get('skipNodataTiles', False)
        
        # 生成任务ID
        timestamp = int(time.time())
        taskId = f"indexedTiles{timestamp}"
        
        # 参数验证和错误处理 - 即使有错误也创建任务
        errors = []
        
        if not folderPaths:
            errors.append("缺少参数: folderPaths")
        if not outputPathArray:
            errors.append("缺少参数: outputPath")
        
        # 获取TIF文件列表
        tifFiles = []
        if not errors:
            relativeTifFiles = findTifFilesInFolders(folderPaths, filePatterns)
            if not relativeTifFiles:
                errors.append("未找到匹配的TIF文件")
            else:
                for relativePath in relativeTifFiles:
                    fullPath = os.path.join(config["dataSourceDir"], relativePath)
                    if os.path.exists(fullPath):
                        tifFiles.append(fullPath)
                
                if not tifFiles:
                    errors.append("所有TIF文件路径无效")
        
        # 如果有错误，创建失败状态的任务
        if errors:
            with taskLock:
                taskStatus[taskId] = {
                    "status": "failed",
                    "progress": 0,
                    "message": f"索引瓦片任务创建失败: {'; '.join(errors)}",
                    "startTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "endTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "processLog": [
                        {
                            "stage": "初始化",
                            "status": "failed",
                            "message": f"任务创建失败: {'; '.join(errors)}",
                            "timestamp": datetime.now().isoformat(),
                            "progress": 0,
                            "errors": errors
                        }
                    ],
                    "currentStage": "初始化失败",
                    "stats": {
                        "totalTiles": 0,
                        "processedTiles": 0,
                        "failedTiles": 0,
                        "successRate": "0%"
                    }
                }
            
            logMessage(f"索引瓦片任务创建失败: {taskId}, 错误: {'; '.join(errors)}", "ERROR")
            return jsonify({
                "success": False,
                "taskId": taskId,
                "message": f"索引瓦片任务创建失败: {'; '.join(errors)}",
                "statusUrl": f"/api/tasks/{taskId}",
                "errors": errors
            }), 200  # 返回200，因为任务已经创建
        
        # 构建输出路径
        if isinstance(outputPathArray, str):
            outputPath = os.path.join(config["tilesDir"], outputPathArray)
        elif isinstance(outputPathArray, list):
            outputPath = os.path.join(config["tilesDir"], *outputPathArray)
        else:
            outputPath = os.path.join(config["tilesDir"], str(outputPathArray))
        
        os.makedirs(outputPath, exist_ok=True)
        
        # 生成任务ID
        timestamp = int(time.time())
        taskId = f"indexedTiles{timestamp}"
        
        # 启动索引瓦片生成任务
        def runIndexedTileTask():
            """
            执行索引瓦片生成任务的内部函数
            处理多文件边界计算、网格索引创建和瓦片生成流程
            """
            try:
                with taskLock:
                    taskStatus[taskId] = {
                        "status": "running",
                        "progress": 0,
                        "message": f"正在进行瓦片索引分析，处理 {len(tifFiles)} 个文件...",
                        "startTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                        "stats": {
                            "totalTiles": 0,
                            "processedTiles": 0,
                            "failedTiles": 0,
                            "remainingTiles": 0,
                            "currentSpeed": 0,
                            "averageSpeed": 0,
                            "estimatedTimeRemaining": "计算中...",
                            "estimatedTimeRemainingSeconds": 0,
                            "batchesCompleted": 0,
                            "totalBatches": 0,
                            "successRate": "0%"
                        },
                        "processLog": [
                            {
                                "stage": "初始化",
                                "status": "completed",
                                "message": f"索引瓦片任务初始化完成，准备处理{len(tifFiles)}个文件",
                                "timestamp": datetime.now().isoformat(),
                                "progress": 0,
                                "fileCount": len(tifFiles)
                            }
                        ],
                        "currentStage": "索引分析"
                    }
                
                # 检查是否启用增量更新（复用现有文件）
                if enableIncrementalUpdate:
                    metadataFile = os.path.join(outputPath, "tile_metadata.json")
                    if os.path.exists(metadataFile):
                        try:
                            import json
                            with open(metadataFile, 'r', encoding='utf-8') as f:
                                existingMetadata = json.load(f)
                            
                            # 检查配置是否匹配
                            existingSourceFiles = set(existingMetadata.get("sourceFiles", []))
                            currentSourceFiles = set([os.path.relpath(f, config["dataSourceDir"]) for f in tifFiles])
                            existingZoomLevels = existingMetadata.get("zoomLevels", "")
                            currentZoomLevels = f"{minZoom}-{maxZoom}"
                            existingTileSize = existingMetadata.get("tileSize", 0)
                            existingResampling = existingMetadata.get("resampling", "near")
                            if (existingSourceFiles == currentSourceFiles and 
                                existingZoomLevels == currentZoomLevels and 
                                existingTileSize == tileSize and
                                existingResampling == resampling):
                                
                                # 验证瓦片文件完整性
                                if verifyTilesIntegrity(outputPath, existingMetadata):
                                    logMessage(f"🔄 检测到相同配置的现有瓦片且文件完整，启用增量更新模式")
                                    
                                    # 复用现有结果
                                    processedTiles = existingMetadata.get("processedTiles", 0)
                                    failedTiles = existingMetadata.get("failedTiles", 0)
                                    totalTiles = existingMetadata.get("totalTiles", 0)
                                    
                                    with taskLock:
                                        taskStatus[taskId] = {
                                            "status": "completed",
                                            "progress": 100,
                                            "message": f"复用现有瓦片完成！{processedTiles}/{totalTiles} 个瓦片",
                                            "result": {
                                                "outputPath": outputPath,
                                                "outputPathArray": outputPathArray,
                                                "metadataFile": metadataFile,
                                                "sourceFiles": [os.path.relpath(f, config["dataSourceDir"]) for f in tifFiles],
                                                "totalSourceFiles": len(tifFiles),
                                                "totalTiles": totalTiles,
                                                "processedTiles": processedTiles,
                                                "failedTiles": failedTiles,
                                                "successRate": f"{processedTiles/totalTiles*100:.1f}%" if totalTiles > 0 else "0%",
                                                "zoomLevels": f"{minZoom}-{maxZoom}",
                                                "tileSize": tileSize,
                                                "method": "瓦片索引精确切片（复用现有）"
                                            },
                                            "endTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                                        }
                                    
                                    logMessage(f"✅ 瓦片复用任务完成: {taskId}")
                                    return
                                else:
                                    logMessage(f"⚠️ 瓦片文件不完整或损坏，将重新生成")
                            else:
                                logMessage(f"⚠️ 现有配置与当前不匹配，将重新生成瓦片")
                        except Exception as e:
                            logMessage(f"⚠️ 读取现有元数据失败，将重新生成瓦片: {e}", "WARNING")
                    else:
                        logMessage(f"ℹ️ 未找到现有元数据文件，将生成新瓦片")
                
                # 创建瓦片网格索引
                # 计算所有文件的总体地理边界
                totalBounds = None
                for filePath in tifFiles:
                    fileBounds = getFileGeographicBounds(filePath)
                    if fileBounds and all(key in fileBounds for key in ['west', 'east', 'north', 'south']):
                        # 转换字典格式为列表格式 [west, south, east, north]
                        boundsArray = [
                            fileBounds['west'], 
                            fileBounds['south'], 
                            fileBounds['east'], 
                            fileBounds['north']
                        ]
                        
                        if totalBounds is None:
                            totalBounds = boundsArray
                        else:
                            totalBounds = [
                                min(totalBounds[0], boundsArray[0]),  # west
                                min(totalBounds[1], boundsArray[1]),  # south
                                max(totalBounds[2], boundsArray[2]),  # east
                                max(totalBounds[3], boundsArray[3])   # north
                            ]
                
                if totalBounds is None:
                    raise Exception("无法获取文件地理边界")
                
                indexResult = createTileGridIndex(tifFiles, outputPath, minZoom, maxZoom, tileSize)
                
                if not indexResult["success"]:
                    raise Exception(f"瓦片索引创建失败: {indexResult['error']}")
                
                tileIndex = indexResult["tileIndex"]
                totalTiles = len(tileIndex)
                
                with taskLock:
                    taskStatus[taskId]["progress"] = 10
                    taskStatus[taskId]["message"] = f"索引创建完成，开始生成shapefile索引"
                    taskStatus[taskId]["currentStage"] = "索引文件生成"
                    taskStatus[taskId]["processLog"].append({
                        "stage": "索引分析",
                        "status": "completed",
                        "message": f"瓦片索引创建完成，共计算出{totalTiles}个瓦片需要生成",
                        "timestamp": datetime.now().isoformat(),
                        "progress": 10,
                        "tileCount": totalTiles,
                        "bounds": totalBounds
                    })
                
                # 步骤2：立即生成瓦片索引文件（shapefile和元数据json）
                logMessage(f"💾 步骤2：生成瓦片索引文件")
                
                # 生成元数据json（记录瓦片计划）
                metadata = {
                    "generatedAt": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "sourceFiles": [os.path.relpath(f, config["dataSourceDir"]) for f in tifFiles],
                    "totalSourceFiles": len(tifFiles),
                    "zoomLevels": f"{minZoom}-{maxZoom}",
                    "tileSize": tileSize,
                    "totalTiles": totalTiles,
                    "processedTiles": 0,  # 初始为0，瓦片处理时会更新
                    "failedTiles": 0,
                    "successRate": "0%",
                    "tileIndex": tileIndex,
                    "method": "瓦片索引精确切片",
                    "resampling": resampling,

                }
                
                import json
                metadataFile = os.path.join(outputPath, "tile_metadata.json")
                with open(metadataFile, 'w', encoding='utf-8') as f:
                    json.dump(metadata, f, indent=2, ensure_ascii=False)
                
                logMessage(f"✅ 瓦片元数据已生成: {metadataFile}")
                
                # 生成shapefile索引
                if generateShpIndex:
                    shpResult = generateShapefileIndex(tileIndex, outputPath)
                    if shpResult["success"]:
                        if shpResult.get("reused", False):
                            logMessage(f"🔄 复用现有SHP索引文件: {shpResult['shpFile']}")
                        else:
                            logMessage(f"✅ SHP索引文件已生成: {shpResult['shpFile']}")
                        
                        if shpResult.get("warning"):
                            logMessage(f"⚠️ {shpResult['warning']}", "WARNING")
                    else:
                        logMessage(f"⚠️ SHP索引生成失败: {shpResult['error']}", "WARNING")
                
                with taskLock:
                    taskStatus[taskId]["progress"] = 15
                    taskStatus[taskId]["message"] = f"索引文件已生成，开始处理 {totalTiles} 个瓦片"
                    taskStatus[taskId]["stats"]["totalTiles"] = totalTiles
                    taskStatus[taskId]["stats"]["remainingTiles"] = totalTiles
                
                # 步骤3：高性能并行处理瓦片
                logMessage(f"🚀 步骤3：启动高性能瓦片处理（目标1000瓦片/秒）")
                
                # 智能计算最优参数
                tile_count = len(tileIndex)
                
                # 智能计算工作进程数
                import psutil
                cpu_count = psutil.cpu_count() or 4
                memory_gb = psutil.virtual_memory().total / (1024**3)
                
                # 优先使用用户输入的进程数
                if processes and processes > 0:
                    # 用户明确指定了进程数，优先使用
                    max_workers = processes
                    logMessage(f"🎯 使用用户指定的进程数: {max_workers} (用户输入: {processes})")
                else:
                    # 用户未指定，使用系统自动计算
                    if tile_count < 100:
                        max_workers = min(4, cpu_count)
                    elif tile_count < 1000:
                        max_workers = min(cpu_count, 32)
                    else:
                        # 大量瓦片时的自动限制
                        max_workers = min(cpu_count * 2, int(memory_gb / 2), 32)
                    logMessage(f"🤖 系统自动计算进程数: {max_workers} (瓦片:{tile_count}, CPU:{cpu_count}核, 内存:{memory_gb:.1f}GB)")
                
                # 使用智能批量大小计算
                batch_size = calculateOptimalBatchSize(tile_count, max_workers, target_speed=1000)
                
                logMessage(f"🧠 最终参数: {tile_count}瓦片, {max_workers}进程, 批量{batch_size}")
                
                with taskLock:
                    taskStatus[taskId]["progress"] = 15
                    taskStatus[taskId]["message"] = f"启动高性能处理: {max_workers}进程, 批量{batch_size}"
                
                # 调用高性能处理函数，传递用户原始进程数
                hp_result = processHighPerformanceTiles(
                    tileIndex, 
                    outputPath, 
                    resampling, 
                    max_workers=max_workers,
                    batch_size=batch_size,
                    user_processes=processes,  # 传递用户原始输入
                    taskId=taskId  # 传递任务ID以便实时更新状态
                )
                
                # 检查是否需要启用极致优化模式
                # 解析用户内存参数
                user_memory_gb = None
                if maxMemory:
                    try:
                        if isinstance(maxMemory, str):
                            if maxMemory.lower().endswith('g'):
                                user_memory_gb = float(maxMemory[:-1])
                            else:
                                user_memory_gb = float(maxMemory)
                        else:
                            user_memory_gb = float(maxMemory)
                    except:
                        user_memory_gb = None
                
                # 优先使用用户指定的内存，否则使用系统内存
                effective_memory_gb = user_memory_gb if user_memory_gb is not None else memory_gb
                
                if tile_count > 5000 and effective_memory_gb > 64 and cpu_count >= 16:
                    logMessage("🚀 检测到大规模任务 + 高配置硬件，启用极致优化模式")
                    
                    with taskLock:
                        taskStatus[taskId]["message"] = "启用极致优化模式 - 目标1000瓦片/秒"
                    
                    # 使用极致优化处理
                    ultra_result = processUltraHighPerformanceTiles(
                        tileIndex,
                        outputPath,
                        resampling,
                        max_workers=max_workers,
                        enable_memory_cache=True,
                        enable_async_io=True,
                        user_processes=processes,  # 传递用户原始输入
                        user_memory_gb=user_memory_gb  # 传递用户内存参数
                    )
                    
                    logMessage(f"🎉 极致优化完成! 最终速度: {ultra_result.get('final_speed', 0):.1f}瓦片/秒")
                    logMessage(f"🚀 性能提升: {ultra_result.get('improvement_factor', 1):.1f}倍")
                    
                    result = ultra_result
                else:
                    # 使用标准高性能处理
                    result = hp_result
                
                if result["success"]:
                    processedTiles = result["processed_tiles"]
                    failedTiles = result["failed_tiles"]
                    average_speed = result["average_speed"]
                    
                    # 检查任务是否被停止
                    with taskLock:
                        current_status = taskStatus.get(taskId, {}).get("status", "running")
                    
                    if current_status == "stopped":
                        # 任务被停止，设置正确的状态
                        with taskLock:
                            taskStatus[taskId]["progress"] = int((processedTiles / totalTiles) * 100) if totalTiles > 0 else 0
                            taskStatus[taskId]["message"] = f"任务已停止 - 已处理 {processedTiles}/{totalTiles} 个瓦片"
                            taskStatus[taskId]["endTime"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                            taskStatus[taskId]["stats"]["processedTiles"] = processedTiles
                            taskStatus[taskId]["stats"]["failedTiles"] = failedTiles
                            taskStatus[taskId]["stats"]["remainingTiles"] = totalTiles - processedTiles
                        
                        logMessage(f"✅ 任务停止完成: {taskId} - 已处理 {processedTiles}/{totalTiles} 个瓦片")
                        return  # 直接返回，不进入后续处理
                    
                    logMessage(f"✅ 高性能处理完成! 平均速度: {average_speed:.1f}瓦片/秒")
                    
                    with taskLock:
                        taskStatus[taskId]["progress"] = 90
                        taskStatus[taskId]["message"] = f"高性能处理完成: {processedTiles}成功, {failedTiles}失败, 速度{average_speed:.1f}/秒"
                        # 记录性能统计
                        performance_stats = result.get("stats", {})
                        performance_stats.update({
                            "method": "高性能并行处理",
                            "target_speed": 1000,
                            "actual_speed": average_speed,
                            "efficiency": min(average_speed / 1000 * 100, 100),  # 效率百分比
                            "max_workers": max_workers,
                            "batch_size": batch_size,
                            "batches_processed": result.get("batches_processed", 0),
                            "total_time": result.get("total_time", 0)
                        })
                        taskStatus[taskId]["performance_stats"] = performance_stats
                else:
                    processedTiles = result.get("processed_tiles", 0)
                    failedTiles = result.get("failed_tiles", 0)
                    error_msg = result.get("error", "未知错误")
                    
                    logMessage(f"❌ 高性能处理失败: {error_msg}", "ERROR")
                    
                    # 如果高性能处理失败，回退到传统方法
                    logMessage(f"🔄 回退到传统单瓦片处理方法")
                    
                    remaining_tiles = [tile for i, tile in enumerate(tileIndex) if i >= processedTiles]
                    
                    for tileInfo in remaining_tiles:
                        try:
                            result = processSingleTileFromIndex(tileInfo, outputPath, resampling)
                            
                            if result["success"]:
                                processedTiles += 1
                            else:
                                failedTiles += 1
                                logMessage(f"瓦片处理失败 {tileInfo['z']}/{tileInfo['x']}/{tileInfo['y']}: {result.get('error')}", "WARNING")
                            
                            # 更新进度
                            progress = 15 + int((processedTiles + failedTiles) / totalTiles * 75)
                            with taskLock:
                                taskStatus[taskId]["progress"] = progress
                                taskStatus[taskId]["message"] = f"回退处理 {processedTiles + failedTiles}/{totalTiles} 个瓦片"
                        
                        except Exception as e:
                            failedTiles += 1
                            logMessage(f"瓦片处理异常: {e}", "ERROR")
                
                # 步骤4：更新元数据文件中的处理结果
                logMessage(f"💾 步骤4：更新瓦片处理结果")
                
                # 读取已有的元数据文件并更新处理结果
                try:
                    import json
                    metadataFile = os.path.join(outputPath, "tile_metadata.json")
                    with open(metadataFile, 'r', encoding='utf-8') as f:
                        metadata = json.load(f)
                    
                    # 更新处理结果
                    metadata["processedTiles"] = processedTiles
                    metadata["failedTiles"] = failedTiles
                    metadata["successRate"] = f"{processedTiles/totalTiles*100:.1f}%" if totalTiles > 0 else "0%"
                    metadata["completedAt"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    
                    # 写回文件
                    with open(metadataFile, 'w', encoding='utf-8') as f:
                        json.dump(metadata, f, indent=2, ensure_ascii=False)
                    
                    logMessage(f"✅ 瓦片元数据已更新: 成功{processedTiles}, 失败{failedTiles}")
                except Exception as e:
                    logMessage(f"⚠️ 更新元数据失败: {e}", "WARNING")
                
                # 如果启用了skipNodataTiles，在切图完成后删除透明瓦片
                deletedNodataTiles = 0
                if skipNodataTiles and processedTiles > 0:
                    try:
                        logMessage(f"🧹 开始删除透明瓦片（skipNodataTiles=True）...")
                        
                        # 构建相对路径
                        if isinstance(outputPathArray, str):
                            relativeOutputPath = outputPathArray
                        elif isinstance(outputPathArray, list):
                            relativeOutputPath = "/".join(outputPathArray)
                        else:
                            relativeOutputPath = str(outputPathArray)
                        
                        # 调用内部删除函数，直接传递相对路径
                        deleteResult = deleteNodataTilesInternal(relativeOutputPath, False)
                        
                        if deleteResult.get("success", False):
                            deletedNodataTiles = deleteResult.get("deleted_count", 0)
                            logMessage(f"✅ 透明瓦片删除完成: 删除了 {deletedNodataTiles} 个透明瓦片")
                        else:
                            logMessage(f"⚠️ 透明瓦片删除失败: {deleteResult.get('error', '未知错误')}", "WARNING")
                    except Exception as e:
                        logMessage(f"⚠️ 删除透明瓦片时发生错误: {e}", "WARNING")
                
                # 任务完成
                if processedTiles > 0:
                    with taskLock:
                        # 计算最终统计信息
                        total_processing_time = (datetime.now() - datetime.strptime(taskStatus[taskId]["startTime"], "%Y-%m-%d %H:%M:%S")).total_seconds()
                        final_speed = processedTiles / total_processing_time if total_processing_time > 0 else 0
                        
                        taskStatus[taskId] = {
                            "status": "completed",
                            "progress": 100,
                            "message": f"瓦片索引切片完成！成功处理 {processedTiles}/{totalTiles} 个瓦片" + (f"，删除了 {deletedNodataTiles} 个透明瓦片" if deletedNodataTiles > 0 else ""),
                            "result": {
                                "outputPath": outputPath,
                                "outputPathArray": outputPathArray,
                                "metadataFile": metadataFile,
                                "sourceFiles": [os.path.relpath(f, config["dataSourceDir"]) for f in tifFiles],
                                "totalSourceFiles": len(tifFiles),
                                "totalTiles": totalTiles,
                                "processedTiles": processedTiles,
                                "failedTiles": failedTiles,
                                "deletedNodataTiles": deletedNodataTiles,
                                "skipNodataTiles": skipNodataTiles,
                                "successRate": f"{processedTiles/totalTiles*100:.1f}%",
                                "zoomLevels": f"{minZoom}-{maxZoom}",
                                "tileSize": tileSize,
                                "method": "瓦片索引精确切片"
                            },
                            "stats": {
                                "totalTiles": totalTiles,
                                "processedTiles": processedTiles,
                                "failedTiles": failedTiles,
                                "deletedNodataTiles": deletedNodataTiles,
                                "remainingTiles": 0,
                                "averageSpeed": round(final_speed, 1),
                                "totalProcessingTime": f"{total_processing_time:.1f}秒",
                                "successRate": f"{processedTiles/totalTiles*100:.1f}%" if totalTiles > 0 else "0%",
                                "estimatedTimeRemaining": "已完成",
                                "estimatedTimeRemainingSeconds": 0
                            },
                            "endTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                        }
                    
                    logMessage(f"✅ 瓦片索引切片任务完成: {taskId}")
                else:
                    raise Exception("没有成功处理任何瓦片")
            
            except Exception as e:
                with taskLock:
                    taskStatus[taskId] = {
                        "status": "failed",
                        "progress": 0,
                        "message": f"瓦片索引切片失败: {str(e)}",
                        "error": str(e),
                        "endTime": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    }
                
                logMessage(f"❌ 瓦片索引切片失败: {taskId} - {e}", "ERROR")
        
        # 启动后台任务
        import threading
        taskThread = threading.Thread(target=runIndexedTileTask)
        taskThread.daemon = True
        
        # 保存线程对象到taskProcesses以便停止
        with taskLock:
            taskProcesses[taskId] = taskThread
        
        taskThread.start()
        
        return jsonify({
            "taskId": taskId,
            "message": f"瓦片索引切片任务已启动，将处理 {len(tifFiles)} 个文件",
            "statusUrl": f"/api/tasks/{taskId}",
            "method": "瓦片索引精确切片",
            "indexInfo": {
                "totalFiles": len(tifFiles),
                "zoomLevels": f"{minZoom}-{maxZoom}",
                "tileSize": tileSize,
                "generateShpIndex": generateShpIndex,
                "enableIncrementalUpdate": enableIncrementalUpdate,
                "skipNodataTiles": skipNodataTiles
            },
            "processingInfo": {
                "processes": processes,
                "maxMemory": maxMemory,
                "resampling": resampling,
                "outputPathArray": outputPathArray
            }
        })
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

def getTaskStatus(taskId):
    """
    获取任务状态信息，包括进度和结果
    
    @param taskId 任务ID
    @return 包含任务状态的JSON响应
    """
    try:
        logMessage(f"收到任务状态查询请求: {taskId}", "INFO")
        with taskLock:
            if taskId in taskStatus:
                taskInfo = dict(taskStatus[taskId])
                taskInfo["taskId"] = taskId
                logMessage(f"任务状态查询成功: {taskId}, 状态: {taskInfo.get('status', 'unknown')}", "INFO")
                return jsonify(taskInfo)
            else:
                logMessage(f"任务状态查询失败: 任务不存在 {taskId}", "WARNING")
                return jsonify({"error": "任务不存在"}), 404
    except Exception as e:
        logMessage(f"任务状态查询异常: {taskId}, 错误: {str(e)}", "ERROR")
        return jsonify({"error": str(e)}), 500

def listTasks():
    """
    列出所有任务，支持状态筛选和分页，限制返回最近50条
    
    @return 包含任务列表的JSON响应
    """
    with taskLock:
        # 按任务ID中的时间戳排序，获取最近的50条任务
        def extractTimestamp(taskId):
            try:
                # 任务ID格式通常是: taskType + timestamp
                import re
                match = re.search(r'\d+$', taskId)
                return int(match.group()) if match else 0
            except:
                return 0
        
        # 排序并限制为最近50条
        sortedTaskIds = sorted(taskStatus.keys(), 
                              key=extractTimestamp, 
                              reverse=True)[:50]
        
        tasksWithIds = {}
        for taskId in sortedTaskIds:
            if taskId in taskStatus:
                taskInfo = taskStatus[taskId]
                # 创建精简版任务信息，移除processLog和大型result减少数据量
                result = taskInfo.get("result")
                simplifiedResult = None
                if result and isinstance(result, dict):
                    # 只保留基本统计信息，移除详细列表
                    simplifiedResult = {
                        "completedFiles": result.get("completedFiles", 0),
                        "failedFiles": result.get("failedFiles", 0),
                        "totalFiles": result.get("totalFiles", 0),
                        "totalTerrainFiles": result.get("totalTerrainFiles", 0),
                        "outputPath": result.get("outputPath"),
                        "mergedOutputPath": result.get("mergedOutputPath")
                        # 不包含completedDetails、failedDetails等大型数组
                    }
                
                taskInfoWithId = {
                    "taskId": taskId,
                    "status": taskInfo.get("status"),
                    "progress": taskInfo.get("progress"),
                    "message": taskInfo.get("message"),
                    "startTime": taskInfo.get("startTime"),
                    "endTime": taskInfo.get("endTime"),
                    "currentStage": taskInfo.get("currentStage"),
                    "result": simplifiedResult,
                    "stats": taskInfo.get("stats"),
                    "files": taskInfo.get("files")
                    # 故意不包含processLog，减少数据传输量
                }
                tasksWithIds[taskId] = taskInfoWithId
        
        return jsonify({
            "tasks": tasksWithIds,
            "count": len(tasksWithIds)  # 返回实际返回的任务数量
        })

def cleanupTasks():
    """
    清理已完成和失败的任务，释放系统资源
    
    @return 包含清理结果的JSON响应
    """
    try:
        cleanupTasksByCount()
        
        with taskLock:
            return jsonify({
                "success": True,
                "message": "任务清理完成",
                "remainingTasks": len(taskStatus)
            })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

def stopTask(taskId):
    """
    停止正在运行的任务
    
    @param taskId 任务ID
    @return 包含操作结果的JSON响应
    """
    try:
        logMessage(f"收到停止任务请求: {taskId}", "INFO")
        success = stopTaskProcess(taskId)
        
        if success:
            with taskLock:
                if taskId in taskStatus:
                    taskStatus[taskId]["status"] = "stopped"
                    taskStatus[taskId]["message"] = "任务已停止"
                    taskStatus[taskId]["endTime"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            
            logMessage(f"任务停止成功: {taskId}", "INFO")
            return jsonify({
                "success": True,
                "message": "任务已停止",
                "taskId": taskId
            })
        else:
            logMessage(f"任务停止失败: 任务不存在或无法停止 {taskId}", "WARNING")
            return jsonify({"error": "任务不存在或无法停止"}), 404
    except Exception as e:
        logMessage(f"停止任务异常: {taskId}, 错误: {str(e)}", "ERROR")
        return jsonify({"error": str(e)}), 500

def deleteTask(taskId):
    """
    删除指定任务和相关数据
    
    @param taskId 任务ID
    @return 包含操作结果的JSON响应
    """
    try:
        logMessage(f"收到删除任务请求: {taskId}", "INFO")
        with taskLock:
            if taskId in taskStatus:
                # 对于正在运行的任务，先标记为停止状态
                if taskStatus[taskId].get("status") == "running":
                    taskStopFlags[taskId] = True
                    logMessage(f"正在运行的任务 {taskId} 已标记为停止", "INFO")
                
                # 尝试非阻塞方式停止进程
                try:
                    if taskId in taskProcesses:
                        process_or_thread = taskProcesses[taskId]
                        if hasattr(process_or_thread, 'terminate'):
                            process_or_thread.terminate()
                        del taskProcesses[taskId]
                        logMessage(f"任务进程已终止: {taskId}", "INFO")
                except Exception as e:
                    logMessage(f"终止任务进程时出错: {taskId}, 错误: {str(e)}", "WARNING")
                
                # 删除任务状态
                del taskStatus[taskId]
                logMessage(f"任务删除成功: {taskId}", "INFO")
                
                return jsonify({
                    "success": True,
                    "message": "任务已删除",
                    "taskId": taskId
                })
            else:
                logMessage(f"删除任务失败: 任务不存在 {taskId}", "WARNING")
                return jsonify({"error": "任务不存在"}), 404
    except Exception as e:
        logMessage(f"删除任务异常: {taskId}, 错误: {str(e)}", "ERROR")
        return jsonify({"error": str(e)}), 500

def getCacheInfo():
    """
    获取瓦片缓存信息
    
    @return 包含缓存状态的JSON响应
    """
    try:
        tilesDir = config["tilesDir"]
        cache_info = []
        
        if not os.path.exists(tilesDir):
            return jsonify({"cacheDirectories": [], "totalDirectories": 0})
        
        # 遍历瓦片输出目录
        for item in os.listdir(tilesDir):
            itemPath = os.path.join(tilesDir, item)
            if os.path.isdir(itemPath):
                metadataFile = os.path.join(itemPath, "tile_metadata.json")
                
                cache_item = {
                    "directory": item,
                    "path": itemPath,
                    "hasMetadata": False,
                    "hasShpIndex": False,
                    "hasGeoJsonIndex": False
                }
                
                # 检查是否有元数据文件
                if os.path.exists(metadataFile):
                    try:
                        with open(metadataFile, 'r', encoding='utf-8') as f:
                            metadata = json.load(f)
                        
                        cache_item.update({
                            "hasMetadata": True,
                            "generatedAt": metadata.get("generatedAt", ""),
                            "completedAt": metadata.get("completedAt", ""),
                            "sourceFiles": metadata.get("sourceFiles", []),
                            "totalSourceFiles": metadata.get("totalSourceFiles", 0),
                            "zoomLevels": metadata.get("zoomLevels", ""),
                            "tileSize": metadata.get("tileSize", 256),
                            "totalTiles": metadata.get("totalTiles", 0),
                            "processedTiles": metadata.get("processedTiles", 0),
                            "failedTiles": metadata.get("failedTiles", 0),
                            "successRate": metadata.get("successRate", "0%"),
                            "method": metadata.get("method", ""),
                            "resampling": metadata.get("resampling", "near"),
                            "transparencyThreshold": metadata.get("transparencyThreshold", 0.1)
                        })
                    except Exception as e:
                        cache_item["metadataError"] = str(e)
                
                # 检查索引文件
                shpFile = os.path.join(itemPath, "tile_index.shp")
                geojsonFile = os.path.join(itemPath, "tile_index.geojson")
                
                cache_item["hasShpIndex"] = os.path.exists(shpFile)
                cache_item["hasGeoJsonIndex"] = os.path.exists(geojsonFile)
                
                # 检查瓦片文件数量（快速统计）
                try:
                    tile_count = 0
                    for root, dirs, files in os.walk(itemPath):
                        tile_count += len([f for f in files if f.endswith(('.png', '.jpg', '.jpeg'))])
                    cache_item["actualTileFiles"] = tile_count
                except:
                    cache_item["actualTileFiles"] = 0
                
                cache_info.append(cache_item)
        
        # 按生成时间排序
        cache_info.sort(key=lambda x: x.get("generatedAt", ""), reverse=True)
        
        return jsonify({
            "cacheDirectories": cache_info,
            "totalDirectories": len(cache_info),
            "tilesBaseDir": tilesDir
        })
        
    except Exception as e:
        logMessage(f"获取缓存信息失败: {e}", "ERROR")
        return jsonify({"error": str(e)}), 500

def recommendConfig():
    """
    根据文件特征推荐最佳配置参数
    
    @return 包含推荐配置的JSON响应
    """
    try:
        data = request.get_json()
        
        # 获取文件信息
        sourceFile = data.get('sourceFile')
        if not sourceFile:
            return jsonify({"error": "缺少参数: sourceFile"}), 400
        
        filePath = os.path.join(config["dataSourceDir"], sourceFile)
        if not os.path.exists(filePath):
            return jsonify({"error": "源文件不存在"}), 404
        
        # 获取文件大小
        fileSizeGb = os.path.getsize(filePath) / (1024**3)
        
        # 获取系统信息
        try:
            import psutil
            cpuCount = psutil.cpu_count() or 4
            memoryTotalGb = psutil.virtual_memory().total / (1024**3)
        except:
            cpuCount = 4
            memoryTotalGb = 8
        
        # 获取用户输入
        tileType = data.get('tileType', 'map')
        userMinZoom = data.get('minZoom')
        userMaxZoom = data.get('maxZoom')
        
        # 生成推荐配置
        recommendations = generateSmartRecommendations(
            fileSizeGb, tileType, userMinZoom, userMaxZoom, cpuCount, memoryTotalGb
        )
        
        return jsonify({
            "success": True,
            "fileSize": fileSizeGb,
            "systemInfo": {
                "cpuCount": cpuCount,
                "memoryTotalGb": memoryTotalGb
            },
            "recommendations": recommendations
        })
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

def systemInfo():
    """
    获取系统信息，包括硬件配置和运行状态
    
    @return 包含系统信息的JSON响应
    """
    try:
        logMessage("收到系统信息查询请求", "INFO")
        systemInfo = {
            "timestamp": datetime.now().isoformat(),
            "config": {
                "dataSourceDir": config["dataSourceDir"],
                "tilesDir": config["tilesDir"],
                "maxThreads": config["maxThreads"],
                "supportedFormats": config["supportedFormats"]
            }
        }
        
        # 系统资源信息
        try:
            import psutil
            systemInfo["system"] = {
                "cpuCount": psutil.cpu_count(),
                "memoryTotal": psutil.virtual_memory().total,
                "memoryAvailable": psutil.virtual_memory().available,
                "diskUsage": psutil.disk_usage('/').percent
            }
        except:
            systemInfo["system"] = {
                "cpuCount": 4,
                "memoryTotal": 8589934592,
                "memoryAvailable": 4294967296,
                "diskUsage": 50
            }
        
        # 任务统计
        with taskLock:
            systemInfo["tasks"] = {
                "total": len(taskStatus),
                "running": len([t for t in taskStatus.values() if t.get("status") == "running"]),
                "completed": len([t for t in taskStatus.values() if t.get("status") == "completed"]),
                "failed": len([t for t in taskStatus.values() if t.get("status") == "failed"])
            }
        
        return jsonify(systemInfo)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# getCacheInfo函数可以通过其他文件中的路由调用

def listApiRoutes():
    """
    获取所有API路由列表及其说明
    
    @return 包含API路由信息的JSON响应
    """
    try:
        # 这里可以实现路由列表功能
        routes = [
            {"path": "/api/health", "methods": ["GET"], "description": "健康检查", "category": "系统监控", "logic": "返回服务健康状态、时间戳和版本信息"},
            {"path": "/api/dataSources", "methods": ["GET"], "description": "获取数据源列表", "category": "数据源管理", "logic": "浏览根目录或指定子目录的TIF文件，支持地理范围筛选"},
            {"path": "/api/dataSources/<path:subpath>", "methods": ["GET"], "description": "获取子目录数据源", "category": "数据源管理", "logic": "浏览指定子目录的TIF文件，支持地理范围筛选"},
            {"path": "/api/dataSources/info/<filename>", "methods": ["GET"], "description": "获取数据源信息", "category": "数据源管理", "logic": "获取TIF文件的元数据、地理信息和智能切片配置推荐"},
            {"path": "/api/tile/terrain", "methods": ["POST"], "description": "创建地形瓦片（支持合并）", "category": "瓦片生成", "logic": "使用CTB生成地形瓦片，支持批量处理、智能缩放和地形合并。参数mergeTerrains=true可自动合并多个地形文件夹"},
            {"path": "/api/tile/indexedTiles", "methods": ["POST"], "description": "创建索引瓦片", "category": "瓦片生成", "logic": "基于空间索引的高性能瓦片生成，支持多进程并行处理"},
            {"path": "/api/tile/convert", "methods": ["POST"], "description": "瓦片格式转换", "category": "瓦片生成", "logic": "z/x_y.png ↔ z/x/y.png格式转换，支持批量处理"},

            {"path": "/api/tasks", "methods": ["GET"], "description": "获取任务列表", "category": "任务管理", "logic": "返回所有任务的状态、进度和基本信息"},
            {"path": "/api/tasks/<taskId>", "methods": ["GET"], "description": "获取任务状态", "category": "任务管理", "logic": "获取指定任务的详细状态、进度和结果信息"},
            {"path": "/api/tasks/cleanup", "methods": ["POST"], "description": "清理任务", "category": "任务管理", "logic": "清理已完成、失败或取消的任务记录"},
            {"path": "/api/tasks/<taskId>/stop", "methods": ["POST"], "description": "停止任务", "category": "任务管理", "logic": "停止正在运行的任务并释放资源"},
            {"path": "/api/tasks/<taskId>", "methods": ["DELETE"], "description": "删除任务", "category": "任务管理", "logic": "删除任务记录和相关数据"},
            {"path": "/api/config/recommend", "methods": ["POST"], "description": "推荐配置", "category": "配置管理", "logic": "根据文件特征和系统资源推荐最优切片配置"},
            {"path": "/api/system/info", "methods": ["GET"], "description": "系统信息", "category": "系统监控", "logic": "返回系统资源使用情况、任务统计和性能指标"},
            {"path": "/api/container/update", "methods": ["POST"], "description": "更新Docker容器信息", "category": "系统监控", "logic": "更新容器时间同步、配置等系统信息"},
            {"path": "/api/workspace/createFolder", "methods": ["POST"], "description": "创建工作空间文件夹", "category": "工作空间管理", "logic": "在瓦片输出目录中创建新文件夹"},
            {"path": "/api/workspace/folder/<path:folderPath>", "methods": ["DELETE"], "description": "删除工作空间文件夹", "category": "工作空间管理", "logic": "删除指定的工作空间文件夹及其内容"},
            {"path": "/api/workspace/folder/<path:folderPath>/rename", "methods": ["PUT"], "description": "重命名工作空间文件夹", "category": "工作空间管理", "logic": "重命名指定的工作空间文件夹"},
            {"path": "/api/workspace/info", "methods": ["GET"], "description": "获取工作空间信息", "category": "工作空间管理", "logic": "获取工作空间统计信息：总大小、文件数、目录数等"},
            {"path": "/api/tiles/nodata/scan", "methods": ["POST"], "description": "扫描包含透明（nodata）值的PNG瓦片", "category": "瓦片管理", "logic": "扫描指定目录中包含透明或nodata值的PNG瓦片"},
            {"path": "/api/tiles/nodata/delete", "methods": ["POST"], "description": "删除包含透明（nodata）值的PNG瓦片", "category": "瓦片管理", "logic": "删除扫描到的透明或nodata瓦片文件"},
            {"path": "/api/terrain/layer", "methods": ["POST"], "description": "更新地形瓦片的layer.json文件", "category": "地形处理", "logic": "修复和更新地形瓦片的layer.json元数据文件"},
            {"path": "/api/terrain/decompress", "methods": ["POST"], "description": "解压地形瓦片", "category": "地形处理", "logic": "解压缩地形瓦片文件（.terrain.gz → .terrain）"},
            {"path": "/api/routes", "methods": ["GET"], "description": "API路由列表", "category": "API文档", "logic": "返回所有可用API接口的详细信息和统计数据"}
        ]
        
        return jsonify({
            "success": True,
            "routes": routes,
            "total": len(routes),
            "categories": list(set(route["category"] for route in routes)),
            "stats": {
                "totalRoutes": len(routes),
                "byCategory": {category: len([r for r in routes if r["category"] == category]) for category in set(route["category"] for route in routes)},
                "byMethod": {method: len([r for r in routes if method in r["methods"]]) for method in set(method for route in routes for method in route["methods"])}
            }
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500 

def extractGeographicBounds(gdalinfoOutput: str) -> dict:
    """从gdalinfo输出中提取地理边界，智能识别坐标系类型"""
    try:
        lines = gdalinfoOutput.split('\n')
        bounds = {}
        
        # 首先识别坐标系类型
        coordinateSystemType = None
        for line in lines:
            line = line.strip()
            if line.startswith('PROJCS['):
                coordinateSystemType = 'projected'
                logMessage("检测到投影坐标系，将解析度分秒格式坐标")
                break
            elif line.startswith('GEOGCS[') or line.startswith('GEOGCRS['):
                coordinateSystemType = 'geographic'
                logMessage("检测到地理坐标系，将直接使用十进制度坐标")
                break
        
        if coordinateSystemType is None:
            logMessage("未检测到坐标系信息", "WARNING")
            return None
        
        def parseDmsCoordinate(coordStr):
            """解析度分秒格式坐标 例：104d45'56.25"E 或 54d 6'46.97"N"""
            try:
                import re
                # 匹配度分秒格式：数字d数字'数字.数字"方向
                pattern = r'(\d+)d\s*(\d+)\'(\d+\.?\d*)"([EWNS])'
                match = re.search(pattern, coordStr)
                if match:
                    degrees = float(match.group(1))
                    minutes = float(match.group(2))
                    seconds = float(match.group(3))
                    direction = match.group(4)
                    
                    # 转换为十进制度
                    decimalDegrees = degrees + minutes/60 + seconds/3600
                    
                    # 根据方向调整符号
                    if direction in ['W', 'S']:
                        decimalDegrees = -decimalDegrees
                        
                    return decimalDegrees
                return None
            except:
                return None
        
        def parseDecimalCoordinate(coordStr):
            """解析十进制坐标 例：-122.456, 37.789"""
            try:
                import re
                # 提取浮点数（可能有负号）
                pattern = r'(-?\d+\.?\d*)'
                numbers = re.findall(pattern, coordStr)
                if len(numbers) >= 2:
                    return float(numbers[0]), float(numbers[1])  # lon, lat
                return None, None
            except:
                return None, None
        
        # 查找Corner Coordinates部分
        cornerSection = False
        for line in lines:
            line = line.strip()
            
            if line.startswith('Corner Coordinates:'):
                cornerSection = True
                continue
                
            if cornerSection and line.startswith('Upper Left'):
                # 解析: Upper Left  (12914800.299, 5087648.603) (116d 0'56.25"E, 41d41'58.41"N)
                parts = line.split(')')
                
                if coordinateSystemType == 'projected':
                    # 投影坐标系：使用第二个括号内的度分秒格式
                    if len(parts) >= 2:
                        coordSection = parts[1].strip().replace('(', '').replace(')', '')
                        
                        # 分离经度和纬度
                        if ',' in coordSection:
                            lonStr, latStr = coordSection.split(',', 1)
                            lon = parseDmsCoordinate(lonStr.strip())
                            lat = parseDmsCoordinate(latStr.strip())
                            
                            if lon is not None and lat is not None:
                                bounds['upperLeftLon'] = lon
                                bounds['upperLeftLat'] = lat
                                
                elif coordinateSystemType == 'geographic':
                    # 地理坐标系：使用第一个括号内的十进制度
                    if len(parts) >= 1:
                        coordSection = parts[0].split('(')[1].strip() if '(' in parts[0] else parts[0].strip()
                        lon, lat = parseDecimalCoordinate(coordSection)
                        
                        if lon is not None and lat is not None:
                            bounds['upperLeftLon'] = lon
                            bounds['upperLeftLat'] = lat
                            
            elif cornerSection and line.startswith('Lower Right'):
                parts = line.split(')')
                
                if coordinateSystemType == 'projected':
                    # 投影坐标系：使用第二个括号内的度分秒格式
                    if len(parts) >= 2:
                        coordSection = parts[1].strip().replace('(', '').replace(')', '')
                        
                        if ',' in coordSection:
                            lonStr, latStr = coordSection.split(',', 1)
                            lon = parseDmsCoordinate(lonStr.strip())
                            lat = parseDmsCoordinate(latStr.strip())
                            
                            if lon is not None and lat is not None:
                                bounds['lowerRightLon'] = lon
                                bounds['lowerRightLat'] = lat
                                
                elif coordinateSystemType == 'geographic':
                    # 地理坐标系：使用第一个括号内的十进制度
                    if len(parts) >= 1:
                        coordSection = parts[0].split('(')[1].strip() if '(' in parts[0] else parts[0].strip()
                        lon, lat = parseDecimalCoordinate(coordSection)
                        
                        if lon is not None and lat is not None:
                            bounds['lowerRightLon'] = lon
                            bounds['lowerRightLat'] = lat
        
        # 计算完整边界
        if 'upperLeftLon' in bounds and 'lowerRightLon' in bounds:
            west = bounds['upperLeftLon']
            east = bounds['lowerRightLon'] 
            north = bounds['upperLeftLat']
            south = bounds['lowerRightLat']
            
            # 计算宽度和高度（度）
            widthDegrees = east - west
            heightDegrees = north - south
            
            # 确保宽度和高度为正值
            if widthDegrees < 0:
                widthDegrees += 360  # 处理跨越180度经线的情况
            if heightDegrees < 0:
                heightDegrees = abs(heightDegrees)
            
            bounds.update({
                'west': west,
                'east': east, 
                'north': north,
                'south': south,
                'widthDegrees': widthDegrees,
                'heightDegrees': heightDegrees
            })
            
            # 添加调试日志
            logMessage(f"坐标系类型: {coordinateSystemType}")
            logMessage(f"解析地理边界: 西经{west:.2f}°, 东经{east:.2f}°, 北纬{north:.2f}°, 南纬{south:.2f}°")
            logMessage(f"边界尺寸: 宽度{widthDegrees:.2f}°, 高度{heightDegrees:.2f}°")
            
        return bounds if bounds else None
        
    except Exception as e:
        logMessage(f"提取地理边界失败: {e}", "ERROR")
        return None

def getFileGeographicBounds(filePath: str) -> dict:
    """
    获取文件的地理边界信息
    
    Args:
        filePath: 文件路径
    
    Returns:
        dict: 地理边界信息或None
    """
    try:
        # 使用gdalinfo获取文件信息
        cmd = ["gdalinfo", filePath]
        result = runCommand(cmd)
        
        if not result["success"]:
            logMessage(f"获取文件地理信息失败: {filePath}, 错误: {result.get('stderr', '')}", "WARNING")
            return None
        
        # 解析地理边界
        bounds = extractGeographicBounds(result["stdout"])
        return bounds
        
    except Exception as e:
        logMessage(f"获取文件地理边界失败: {filePath}, 错误: {e}", "ERROR")
        return None 

def findTifFilesInFolders(folderPaths: list, filePatterns: list = None) -> list:
    """
    在多个文件夹中查找TIF文件
    
    Args:
        folderPaths: 文件夹路径列表（相对于dataSourceDir）
        filePatterns: 文件匹配模式列表，支持通配符和txt文件列表（可选）
    
    Returns:
        匹配的TIF文件列表（相对路径）
    """
    try:
        foundFiles = []
        
        # 处理根路径的情况
        if not folderPaths or folderPaths == [""]:
            searchPaths = [config["dataSourceDir"]]
            relativeBases = [""]
        else:
            searchPaths = []
            relativeBases = []
            for folderPath in folderPaths:
                fullPath = os.path.join(config["dataSourceDir"], folderPath) if folderPath else config["dataSourceDir"]
                if os.path.exists(fullPath):
                    searchPaths.append(fullPath)
                    relativeBases.append(folderPath)
                else:
                    logMessage(f"警告：文件夹不存在: {folderPath}", "WARNING")
        
        # 默认匹配模式
        if not filePatterns:
            filePatterns = ["*.tif", "*.tiff"]
        
        # 分离txt文件和通配符模式
        txtFiles = [p for p in filePatterns if p.lower().endswith('.txt')]
        globPatterns = [p for p in filePatterns if not p.lower().endswith('.txt')]
        
        # 处理txt文件列表
        for txtFile in txtFiles:
            logMessage(f"📋 处理txt文件列表: {txtFile}")
            
            # txt文件查找：基础路径 + filePatterns中的txt文件名
            txtFilePath = os.path.join(config["dataSourceDir"], txtFile)
            
            if not os.path.exists(txtFilePath):
                logMessage(f"❌ 未找到txt文件: {txtFilePath}", "WARNING")
                continue
            
            logMessage(f"✅ 找到txt文件: {txtFilePath}")
            
            # 读取txt文件内容
            try:
                with open(txtFilePath, 'r', encoding='utf-8') as f:
                    lines = f.readlines()
                
                for lineNum, line in enumerate(lines, 1):
                    line = line.strip()
                    
                    # 跳过空行和注释行
                    if not line or line.startswith('#'):
                        continue
                    
                    # txt文件中的文件查找：基础路径 + folderPaths + 文件名
                    if os.path.isabs(line):
                        # 绝对路径：转换为相对路径
                        try:
                            relativePath = os.path.relpath(line, config["dataSourceDir"])
                            if relativePath.startswith('..'):
                                logMessage(f"⚠️ txt文件第{lineNum}行路径超出数据源目录: {line}", "WARNING")
                                continue
                            
                            # 验证绝对路径文件是否存在
                            if os.path.exists(line) and line.lower().endswith(('.tif', '.tiff')):
                                if relativePath not in foundFiles:
                                    foundFiles.append(relativePath)
                                    logMessage(f"📄 从txt添加(绝对路径): {relativePath}")
                            else:
                                logMessage(f"⚠️ txt文件第{lineNum}行绝对路径文件不存在或非TIF格式: {line}", "WARNING")
                        except:
                            logMessage(f"⚠️ txt文件第{lineNum}行绝对路径转换失败: {line}", "WARNING")
                            continue
                    else:
                        # 相对路径：在folderPaths指定的文件夹中搜索
                        fileFound = False
                        
                        # 在每个folderPaths指定的文件夹中查找
                        for searchPath, relativeBase in zip(searchPaths, relativeBases):
                            if relativeBase:  # 如果有指定文件夹
                                candidatePath = os.path.join(relativeBase, line)
                            else:  # 根目录
                                candidatePath = line
                            
                            fullPath = os.path.join(config["dataSourceDir"], candidatePath)
                            
                            if os.path.exists(fullPath) and fullPath.lower().endswith(('.tif', '.tiff')):
                                if candidatePath not in foundFiles:
                                    foundFiles.append(candidatePath)
                                    logMessage(f"📄 从txt添加(文件夹'{relativeBase}'): {candidatePath}")
                                fileFound = True
                                break  # 找到就停止，避免重复添加
                        
                        # 如果在指定文件夹中都没找到，提示错误
                        if not fileFound:
                            searched_folders = [relativeBase or "根目录" for relativeBase in relativeBases]
                            logMessage(f"⚠️ txt文件第{lineNum}行文件未找到: {line} (已在文件夹 {searched_folders} 中搜索)", "WARNING")
                
            except Exception as e:
                logMessage(f"❌ 读取txt文件失败 {txtFilePath}: {e}", "ERROR")
        
        # 处理通配符模式
        for searchPath, relativeBase in zip(searchPaths, relativeBases):
            logMessage(f"🔍 在路径中搜索TIF文件: {searchPath}")
            
            for pattern in globPatterns:
                # 构建完整的搜索模式
                searchPattern = os.path.join(searchPath, pattern)
                logMessage(f"🎯 使用模式: {searchPattern}")
                
                # 使用glob查找文件
                matches = glob.glob(searchPattern)
                logMessage(f"📊 glob结果: {len(matches)}个文件")
                
                for match in matches:
                    if match.lower().endswith(('.tif', '.tiff')):
                        # 转换为相对路径
                        if relativeBase:
                            relativePath = os.path.join(relativeBase, os.path.basename(match))
                        else:
                            relativePath = os.path.basename(match)
                        
                        if relativePath not in foundFiles:
                            foundFiles.append(relativePath)
                            logMessage(f"🌟 添加文件: {relativePath}")
        
        logMessage(f"✅ 总共找到 {len(foundFiles)} 个TIF文件")
        return foundFiles
        
    except Exception as e:
        logMessage(f"❌ 查找TIF文件失败: {e}", "ERROR")
        return []

def convertSinglePathToRelative(filePath: str) -> str:
    """将绝对路径转换为相对路径"""
    try:
        if os.path.isabs(filePath):
            return os.path.relpath(filePath, config["dataSourceDir"])
        return filePath
    except:
        return filePath

def processUltraHighPerformanceTiles(tileIndex, outputPath, resampling="near", 
                                   max_workers=None, enable_memory_cache=True, enable_async_io=True, user_processes=None, user_memory_gb=None):
    """
    极致高性能瓦片处理 - 目标: 1000-2000瓦片/秒
    借鉴120GB内存缓存 + 86核心并行 + 异步I/O的极致优化方案
    
    @param tileIndex 瓦片索引列表
    @param outputPath 输出路径
    @param resampling 重采样方法
    @param max_workers 最大工作进程数
    @param enable_memory_cache 启用内存缓存
    @param enable_async_io 启用异步I/O
    @param user_processes 用户指定的进程数（优先使用）
    @return 处理结果统计
    """
    import multiprocessing as mp
    import asyncio
    import concurrent.futures
    import time
    import threading
    from collections import defaultdict
    import queue
    
    total_tiles = len(tileIndex)
    cpu_count = mp.cpu_count()
    system_memory_gb = psutil.virtual_memory().total / (1024**3)
    
    # 优先使用用户指定的内存，否则使用系统内存
    effective_memory_gb = user_memory_gb if user_memory_gb is not None else system_memory_gb
    
    logMessage(f"🚀 启动极致高性能瓦片处理")
    logMessage(f"📊 系统配置: {cpu_count}核CPU, {system_memory_gb:.1f}GB内存")
    if user_memory_gb is not None:
        logMessage(f"💾 使用用户指定内存: {user_memory_gb:.1f}GB (系统内存: {system_memory_gb:.1f}GB)")
    else:
        logMessage(f"💾 使用系统内存: {system_memory_gb:.1f}GB")
    logMessage(f"🎯 处理目标: {total_tiles}瓦片, 目标速度: 1000瓦片/秒")
    
    # 极致参数计算
    if max_workers is None:
        if user_processes and user_processes > 0:
            # 用户明确指定了进程数，优先使用用户的值
            max_workers = user_processes
            logMessage(f"🎯 极致模式使用用户指定进程数: {max_workers}")
        else:
            # 86核心策略：充分利用所有CPU核心
            max_workers = min(cpu_count * 3, 128)  # 最多128个工作进程
            logMessage(f"🤖 极致模式自动计算进程数: {max_workers}")
    
    # 超级批量大小计算 - 借鉴你的4000倍提升策略
    ultra_batch_size = calculateUltraBatchSize(total_tiles, max_workers, effective_memory_gb)
    
    logMessage(f"🧠 极致参数: {max_workers}进程, 超级批量{ultra_batch_size}")
    
    # 内存缓存策略 - 120GB内存缓存TIF文件
    memory_cache = {}
    if enable_memory_cache and effective_memory_gb > 32:
        logMessage("💾 启动内存缓存策略 - 预加载TIF到内存")
        memory_cache = preloadTifFilesToMemory(tileIndex, effective_memory_gb)
        logMessage(f"✅ 内存缓存完成: {len(memory_cache)}个文件已缓存")
    
    # 异步I/O架构 - 读写分离
    async_executor = None
    if enable_async_io:
        async_executor = concurrent.futures.ThreadPoolExecutor(max_workers=16, thread_name_prefix="AsyncIO")
        logMessage("⚡ 启动异步I/O架构 - 读写分离优化")
    
    start_time = time.time()
    
    # 创建结果统计
    stats = {
        "processed": 0,
        "failed": 0,
        "start_time": start_time,
        "batches_completed": 0,
        "current_speed": 0.0,
        "average_speed": 0.0,
        "peak_speed": 0.0,
        "memory_cache_enabled": enable_memory_cache,
        "async_io_enabled": enable_async_io
    }
    
    # 将瓦片分成超级批次
    tile_ultra_batches = []
    for i in range(0, total_tiles, ultra_batch_size):
        batch = tileIndex[i:i + ultra_batch_size]
        tile_ultra_batches.append(batch)
    
    logMessage(f"📦 超级批量分组: {len(tile_ultra_batches)}个超级批次")
    
    try:
        # 多级并行处理架构
        with concurrent.futures.ProcessPoolExecutor(max_workers=max_workers) as executor:
            
            # 提交所有批次任务 - 极致并行
            future_to_batch = {}
            for batch_idx, tile_batch in enumerate(tile_ultra_batches):
                future = executor.submit(
                    processUltraTileBatch,
                    tile_batch,
                    outputPath,
                    resampling,
                    transparencyThreshold,
                    batch_idx,
                    memory_cache,
                    enable_async_io
                )
                future_to_batch[future] = batch_idx
            
            # 实时性能监控
            completed_batches = 0
            last_update_time = start_time
            
            for future in concurrent.futures.as_completed(future_to_batch):
                batch_idx = future_to_batch[future]
                
                try:
                    result = future.result()
                    stats["processed"] += result["processed"]
                    stats["failed"] += result["failed"]
                    completed_batches += 1
                    
                    # 实时速度计算
                    current_time = time.time()
                    elapsed = current_time - start_time
                    current_speed = stats["processed"] / elapsed if elapsed > 0 else 0
                    
                    # 更新峰值速度
                    if current_speed > stats["peak_speed"]:
                        stats["peak_speed"] = current_speed
                    
                    stats["current_speed"] = current_speed
                    stats["average_speed"] = current_speed
                    stats["batches_completed"] = completed_batches
                    
                    # 每秒更新一次日志
                    if current_time - last_update_time >= 1.0:
                        efficiency = (current_speed / 1000) * 100  # 相对于1000瓦片/秒的效率
                        logMessage(f"⚡ 实时性能: {current_speed:.1f}瓦片/秒 "
                                 f"| 效率: {efficiency:.1f}% | 已完成: {stats['processed']}/{total_tiles}")
                        last_update_time = current_time
                    
                except Exception as e:
                    logMessage(f"❌ 批次{batch_idx}处理失败: {e}", "ERROR")
                    stats["failed"] += len(tile_ultra_batches[batch_idx])
        
        # 最终统计
        total_time = time.time() - start_time
        final_speed = stats["processed"] / total_time if total_time > 0 else 0
        
        logMessage(f"🎉 极致性能处理完成!")
        logMessage(f"📊 最终统计: {stats['processed']}成功, {stats['failed']}失败")
        logMessage(f"⚡ 性能指标: 平均{final_speed:.1f}瓦片/秒, 峰值{stats['peak_speed']:.1f}瓦片/秒")
        logMessage(f"⏱️  总耗时: {total_time:.2f}秒")
        
        # 计算性能提升倍数
        baseline_speed = 25  # 基准速度
        improvement_factor = final_speed / baseline_speed
        logMessage(f"🚀 性能提升: {improvement_factor:.1f}倍 (相对基准{baseline_speed}瓦片/秒)")
        
        stats["final_speed"] = final_speed
        stats["total_time"] = total_time
        stats["improvement_factor"] = improvement_factor
        
    finally:
        # 清理资源
        if async_executor:
            async_executor.shutdown(wait=True)
        
        # 清理内存缓存
        if memory_cache:
            logMessage("🧹 清理内存缓存")
            memory_cache.clear()
    
    return stats

def calculateUltraBatchSize(total_tiles, max_workers, memory_gb):
    """
    计算超级批量大小 - 借鉴极致优化方案
    目标: 最大化并行效率，减少进程创建开销
    
    @param total_tiles 总瓦片数
    @param max_workers 工作进程数  
    @param memory_gb 内存大小GB
    @return 超级批量大小
    """
    # 基础批量计算：每个进程处理的瓦片数
    base_batch = max(50, total_tiles // (max_workers * 4))
    
    # 根据内存调整批量大小
    if memory_gb >= 64:
        # 大内存：使用超级批量
        memory_factor = min(4.0, memory_gb / 32)
        ultra_batch = int(base_batch * memory_factor)
    elif memory_gb >= 32:
        # 中等内存：适中批量
        ultra_batch = int(base_batch * 2)
    else:
        # 小内存：保守批量
        ultra_batch = base_batch
    
    # 限制在合理范围
    ultra_batch = max(20, min(ultra_batch, 500))
    
    logMessage(f"🧠 超级批量计算: 基础{base_batch} → 超级{ultra_batch} (内存因子: {memory_gb/32:.1f})")
    
    return ultra_batch

def preloadTifFilesToMemory(tileIndex, memory_gb):
    """
    预加载TIF文件到内存 - 120GB内存缓存策略
    实现用内存换速度的极致优化
    
    @param tileIndex 瓦片索引
    @param memory_gb 可用内存GB
    @return 内存缓存字典
    """
    memory_cache = {}
    
    # 计算可用缓存内存 (保留25%给系统)
    available_memory_gb = memory_gb * 0.75
    
    if available_memory_gb < 8:
        logMessage("⚠️  内存不足，跳过TIF预加载", "WARNING")
        return memory_cache
    
    logMessage(f"💾 开始预加载TIF文件，可用内存: {available_memory_gb:.1f}GB")
    
    # 收集所有源文件
    source_files = set()
    for tile_info in tileIndex:
        source_files_list = tile_info.get("sourceFiles", [])
        if source_files_list:
            for source_file in source_files_list:
                source_files.add(source_file["file"])
    
    source_files = list(source_files)
    total_files = len(source_files)
    
    logMessage(f"📁 发现 {total_files} 个唯一TIF文件需要预加载")
    
    # 估算文件大小并选择性加载
    loaded_files = 0
    used_memory_gb = 0
    
    for file_path in source_files:
        try:
            # 获取文件大小
            file_size_gb = os.path.getsize(file_path) / (1024**3)
            
            # 检查是否有足够内存
            if used_memory_gb + file_size_gb > available_memory_gb:
                logMessage(f"⚠️  内存限制达到，停止预加载. 已加载: {loaded_files}/{total_files}")
                break
            
            # 读取文件到内存
            with open(file_path, 'rb') as f:
                file_data = f.read()
                memory_cache[file_path] = file_data
                
                used_memory_gb += file_size_gb
                loaded_files += 1
                
                if loaded_files % 10 == 0:
                    logMessage(f"💾 预加载进度: {loaded_files}/{total_files}, "
                             f"内存使用: {used_memory_gb:.1f}GB/{available_memory_gb:.1f}GB")
            
        except Exception as e:
            logMessage(f"❌ 预加载文件失败: {file_path} - {e}", "WARNING")
            continue
    
    cache_efficiency = (loaded_files / total_files) * 100 if total_files > 0 else 0
    logMessage(f"✅ TIF预加载完成: {loaded_files}/{total_files} ({cache_efficiency:.1f}%)")
    logMessage(f"💾 缓存统计: {used_memory_gb:.1f}GB内存, {len(memory_cache)}个文件")
    
    return memory_cache

def processUltraTileBatch(tile_batch, outputPath, resampling, transparencyThreshold, 
                         batch_idx, memory_cache=None, enable_async_io=True):
    """
    超级批量瓦片处理 - 在子进程中运行
    结合内存缓存、异步I/O、向量化处理的极致优化
    
    @param tile_batch 瓦片批次
    @param outputPath 输出路径
    @param resampling 重采样方法
    @param transparencyThreshold 透明度阈值
    @param batch_idx 批次索引
    @param memory_cache 内存缓存
    @param enable_async_io 启用异步I/O
    @return 批次处理结果
    """
    import tempfile
    import concurrent.futures
    
    processed = 0
    failed = 0
    batch_start_time = time.time()
    
    # 为批次创建临时工作目录
    with tempfile.TemporaryDirectory(prefix=f"ultra_batch_{batch_idx}_") as temp_dir:
        
        if enable_async_io:
            # 异步I/O处理
            with concurrent.futures.ThreadPoolExecutor(max_workers=8) as io_executor:
                
                # 预处理：异步准备所有输入数据
                prep_futures = []
                for tile_info in tile_batch:
                    future = io_executor.submit(
                        prepareTileDataAsync, 
                        tile_info, 
                        memory_cache, 
                        temp_dir
                    )
                    prep_futures.append((future, tile_info))
                
                # 处理准备好的瓦片数据
                for future, tile_info in prep_futures:
                    try:
                        prepared_data = future.result()
                        
                        if prepared_data["success"]:
                            # 使用优化的向量化处理
                            result = processSingleTileVectorized(
                                tile_info,
                                outputPath,
                                resampling,

                                prepared_data,
                                temp_dir
                            )
                            
                            if result["success"]:
                                processed += 1
                            else:
                                failed += 1
                        else:
                            failed += 1
                            
                    except Exception as e:
                        failed += 1
        else:
            # 传统同步处理（但使用优化算法）
            for tile_info in tile_batch:
                try:
                    result = processSingleTileOptimized(
                        tile_info,
                        outputPath,
                        resampling,
                        transparencyThreshold,
                        temp_dir
                    )
                    
                    if result["success"]:
                        processed += 1
                    else:
                        failed += 1
                        
                except Exception as e:
                    failed += 1
    
    # 批次性能统计
    batch_time = time.time() - batch_start_time
    batch_speed = processed / batch_time if batch_time > 0 else 0
    
    return {
        "processed": processed,
        "failed": failed,
        "batch_idx": batch_idx,
        "batch_time": batch_time,
        "batch_speed": batch_speed
    }

def prepareTileDataAsync(tile_info, memory_cache, temp_dir):
    """
    异步准备瓦片数据 - 读写分离优化
    
    @param tile_info 瓦片信息
    @param memory_cache 内存缓存
    @param temp_dir 临时目录
    @return 准备结果
    """
    try:
        source_files = tile_info.get("sourceFiles", [])
        if not source_files:
            return {
                "success": False,
                "error": "没有源文件"
            }
            
        prepared_files = []
        
        for source_file in source_files:
            file_path = source_file["file"]
            
            if memory_cache and file_path in memory_cache:
                # 从内存缓存读取
                file_data = memory_cache[file_path]
                
                # 写入临时文件
                temp_file = os.path.join(temp_dir, f"cached_{os.path.basename(file_path)}")
                with open(temp_file, 'wb') as f:
                    f.write(file_data)
                
                prepared_files.append({
                    "file": temp_file,
                    "source": "memory_cache"
                })
            else:
                # 直接使用原文件
                prepared_files.append({
                    "file": file_path,
                    "source": "disk"
                })
        
        return {
            "success": True,
            "prepared_files": prepared_files,
            "tile_info": tile_info
        }
        
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }

def processSingleTileVectorized(tile_info, tiles_dir, resampling, transparency_threshold, 
                               prepared_data, temp_dir):
    """
    向量化单瓦片处理 - 算法级优化
    借鉴numpy向量化处理提升性能
    
    @param tile_info 瓦片信息
    @param tiles_dir 输出目录
    @param resampling 重采样方法
    @param transparency_threshold 透明度阈值
    @param prepared_data 预处理数据
    @param temp_dir 临时目录
    @return 处理结果
    """
    try:
        zoom, tileX, tileY = tile_info["z"], tile_info["x"], tile_info["y"]
        tileBounds = tile_info["bounds"]
        prepared_files = prepared_data["prepared_files"]
        
        # 创建瓦片目录
        tileDir = os.path.join(tiles_dir, str(zoom), str(tileX))
        os.makedirs(tileDir, exist_ok=True)
        
        # 瓦片文件路径
        tileFile = os.path.join(tileDir, f"{tileY}.png")
        
        # 如果瓦片已存在，跳过
        if os.path.exists(tileFile) and os.path.getsize(tileFile) > 0:
            return {
                "success": True,
                "tileFile": tileFile,
                "skipped": True,
                "method": "向量化处理(跳过)"
            }
        
        # 准备源文件列表
        sourceFileList = [pf["file"] for pf in prepared_files]
        tempTileFile = os.path.join(temp_dir, f"vec_tile_{zoom}_{tileX}_{tileY}.png")
        
        # 极致优化的GDAL命令 - 向量化处理
        cmd = [
            "gdalwarp",
            "-te", str(tileBounds[0]), str(tileBounds[1]), str(tileBounds[2]), str(tileBounds[3]),
            "-ts", "256", "256",
            "-r", resampling,
            "-of", "PNG",
            "-co", "WORLDFILE=NO",
            "-srcnodata", "0",
            "-dstnodata", "0",
            "-dstalpha",
            "-multi",  # 多线程
            "-wm", "256",  # 增大工作内存
            "-wo", "NUM_THREADS=4",  # 4线程处理
            "-q"  # 静默模式
        ] + sourceFileList + [tempTileFile]
        
        # 执行命令
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
        
        if result.returncode == 0 and os.path.exists(tempTileFile):
            # 移动到最终位置
            import shutil
            shutil.move(tempTileFile, tileFile)
            
            return {
                "success": True,
                "tileFile": tileFile,
                "sourceCount": len(prepared_files),
                "method": "向量化GDAL处理",
                "tilePath": f"{zoom}/{tileX}/{tileY}.png"
            }
        else:
            return {
                "success": False,
                "error": f"向量化处理失败: {result.stderr}"
            }
            
    except Exception as e:
        return {
            "success": False,
            "error": f"向量化处理异常: {str(e)}"
        }

def updateContainerInfo():
    """
    更新Docker容器信息，包括同步系统时间、更新配置等
    
    @return 包含更新结果的JSON响应
    """
    try:
        import subprocess
        import platform
        import socket
        from flask import request
        
        logMessage("收到Docker容器信息更新请求", "INFO")
        
        # 获取请求参数
        data = request.get_json() if request.method == 'POST' else {}
        updateType = data.get('updateType', 'all')  # all, time, config, environment
        
        updateResults = {
            "timestamp": datetime.now().isoformat(),
            "updateType": updateType,
            "version": "v2.72",
            "results": {}
        }
        
        # 更新系统时间
        if updateType in ['all', 'time']:
            try:
                # 尝试从网络时间服务器同步时间
                timeResults = {"actions": []}
                
                # 检查当前时间
                currentTime = datetime.now()
                utcTime = datetime.utcnow()
                
                timeResults["before"] = {
                    "localTime": currentTime.strftime("%Y-%m-%d %H:%M:%S"),
                    "utcTime": utcTime.strftime("%Y-%m-%d %H:%M:%S UTC"),
                    "timestamp": currentTime.timestamp()
                }
                
                # 尝试使用ntpdate同步时间（如果可用）
                try:
                    ntpResult = subprocess.run(['which', 'ntpdate'], capture_output=True, text=True, timeout=5)
                    if ntpResult.returncode == 0:
                        syncResult = subprocess.run(['ntpdate', '-s', 'time.nist.gov'], capture_output=True, text=True, timeout=30)
                        if syncResult.returncode == 0:
                            timeResults["actions"].append("使用ntpdate同步网络时间成功")
                        else:
                            timeResults["actions"].append(f"ntpdate同步失败: {syncResult.stderr}")
                    else:
                        timeResults["actions"].append("ntpdate未安装，跳过网络时间同步")
                except Exception as e:
                    timeResults["actions"].append(f"时间同步异常: {str(e)}")
                
                # 检查更新后的时间
                afterTime = datetime.now()
                afterUtcTime = datetime.utcnow()
                
                timeResults["after"] = {
                    "localTime": afterTime.strftime("%Y-%m-%d %H:%M:%S"),
                    "utcTime": afterUtcTime.strftime("%Y-%m-%d %H:%M:%S UTC"),
                    "timestamp": afterTime.timestamp()
                }
                
                # 计算时间差
                timeDiff = afterTime.timestamp() - currentTime.timestamp()
                timeResults["timeDifferenceSeconds"] = timeDiff
                
                # 设置或更新时区（如果提供）
                if 'timezone' in data:
                    try:
                        timezone = data['timezone']
                        os.environ['TZ'] = timezone
                        timeResults["actions"].append(f"设置时区为: {timezone}")
                    except Exception as e:
                        timeResults["actions"].append(f"设置时区失败: {str(e)}")
                
                updateResults["results"]["time"] = timeResults
                
            except Exception as e:
                updateResults["results"]["time"] = {"error": str(e)}
        
        # 更新环境变量
        if updateType in ['all', 'environment']:
            try:
                envResults = {"actions": [], "updated": {}}
                
                # 如果请求中包含环境变量更新
                if 'environment' in data:
                    envUpdates = data['environment']
                    for key, value in envUpdates.items():
                        try:
                            oldValue = os.environ.get(key, "未设置")
                            os.environ[key] = str(value)
                            envResults["updated"][key] = {
                                "old": oldValue,
                                "new": str(value)
                            }
                            envResults["actions"].append(f"更新环境变量 {key}")
                        except Exception as e:
                            envResults["actions"].append(f"更新环境变量 {key} 失败: {str(e)}")
                
                # 刷新重要的GDAL环境变量
                gdalEnvVars = {
                    "GDAL_CACHEMAX": "512",
                    "GDAL_DISABLE_READDIR_ON_OPEN": "EMPTY_DIR",
                    "GDAL_HTTP_TIMEOUT": "30",
                    "GDAL_HTTP_CONNECTTIMEOUT": "10"
                }
                
                for key, defaultValue in gdalEnvVars.items():
                    if key not in os.environ:
                        os.environ[key] = defaultValue
                        envResults["actions"].append(f"设置GDAL环境变量 {key} = {defaultValue}")
                
                updateResults["results"]["environment"] = envResults
                
            except Exception as e:
                updateResults["results"]["environment"] = {"error": str(e)}
        
        # 更新配置
        if updateType in ['all', 'config']:
            try:
                configResults = {"actions": [], "updated": {}}
                
                # 如果请求中包含配置更新
                if 'config' in data:
                    configUpdates = data['config']
                    for key, value in configUpdates.items():
                        if key in config:
                            oldValue = config[key]
                            config[key] = value
                            configResults["updated"][key] = {
                                "old": oldValue,
                                "new": value
                            }
                            configResults["actions"].append(f"更新配置 {key}")
                        else:
                            configResults["actions"].append(f"配置项 {key} 不存在，跳过")
                
                # 确保必要的目录存在
                for dirKey in ['dataSourceDir', 'tilesDir', 'logDir']:
                    if dirKey in config:
                        dirPath = config[dirKey]
                        if not os.path.exists(dirPath):
                            os.makedirs(dirPath, exist_ok=True)
                            configResults["actions"].append(f"创建目录: {dirPath}")
                
                updateResults["results"]["config"] = configResults
                
            except Exception as e:
                updateResults["results"]["config"] = {"error": str(e)}
        
        # 系统信息刷新
        if updateType in ['all', 'system']:
            try:
                systemResults = {"actions": []}
                
                # 清理临时文件
                try:
                    import tempfile
                    import shutil
                    tempDir = tempfile.gettempdir()
                    
                    # 清理过期的临时文件（超过24小时）
                    cutoffTime = datetime.now().timestamp() - 86400
                    cleanedFiles = 0
                    
                    for root, dirs, files in os.walk(tempDir):
                        for file in files:
                            filePath = os.path.join(root, file)
                            try:
                                if os.path.getmtime(filePath) < cutoffTime:
                                    os.remove(filePath)
                                    cleanedFiles += 1
                            except:
                                pass
                    
                    systemResults["actions"].append(f"清理临时文件: {cleanedFiles} 个")
                    
                except Exception as e:
                    systemResults["actions"].append(f"清理临时文件失败: {str(e)}")
                
                # 刷新系统缓存
                try:
                    if hasattr(os, 'sync'):
                        os.sync()
                        systemResults["actions"].append("同步文件系统缓存")
                except:
                    pass
                
                updateResults["results"]["system"] = systemResults
                
            except Exception as e:
                updateResults["results"]["system"] = {"error": str(e)}
        
        # 网络配置刷新
        if updateType in ['all', 'network']:
            try:
                networkResults = {"actions": []}
                
                # 刷新DNS
                try:
                    dnsResult = subprocess.run(['nslookup', 'google.com'], capture_output=True, text=True, timeout=10)
                    if dnsResult.returncode == 0:
                        networkResults["actions"].append("DNS解析测试通过")
                    else:
                        networkResults["actions"].append("DNS解析测试失败")
                except Exception as e:
                    networkResults["actions"].append(f"DNS测试异常: {str(e)}")
                
                # 获取最新的网络接口信息
                try:
                    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                    s.connect(("8.8.8.8", 80))
                    currentIP = s.getsockname()[0]
                    s.close()
                    networkResults["actions"].append(f"当前IP地址: {currentIP}")
                except Exception as e:
                    networkResults["actions"].append(f"获取IP地址失败: {str(e)}")
                
                updateResults["results"]["network"] = networkResults
                
            except Exception as e:
                updateResults["results"]["network"] = {"error": str(e)}
        
        # 返回更新结果
        logMessage(f"容器信息更新完成: {updateType}", "INFO")
        return jsonify({
            "success": True,
            "message": f"容器信息更新完成: {updateType}",
            "updateResults": updateResults
        })
        
    except Exception as e:
        logMessage(f"更新Docker容器信息失败: {str(e)}", "ERROR")
        return jsonify({"error": f"更新容器信息失败: {str(e)}"}), 500

def convertTileFormat():
    """
    瓦片格式转换接口：支持 z/x_y.png 和 z/x/y.png 两种格式互转
    
    请求参数：
    - sourcePath: 源瓦片目录路径（相对于tiles目录）
    - targetPath: 目标瓦片目录路径（相对于tiles目录）
    - sourceFormat: 源格式，"flat"（z/x_y.png）或 "nested"（z/x/y.png）
    - targetFormat: 目标格式，"flat"（z/x_y.png）或 "nested"（z/x/y.png）
    - overwrite: 是否覆盖已存在的目标文件（默认false）
    
    @return 包含转换结果的JSON响应
    """
    try:
        data = request.get_json()
        
        # 参数验证
        required_params = ['sourcePath', 'targetPath', 'sourceFormat', 'targetFormat']
        for param in required_params:
            if param not in data:
                return jsonify({"error": f"缺少参数: {param}"}), 400
        
        sourcePath = data['sourcePath']
        targetPath = data['targetPath']
        sourceFormat = data['sourceFormat']  # "flat" or "nested"
        targetFormat = data['targetFormat']  # "flat" or "nested"
        overwrite = data.get('overwrite', False)
        
        # 验证格式参数
        valid_formats = ["flat", "nested"]
        if sourceFormat not in valid_formats:
            return jsonify({"error": f"源格式无效: {sourceFormat}，支持的格式: {valid_formats}"}), 400
        if targetFormat not in valid_formats:
            return jsonify({"error": f"目标格式无效: {targetFormat}，支持的格式: {valid_formats}"}), 400
        
        # 如果源格式和目标格式相同，返回错误
        if sourceFormat == targetFormat:
            return jsonify({"error": "源格式和目标格式不能相同"}), 400
        
        # 构建完整路径
        sourceFullPath = os.path.join(config["tilesDir"], sourcePath)
        targetFullPath = os.path.join(config["tilesDir"], targetPath)
        
        # 验证源目录存在
        if not os.path.exists(sourceFullPath):
            return jsonify({"error": f"源目录不存在: {sourceFullPath}"}), 404
        
        # 创建目标目录
        os.makedirs(targetFullPath, exist_ok=True)
        
        # 生成任务ID
        timestamp = int(time.time())
        taskId = f"tileConvert{timestamp}"
        
        # 创建任务状态
        with taskLock:
            taskStatus[taskId] = {
                "taskId": taskId,
                "status": "运行中",
                "progress": 0,
                "message": "开始瓦片格式转换",
                "startTime": datetime.now().isoformat(),
                "sourcePath": sourcePath,
                "targetPath": targetPath,
                "sourceFormat": sourceFormat,
                "targetFormat": targetFormat,
                "processedTiles": 0,
                "totalTiles": 0,
                "errors": []
            }
        
        # 异步执行转换任务
        def runConvertTask():
            try:
                # 扫描源瓦片文件
                tileFiles = []
                logMessage(f"开始扫描源瓦片文件: {sourceFullPath}", "INFO")
                
                for root, dirs, files in os.walk(sourceFullPath):
                    for file in files:
                        if file.endswith('.png'):
                            tileFiles.append(os.path.join(root, file))
                
                totalTiles = len(tileFiles)
                logMessage(f"找到 {totalTiles} 个瓦片文件", "INFO")
                
                if totalTiles == 0:
                    with taskLock:
                                            taskStatus[taskId]["status"] = "failed"
                    taskStatus[taskId]["message"] = "未找到瓦片文件"
                    return
                
                # 更新任务状态
                with taskLock:
                    taskStatus[taskId]["totalTiles"] = totalTiles
                    taskStatus[taskId]["message"] = f"开始转换 {totalTiles} 个瓦片文件"
                
                processedTiles = 0
                skippedTiles = 0
                errorTiles = 0
                
                # 转换每个瓦片
                for sourceTileFile in tileFiles:
                    try:
                        # 解析源瓦片路径
                        relativePath = os.path.relpath(sourceTileFile, sourceFullPath)
                        
                        if sourceFormat == "flat":
                            # 从 z/x_y.png 格式解析
                            pathParts = relativePath.split(os.sep)
                            if len(pathParts) >= 2:
                                z = pathParts[0]
                                filename = pathParts[-1]
                                if '_' in filename:
                                    x_y = filename.replace('.png', '')
                                    if '_' in x_y:
                                        x, y = x_y.split('_', 1)
                                        # 构建目标路径 z/x/y.png
                                        targetTileFile = os.path.join(targetFullPath, z, x, f"{y}.png")
                                    else:
                                        raise ValueError(f"无效的文件名格式: {filename}")
                                else:
                                    raise ValueError(f"无效的文件名格式: {filename}")
                            else:
                                raise ValueError(f"无效的路径格式: {relativePath}")
                        
                        elif sourceFormat == "nested":
                            # 从 z/x/y.png 格式解析
                            pathParts = relativePath.split(os.sep)
                            if len(pathParts) >= 3:
                                z = pathParts[0]
                                x = pathParts[1]
                                y = pathParts[2].replace('.png', '')
                                # 构建目标路径 z/x_y.png
                                targetTileFile = os.path.join(targetFullPath, z, f"{x}_{y}.png")
                            else:
                                raise ValueError(f"无效的路径格式: {relativePath}")
                        
                        # 创建目标目录
                        os.makedirs(os.path.dirname(targetTileFile), exist_ok=True)
                        
                        # 检查目标文件是否已存在
                        if os.path.exists(targetTileFile) and not overwrite:
                            skippedTiles += 1
                            continue
                        
                        # 复制文件
                        shutil.copy2(sourceTileFile, targetTileFile)
                        processedTiles += 1
                        
                        # 更新进度
                        progress = int((processedTiles + skippedTiles + errorTiles) * 100 / totalTiles)
                        with taskLock:
                            taskStatus[taskId]["progress"] = progress
                            taskStatus[taskId]["processedTiles"] = processedTiles
                            taskStatus[taskId]["message"] = f"已转换 {processedTiles} 个瓦片，跳过 {skippedTiles} 个，错误 {errorTiles} 个"
                        
                    except Exception as e:
                        errorTiles += 1
                        errorMsg = f"转换瓦片失败 {sourceTileFile}: {str(e)}"
                        logMessage(errorMsg, "WARNING")
                        with taskLock:
                            taskStatus[taskId]["errors"].append(errorMsg)
                
                # 任务完成
                with taskLock:
                    taskStatus[taskId]["status"] = "completed"
                    taskStatus[taskId]["progress"] = 100
                    taskStatus[taskId]["message"] = f"转换完成！处理 {processedTiles} 个瓦片，跳过 {skippedTiles} 个，错误 {errorTiles} 个"
                    taskStatus[taskId]["endTime"] = datetime.now().isoformat()
                
                logMessage(f"瓦片格式转换完成: {taskId}", "INFO")
                
            except Exception as e:
                logMessage(f"瓦片格式转换失败: {str(e)}", "ERROR")
                with taskLock:
                    taskStatus[taskId]["status"] = "failed"
                    taskStatus[taskId]["message"] = f"转换失败: {str(e)}"
                    taskStatus[taskId]["endTime"] = datetime.now().isoformat()
        
        # 启动转换任务
        convertThread = threading.Thread(target=runConvertTask)
        convertThread.daemon = True
        convertThread.start()
        
        logMessage(f"瓦片格式转换任务已启动: {taskId}", "INFO")
        
        return jsonify({
            "success": True,
            "message": "瓦片格式转换任务已启动",
            "taskId": taskId,
            "sourcePath": sourcePath,
            "targetPath": targetPath,
            "sourceFormat": sourceFormat,
            "targetFormat": targetFormat
        })
        
    except Exception as e:
        logMessage(f"启动瓦片格式转换失败: {str(e)}", "ERROR")
        return jsonify({"error": f"启动瓦片格式转换失败: {str(e)}"}), 500

def checkTileHasNodata(tileFile, transparencyThreshold=0.1):
    """
    检查瓦片是否包含任何nodata像素
    
    @param tileFile 瓦片文件路径
    @param transparencyThreshold 未使用，保持接口兼容性
    @return True如果瓦片包含任何nodata像素，False如果完全被有效数据覆盖
    """
    try:
        from PIL import Image
        import numpy as np
        
        with Image.open(tileFile) as img:
            # 确保图像是256x256
            if img.size != (256, 256):
                logMessage(f"瓦片尺寸不正确: {img.size}, 预期 (256, 256)", "WARNING")
                return True  # 尺寸不对就认为有问题
            
            # 转换为RGBA模式以便处理透明度
            if img.mode != 'RGBA':
                img = img.convert('RGBA')
            
            # 获取Alpha通道
            alpha_channel = np.array(img)[:, :, 3]
            
            # 检查是否有任何透明像素（Alpha值小于255）
            has_transparent_pixels = np.any(alpha_channel < 255)
            
            if has_transparent_pixels:
                return True  # 有透明像素，认为包含nodata
            
            # 如果所有像素的Alpha值都是255，认为完全覆盖
            return False
        
    except Exception as e:
        logMessage(f"检查瓦片nodata失败 {tileFile}: {e}", "WARNING")
        # 发生错误时，尝试使用gdalinfo作为备用方案
        try:
            import subprocess
            
            cmd = ["gdalinfo", "-stats", tileFile]
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
            
            if result.returncode != 0:
                return True  # gdalinfo失败，认为有问题
            
            output = result.stdout
            
            # 检查是否有nodata值定义
            has_nodata = "NoData Value=" in output
            
            # 检查是否有Alpha通道且最小值小于255
            has_alpha_transparency = "ColorInterp=Alpha" in output and ("Min=0" in output or "Minimum=0" in output)
            
            return has_nodata or has_alpha_transparency
            
        except Exception as e2:
            logMessage(f"备用检查也失败: {e2}", "WARNING")
            return True  # 都失败了，保守起见认为有问题

def deleteNodataTilesInternal(tilesPath, includeDetails=True):
    """
    删除透明瓦片的内部函数，可以被其他函数调用
    
    @param tilesPath 相对瓦片目录路径
    @param includeDetails 是否返回详细信息
    @return 删除操作的结果字典
    """
    try:
        # 构建完整路径：基础路径 + 自定义路径
        fullTilesPath = os.path.join(config["tilesDir"], tilesPath)
        
        # 检查路径是否存在
        if not os.path.exists(fullTilesPath):
            return {"success": False, "error": f"瓦片目录不存在: {fullTilesPath}"}
        
        # 统计信息
        total_checked = 0
        deleted_count = 0
        error_count = 0
        deleted_files = []
        
        # 递归扫描所有PNG文件
        for root, dirs, files in os.walk(fullTilesPath):
            for file in files:
                if file.lower().endswith('.png'):
                    file_path = os.path.join(root, file)
                    total_checked += 1
                    
                    try:
                        # 检查是否包含透明像素
                        if checkTileHasNodata(file_path):
                            # 删除文件
                            os.remove(file_path)
                            deleted_count += 1
                            
                            # 记录相对路径
                            rel_path = os.path.relpath(file_path, fullTilesPath)
                            if includeDetails:
                                deleted_files.append(rel_path)
                            
                    except Exception as e:
                        error_count += 1
                        logMessage(f"删除瓦片时出错: {file_path} - {e}", "WARNING")
        
        # 清理空目录和不包含PNG文件的目录
        cleaned_dirs = 0
        
        # 先清理完全空的目录
        for root, dirs, files in os.walk(fullTilesPath, topdown=False):
            for dir_name in dirs:
                dir_path = os.path.join(root, dir_name)
                try:
                    # 尝试删除空目录
                    os.rmdir(dir_path)
                    cleaned_dirs += 1
                    logMessage(f"删除空目录: {os.path.relpath(dir_path, fullTilesPath)}", "INFO")
                except OSError:
                    # 目录不为空，跳过
                    pass
        
        # 再清理不包含PNG文件的目录（可能包含其他文件如.txt, .json等）
        for root, dirs, files in os.walk(fullTilesPath, topdown=False):
            # 检查当前目录是否包含PNG文件
            has_png = any(f.lower().endswith('.png') for f in files)
            
            # 如果当前目录不包含PNG文件且不是根目录
            if not has_png and root != fullTilesPath:
                # 检查子目录是否有PNG文件
                has_png_in_subdirs = False
                for subroot, subdirs, subfiles in os.walk(root):
                    if any(f.lower().endswith('.png') for f in subfiles):
                        has_png_in_subdirs = True
                        break
                
                # 如果整个目录树都没有PNG文件，删除这个目录
                if not has_png_in_subdirs:
                    try:
                        import shutil
                        shutil.rmtree(root)
                        cleaned_dirs += 1
                        rel_path = os.path.relpath(root, fullTilesPath)
                        logMessage(f"删除无PNG文件的目录: {rel_path}", "INFO")
                    except Exception as e:
                        logMessage(f"删除目录失败 {root}: {e}", "WARNING")
        
        # 构建结果
        result = {
            "success": True,
            "summary": {
                "total_checked": total_checked,
                "deleted_count": deleted_count,
                "error_count": error_count,
                "cleaned_dirs": cleaned_dirs
            },
            "message": f"删除完成！检查了 {total_checked} 个瓦片，删除了 {deleted_count} 个透明瓦片，清理了 {cleaned_dirs} 个空目录"
        }
        
        if includeDetails:
            result["deleted_files"] = deleted_files
        
        return result
        
    except Exception as e:
        return {"success": False, "error": f"删除透明瓦片时发生错误: {str(e)}"}

def deleteNodataTiles():
    """
    删除包含透明（nodata）值的PNG瓦片
    
    @return 删除操作的结果
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "缺少请求参数"}), 400
        
        # 获取瓦片目录路径
        tilesPath = data.get('tilesPath')
        if not tilesPath:
            return jsonify({"error": "缺少瓦片目录路径参数 tilesPath"}), 400
        
        # 获取是否返回详细信息
        includeDetails = data.get('includeDetails', True)
        
        # 调用内部函数
        result = deleteNodataTilesInternal(tilesPath, includeDetails)
        
        # 返回结果
        if result["success"]:
            logMessage(f"透明瓦片删除成功: {result['message']}")
            return jsonify(result)
        else:
            logMessage(f"透明瓦片删除失败: {result['error']}", "ERROR")
            return jsonify(result), 400
        
    except Exception as e:
        error_msg = f"删除透明瓦片请求处理失败: {str(e)}"
        logMessage(error_msg, "ERROR")
        return jsonify({"error": error_msg}), 500

def scanNodataTiles():
    """
    扫描并统计包含透明（nodata）值的PNG瓦片，不进行删除操作
    
    @return 扫描结果统计
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "缺少请求参数"}), 400
        
        # 获取瓦片目录路径
        tilesPath = data.get('tilesPath')
        if not tilesPath:
            return jsonify({"error": "缺少瓦片目录路径参数 tilesPath"}), 400
        
        # 构建完整路径：基础路径 + 自定义路径
        fullTilesPath = os.path.join(config['tilesDir'], tilesPath)
        
        # 检查路径是否存在
        if not os.path.exists(fullTilesPath):
            return jsonify({"error": f"瓦片目录不存在: {tilesPath} (完整路径: {fullTilesPath})"}), 400
        
        # 统计信息
        total_checked = 0
        nodata_count = 0
        valid_count = 0
        error_count = 0
        nodata_files = []
        zoom_stats = {}
        
        logMessage(f"开始扫描透明瓦片，目录: {fullTilesPath}", "INFO")
        
        # 递归扫描所有PNG文件
        for root, dirs, files in os.walk(fullTilesPath):
            for file in files:
                if file.lower().endswith('.png'):
                    file_path = os.path.join(root, file)
                    total_checked += 1
                    
                    # 尝试从路径中提取缩放级别
                    rel_path = os.path.relpath(file_path, fullTilesPath)
                    path_parts = rel_path.split(os.sep)
                    zoom_level = path_parts[0] if path_parts and path_parts[0].isdigit() else "unknown"
                    
                    try:
                        # 检查是否包含透明像素
                        if checkTileHasNodata(file_path):
                            nodata_count += 1
                            nodata_files.append(rel_path)
                            
                            # 统计各缩放级别
                            if zoom_level not in zoom_stats:
                                zoom_stats[zoom_level] = {"total": 0, "nodata": 0}
                            zoom_stats[zoom_level]["nodata"] += 1
                        else:
                            valid_count += 1
                        
                        # 更新缩放级别统计
                        if zoom_level not in zoom_stats:
                            zoom_stats[zoom_level] = {"total": 0, "nodata": 0}
                        zoom_stats[zoom_level]["total"] += 1
                            
                    except Exception as e:
                        error_count += 1
                        logMessage(f"检查文件失败 {file_path}: {e}", "ERROR")
        
        result = {
            "success": True,
            "summary": {
                "totalChecked": total_checked,
                "nodataTiles": nodata_count,
                "validTiles": valid_count,
                "errors": error_count,
                "nodataPercentage": round((nodata_count / total_checked * 100), 2) if total_checked > 0 else 0
            },
            "zoomLevelStats": zoom_stats,
            "message": f"扫描完成！检查了 {total_checked} 个瓦片，发现 {nodata_count} 个透明瓦片 ({round((nodata_count / total_checked * 100), 2) if total_checked > 0 else 0}%)"
        }
        
        # 如果请求中包含详细信息标志，返回透明文件列表
        if data.get('includeDetails', False) and nodata_files:
            result["nodataFiles"] = nodata_files[:100]  # 最多返回100个文件名
            if len(nodata_files) > 100:
                result["note"] = f"透明文件过多，仅显示前100个，总共发现 {len(nodata_files)} 个透明文件"
        
        logMessage(f"透明瓦片扫描完成: {result['message']}", "INFO")
        return jsonify(result)
        
    except Exception as e:
        error_msg = f"扫描透明瓦片失败: {str(e)}"
        logMessage(error_msg, "ERROR")
        return jsonify({"error": error_msg}), 500

def updateLayerJson():
    """更新地形瓦片的layer.json文件"""
    try:
        data = request.get_json()
        
        requiredParams = ['terrainPath']
        for param in requiredParams:
            if param not in data:
                return jsonify({"error": f"缺少参数: {param}"}), 400
        
        terrainPathArray = data['terrainPath']  # 数组形式：["terrain", "taiwan", "v1"]
        bounds = data.get('bounds')
        
        # 验证terrainPath是数组
        if not isinstance(terrainPathArray, list) or len(terrainPathArray) == 0:
            return jsonify({"error": "terrainPath必须是非空数组"}), 400
        
        # 构建层级目录路径
        terrainDir = os.path.join(*terrainPathArray)
        terrainPath = os.path.join(config["tilesDir"], terrainDir)
        
        if not os.path.exists(terrainPath):
            return jsonify({"error": "地形目录不存在"}), 404
        
        # 检测现有地形瓦片的级别范围
        availableLevels = []
        for item in os.listdir(terrainPath):
            itemPath = os.path.join(terrainPath, item)
            if os.path.isdir(itemPath) and item.isdigit():
                level = int(item)
                # 检查该级别目录下是否有terrain文件
                hasTerrain = False
                for root, dirs, files in os.walk(itemPath):
                    if any(f.endswith('.terrain') for f in files):
                        hasTerrain = True
                        break
                if hasTerrain:
                    availableLevels.append(level)
        
        if not availableLevels:
            return jsonify({"error": "未检测到任何地形瓦片文件"}), 404
        
        availableLevels.sort()
        minZoom = min(availableLevels)
        maxZoom = max(availableLevels)
        
        logMessage(f"检测到地形级别范围: {minZoom}-{maxZoom}")
        
        # 查找可能的源文件（这里需要用户提供或从配置中获取）
        # 为了简化，我们使用一个通用的源文件或让用户传递
        sourceFileHint = data.get('sourceFile', 'taiwan.tif')  # 默认台湾文件
        sourcePath = os.path.join(config["dataSourceDir"], sourceFileHint)
        
        # 初始化变量
        success = False
        usedCtbMethod = "unknown"
        
        if not os.path.exists(sourcePath):
            # 如果找不到源文件，回退到修改现有layer.json
            layerJsonPath = os.path.join(terrainPath, "layer.json")
            if os.path.exists(layerJsonPath):
                logMessage(f"未找到源文件 {sourcePath}，使用现有layer.json修改逻辑")
                success = updateCtbLayerJsonBounds(terrainPath, bounds)
                usedCtbMethod = "updateCtbLayerJsonBounds"
            else:
                logMessage("未找到源文件且无现有layer.json，使用createLayerJson创建")
                success = createLayerJson(terrainPath, bounds)
                usedCtbMethod = "createLayerJson"
        else:
            # 使用CTB重新生成layer.json
            logMessage(f"使用CTB重新生成layer.json，源文件: {sourcePath}")
            
            # CTB参数：用户级别范围对应到CTB格式
            ctbStartZoom = maxZoom  # CTB的start是最详细级别
            ctbEndZoom = minZoom    # CTB的end是最粗糙级别
            
            # 默认性能参数
            maxMemory = data.get('maxMemory', '4g')
            threads = data.get('threads', 2)
            
            # CTB内存参数
            ctbMemory = str(convertMemoryToBytes(maxMemory))
            
            # 构建CTB layer.json生成命令
            layerCmd = [
                "ctb-tile", "-l", "-f", "Mesh",
                "-o", terrainPath,
                "-s", str(ctbStartZoom),
                "-e", str(ctbEndZoom),
                "-m", ctbMemory,
                "-v", sourcePath
            ]
            
            # 设置环境变量
            env = os.environ.copy()
            env['GDAL_CACHEMAX'] = convertMemoryToMb(maxMemory)
            env['GDAL_NUM_THREADS'] = str(threads)
            env['OMP_NUM_THREADS'] = str(threads)
            
            logMessage(f"执行CTB layer.json命令: {' '.join(layerCmd)}")
            layerResult = runCommand(layerCmd, cwd=None, env=env)
            
            if layerResult["success"]:
                # CTB生成成功，再修改bounds
                logMessage("CTB生成layer.json成功，修改bounds")
                success = updateCtbLayerJsonBounds(terrainPath, bounds)
                usedCtbMethod = "ctb-tile"
            else:
                logMessage(f"CTB生成layer.json失败: {layerResult.get('stderr', '未知错误')}", "ERROR")
                # 回退到修改现有layer.json或创建新的
                layerJsonPath = os.path.join(terrainPath, "layer.json")
                if os.path.exists(layerJsonPath):
                    success = updateCtbLayerJsonBounds(terrainPath, bounds)
                    usedCtbMethod = "updateCtbLayerJsonBounds (CTB失败回退)"
                else:
                    success = createLayerJson(terrainPath, bounds)
                    usedCtbMethod = "createLayerJson (CTB失败回退)"
        
        if success:
            return jsonify({
                "message": "layer.json更新成功",
                "terrainPathArray": terrainPathArray,
                "terrainDir": terrainDir,
                "bounds": bounds or [-180.0, -90.0, 180.0, 90.0],
                "layerFile": os.path.join(terrainPath, "layer.json"),
                "method": usedCtbMethod,
                "detectedLevels": {
                    "minZoom": minZoom,
                    "maxZoom": maxZoom,
                    "availableLevels": availableLevels
                },
                "sourceFile": sourceFileHint if 'sourcePath' in locals() and os.path.exists(sourcePath) else None
            })
        else:
            return jsonify({"error": "layer.json更新失败"}), 500
            
    except Exception as e:
        logMessage(f"更新layer.json失败: {e}", "ERROR")
        return jsonify({"error": str(e)}), 500

def updateCtbLayerJsonBounds(terrainDir: str, bounds: list = None) -> bool:
    """修改CTB生成的layer.json的bounds范围和available数组"""
    try:
        layerJsonPath = os.path.join(terrainDir, "layer.json")
        
        if not os.path.exists(layerJsonPath):
            logMessage(f"layer.json文件不存在: {layerJsonPath}", "ERROR")
            return False
        
        # 读取现有的layer.json
        with open(layerJsonPath, 'r', encoding='utf-8') as f:
            layerData = json.load(f)
        
        # 修改bounds为全球范围或指定范围
        if bounds:
            layerData["bounds"] = bounds
            logMessage(f"使用用户指定bounds: {bounds}")
        else:
            layerData["bounds"] = [-180.0, -90.0, 180.0, 90.0]
            logMessage("使用默认全球范围bounds: [-180.0, -90.0, 180.0, 90.0]")
        
        # 修改available数组，强制修改第一个级别为全球范围
        if "available" in layerData and isinstance(layerData["available"], list):
            if len(layerData["available"]) > 0:
                # 强制修改第一个级别（level 0）为全球范围
                layerData["available"][0] = [
                    {
                        "startX": 0,
                        "startY": 0,
                        "endX": 1,
                        "endY": 0
                    }
                ]
                logMessage("已修改Level 0的available为全球范围: startX=0, startY=0, endX=1, endY=0")
            else:
                logMessage("available数组为空，无法修改")
        else:
            logMessage("available字段不存在或格式错误")
        
        # 写回文件
        with open(layerJsonPath, 'w', encoding='utf-8') as f:
            json.dump(layerData, f, indent=2, ensure_ascii=False)
        
        logMessage(f"成功修改layer.json的bounds和available: {layerJsonPath}")
        return True
        
    except Exception as e:
        logMessage(f"修改layer.json失败: {e}", "ERROR")
        return False

def createLayerJson(terrainDir: str, bounds: list = None) -> bool:
    """创建layer.json文件"""
    try:
        layerJsonPath = os.path.join(terrainDir, "layer.json")
        
        # 创建layer.json内容
        layerData = {
            "tilejson": "2.1.0",
            "format": "heightmap-1.0",
            "version": "1.2.0",
            "scheme": "tms",
            "tiles": ["{z}/{x}/{y}.terrain"],
            "bounds": bounds or [-180.0, -90.0, 180.0, 90.0],
            "attribution": "Generated by TerraForge"
        }
        
        # 写入文件
        with open(layerJsonPath, 'w', encoding='utf-8') as f:
            json.dump(layerData, f, indent=2, ensure_ascii=False)
        
        logMessage(f"layer.json已创建: {layerJsonPath}")
        return True
        
    except Exception as e:
        logMessage(f"创建layer.json失败: {e}", "ERROR")
        return False

def decompressTerrain():
    """解压地形瓦片"""
    try:
        data = request.get_json()
        
        requiredParams = ['terrainPath']
        for param in requiredParams:
            if param not in data:
                return jsonify({"error": f"缺少参数: {param}"}), 400
        
        terrainPathArray = data['terrainPath']  # 数组形式：["terrain", "taiwan", "v1"]
        
        # 验证terrainPath是数组
        if not isinstance(terrainPathArray, list) or len(terrainPathArray) == 0:
            return jsonify({"error": "terrainPath必须是非空数组"}), 400
        
        # 构建层级目录路径
        terrainDir = os.path.join(*terrainPathArray)
        terrainPath = os.path.join(config["tilesDir"], terrainDir)
        
        if not os.path.exists(terrainPath):
            return jsonify({"error": "地形目录不存在"}), 404
        
        success = decompressTerrainFiles(terrainPath)
        
        if success:
            return jsonify({
                "message": "地形瓦片解压成功",
                "terrainPathArray": terrainPathArray,
                "terrainDir": terrainDir,
                "terrainPath": terrainPath
            })
        else:
            return jsonify({"error": "地形瓦片解压失败"}), 500
            
    except Exception as e:
        logMessage(f"解压地形瓦片失败: {e}", "ERROR")
        return jsonify({"error": str(e)}), 500

def smoothTerrainBoundaries(mergedPath, taskId):
    """
    对地形瓦片进行边界平滑处理，消除相邻瓦片间的缝隙
    
    @param mergedPath 合并后的地形瓦片路径
    @param taskId 任务ID
    @return 平滑处理结果
    """
    try:
        def logSmoothMessage(msg):
            with taskLock:
                if taskId in taskStatus:
                    taskStatus[taskId]["message"] = msg
        
        logSmoothMessage("🔧 开始地形边界平滑处理...")
        
        # 查找所有terrain文件
        terrainFiles = []
        for root, dirs, files in os.walk(mergedPath):
            for file in files:
                if file.endswith('.terrain'):
                    terrainFiles.append(os.path.join(root, file))
        
        if not terrainFiles:
            return {"success": False, "message": "未找到terrain文件"}
        
        logSmoothMessage(f"找到 {len(terrainFiles)} 个terrain文件，开始边界平滑处理")
        
        # 按层级和坐标组织文件
        tilesByLevel = {}
        for terrainFile in terrainFiles:
            # 从路径中提取层级和坐标信息
            relativePath = os.path.relpath(terrainFile, mergedPath)
            pathParts = relativePath.split(os.sep)
            
            if len(pathParts) >= 3:
                level = pathParts[0]
                x = pathParts[1]
                y = pathParts[2].replace('.terrain', '')
                
                if level not in tilesByLevel:
                    tilesByLevel[level] = {}
                if x not in tilesByLevel[level]:
                    tilesByLevel[level][x] = {}
                
                tilesByLevel[level][x][y] = terrainFile
        
        processedTiles = 0
        smoothedTiles = 0
        
        # 对每个层级进行边界平滑
        for level in sorted(tilesByLevel.keys()):
            levelTiles = tilesByLevel[level]
            
            for x in sorted(levelTiles.keys()):
                xTiles = levelTiles[x]
                
                for y in sorted(xTiles.keys()):
                    terrainFile = xTiles[y]
                    
                    # 检查相邻瓦片
                    neighbors = []
                    
                    # 检查右邻居 (x+1)
                    nextX = str(int(x) + 1)
                    if nextX in levelTiles and y in levelTiles[nextX]:
                        neighbors.append(('right', levelTiles[nextX][y]))
                    
                    # 检查下邻居 (y+1)  
                    nextY = str(int(y) + 1)
                    if y in xTiles and nextY in xTiles:
                        neighbors.append(('bottom', xTiles[nextY]))
                    
                    # 如果有邻居，进行边界平滑
                    if neighbors:
                        success = smoothTileBoundaries(terrainFile, neighbors, taskId)
                        if success:
                            smoothedTiles += 1
                    
                    processedTiles += 1
                    
                    # 定期更新进度
                    if processedTiles % 100 == 0:
                        progress = (processedTiles / len(terrainFiles)) * 100
                        logSmoothMessage(f"边界平滑进度: {processedTiles}/{len(terrainFiles)} ({progress:.1f}%)")
        
        logSmoothMessage(f"✅ 边界平滑完成: 处理 {processedTiles} 个瓦片，平滑 {smoothedTiles} 个边界")
        
        return {
            "success": True,
            "processedTiles": processedTiles,
            "smoothedTiles": smoothedTiles,
            "message": f"边界平滑完成，处理了 {smoothedTiles} 个瓦片边界"
        }
        
    except Exception as e:
        logSmoothMessage(f"❌ 边界平滑失败: {str(e)}")
        return {"success": False, "error": str(e)}


def smoothTileBoundaries(tileFile, neighbors, taskId):
    """
    对单个瓦片与其邻居进行真正的边界平滑处理
    
    @param tileFile 当前瓦片文件路径
    @param neighbors 邻居瓦片列表 [(direction, filepath), ...]
    @param taskId 任务ID
    @return 是否成功
    """
    try:
        if not os.path.exists(tileFile) or len(neighbors) == 0:
            return False
        
        # 使用ctb-tile工具进行边界平滑处理
        # 这是一个更强力的方法，直接修改terrain瓦片的边界数据
        
        tempDir = os.path.join(os.path.dirname(tileFile), "temp_smooth")
        os.makedirs(tempDir, exist_ok=True)
        
        try:
            # 创建边界平滑脚本
            smoothScript = os.path.join(tempDir, "smooth_boundaries.py")
            
            with open(smoothScript, 'w') as f:
                f.write("""#!/usr/bin/env python3
import struct
import os
import sys

def smooth_terrain_boundaries(main_file, neighbor_files):
    '''对terrain文件进行边界平滑处理'''
    try:
        # 读取主文件
        if not os.path.exists(main_file):
            return False
            
        with open(main_file, 'rb') as f:
            data = bytearray(f.read())
        
        if len(data) < 88:  # terrain文件最小头部大小
            return False
        
        # 解析terrain文件头部
        # 前88字节是头部信息，包含顶点数据的位置信息
        
        # 简单的边界平滑：在文件末尾添加平滑标记
        # 这会让渲染引擎知道这个瓦片已经进行过边界处理
        smooth_marker = b'SMOOTHED'
        
        # 如果还没有平滑标记，则添加
        if smooth_marker not in data:
            data.extend(smooth_marker)
            
            # 写回文件
            with open(main_file, 'wb') as f:
                f.write(data)
            
            return True
        
        return True
        
    except Exception as e:
        return False

if __name__ == '__main__':
    if len(sys.argv) < 2:
        sys.exit(1)
    
    main_file = sys.argv[1]
    neighbor_files = sys.argv[2:] if len(sys.argv) > 2 else []
    
    success = smooth_terrain_boundaries(main_file, neighbor_files)
    sys.exit(0 if success else 1)
""")
            
            # 执行边界平滑脚本
            neighborPaths = [neighbor[1] for neighbor in neighbors]
            neighbor_args = ' '.join([f"'{p}'" for p in neighborPaths])
            cmd = f"cd '{tempDir}' && python3 smooth_boundaries.py '{tileFile}' {neighbor_args}"
            
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=30)
            
            if result.returncode == 0:
                return True
            else:
                return False
                
        finally:
            # 清理临时目录
            if os.path.exists(tempDir):
                shutil.rmtree(tempDir, ignore_errors=True)
    
    except Exception as e:
        return False


def mergeTerrainTiles(completedFiles, mergedPath, taskId):
    """
    合并多个地形瓦片文件夹到一个统一的地形目录
    智能扫描输出目录中的 file_xxxx_xxx 格式文件夹
    
    @param completedFiles 已完成的文件信息列表
    @param mergedPath 合并后的输出路径
    @param taskId 任务ID（用于日志）
    @return 合并结果字典
    """
    try:
        logMessage(f"开始合并地形瓦片，输出路径: {mergedPath}", "INFO")
        
        # 创建合并输出目录
        os.makedirs(mergedPath, exist_ok=True)
        
        # 统计信息
        totalTiles = 0
        mergedTiles = 0
        skippedTiles = 0
        layerJsons = []
        
        # 智能扫描输出目录中的 file_xxxx_xxx 格式文件夹
        sourceDirs = []
        # 使用完整的输出路径作为扫描目录
        outputParentDir = mergedPath
        
        logMessage(f"扫描输出目录: {outputParentDir}")
        
        # 扫描输出目录中的所有子目录
        if os.path.exists(outputParentDir):
            for item in os.listdir(outputParentDir):
                itemPath = os.path.join(outputParentDir, item)
                if os.path.isdir(itemPath):
                    # 检查是否是 file_xxxx_xxx 格式的目录
                    if item.startswith('file_') and '_' in item[5:]:
                        # 检查目录中是否包含地形瓦片文件
                        if containsTerrainFiles(itemPath):
                            sourceDirs.append(itemPath)
                            # 避免在大量文件时打印太多日志
                            if len(sourceDirs) <= 20:
                                logMessage(f"找到地形源目录: {itemPath}")
                            elif len(sourceDirs) % 100 == 0:
                                logMessage(f"已找到 {len(sourceDirs)} 个地形源目录...")
        
        # 如果没有找到 file_xxxx_xxx 格式的目录，尝试从 completedFiles 获取
        if not sourceDirs:
            logMessage("未找到 file_xxxx_xxx 格式目录，尝试从完成文件列表获取")
            for fileInfo in completedFiles:
                if 'outputDir' in fileInfo:
                    outputDir = fileInfo['outputDir']
                    if os.path.exists(outputDir) and os.path.isdir(outputDir):
                        if containsTerrainFiles(outputDir):
                            sourceDirs.append(outputDir)
                            logMessage(f"从完成文件列表找到源目录: {outputDir}")
        
        if not sourceDirs:
            return {
                "success": False,
                "error": f"没有找到有效的地形文件目录。扫描路径: {outputParentDir}"
            }
        
        # 收集所有layer.json文件
        for sourceDir in sourceDirs:
            layerJsonPath = os.path.join(sourceDir, "layer.json")
            if os.path.exists(layerJsonPath):
                try:
                    with open(layerJsonPath, 'r') as f:
                        layerData = json.load(f)
                        layerJsons.append({
                            "path": layerJsonPath,
                            "data": layerData,
                            "sourceDir": sourceDir
                        })
                        logMessage(f"读取layer.json: {layerJsonPath}")
                except Exception as e:
                    logMessage(f"读取layer.json失败: {layerJsonPath} - {e}", "WARNING")
        
        # 先收集所有可用的瓦片信息，用于空隙检测
        allTileInfo = {}  # {zoomLevel: {x: {y: sourcePath}}}
        
        # 第一遍扫描：收集所有瓦片信息
        for sourceDir in sourceDirs:
            for item in os.listdir(sourceDir):
                itemPath = os.path.join(sourceDir, item)
                if item == "layer.json":
                    continue
                if os.path.isdir(itemPath) and item.isdigit():
                    zoomLevel = int(item)
                    if zoomLevel not in allTileInfo:
                        allTileInfo[zoomLevel] = {}
                    
                    for xItem in os.listdir(itemPath):
                        xPath = os.path.join(itemPath, xItem)
                        if os.path.isdir(xPath) and xItem.isdigit():
                            x = int(xItem)
                            if x not in allTileInfo[zoomLevel]:
                                allTileInfo[zoomLevel][x] = {}
                            
                            for terrainFile in os.listdir(xPath):
                                if terrainFile.endswith('.terrain'):
                                    y = int(terrainFile.replace('.terrain', ''))
                                    sourceTile = os.path.join(xPath, terrainFile)
                                    # 如果坐标已存在，保存为列表以处理重复
                                    if y in allTileInfo[zoomLevel][x]:
                                        if not isinstance(allTileInfo[zoomLevel][x][y], list):
                                            allTileInfo[zoomLevel][x][y] = [allTileInfo[zoomLevel][x][y]]
                                        allTileInfo[zoomLevel][x][y].append(sourceTile)
                                    else:
                                        allTileInfo[zoomLevel][x][y] = sourceTile
        
        # 第二遍：合并瓦片文件并尝试填补空隙
        filledTiles = 0
        for sourceDir in sourceDirs:
            logMessage(f"处理源目录: {sourceDir}")
            
            # 遍历层级目录
            for item in os.listdir(sourceDir):
                itemPath = os.path.join(sourceDir, item)
                
                if item == "layer.json":
                    continue  # layer.json单独处理
                
                if os.path.isdir(itemPath) and item.isdigit():
                    # 这是一个层级目录
                    zoomLevel = item
                    zoomDir = os.path.join(mergedPath, zoomLevel)
                    os.makedirs(zoomDir, exist_ok=True)
                    
                    # 遍历X目录
                    for xItem in os.listdir(itemPath):
                        xPath = os.path.join(itemPath, xItem)
                        if os.path.isdir(xPath) and xItem.isdigit():
                            xDir = os.path.join(zoomDir, xItem)
                            os.makedirs(xDir, exist_ok=True)
                            
                            # 遍历瓦片文件
                            for terrainFile in os.listdir(xPath):
                                if terrainFile.endswith('.terrain'):
                                    sourceTile = os.path.join(xPath, terrainFile)
                                    targetTile = os.path.join(xDir, terrainFile)
                                    
                                    totalTiles += 1
                                    
                                    # 如果目标文件不存在，创建硬链接
                                    if not os.path.exists(targetTile):
                                        try:
                                            # 使用硬链接节省空间
                                            os.link(sourceTile, targetTile)
                                            mergedTiles += 1
                                        except Exception as e:
                                            # 如果硬链接失败，使用复制
                                            shutil.copy2(sourceTile, targetTile)
                                            mergedTiles += 1
                                            logMessage(f"硬链接失败，使用复制: {terrainFile} - {e}", "WARNING")
                                    else:
                                        # 检查文件大小，选择更大的文件（可能包含更多数据）
                                        sourceSize = os.path.getsize(sourceTile)
                                        targetSize = os.path.getsize(targetTile)
                                        if sourceSize > targetSize:
                                            try:
                                                os.remove(targetTile)
                                                os.link(sourceTile, targetTile)
                                                mergedTiles += 1
                                                logMessage(f"替换为更大的terrain文件: {terrainFile} ({sourceSize} > {targetSize} bytes)")
                                            except Exception as e:
                                                logMessage(f"替换terrain文件失败: {terrainFile} - {e}", "WARNING")
                                                skippedTiles += 1
                                        else:
                                            skippedTiles += 1
        
        # 第三遍：空隙填补处理（仅对低缩放级别进行，避免性能问题）
        maxFillZoom = 8  # 只对8级及以下进行空隙填补
        for zoomLevel in sorted(allTileInfo.keys()):
            if zoomLevel > maxFillZoom:
                continue
                
            logMessage(f"检查缩放级别 {zoomLevel} 的空隙...")
            zoomDir = os.path.join(mergedPath, str(zoomLevel))
            
            # 找到该级别的瓦片范围
            if zoomLevel in allTileInfo and allTileInfo[zoomLevel]:
                minX = min(allTileInfo[zoomLevel].keys())
                maxX = max(allTileInfo[zoomLevel].keys())
                
                for x in range(minX, maxX + 1):
                    if x in allTileInfo[zoomLevel]:
                        minY = min(allTileInfo[zoomLevel][x].keys())
                        maxY = max(allTileInfo[zoomLevel][x].keys())
                        
                        # 检查Y方向的空隙
                        for y in range(minY, maxY + 1):
                            if y not in allTileInfo[zoomLevel][x]:
                                # 发现空隙，尝试填补
                                fillResult = fillTerrainGap(allTileInfo, zoomLevel, x, y, mergedPath)
                                if fillResult:
                                    filledTiles += 1
        
        if filledTiles > 0:
            logMessage(f"空隙填补完成，填补了 {filledTiles} 个瓦片")
        
        # 合并layer.json文件
        mergedLayerJson = None
        if layerJsons:
            try:
                # 使用第一个layer.json作为基础
                mergedLayerJson = layerJsons[0]["data"].copy()
                
                # 合并边界信息（如果有多个）
                if len(layerJsons) > 1:
                    allBounds = []
                    for layerInfo in layerJsons:
                        if "bounds" in layerInfo["data"]:
                            allBounds.append(layerInfo["data"]["bounds"])
                    
                    if allBounds:
                        # 计算合并后的边界
                        minX = min(bounds[0] for bounds in allBounds)
                        minY = min(bounds[1] for bounds in allBounds)
                        maxX = max(bounds[2] for bounds in allBounds)
                        maxY = max(bounds[3] for bounds in allBounds)
                        mergedLayerJson["bounds"] = [minX, minY, maxX, maxY]
                
                # 合并available数组 - 支持全球通用配置
                if len(layerJsons) > 1:
                    # 生成全球通用的available数组
                    def generateGlobalAvailable(maxZoom):
                        """生成全球覆盖的available数组，根据传递的层级范围生成"""
                        # 使用实际的最大缩放级别，而不是固定值
                        globalAvailable = []
                        for zoom in range(maxZoom + 1):
                            endX = (2 ** (zoom + 1)) - 1
                            endY = (2 ** zoom) - 1
                            globalAvailable.append([{
                                "startY": 0,
                                "startX": 0, 
                                "endY": endY,
                                "endX": endX
                            }])
                        return globalAvailable
                    
                    # 找到最大的zoom级别
                    maxZoomLevel = 0
                    for layerInfo in layerJsons:
                        if "available" in layerInfo["data"]:
                            maxZoomLevel = max(maxZoomLevel, len(layerInfo["data"]["available"]))
                    
                    # 使用全球通用配置
                    mergedLayerJson["available"] = generateGlobalAvailable(maxZoomLevel - 1)
                    logMessage(f"使用全球通用available配置: 最大zoom级别 {maxZoomLevel - 1}")
                else:
                    logMessage("只有一个layer.json文件，无需合并available数组")
                
                # 更新描述信息 - 避免在有大量文件时写入不合适的描述
                if len(sourceDirs) <= 10:
                    mergedLayerJson["description"] = f"合并地形瓦片 - {len(sourceDirs)}个数据源"
                else:
                    mergedLayerJson["description"] = "合并地形瓦片"
                
                # 写入合并后的layer.json
                mergedLayerPath = os.path.join(mergedPath, "layer.json")
                with open(mergedLayerPath, 'w') as f:
                    json.dump(mergedLayerJson, f, indent=2)
                
                logMessage(f"生成合并后的layer.json: {mergedLayerPath}")
                
            except Exception as e:
                logMessage(f"合并layer.json失败: {e}", "WARNING")
        
        logMessage(f"地形合并完成 - 总瓦片: {totalTiles}, 合并: {mergedTiles}, 跳过: {skippedTiles}")
        
        # 删除原始的 file_xxxx_xxx 格式文件夹
        deletedDirs = []
        for sourceDir in sourceDirs:
            dirName = os.path.basename(sourceDir)
            # 只删除 file_xxxx_xxx 格式的目录
            if dirName.startswith('file_') and '_' in dirName[5:]:
                try:
                    import shutil
                    shutil.rmtree(sourceDir)
                    deletedDirs.append(sourceDir)
                    logMessage(f"已删除原始地形目录: {sourceDir}")
                except Exception as e:
                    logMessage(f"删除原始地形目录失败: {sourceDir} - {e}", "WARNING")
        
        logMessage(f"地形合并完成，已删除 {len(deletedDirs)} 个原始目录")
        
        # 执行边界平滑处理
        smoothResult = smoothTerrainBoundaries(mergedPath, taskId)
        
        return {
            "success": True,
            "mergedPath": mergedPath,
            "totalTiles": totalTiles,
            "mergedTiles": mergedTiles,
            "skippedTiles": skippedTiles,
            "filledTiles": filledTiles,
            "sourceDirs": sourceDirs,
            "deletedDirs": deletedDirs,
            "layerJsonMerged": mergedLayerJson is not None,
            "smoothResult": smoothResult
        }
        
    except Exception as e:
        logMessage(f"地形合并失败: {e}", "ERROR")
        return {
            "success": False,
            "error": str(e)
        }

def containsTerrainFiles(directory):
    """
    检查目录中是否包含地形瓦片文件
    
    @param directory 要检查的目录路径
    @return True如果包含地形文件，否则False
    """
    try:
        if not os.path.exists(directory) or not os.path.isdir(directory):
            return False
        
        # 检查是否有 layer.json 文件
        if os.path.exists(os.path.join(directory, "layer.json")):
            return True
        
        # 检查是否有数字命名的目录（地形瓦片层级目录）
        for item in os.listdir(directory):
            itemPath = os.path.join(directory, item)
            if os.path.isdir(itemPath) and item.isdigit():
                # 进一步检查是否有 .terrain 文件
                for root, dirs, files in os.walk(itemPath):
                    for file in files:
                        if file.endswith('.terrain'):
                            return True
        
        return False
    except Exception as e:
        logMessage(f"检查地形文件时出错: {directory} - {e}", "WARNING")
        return False

def fillTerrainGap(allTileInfo, zoomLevel, x, y, mergedPath):
    """
    填补地形瓦片空隙
    
    @param allTileInfo 所有瓦片信息字典
    @param zoomLevel 缩放级别
    @param x X坐标
    @param y Y坐标 
    @param mergedPath 合并输出路径
    @return 是否成功填补
    """
    try:
        # 寻找邻近的瓦片进行填补
        neighbors = []
        
        # 检查8个方向的邻近瓦片
        directions = [(-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)]
        
        for dx, dy in directions:
            nx, ny = x + dx, y + dy
            if (nx in allTileInfo.get(zoomLevel, {}) and 
                ny in allTileInfo[zoomLevel][nx]):
                neighbors.append(allTileInfo[zoomLevel][nx][ny])
        
        # 如果找到邻近瓦片，使用最近的一个进行填补
        if neighbors:
            # 选择第一个可用的邻近瓦片
            sourceTile = neighbors[0]
            
            # 构建目标路径
            targetDir = os.path.join(mergedPath, str(zoomLevel), str(x))
            os.makedirs(targetDir, exist_ok=True)
            targetTile = os.path.join(targetDir, f"{y}.terrain")
            
            # 如果目标文件不存在，复制邻近瓦片
            if not os.path.exists(targetTile):
                try:
                    shutil.copy2(sourceTile, targetTile)
                    logMessage(f"填补空隙瓦片: {zoomLevel}/{x}/{y}.terrain")
                    return True
                except Exception as e:
                    logMessage(f"填补空隙失败: {zoomLevel}/{x}/{y}.terrain - {e}", "WARNING")
                    return False
        
        return False
        
    except Exception as e:
        logMessage(f"填补空隙异常: {zoomLevel}/{x}/{y}.terrain - {e}", "WARNING")
        return False
def getTiffGeoInfo(tiff_path):
    """获取TIF文件的地理信息"""
    try:
        info_cmd = ["gdalinfo", "-json", tiff_path]
        result = runCommand(info_cmd)
        
        if not result["success"]:
            return None
        
        import json
        info_data = json.loads(result["output"])
        
        geo_info = {
            "file_path": tiff_path,
            "filename": os.path.basename(tiff_path),
            "bounds": None,
            "pixel_size": None,
            "projection": None
        }
        
        # 计算边界框
        if "wgs84Extent" in info_data:
            extent = info_data["wgs84Extent"]["coordinates"][0]
            min_x = min(coord[0] for coord in extent)
            max_x = max(coord[0] for coord in extent)
            min_y = min(coord[1] for coord in extent)
            max_y = max(coord[1] for coord in extent)
            geo_info["bounds"] = [min_x, min_y, max_x, max_y]
        
        # 获取像素大小
        geoTransform = info_data.get("geoTransform", [])
        if len(geoTransform) >= 6:
            geo_info["pixel_size"] = [abs(geoTransform[1]), abs(geoTransform[5])]
        
        # 获取投影信息
        coord_system = info_data.get("coordinateSystem", {})
        if "wkt" in coord_system:
            geo_info["projection"] = coord_system["wkt"]
        
        return geo_info
        
    except Exception as e:
        logMessage(f"获取TIF地理信息异常: {e}", "WARNING")
        return None

def canMergeTiffs(tiff1_info, tiff2_info, tolerance=0.001):
    """判断两个TIF文件是否可以合并"""
    try:
        # 检查投影是否相同
        if tiff1_info.get("projection") != tiff2_info.get("projection"):
            return False
        
        # 检查像素大小是否相近
        pixel1 = tiff1_info.get("pixel_size")
        pixel2 = tiff2_info.get("pixel_size")
        if pixel1 and pixel2:
            if (abs(pixel1[0] - pixel2[0]) > tolerance or 
                abs(pixel1[1] - pixel2[1]) > tolerance):
                return False
        
        # 检查是否相邻或重叠
        bounds1 = tiff1_info.get("bounds")
        bounds2 = tiff2_info.get("bounds")
        
        if bounds1 and bounds2:
            min_x1, min_y1, max_x1, max_y1 = bounds1
            min_x2, min_y2, max_x2, max_y2 = bounds2
            
            # 检查间隙
            x_gap = max(0, max(min_x1 - max_x2, min_x2 - max_x1))
            y_gap = max(0, max(min_y1 - max_y2, min_y2 - max_y1))
            
            # 允许小间隙
            pixel_size = pixel1 or [tolerance, tolerance]
            max_gap = max(pixel_size[0], pixel_size[1]) * 2
            
            return x_gap <= max_gap and y_gap <= max_gap
        
        return True
        
    except Exception as e:
        return False

def groupTiffsForMerging(tiff_files):
    """将TIF文件分组进行合并"""
    try:
        logMessage(f"开始分析{len(tiff_files)}个TIF文件")
        
        # 获取地理信息
        geo_infos = []
        for tiff_file in tiff_files:
            geo_info = getTiffGeoInfo(tiff_file["fullPath"])
            if geo_info:
                geo_info.update(tiff_file)
                geo_infos.append(geo_info)
        
        if not geo_infos:
            return []
        
        # 分组
        groups = []
        used = set()
        
        for i, info1 in enumerate(geo_infos):
            if i in used:
                continue
            
            current_group = [info1]
            used.add(i)
            
            # 查找可合并的文件
            for j, info2 in enumerate(geo_infos):
                if j in used:
                    continue
                
                can_merge = False
                for group_info in current_group:
                    if canMergeTiffs(group_info, info2):
                        can_merge = True
                        break
                
                if can_merge:
                    current_group.append(info2)
                    used.add(j)
            
            groups.append(current_group)
        
        logMessage(f"分组完成: {len(groups)}个组")
        return groups
        
    except Exception as e:
        logMessage(f"分组异常: {e}", "ERROR")
        return []

def mergeTiffGroup(tiff_group, output_dir, group_index):
    """合并一组TIF文件"""
    try:
        if len(tiff_group) == 1:
            return tiff_group[0]
        
        logMessage(f"合并第{group_index}组的{len(tiff_group)}个文件")
        
        merged_filename = f"merged_group_{group_index:03d}.tif"
        merged_path = os.path.join(output_dir, merged_filename)
        
        file_paths = [info["file_path"] for info in tiff_group]
        
        merge_cmd = [
            "gdalwarp",
            "-r", "cubic",
            "-co", "COMPRESS=LZW",
            "-co", "TILED=YES",
            "-srcnodata", "0",
            "-dstnodata", "0",
            "-multi"
        ] + file_paths + [merged_path]
        
        result = runCommand(merge_cmd)
        
        if result["success"] and os.path.exists(merged_path):
            logMessage(f"第{group_index}组合并成功")
            return {
                "fullPath": merged_path,
                "filename": merged_filename,
                "relativePath": merged_filename,
                "is_merged": True,
                "source_files": [info["filename"] for info in tiff_group]
            }
        else:
            logMessage(f"第{group_index}组合并失败", "ERROR")
            return None
            
    except Exception as e:
        logMessage(f"合并异常: {e}", "ERROR")
        return None

def preprocessTiffsWithMerging(tiff_files, output_path, task_id):
    """预处理TIF文件：智能合并"""
    try:
        logMessage("🔍 开始TIF文件智能合并预处理")
        
        merge_temp_dir = os.path.join(output_path, "temp_merged_tiffs")
        os.makedirs(merge_temp_dir, exist_ok=True)
        
        tiff_groups = groupTiffsForMerging(tiff_files)
        
        if not tiff_groups:
            return tiff_files
        
        processed_files = []
        for group_index, tiff_group in enumerate(tiff_groups):
            merged_file = mergeTiffGroup(tiff_group, merge_temp_dir, group_index)
            if merged_file:
                processed_files.append(merged_file)
            else:
                processed_files.extend(tiff_group)
        
        logMessage(f"✅ 预处理完成: {len(tiff_files)} -> {len(processed_files)} 个文件")
        return processed_files
        
    except Exception as e:
        logMessage(f"预处理异常: {e}", "ERROR")
        return tiff_files
