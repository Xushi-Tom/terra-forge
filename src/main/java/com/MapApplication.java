package com;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;

/**
 * TerraForge地形地图切片服务 - 主应用启动类
 * 
 * <p>TerraForge是一个高性能的地理信息处理平台，专注于地形和地图的瓦片切片服务。
 * 本系统集成了先进的量化网格算法、半边数据结构和防缝隙技术，
 * 为GIS应用提供专业级的地形数据处理能力。</p>
 * 
 * <h3>核心技术特性：</h3>
 * <ul>
 *   <li><b>量化网格算法</b>：基于半边数据结构的高精度三维地形网格生成</li>
 *   <li><b>防缝隙技术</b>：精确像素计算和边界扩展策略，解决瓦片间缝隙问题</li>
 *   <li><b>自适应细分</b>：根据地形复杂度动态调整三角网密度</li>
 *   <li><b>多格式支持</b>：支持TIF、GeoTIFF等主流地理数据格式</li>
 *   <li><b>分布式处理</b>：多进程并行处理，充分利用系统资源</li>
 * </ul>
 * 
 * <h3>系统架构：</h3>
 * <ul>
 *   <li><b>Spring Boot</b>：提供RESTful API和业务逻辑处理</li>
 *   <li><b>MyBatis Plus</b>：数据持久化和工作空间管理</li>
 *   <li><b>GeoTools</b>：地理坐标系转换和地理计算</li>
 *   <li><b>Python引擎</b>：集成GDAL、CTB等专业地理处理工具</li>
 * </ul>
 * 
 * <h3>主要功能模块：</h3>
 * <ul>
 *   <li>地形切片：基于Java原生算法的高精度地形瓦片生成</li>
 *   <li>地图切片：防缝隙地图瓦片切片服务</li>
 *   <li>工作空间管理：多层级工作空间和文件管理</li>
 *   <li>任务监控：实时进度监控和任务生命周期管理</li>
 *   <li>智能推荐：根据数据特征自动推荐最佳处理参数</li>
 * </ul>
 * 
 * @author TerraForge开发团队
 * @version 2.8.0
 * @since 2025-01-01
 * @see com.controller.TerraForgeCommonController 通用API控制器
 * @see com.service.TerraForgeCommonService 核心服务接口
 * @see com.terrain.common.HalfEdgeSurface 半边数据结构核心实现
 */
@Controller
@MapperScan("com.mapper")
@SpringBootApplication
public class MapApplication {

    /**
     * Spring Boot应用启动入口点
     * 
     * <p>启动TerraForge服务，初始化所有核心组件：</p>
     * <ul>
     *   <li>Web容器和RESTful API服务</li>
     *   <li>数据库连接池和MyBatis映射器</li>
     *   <li>地理处理引擎和算法库</li>
     *   <li>任务调度器和监控服务</li>
     * </ul>
     * 
     * <p><b>默认端口</b>：8080</p>
     * <p><b>健康检查</b>：GET /health</p>
     * <p><b>API文档</b>：GET /swagger-ui.html</p>
     * 
     * @param args 命令行参数，支持--server.port等Spring Boot标准参数
     */
    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(MapApplication.class, args);
    }
}