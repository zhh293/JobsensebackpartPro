package com.tmd.ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.*;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.OpenAiModerationModel;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.ai.openai.api.OpenAiModerationApi;
import org.springframework.ai.openai.OpenAiModerationOptions;
import org.springframework.ai.moderation.ModerationPrompt;
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


    // 注入OpenAI图像模型
    private final OpenAiImageModel openAiImageModel;
    
    // 注入OpenAI审核模型
    private final OpenAiModerationModel openAiModerationModel;

    private final OpenAiImageModel openAiImageModelCustom;



    /**
     * 根据文本生成图像 - 基础版本
     *
     * @param msg 图像描述文本
     * @return 生成的图像URL列表
     */
    @PostMapping("/image")
    public String imageGen(@RequestParam(value = "msg", defaultValue = "画一只小猫") String msg,
                           @RequestParam(value = "useCustomModel", required = false, defaultValue = "false") boolean useCustomModel){

        //useCustomModel 为true时，使用自定义配置
        OpenAiImageModel imageModel = useCustomModel ? openAiImageModelCustom : openAiImageModel;

        ImageOptions options = ImageOptionsBuilder.builder().model("dall-e-3").height(1024).width(1024).build();
        ImagePrompt imagePrompt = new ImagePrompt(msg, options);
        ImageResponse response = imageModel.call(imagePrompt);
        String imageUrl = response.getResult().getOutput().getUrl();
//        logger.info("imageUrl:{}",imageUrl);
        return "<html><body><img src=\"" + imageUrl + "\" alt=\"Generated Image\" /></body></html>";
    }

    /**
     * 根据文本生成图像 - 自定义参数版本
     *
     * @param request 包含图像参数的请求
     * @return 生成的图像URL列表
     */
    @PostMapping("/image/custom")
    public ResponseEntity<ImageResponse> generateImageCustom(@RequestBody Map<String, Object> request) {
        try {
            String prompt = (String) request.get("prompt");
            
            // 检查输入内容是否合规
            if (!isTextContentAllowed(prompt)) {
                return ResponseEntity.badRequest().build();
            }
            
            String model = (String) request.getOrDefault("model", OpenAiImageApi.DEFAULT_IMAGE_MODEL);
            String size = (String) request.getOrDefault("size", "1024x1024");
            String quality = (String) request.getOrDefault("quality", "standard");
            Integer n = (Integer) request.getOrDefault("n", 1);
            String style = (String) request.getOrDefault("style", "vivid");

            ImagePrompt imagePrompt = new ImagePrompt(
                prompt,
                OpenAiImageOptions.builder()
                    .model(model)
                    .quality(quality)
                    .style(style)
                    .build()
            );

            ImageResponse response = openAiImageModel.call(imagePrompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("自定义图像生成失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 生成面试场景图像
     *
     * @param request 面试场景参数
     * @return 生成的面试场景图像
     */
    @PostMapping("/image/interview-scene")
    public ResponseEntity<ImageResponse> generateInterviewScene(@RequestBody Map<String, Object> request) {
        /*OpenAiImageModel imageModel = useCustomModel ? openAiImageModelCustom : openAiImageModel;

        ImageOptions options = ImageOptionsBuilder.builder().model("dall-e-3").height(1024).width(1024).build();
        ImagePrompt imagePrompt = new ImagePrompt(msg, options);
        ImageResponse response = imageModel.call(imagePrompt);
        String imageUrl = response.getResult().getOutput().getUrl();*/
        try {
            String jobTitle = (String) request.getOrDefault("jobTitle", "软件工程师");
            String experienceLevel = (String) request.getOrDefault("experienceLevel", "中级");
            String interviewType = (String) request.getOrDefault("interviewType", "技术面试");

            String prompt = String.format(
                "创建一个描绘%s级别的%s场景的图像。展示一个专业的面试环境，包括面试官和候选人。" +
                "风格应该是现实主义，专业的办公环境，光线良好，给人积极的印象。",
                experienceLevel, jobTitle, interviewType);
                
            // 检查输入内容是否合规
//            if (!isTextContentAllowed(prompt)) {
//                return ResponseEntity.badRequest().build();
//            }

            ImagePrompt imagePrompt = new ImagePrompt(
                prompt,
                ImageOptionsBuilder.builder()
                    .model("dall-e-3")
                        .height(1024)
                        .width(1024)
                    .style("vivid")
                    .build()
            );

            ImageResponse response = openAiImageModel.call(imagePrompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("面试场景图像生成失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 生成技能雷达图
     *
     * @param request 技能参数
     * @return 生成的技能雷达图
     */
    @PostMapping("/image/skill-radar")
    public ResponseEntity<ImageResponse> generateSkillRadar(@RequestBody Map<String, Object> request) {
        try {
            String candidateName = (String) request.getOrDefault("candidateName", "候选人");

            String prompt = String.format(
                "为候选人%s创建一个专业的技能雷达图。图表应包括技术技能、沟通能力、团队合作、问题解决和领导力等维度。" +
                "使用现代、清晰的设计风格，颜色搭配专业，适合在面试报告中使用。",
                candidateName);
                
            // 检查输入内容是否合规
//            if (!isTextContentAllowed(prompt)) {
//                return ResponseEntity.badRequest().build();
//            }

            ImagePrompt imagePrompt = new ImagePrompt(
                    prompt,
                    ImageOptionsBuilder.builder()
                            .model("dall-e-3")
                            .height(1024)
                            .width(1024)
                            .style("vivid")
                            .build()
            );

            ImageResponse response = openAiImageModel.call(imagePrompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("技能雷达图生成失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 检查文本内容是否符合规范
     *
     * @param text 待检查的文本
     * @return true表示内容合规，false表示内容不合规
     */
    private boolean isTextContentAllowed(String text) {
        try {
            // 构建审核选项
            // 调用OpenAI的"text-moderation-latest"模型进行审核
//            这是OpenAI官方训练好的模型，专门用于检测文本中的有害内容
            OpenAiModerationOptions options = OpenAiModerationOptions.builder()
                    .model(OpenAiModerationApi.DEFAULT_MODERATION_MODEL)
                    .build();

            // 创建审核提示
            ModerationPrompt prompt = new ModerationPrompt(text, options);

            // 调用审核模型
            ModerationResponse response = openAiModerationModel.call(prompt);

            // 检查是否有违规内容
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                var moderationResult = response.getResult().getOutput();
                if (!moderationResult.getResults().isEmpty()) {
                    var result = moderationResult.getResults().get(0);
                    // 如果标记为违规，则返回false
                    if (result.isFlagged()) {
                        log.warn("检测到违规内容: {}", text);
                        return false;
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("内容审核过程中发生错误: ", e);
            // 出错时保守地认为内容不合规
            return false;
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