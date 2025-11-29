package com.tmd.ai;

import com.tmd.ai.MCP.DataService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class AgentAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentAiApplication.class, args);
    }
    @Bean
    public ToolCallbackProvider addressDateTools(DataService dateService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(dateService).build();
    }


}
