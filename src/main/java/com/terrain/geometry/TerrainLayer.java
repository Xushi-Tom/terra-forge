package com.terrain.geometry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.utils.FileUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Getter
@Setter
@Slf4j
public class TerrainLayer {
     /**
     * 存储可用瓦片范围的列表，使用 final 修饰确保引用不可变。
     * 列表中的每个元素是一个 TileRange 对象，代表一个层级的瓦片范围信息。
     */
    private final List<TileRange> available = new ArrayList<>();
    /**
     * TileJSON 规范的版本号，用于描述瓦片数据的格式标准。
     */
    private String tilejson = null;
    /**
     * 地形图层的名称，用于标识该图层。
     */
    private String name = null;
    /**
     * 地形图层的详细描述信息，用于说明该图层的用途、来源等。
     */
    private String description = null;
    /**
     * 地形图层的版本号，用于标识数据的更新版本。
     */
    private String version = null;
    /**
     * 瓦片数据的格式，例如 "quantized-mesh-1.0"。
     */
    private String format = null;
    /**
     * 地形图层数据的版权归属信息。
     */
    private String attribution = null;
    /**
     * 地形图层的模板名称，用于指定显示样式或行为。
     */
    private String template = null;
    /**
     * 地形图层的图例信息，用于解释地图上的符号和颜色。
     */
    private String legend = null;
    /**
     * 瓦片数据的排列方案，例如 "tms"。
     */
    private String scheme = null;
    /**
     * 地形图层的扩展信息列表，用于存储额外的自定义信息。
     */
    private List<String> extensions = null;
    /**
     * 瓦片数据的 URL 模板数组，用于获取瓦片资源。
     */
    private String[] tiles = null;
    /**
     * 地形图层的坐标参考系统，使用 EPSG 代码表示，例如 "EPSG:4326"。
     */
    private String projection = null;
    /**
     * 地形图层的地理边界数组，包含四个元素，分别为最小经度、最小纬度、最大经度、最大纬度。
     */
    private double[] bounds = null;


    public TerrainLayer() {
        this.setDefault();
    }

    public HashMap<Integer, TileRange> getTilesRangeMap() {
        HashMap<Integer, TileRange> tilesRangeMap = new HashMap<>();

        for (TileRange tilesRange : this.available) {
            tilesRangeMap.put(tilesRange.getTileDepth(), tilesRange);
        }

        return tilesRangeMap;
    }

    public void setDefault() {
        this.tilejson = "2.1.0";
        this.name = "insert name here";
        this.description = "insert description here";
        this.version = "1.1.0";
        this.format = "quantized-mesh-1.0";
        this.attribution = "insert attribution here";
        this.template = "terrain";
        this.legend = "insert legend here";
        this.scheme = "tms";
        this.tiles = new String[1];
        this.tiles[0] = "{z}/{x}/{y}.terrain?v={version}";
        this.projection = "EPSG:4326";
        this.extensions = new ArrayList<>();
        this.bounds = new double[4];
        this.bounds[0] = 0.0;
        this.bounds[1] = 0.0;
        this.bounds[2] = 0.0;
        this.bounds[3] = 0.0;
    }

    public void addExtension(String extension) {
        if (this.extensions == null) {
            this.extensions = new ArrayList<>();
        }
        this.extensions.add(extension);
    }

    public boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 生成可用瓦片信息并计算图层边界。
     * 该方法会遍历指定输入目录下的文件结构，提取瓦片的层级、X 坐标和 Y 坐标信息，
     * 并将这些信息封装到 TileRange 对象中添加到 available 列表里。
     * 最后根据最高层级的瓦片信息计算图层的地理边界。
     *
     * @param inputPath 包含瓦片文件的输入目录路径
     */
    public void generateAvailableTiles(String inputPath) {
        // 创建表示输入目录的 File 对象
        File inputDirectory = new File(inputPath);
        // 检查输入目录是否存在
        if (!inputDirectory.exists()) {
            // 若不存在，记录错误日志并返回
            log.error("输入路径不存在");
            return;
        }

        // 用于存储所有检测到的瓦片深度（Z 坐标），使用 LinkedHashSet 保持插入顺序
        Set<Integer> depthZ = new LinkedHashSet<>();
        // 获取输入目录下的所有文件和文件夹
        File[] depthFiles = inputDirectory.listFiles();
        // 对获取到的文件和文件夹进行排序
        assert depthFiles != null;
        Arrays.sort(depthFiles);
        // 遍历输入目录下的每个文件和文件夹
        for (File depthFile : depthFiles) {
            // 检查当前文件是否为文件夹
            if (depthFile.isDirectory()) {
                // 检查文件夹名称是否为整数
                if (!isInteger(depthFile.getName())) {
                    // 若不是整数，跳过当前循环
                    continue;
                }
                // 用于存储当前深度下所有检测到的瓦片 X 坐标
                Set<Integer> tileX = new LinkedHashSet<>();
                // 用于存储当前深度下所有检测到的瓦片 Y 坐标
                Set<Integer> tileY = new LinkedHashSet<>();
                // 将文件夹名称转换为整数作为瓦片深度
                int tileDepth = Integer.parseInt(depthFile.getName());

                // 记录开始处理当前深度瓦片的日志信息
                log.info("[生成][layer.json] 开始生成 layer.json 文件。瓦片层级: " + tileDepth);

                // 将当前瓦片深度添加到 depthZ 集合中
                depthZ.add(tileDepth);
                // 获取当前深度文件夹下的所有文件和文件夹
                File[] tileXFiles = depthFile.listFiles();
                // 遍历当前深度文件夹下的每个文件和文件夹
                for (File tileXFile : tileXFiles) {
                    // 检查当前文件是否为文件夹
                    if (tileXFile.isDirectory()) {
                        // 检查文件夹名称是否为整数
                        if (!isInteger(tileXFile.getName())) {
                            // 若不是整数，跳过当前循环
                            continue;
                        }

                        // 将文件夹名称转换为整数并添加到 tileX 集合中
                        tileX.add(Integer.parseInt(tileXFile.getName()));
                        // 获取当前 X 坐标文件夹下的所有文件和文件夹
                        File[] tileYFiles = tileXFile.listFiles();
                        // 遍历当前 X 坐标文件夹下的每个文件和文件夹
                        assert tileYFiles != null;
                        for (File tileYFile : tileYFiles) {
                            // 检查当前文件是否为普通文件
                            if (tileYFile.isFile()) {
                                // 获取文件名并按点分割，取第一部分
                                String tileYFileName = tileYFile.getName().split("\\.")[0];
                                // 检查分割后的文件名是否为整数
                                if (!isInteger(tileYFileName)) {
                                    // 若不是整数，跳过当前循环
                                    continue;
                                }
                                // 将分割后的文件名转换为整数并添加到 tileY 集合中
                                tileY.add(Integer.parseInt(tileYFileName));
                            }
                        }
                    }
                }
                // 创建一个新的 TileRange 对象
                TileRange tilesRange = new TileRange();
                // 设置 TileRange 对象的瓦片深度
                tilesRange.setTileDepth(tileDepth);
                // 设置 TileRange 对象的最小 X 坐标
                tilesRange.setMinTileX(Collections.min(tileX));
                // 设置 TileRange 对象的最大 X 坐标
                tilesRange.setMaxTileX(Collections.max(tileX));
                // 设置 TileRange 对象的最小 Y 坐标
                tilesRange.setMinTileY(Collections.min(tileY));
                // 设置 TileRange 对象的最大 Y 坐标
                tilesRange.setMaxTileY(Collections.max(tileY));
                // 将 TileRange 对象添加到 available 列表中
                available.add(tilesRange);
            }
        }
        // 记录所有可用瓦片的信息日志
        log.info("可用瓦片: " + available);
        // 记录所有检测到的瓦片深度信息日志
        log.info("瓦片深度: " + depthZ);
        // 对 available 列表按瓦片深度进行排序
        available.sort(Comparator.comparingInt(TileRange::getTileDepth));

        // 初始化地理边界的最小经度、最大经度、最小纬度和最大纬度
        double minLon = -180.0;
        double maxLon = 180.0;
        double minLat = -90.0;
        double maxLat = 90.0;

        // 获取 available 列表中最后一个 TileRange 对象，即最高层级的瓦片信息
        TileRange lastTilesRange = available.get(available.size() - 1);
        // 获取最高层级的瓦片深度
        int lastTileDepth = lastTilesRange.getTileDepth();
        // 获取最高层级的最小 X 坐标
        int lastMinTileX = lastTilesRange.getMinTileX();
        // 获取最高层级的最大 X 坐标
        int lastMaxTileX = lastTilesRange.getMaxTileX();
        // 获取最高层级的最小 Y 坐标
        int lastMinTileY = lastTilesRange.getMinTileY();
        // 获取最高层级的最大 Y 坐标
        int lastMaxTileY = lastTilesRange.getMaxTileY();

        // 计算最高层级瓦片的宽度
        double tileWidth = 360.0 / Math.pow(2, lastTileDepth + 1);
        // 计算最高层级瓦片的高度
        double tileHeight = 180.0 / Math.pow(2, lastTileDepth);
        // 计算最小经度
        double calcMinLon = lastMinTileX * tileWidth + minLon;
        // 计算最大经度
        double calcMaxLon = (lastMaxTileX + 1) * tileWidth + minLon;
        // 计算最小纬度
        double calcMinLat = (lastMaxTileY + 1) * tileHeight + minLat;
        // 计算最大纬度
        double calcMaxLat = lastMinTileY * tileHeight + minLat;

        // 记录计算得到的最小经度信息日志
        log.info("计算最小经度: " + calcMinLon);
        // 记录计算得到的最大经度信息日志
        log.info("计算最大经度: " + calcMaxLon);
        // 记录计算得到的最小纬度信息日志
        log.info("计算最小纬度: " + calcMinLat);
        // 记录计算得到的最大纬度信息日志
        log.info("计算最大纬度: " + calcMaxLat);

        // 更新最小经度，取当前值和计算值中的较大值
        minLon = Math.max(minLon, calcMinLon);
        // 更新最小纬度，取当前值和计算值中的较大值
        minLat = Math.max(minLat, calcMinLat);
        // 更新最大经度，取当前值和计算值中的较小值
        maxLon = Math.min(maxLon, calcMaxLon);
        // 更新最大纬度，取当前值和计算值中的较小值
        maxLat = Math.min(maxLat, calcMaxLat);

        // 将计算得到的边界信息设置到当前对象的 bounds 数组中
        this.bounds[0] = minLon;
        this.bounds[1] = minLat;
        this.bounds[2] = maxLon;
        this.bounds[3] = maxLat;
    }


    /**
     * 将地形图层信息保存为 JSON 文件。
     * 该方法会根据地形图层的属性构建 JSON 结构，并将其写入指定目录下的指定文件中。
     *
     * @param outputDirectory 输出目录的路径
     * @param layerJsonName   输出 JSON 文件的名称
     */
    public void saveJsonFile(String outputDirectory, String layerJsonName) {
        // 拼接完整的文件路径
        String fullFileName = outputDirectory + File.separator + layerJsonName;
        // 若输出目录不存在，则创建该目录及其所有父目录
        FileUtils.createAllFoldersIfNoExist(outputDirectory);

        // 创建 ObjectMapper 实例，用于处理 JSON 对象
        ObjectMapper objectMapper = new ObjectMapper();
        // 创建 JSON 根节点
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();
        // 向根节点添加基本属性
        objectNodeRoot.put("tilejson", this.tilejson);
        objectNodeRoot.put("name", this.name);
        objectNodeRoot.put("description", this.description);
        objectNodeRoot.put("version", this.version);
        objectNodeRoot.put("format", this.format);
        objectNodeRoot.put("attribution", this.attribution);
        objectNodeRoot.put("template", this.template);
        objectNodeRoot.put("legend", this.legend);
        objectNodeRoot.put("scheme", this.scheme);
        objectNodeRoot.put("projection", this.projection);
        // 向根节点添加 tiles 数组
        objectNodeRoot.putArray("tiles").add(this.tiles[0]);
        // 向根节点添加 bounds 数组
        objectNodeRoot.putArray("bounds").add(this.bounds[0]).add(this.bounds[1]).add(this.bounds[2]).add(this.bounds[3]);

        // 若存在扩展信息，则添加到 JSON 中
        if (this.extensions != null && this.extensions.size() > 0) {
            // 创建 extensions 数组节点
            ArrayNode objectNodeExtensions = objectMapper.createArrayNode();
            // 遍历扩展信息列表，添加到 extensions 数组中
            for (String extension : this.extensions) {
                objectNodeExtensions.add(extension);
            }
            // 将 extensions 数组添加到根节点
            objectNodeRoot.set("extensions", objectNodeExtensions);
        }

        // 创建 available 数组节点
        ArrayNode objectNodeAvailable = objectMapper.createArrayNode();
        // 获取瓦片范围映射表
        HashMap<Integer, TileRange> tilesRangeMap = this.getTilesRangeMap();
        // 遍历瓦片范围映射表
        for (Integer tileDepth : tilesRangeMap.keySet()) {
            // 获取当前层级的瓦片范围
            TileRange tilesRange = tilesRangeMap.get(tileDepth);
            // 创建当前层级的瓦片范围数组节点
            ArrayNode objectNodeTileDepth_array = objectMapper.createArrayNode();
            // 创建当前层级的瓦片范围对象节点
            ObjectNode objectNodeTileDepth = objectMapper.createObjectNode();
            // 向对象节点添加瓦片范围的起始 X 坐标
            objectNodeTileDepth.put("startX", tilesRange.getMinTileX());
            // 向对象节点添加瓦片范围的结束 X 坐标
            objectNodeTileDepth.put("endX", tilesRange.getMaxTileX());
            // 向对象节点添加瓦片范围的起始 Y 坐标
            objectNodeTileDepth.put("startY", tilesRange.getMinTileY());
            // 向对象节点添加瓦片范围的结束 Y 坐标
            objectNodeTileDepth.put("endY", tilesRange.getMaxTileY());

            // 将当前层级的瓦片范围对象节点添加到数组节点中
            objectNodeTileDepth_array.add(objectNodeTileDepth);
            // 将当前层级的瓦片范围数组节点添加到 available 数组节点中
            objectNodeAvailable.add(objectNodeTileDepth_array);
        }

        // 将 available 数组添加到根节点
        objectNodeRoot.set("available", objectNodeAvailable);

        // 保存 JSON 索引文件
        try {
            // 将 ObjectNode 转换为 JsonNode
            JsonNode jsonNode = new ObjectMapper().readTree(objectNodeRoot.toString());
            // 将 JsonNode 写入指定文件
            objectMapper.writeValue(new File(fullFileName), jsonNode);
        } catch (IOException e) {
            // 记录写入文件时的错误信息
            log.error("Error:", e);
        }
    }

}
