package com.tmd.ai.config;

import com.tmd.ai.constants.SystemConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.openai.image.ImageClient;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiModerationModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.ai.openai.api.OpenAiModerationApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CommonConfiguration {

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.base-url}")
    private String openAiBaseUrl;

    /**
     * 构造 RestClient.Builder（截图里构造函数要求的参数之一）
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    /**
     * 构造默认的响应错误处理器（截图里构造函数要求的参数之一）
     */
    @Bean
    public ResponseErrorHandler responseErrorHandler() {
        return new DefaultResponseErrorHandler(); // Spring 自带的默认错误处理器
    }

    /**
     * 2. 构造 MultiValueMap（请求头容器，存放 OpenAI 必需的认证头/格式头）
     * 作用：统一设置所有请求都需要的固定头，避免每次调用都重复加
     */
    @Bean
    public MultiValueMap<String, String> openAiHeaders() {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        // 1. 认证头（OpenAI 必需，格式：Bearer + API Key）
        headers.add("Authorization", "Bearer " + openAiApiKey);
        // 2. 内容格式头（OpenAI API 要求 JSON 格式请求体）
        headers.add("Content-Type", "application/json");
        // 可选：添加其他固定头（如 User-Agent）
        headers.add("User-Agent", "Spring AI Old Client");
        return headers;
    }


    /**
     * 填全 OpenAiImageApi 的构造函数参数（对应截图里的重载）
     */
    /*@Bean
    public OpenAiImageApi openAiImageApi(
            RestClient.Builder restClientBuilder,
            ResponseErrorHandler responseErrorHandler,
            MultiValueMap<String, String> openAiHeaders) {

        // 对应截图里的构造函数：String baseUrl, String apiKey, RestClient.Builder, ResponseErrorHandler
        return new OpenAiImageApi(
                openAiBaseUrl,       // 你的基础URL
                openAiApiKey,// 你的API Key
                openAiHeaders,       // 上面定义的固定头
                restClientBuilder,   // 上面定义的 RestClient.Builder
                responseErrorHandler // 上面定义的默认错误处理器
        );
    }*/
    @Bean
    public OpenAiImageApi openAiImageApi(
            RestClient.Builder restClientBuilder,
            ResponseErrorHandler responseErrorHandler,
            MultiValueMap<String, String> openAiHeaders
    ) {

        // 对应截图里的构造函数：String baseUrl, String apiKey, RestClient.Builder, ResponseErrorHandler
        return new OpenAiImageApi(
                openAiBaseUrl,
                openAiApiKey,
                openAiHeaders,
                restClientBuilder,
                responseErrorHandler
        );
    }

    /**
     * 用 OpenAiImageApi 构造 OpenAiImageModel（和你原来的逻辑一致）
     */
    @Bean
    public OpenAiImageModel openAiImageModel(OpenAiImageApi openAiImageApi) {
        return new OpenAiImageModel(openAiImageApi);
    }

    /**
     * 创建 OpenAiModerationApi Bean
     */
    @Bean
    public OpenAiModerationApi openAiModerationApi(
            RestClient.Builder restClientBuilder,
            ResponseErrorHandler responseErrorHandler,
            MultiValueMap<String, String> openAiHeaders) {
        return new OpenAiModerationApi(
                openAiBaseUrl,
                new SimpleApiKey(openAiApiKey),
                openAiHeaders,
                restClientBuilder,
                responseErrorHandler
        );
    }

    /**
     * 创建 OpenAiModerationModel Bean
     */
    @Bean
    public OpenAiModerationModel openAiModerationModel(OpenAiModerationApi openAiModerationApi) {
        return new OpenAiModerationModel(openAiModerationApi);
    }




    @Bean
    public VectorStore vectorStore(OpenAiEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean("chatClient")
    public ChatClient chatClient(OpenAiChatModel model, @Qualifier("redisChatMemory") ChatMemory chatMemory){
        return ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.CHAT_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)

                )
                .build();
    }
    @Bean("pdfChatClientModel")
    public ChatClient PDFModel(OpenAiChatModel model,@Qualifier("redisChatMemory") ChatMemory chatMemory){
        return ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.PDF_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)

                )
                .build();
    }
    @Bean("pdfChatClient")
    public ChatClient pdfChatClient(OpenAiChatModel model,@Qualifier("redisChatMemory") ChatMemory chatMemory, VectorStore vectorStore) {
        return ChatClient
                .builder(model)
//                .defaultSystem(SystemConstants.PDF_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory),
                        new QuestionAnswerAdvisor(
                                vectorStore,
                                SearchRequest.builder()
                                        .similarityThreshold(0.6)
                                        .topK(2)
                                        .build()
                        )
                )
                .build();
    }
}