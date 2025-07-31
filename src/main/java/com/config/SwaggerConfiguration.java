package com.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger API文档配置类
 * 
 * <p>配置TerraForge地形地图切片服务的API文档生成。
 * 集成了Swagger2和Knife4j，提供美观易用的API文档界面，
 * 支持在线API测试和参数校验。</p>
 * 
 * <h3>集成的文档工具：</h3>
 * <ul>
 *   <li><b>Swagger2</b>：标准的API文档规范和生成工具</li>
 *   <li><b>Knife4j</b>：增强的API文档UI界面，支持更丰富的功能</li>
 *   <li><b>Bean Validator</b>：自动集成参数校验注解到API文档</li>
 * </ul>
 * 
 * <h3>访问地址：</h3>
 * <ul>
 *   <li><b>Swagger原生UI</b>：http://localhost:8080/swagger-ui.html</li>
 *   <li><b>Knife4j增强UI</b>：http://localhost:8080/doc.html</li>
 *   <li><b>API JSON规范</b>：http://localhost:8080/v2/api-docs</li>
 * </ul>
 * 
 * <h3>API分组和组织：</h3>
 * <p>当前配置扫描所有标注了@ApiOperation注解的控制器方法，
 * 主要包含以下API模块：</p>
 * <ul>
 *   <li>地形切片API：高精度地形瓦片生成</li>
 *   <li>地图切片API：防缝隙地图瓦片切片</li>
 *   <li>工作空间管理API：多层级文件和项目管理</li>
 *   <li>任务监控API：实时进度监控和任务控制</li>
 *   <li>系统信息API：健康检查和系统状态</li>
 * </ul>
 * 
 * @author TerraForge开发团队
 * @version 2.8.0
 * @since 2025-01-01
 * @see com.controller.TerraForgeCommonController 主要API控制器
 * @see com.controller.TerrainLayerController 地形层控制器
 * @see com.controller.MapLayerController 地图层控制器
 */
@Configuration
@EnableSwagger2
@EnableKnife4j
@Import(BeanValidatorPluginsConfiguration.class)
public class SwaggerConfiguration {

    /**
     * 创建Swagger API文档的核心配置Bean
     * 
     * <p>配置Swagger文档生成的各种参数，包括扫描范围、路径映射和基本信息。
     * 此方法定义了哪些API接口会被包含在文档中，以及如何组织这些接口。</p>
     * 
     * <h4>配置策略：</h4>
     * <ul>
     *   <li><b>选择性扫描</b>：只扫描标注了@ApiOperation的方法，避免内部接口暴露</li>
     *   <li><b>全路径包含</b>：包含所有符合条件的API路径</li>
     *   <li><b>根路径映射</b>：API路径从根路径开始映射</li>
     * </ul>
     * 
     * <p><b>注意</b>：只有在Controller方法上添加了@ApiOperation注解的接口
     * 才会出现在API文档中，这样可以精确控制文档的内容范围。</p>
     * 
     * @return 配置完成的Docket实例，用于生成API文档
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                // 只扫描标注了@ApiOperation注解的方法，确保API文档的专业性
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                // 包含所有路径，不进行路径过滤
                .paths(PathSelectors.any())
                .build()
                // 设置API路径的基础映射为根路径
                .pathMapping("/")
                // 设置API文档的详细信息
                .apiInfo(apiInfo());
    }

    /**
     * 构建API文档的基本信息
     * 
     * <p>定义API文档的标题、版本、描述、联系人等元数据信息。
     * 这些信息会显示在API文档的头部，为API使用者提供基本的服务信息。</p>
     * 
     * <h4>信息内容：</h4>
     * <ul>
     *   <li><b>服务名称</b>：TerraForge地形地图切片服务</li>
     *   <li><b>服务版本</b>：与项目版本保持同步</li>
     *   <li><b>服务描述</b>：详细说明服务的功能和用途</li>
     *   <li><b>技术支持</b>：提供联系方式和技术支持信息</li>
     * </ul>
     * 
     * @return 包含API基本信息的ApiInfo对象
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                // 设置API文档标题 - 明确服务的核心功能
                .title("TerraForge地形地图切片服务API")
                // 设置API版本号 - 与项目版本保持一致
                .version("2.8.0")
                // 设置API详细描述
                .description("高性能地理信息处理平台，提供地形切片、地图瓦片生成、" +
                           "工作空间管理等专业GIS服务。集成量化网格算法、半边数据结构、" +
                           "防缝隙技术等先进算法，支持大规模地理数据的高效处理。")
                // 设置技术联系人信息
                .contact(new Contact("TerraForge开发团队", 
                                  "https://github.com/terraforge/terraforge-server", 
                                  "support@terraforge.com"))
                // 设置开源许可证信息
                .license("Apache License 2.0")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
                .build();
    }
}
