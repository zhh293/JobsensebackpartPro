package com.tmd.ai.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class RedisChatHistoryRespository implements ChatHistoryRepository {

    private final StringRedisTemplate redisTemplate;
    
    // 过期时间设置为30天（以秒为单位）
    private static final long TTL_SECONDS = 30 * 24 * 60 * 60L;
    
    // Redis key的前缀
    private static final String CHAT_HISTORY_PREFIX = "chat:history:";
    
    @Autowired
    public RedisChatHistoryRespository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String type, String chatId) {
        String key = CHAT_HISTORY_PREFIX + type;
        
        // 使用setIfAbsent确保chatId不会重复添加
        Long added = redisTemplate.opsForSet().add(key, chatId);
        
        // 如果是新添加的元素，则设置过期时间
        if (added != null && added > 0) {
            redisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public List<String> getChatIds(String type) {
        String key = CHAT_HISTORY_PREFIX + type;
        
        // 获取集合中的所有元素
        Set<String> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Collections.emptyList();
        }
        return members.stream().collect(Collectors.toList());
    }
}