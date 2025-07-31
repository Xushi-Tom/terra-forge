package com.service;

import com.dto.terraforge.IndexedTilesRequestDto;
import com.dto.terraforge.IndexedTilesResponseDto;
import com.dto.terraforge.MapTileRequestDto;
import com.dto.terraforge.MapTileResponseDto;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description TerraForge地图切片服务接口
 * @date 2025/7/1 10:00:00
 */
public interface TerraForgeMapService {

    /**
     * 索引瓦片切片 - 基于瓦片索引的精确切片
     * 核心思想：
     * 1. 根据TIF文件生成SHP分幅矢量栅格
     * 2. 记录每个瓦片的详细元数据（位置、关联TIF文件、像素、nodata、透明度等）
     * 3. 使用GDAL精确切割每个瓦片（无VRT中间层）
     */
    IndexedTilesResponseDto createIndexedTiles(IndexedTilesRequestDto request);
}
