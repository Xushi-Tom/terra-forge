package com.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus 配置类
 * 解决达梦数据库兼容性问题
 */
@Configuration
public class MybatisPlusConfig {
    
    /**
     * 分页插件配置
     * 适配 MyBatis Plus 3.2.0 版本
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInterceptor.setLimit(1000L);
        
        // 溢出总页数后是否进行处理
        paginationInterceptor.setOverflow(false);
        
        return paginationInterceptor;
    }
} 