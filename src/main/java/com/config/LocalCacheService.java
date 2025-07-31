package com.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 本地缓存服务
 * 替代Redis，使用单机内存缓存
 * 
 * @author xushi
 * @version 1.0
 */
@Slf4j
@Service
public class LocalCacheService {
    
    private final ConcurrentHashMap<String, CacheItem> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @PostConstruct
    public void init() {
        // 每分钟清理过期缓存
        scheduler.scheduleAtFixedRate(this::cleanExpiredItems, 1, 1, TimeUnit.MINUTES);
        log.info("本地缓存服务已启动");
    }
    
    /**
     * 设置缓存
     */
    public void setValue(String key, String value) {
        setValue(key, value, 3600); // 默认1小时过期
    }
    
    /**
     * 设置缓存（带过期时间）
     */
    public void setValue(String key, String value, long expireSeconds) {
        long expireTime = System.currentTimeMillis() + (expireSeconds * 1000);
        cache.put(key, new CacheItem(value, expireTime));
        log.debug("缓存设置: key={}, value={}, expireSeconds={}", key, value, expireSeconds);
    }
    
    /**
     * 获取缓存
     */
    public String getValue(String key) {
        CacheItem item = cache.get(key);
        if (item == null) {
            log.debug("缓存未命中: key={}", key);
            return null;
        }
        
        if (item.isExpired()) {
            cache.remove(key);
            log.debug("缓存已过期: key={}", key);
            return null;
        }
        
        log.debug("缓存命中: key={}, value={}", key, item.getValue());
        return item.getValue();
    }
    
    /**
     * 删除缓存
     */
    public void delete(String key) {
        cache.remove(key);
        log.debug("缓存删除: key={}", key);
    }
    
    /**
     * 检查缓存是否存在
     */
    public boolean exists(String key) {
        return getValue(key) != null;
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        cache.clear();
        log.info("本地缓存已清空");
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 清理过期项
     */
    private void cleanExpiredItems() {
        int beforeSize = cache.size();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int afterSize = cache.size();
        
        if (beforeSize != afterSize) {
            log.debug("清理过期缓存: {} -> {}", beforeSize, afterSize);
        }
    }
    
    /**
     * 缓存项
     */
    private static class CacheItem {
        private final String value;
        private final long expireTime;
        
        public CacheItem(String value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
        
        public String getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
    
    /**
     * 关闭服务
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        cache.clear();
        log.info("本地缓存服务已关闭");
    }
}
