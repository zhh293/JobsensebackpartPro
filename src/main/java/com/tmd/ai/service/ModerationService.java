package com.tmd.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.openai.OpenAiModerationModel;
import org.springframework.ai.openai.api.OpenAiModerationApi;
import org.springframework.ai.openai.OpenAiModerationOptions;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {

    private final OpenAiModerationModel moderationModel;

    /**
     * 检查文本内容是否符合规范
     *
     * @param text 待检查的文本
     * @return true表示内容合规，false表示内容不合规
     */
    public boolean isTextContentAllowed(String text) {
        try {
            // 构建审核选项
            OpenAiModerationOptions options = OpenAiModerationOptions.builder()
                    .model(OpenAiModerationApi.DEFAULT_MODERATION_MODEL)
                    .build();

            // 创建审核提示
            ModerationPrompt prompt = new ModerationPrompt(text, options);

            // 调用审核模型
            ModerationResponse response = moderationModel.call(prompt);

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
}