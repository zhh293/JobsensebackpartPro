package com.tmd.ai.WebSocketServerAndTongYi;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class APIWebsocketPool {
    @Autowired
    private APIWebsocketPoolConfig config;

    private List<APIWebsocket> pool;
    private ScheduledExecutorService cleanupScheduler;

    private int activeConnections;

    @PostConstruct
    public void createPool() {
        // 创建连接池
        this.pool =new ArrayList<>(config.MAX_CONNECTIONS);
        //初始化连接池
        for (int i = 0; i < config.MAX_CONNECTIONS; i++) {
            APIWebsocket apiWebsocket = new APIWebsocket(APIWebsocket.chatClient);
            pool.add(apiWebsocket);
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
    
    public APIWebsocket getConnection() {
        if(!pool.isEmpty()){
            APIWebsocket apiWebsocket = pool.remove(0);
            activeConnections++;
            return apiWebsocket;
        }
        if(activeConnections < config.MAX_CONNECTIONS){
            APIWebsocket apiWebsocket = new APIWebsocket(APIWebsocket.chatClient);
            activeConnections++;
            System.out.println("创建新的连接");
            return apiWebsocket;
        }
        throw new RuntimeException("没有可用的连接");
    }

    public void releaseConnection(APIWebsocket apiWebsocket) {
        if (apiWebsocket != null) {
            pool.add(apiWebsocket);
            activeConnections--;
        }
    }

    public void closePool() {
        //销毁所有对象，清空所有数据
        pool.clear();
        activeConnections = 0;
        
        // 关闭清理任务
        if (cleanupScheduler != null && !cleanupScheduler.isShutdown()) {
            cleanupScheduler.shutdown();
        }
        
        log.info("连接池销毁完毕");
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