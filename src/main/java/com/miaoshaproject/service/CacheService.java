package com.miaoshaproject.service;

/**
 * 封装本地缓存操作类
 */
public interface CacheService {

    /**
     * 存方法
     * @param key
     * @param value
     */
    void setCommonCache(String key, Object value);

    /**
     * 取方法
     * @param key
     * @return
     */
    Object getFromCommonCache(String key);
}
