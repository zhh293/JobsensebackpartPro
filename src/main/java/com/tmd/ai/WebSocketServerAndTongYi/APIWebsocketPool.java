package com.tmd.ai.WebSocketServerAndTongYi;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class APIWebsocketPool {
    @Autowired
    private APIWebsocketPoolConfig config;

    private BlockingQueue<APIWebsocket> pool;
    private ScheduledExecutorService cleanupScheduler;

    private final AtomicInteger activeConnections=new AtomicInteger(0);

    @PostConstruct
    public void createPool() {
        // 创建连接池
        this.pool =new ArrayBlockingQueue<>(config.MAX_CONNECTIONS);
        //初始化连接池
        for (int i = 0; i < config.MAX_CONNECTIONS; i++) {
            APIWebsocket apiWebsocket = new APIWebsocket(APIWebsocket.chatClient);
            pool.offer(apiWebsocket);
        }
        
        // 启动定时清理任务
        startCleanupTask();
    }
    
    private void startCleanupTask() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        cleanupScheduler.scheduleWithFixedDelay(this::cleanupIdleConnections, 
            config.MAX_IDLE_TIME, config.MAX_IDLE_TIME, TimeUnit.MILLISECONDS);
    }
    
    private void cleanupIdleConnections() {
        // 这里可以实现空闲连接的清理逻辑
        log.debug("执行连接池清理任务，当前活跃连接数: {}", activeConnections);
    }
    
    public APIWebsocket getConnection() throws InterruptedException {
        APIWebsocket connection = pool.poll();
        if (connection != null && isConnectionValid(connection)) {
            activeConnections.incrementAndGet(); // 用 AtomicInteger 保证原子性
            return connection;
        }

        // 2. 池中无可用连接，判断是否允许创建新连接（未达最大总连接数）
        if (activeConnections.get() < config.MAX_TOTAL_CONNECTIONS) {
            APIWebsocket newConn = new APIWebsocket(APIWebsocket.chatClient);
            activeConnections.incrementAndGet();
            log.debug("创建新连接，当前活跃连接数：{}", activeConnections.get());
            return newConn;
        }
        // 3. 已达最大连接数，等待可用连接（直到超时）
        connection = pool.poll(config.MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
        if (connection == null || !isConnectionValid(connection)) {
            throw new RuntimeException("获取连接超时（" + config.MAX_WAIT_TIME + "ms），无可用连接");
        }

        activeConnections.incrementAndGet();
        return connection;
    }

    // 连接可用性检查（关键）
    private boolean isConnectionValid(APIWebsocket connection) {
        // 假设 APIWebsocket 有 isOpen() 方法判断连接状态
        return connection != null && connection.isConnected();
    }
    public void releaseConnection(APIWebsocket conn) {
        if (conn == null) return;

        activeConnections.decrementAndGet();

        // 连接无效：直接关闭
        if (!isConnectionValid(conn)) {
            closeConnection(conn);
            return;
        }

        // 队列满时，关闭多余连接（避免超过核心池大小）
        if (!pool.offer(conn)) {
            closeConnection(conn);
            log.debug("连接池已满，关闭多余空闲连接");
        }
    }
    private void closeConnection(APIWebsocket conn) {
        if (conn != null && conn.isConnected()) {
            try {
                conn.onClose(); // 假设 APIWebsocket 有 close() 方法
            } catch (Exception e) {
                log.error("关闭 WebSocket 连接失败", e);
            }
        }
    }

    public void closePool() {
        // 1. 关闭所有空闲连接
        pool.forEach(this::closeConnection);
        pool.clear();

        // 2. 重置计数器
        activeConnections.set(0);

        // 3. 关闭定时任务（优雅停机）
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdownNow();
            try {
                if (!cleanupScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("定时任务未正常关闭");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("连接池已销毁，释放所有资源");
    }

}

//需要有一个连接池配置
@ConfigurationProperties(prefix = "apiwebsocket.pool")
@Component
class APIWebsocketPoolConfig{
    public int MAX_CONNECTIONS = 10;
    public int MAX_IDLE_TIME = 300000; // 5分钟
    public int MAX_WAIT_TIME = 5000;   // 5秒
    public int MAX_TOTAL_CONNECTIONS = 20;
}