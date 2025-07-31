package com.service;

import com.dto.MapCutRequestDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author xushi
 * @version 1.0
 * @project map
 * @description TMS服务切片接口
 * @date 2025/4/21 03:43:54
 */
public interface MapCutService {

    /**
     * @description 地图切片
     * @param file
     * @param workspace
     * @param minZoom
     * @param maxZoom
     * @return java.lang.String
     * @author xushi
     * @date 2025/4/21 04:21:31
     */
    String tmsCutOfFile(MultipartFile file, String workspace, String type, Integer minZoom, Integer maxZoom);

    /**
     * @description 地图切片
     * @param mapCutRequestDto
     * @return java.lang.String
     * @author xushi
     * @date 2025/4/21 04:21:45
     */
    String tmsCut(MapCutRequestDto mapCutRequestDto);

}
