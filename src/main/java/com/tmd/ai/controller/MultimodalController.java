package com.tmd.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/ai/multimodal")
@Slf4j
@RequiredArgsConstructor
public class MultimodalController {

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openaiBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 文本转语音 - 基础版本
     *
     * @param text 输入文本
     * @return 生成的音频文件
     */
    @PostMapping("/tts")
    public ResponseEntity<byte[]> textToSpeech(@RequestBody String text) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = Map.of(
                    "model", "tts-1",
                    "input", text,
                    "voice", "alloy"
            );

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            // 创建请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            String url = openaiBaseUrl.endsWith("/v1") ? openaiBaseUrl : openaiBaseUrl + "/v1" + "/audio/speech";
            ResponseEntity<byte[]> response = restTemplate.postForEntity(url, requestEntity, byte[].class);

            // 返回音频数据
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                    .body(response.getBody());
        } catch (Exception e) {
            log.error("文本转语音失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 文本转语音 - 自定义参数版本
     *
     * @param request 包含文本和语音参数的请求
     * @return 生成的音频文件
     */
    @PostMapping("/tts/custom")
    public ResponseEntity<byte[]> textToSpeechCustom(@RequestBody Map<String, Object> request) {
        try {
            String text = (String) request.get("text");
            String voice = (String) request.getOrDefault("voice", "alloy");
            String format = (String) request.getOrDefault("format", "mp3");
            Double speed = (Double) request.getOrDefault("speed", 1.0);

            // 构建请求体
            Map<String, Object> requestBody = Map.of(
                    "model", "tts-1",
                    "input", text,
                    "voice", voice,
                    "response_format", format,
                    "speed", speed
            );

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            // 创建请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            String url = openaiBaseUrl.endsWith("/v1") ? openaiBaseUrl : openaiBaseUrl + "/v1" + "/audio/speech";
            ResponseEntity<byte[]> response = restTemplate.postForEntity(url, requestEntity, byte[].class);

            // 返回音频数据
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(getMediaType(format)))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech." + format + "\"")
                    .body(response.getBody());
        } catch (Exception e) {
            log.error("自定义文本转语音失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 语音转文字 - 基础版本
     *
     * @param file 上传的音频文件
     * @return 转录结果
     */
    @PostMapping("/stt")
    public ResponseEntity<String> speechToText(@RequestParam("file") MultipartFile file) {
        try {
            // 创建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("model", "whisper-1");

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openaiApiKey);

            // 创建请求实体
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送请求
            String url = openaiBaseUrl.endsWith("/v1") ? openaiBaseUrl : openaiBaseUrl + "/v1" + "/audio/transcriptions";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            // 返回转录结果
            String text = (String) response.getBody().get("text");
            return ResponseEntity.ok(text);
        } catch (Exception e) {
            log.error("语音转文字失败", e);
            return ResponseEntity.internalServerError().body("语音转文字失败: " + e.getMessage());
        }
    }

    /**
     * 语音转文字 - 自定义参数版本
     *
     * @param file 上传的音频文件
     * @return 转录结果
     */
    @PostMapping("/stt/custom")
    public ResponseEntity<String> speechToTextCustom(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "zh") String language,
            @RequestParam(value = "model", defaultValue = "whisper-1") String model,
            @RequestParam(value = "prompt", required = false) String promptText) {
        try {
            // 创建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("model", model);
            body.add("language", language);
            if (promptText != null && !promptText.isEmpty()) {
                body.add("prompt", promptText);
            }

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openaiApiKey);

            // 创建请求实体
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送请求
            String url = openaiBaseUrl.endsWith("/v1") ? openaiBaseUrl : openaiBaseUrl + "/v1" + "/audio/transcriptions";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            // 返回转录结果
            String text = (String) response.getBody().get("text");
            return ResponseEntity.ok(text);
        } catch (Exception e) {
            log.error("自定义语音转文字失败", e);
            return ResponseEntity.internalServerError().body("语音转文字失败: " + e.getMessage());
        }
    }

    /**
     * 语音转文字 - SRT字幕格式输出
     *
     * @param file 上传的音频文件
     * @return SRT格式的字幕
     */
    @PostMapping("/stt/srt")
    public ResponseEntity<String> speechToTextSrt(@RequestParam("file") MultipartFile file) {
        try {
            // 创建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("model", "whisper-1");
            body.add("response_format", "srt");

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openaiApiKey);

            // 创建请求实体
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送请求
            String url = openaiBaseUrl.endsWith("/v1") ? openaiBaseUrl : openaiBaseUrl + "/v1" + "/audio/transcriptions";
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            // 返回SRT格式的字幕
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(response.getBody());
        } catch (Exception e) {
            log.error("SRT字幕生成失败", e);
            return ResponseEntity.internalServerError().body("SRT字幕生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取媒体类型
     */
    private String getMediaType(String format) {
        return switch (format.toLowerCase()) {
            case "mp3" -> "audio/mpeg";
            case "opus" -> "audio/ogg";
            case "aac" -> "audio/aac";
            case "flac" -> "audio/flac";
            case "wav" -> "audio/wav";
            case "pcm" -> "audio/L24";
            default -> "audio/mpeg";
        };
    }
}