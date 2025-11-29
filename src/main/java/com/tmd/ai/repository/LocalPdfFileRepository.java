package com.tmd.ai.repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalPdfFileRepository implements FileRepository {

    private final VectorStore vectorStore;
    private final StringRedisTemplate redisTemplate;

    // 会话id 与 文件名的对应关系，方便查询会话历史时重新加载文件
    private final Properties chatFiles = new Properties();

    // 定义统一的文件存储目录，使用相对路径而不是绝对路径
    private static final String UPLOAD_DIR = "uploads/pdf/";
    
    // Redis中存储chatId到filename映射的key前缀
    private static final String CHAT_FILE_PREFIX = "chat:file:";
    
    // 过期时间设置为30天（以秒为单位）
    private static final long TTL_SECONDS = 30 * 24 * 60 * 60L;

    //之后最好使用阿里云的oss存储
    @Override
    public boolean save(String chatId, Resource resource) {
        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 1.保存到本地磁盘，使用统一的存储路径
            String filename = resource.getFilename();
            // 使用UUID确保文件名唯一，避免冲突
            String uniqueFilename = UUID.randomUUID().toString() + "_" + Objects.requireNonNull(filename);
            Path targetPath = uploadPath.resolve(uniqueFilename);

            // 只有文件不存在时才复制
            if (!Files.exists(targetPath)) {
                Files.copy(resource.getInputStream(), targetPath);
            }

            // 2.保存映射关系到Redis
            String redisKey = CHAT_FILE_PREFIX + chatId;
            redisTemplate.opsForValue().set(redisKey, uniqueFilename, TTL_SECONDS, TimeUnit.SECONDS);
            
            return true;
        } catch (IOException e) {
            log.error("Failed to save PDF resource.", e);
            return false;
        }
    }

    @Override
    public Resource getFile(String chatId) {
        // 从Redis获取文件名
        String redisKey = CHAT_FILE_PREFIX + chatId;
        String filename = redisTemplate.opsForValue().get(redisKey);
        
        if (filename != null) {
            Path filePath = Paths.get(UPLOAD_DIR, filename);
            return new FileSystemResource(filePath.toFile());
        }
        // 如果找不到文件，返回一个不存在的资源而不是null
        return new FileSystemResource("file/not/found");
    }

    @PostConstruct
    private void init() {
        // 确保上传目录存在
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory.", e);
        }

        // 不再需要从Properties文件加载数据，因为现在使用Redis存储
        
        FileSystemResource vectorResource = new FileSystemResource("chat-pdf.json");
        if (vectorResource.exists()) {
            SimpleVectorStore simpleVectorStore = (SimpleVectorStore) vectorStore;
            simpleVectorStore.load(vectorResource);
        }
    }

    @PreDestroy
    private void persistent() {
        // 不再需要保存Properties到文件，因为现在使用Redis存储

        SimpleVectorStore simpleVectorStore = (SimpleVectorStore) vectorStore;
        simpleVectorStore.save(new File("chat-pdf.json"));
    }
}