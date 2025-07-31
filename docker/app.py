#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
from flask import Flask, request, jsonify
from flask_cors import CORS
from datetime import datetime
import urllib.parse

# 导入配置和服务
from config import config
from service import *
from utils import logMessage, validateWorkspacePath, formatFileSize

# 创建Flask应用
app = Flask(__name__)

# 配置CORS
CORS(app, resources={
    r"/api/*": {
        "origins": "*",
        "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
        "allow_headers": ["Content-Type", "Authorization"]
    }
})

# 配置JSON编码以正确显示中文
app.config['JSON_AS_ASCII'] = False
app.json.ensure_ascii = False

# 添加OPTIONS方法支持
@app.before_request
def handle_options():
    if request.method == "OPTIONS":
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type, Authorization'
        }
        return ('', 204, headers)

# 注册路由 - 使用小驼峰命名
@app.route('/api/health', methods=['GET'])
def health_check():
    return healthCheck()

@app.route('/api/dataSources', methods=['GET'])
@app.route('/api/dataSources/<path:subpath>', methods=['GET'])
def list_data_sources(subpath=''):
    return listDataSources(subpath)

@app.route('/api/dataSources/info/<path:filename>', methods=['GET'])
def get_data_source_info(filename):
    return getDataSourceInfo(filename)

# 工作空间浏览路由
@app.route('/api/results', methods=['GET'])
def browse_results():
    """浏览工作空间目录"""
    return browseDirectory()

@app.route('/api/tile/terrain', methods=['POST'])
def create_terrain_tiles():
    return createTerrainTiles()

@app.route('/api/tile/indexedTiles', methods=['POST'])
def create_indexed_tiles():
    return createIndexedTiles()

@app.route('/api/tile/convert', methods=['POST'])
def convert_tile_format():
    return convertTileFormat()



@app.route('/api/tasks/<taskId>', methods=['GET'])
def get_task_status(taskId):
    return getTaskStatus(taskId)

@app.route('/api/tasks', methods=['GET'])
def list_tasks():
    return listTasks()

@app.route('/api/tasks/cleanup', methods=['POST'])
def cleanup_tasks():
    return cleanupTasks()

@app.route('/api/tasks/<taskId>/stop', methods=['POST'])
def stop_task(taskId):
    return stopTask(taskId)

@app.route('/api/tasks/<taskId>', methods=['DELETE'])
def delete_task(taskId):
    return deleteTask(taskId)

@app.route('/api/config/recommend', methods=['POST'])
def recommend_config():
    return recommendConfig()

@app.route('/api/system/info', methods=['GET'])
def system_info():
    return systemInfo()

@app.route('/api/container/update', methods=['POST'])
def update_container():
    return updateContainerInfo()

@app.route('/api/routes', methods=['GET'])
def list_api_routes():
    return listApiRoutes()

# 工作空间管理路由
@app.route('/api/workspace/createFolder', methods=['POST'])
def create_workspace_folder():
    """创建工作空间文件夹"""
    try:
        data = request.get_json()
        folderPath = data.get('folderPath', '')
        
        if not folderPath:
            return jsonify({"error": "缺少参数: folderPath"}), 400
        
        # 验证路径
        isValid, fullPath = validateWorkspacePath(folderPath)
        if not isValid:
            return jsonify({"error": fullPath}), 400
        
        # 创建文件夹
        if not os.path.exists(fullPath):
            os.makedirs(fullPath, exist_ok=True)
            return jsonify({
                "success": True,
                "message": "文件夹创建成功",
                "folderPath": folderPath
            })
        else:
            return jsonify({"error": "文件夹已存在"}), 409
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/workspace/folder/<path:folderPath>', methods=['DELETE', 'OPTIONS'])
def delete_workspace_folder(folderPath):
    """删除工作空间文件夹"""
    if request.method == 'OPTIONS':
        return ('', 204)
        
    try:
        logMessage(f"收到删除文件夹请求: {folderPath}")
        
        # URL 解码
        folderPath = urllib.parse.unquote(folderPath)
        logMessage(f"URL解码后的路径: {folderPath}")
        
        # 验证路径
        isValid, fullPath = validateWorkspacePath(folderPath)
        logMessage(f"路径验证结果: isValid={isValid}, fullPath={fullPath}")
        
        if not isValid:
            logMessage(f"路径验证失败: {fullPath}")
            return jsonify({"error": fullPath}), 400
        
        # 检查文件夹是否存在
        folderExists = os.path.exists(fullPath)
        isDir = os.path.isdir(fullPath)
        logMessage(f"文件夹检查: exists={folderExists}, isDir={isDir}, path={fullPath}")
        
        if folderExists and isDir:
            try:
                import shutil
                shutil.rmtree(fullPath)
                logMessage(f"文件夹删除成功: {fullPath}")
                return jsonify({
                    "success": True,
                    "message": "文件夹删除成功",
                    "folderPath": folderPath
                })
            except Exception as e:
                logMessage(f"文件夹删除操作失败: {str(e)}", "ERROR")
                return jsonify({
                    "error": f"删除失败: {str(e)}",
                    "folderPath": folderPath
                }), 500
        else:
            logMessage(f"文件夹不存在: {fullPath}")
            return jsonify({
                "error": "文件夹不存在",
                "folderPath": folderPath,
                "fullPath": fullPath,
                "exists": folderExists,
                "isDir": isDir
            }), 404
    
    except Exception as e:
        logMessage(f"删除文件夹处理失败: {str(e)}", "ERROR")
        return jsonify({
            "error": str(e),
            "folderPath": folderPath
        }), 500

@app.route('/api/workspace/folder/<path:folderPath>/rename', methods=['PUT'])
def rename_workspace_folder(folderPath):
    """重命名工作空间文件夹"""
    try:
        data = request.get_json()
        newName = data.get('newName', '')
        
        if not newName:
            return jsonify({"error": "缺少参数: newName"}), 400
        
        # 验证原路径
        isValid, fullPath = validateWorkspacePath(folderPath)
        if not isValid:
            return jsonify({"error": fullPath}), 400
        
        if not os.path.exists(fullPath):
            return jsonify({"error": "源文件夹不存在"}), 404
        
        # 构建新路径
        parentDir = os.path.dirname(fullPath)
        newPath = os.path.join(parentDir, newName)
        
        if os.path.exists(newPath):
            return jsonify({"error": "目标文件夹已存在"}), 409
        
        # 重命名
        os.rename(fullPath, newPath)
        
        return jsonify({
            "success": True,
            "message": "文件夹重命名成功",
            "oldPath": folderPath,
            "newPath": os.path.relpath(newPath, config["tilesDir"])
        })
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/workspace/file/<path:filePath>', methods=['DELETE', 'OPTIONS'])
def delete_workspace_file(filePath):
    """删除工作空间文件"""
    if request.method == 'OPTIONS':
        return ('', 204)
        
    try:
        logMessage(f"收到删除文件请求: {filePath}")
        
        # URL 解码
        filePath = urllib.parse.unquote(filePath)
        logMessage(f"URL解码后的路径: {filePath}")
        
        # 验证路径
        isValid, fullPath = validateWorkspacePath(filePath)
        logMessage(f"路径验证结果: isValid={isValid}, fullPath={fullPath}")
        
        if not isValid:
            logMessage(f"路径验证失败: {fullPath}")
            return jsonify({"error": fullPath}), 400
        
        # 检查文件是否存在
        fileExists = os.path.exists(fullPath)
        isFile = os.path.isfile(fullPath)
        logMessage(f"文件检查: exists={fileExists}, isFile={isFile}, path={fullPath}")
        
        if fileExists and isFile:
            try:
                os.remove(fullPath)
                logMessage(f"文件删除成功: {fullPath}")
                return jsonify({
                    "success": True,
                    "message": "文件删除成功",
                    "filePath": filePath
                })
            except Exception as e:
                logMessage(f"文件删除操作失败: {str(e)}", "ERROR")
                return jsonify({
                    "error": f"删除失败: {str(e)}",
                    "filePath": filePath
                }), 500
        else:
            logMessage(f"文件不存在: {fullPath}")
            return jsonify({
                "error": "文件不存在",
                "filePath": filePath,
                "fullPath": fullPath,
                "exists": fileExists,
                "isFile": isFile
            }), 404
    
    except Exception as e:
        logMessage(f"删除文件处理失败: {str(e)}", "ERROR")
        return jsonify({
            "error": str(e),
            "filePath": filePath
        }), 500

@app.route('/api/workspace/info', methods=['GET'])
def get_workspace_info():
    """获取工作空间信息"""
    try:
        tilesDir = config["tilesDir"]
        
        # 统计信息
        totalSize = 0
        totalFiles = 0
        totalDirs = 0
        
        for root, dirs, files in os.walk(tilesDir):
            totalDirs += len(dirs)
            totalFiles += len(files)
            for file in files:
                try:
                    filePath = os.path.join(root, file)
                    totalSize += os.path.getsize(filePath)
                except:
                    pass
        
        return jsonify({
            "success": True,
            "workspaceInfo": {
                "basePath": tilesDir,
                "totalSize": totalSize,
                "totalSizeFormatted": formatFileSize(totalSize),
                "totalFiles": totalFiles,
                "totalDirectories": totalDirs,
                "lastUpdated": datetime.now().isoformat()
            }
        })
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/tiles/nodata/scan', methods=['POST'])
def scan_nodata_tiles():
    """扫描包含透明(nodata)值的PNG瓦片"""
    return scanNodataTiles()

@app.route('/api/tiles/nodata/delete', methods=['POST'])
def delete_nodata_tiles():
    """删除包含透明(nodata)值的PNG瓦片"""
    return deleteNodataTiles()

@app.route('/api/terrain/layer', methods=['POST'])
def update_layer_json():
    """更新地形瓦片的layer.json文件"""
    return updateLayerJson()

@app.route('/api/terrain/decompress', methods=['POST'])
def decompress_terrain():
    """解压地形瓦片"""
    return decompressTerrain()

if __name__ == '__main__':
    # 确保必要的目录存在
    os.makedirs(config["logDir"], exist_ok=True)
    os.makedirs(config["tilesDir"], exist_ok=True)
    
    logMessage("TerraForge瓦片服务启动 - 模块化架构")
    logMessage(f"数据源目录: {config['dataSourceDir']}")
    logMessage(f"瓦片目录: {config['tilesDir']}")
    logMessage(f"日志目录: {config['logDir']}")
    
    port = int(os.environ.get('PORT', config.get('port', 8000)))
    host = os.environ.get('HOST', config.get('host', '0.0.0.0'))
    debug = config.get('debug', False)
    
    print(f"TerraForge服务启动中...")
    print(f"监听地址: http://{host}:{port}")
    print(f"调试模式: {debug}")
    print(f"工作目录: {os.getcwd()}")
    print("=" * 50)
    
    app.run(host=host, port=port, debug=debug, threaded=True) 