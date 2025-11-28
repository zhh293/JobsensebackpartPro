package com.tmd.ai.WebSocketServerAndTongYi;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class APIWebsocketPool {
    @Autowired
    private APIWebsocketPoolConfig config;


}

//需要有一个连接池配置
@ConfigurationProperties(prefix = "apiwebsocket.pool")
@Component
class APIWebsocketPoolConfig{
    public int MAX_CONNECTIONS;
    public int MAX_IDLE_TIME;
    public int MAX_WAIT_TIME;
    public int MAX_TOTAL_CONNECTIONS;
}