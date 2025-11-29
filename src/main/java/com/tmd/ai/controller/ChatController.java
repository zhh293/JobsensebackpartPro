package com.tmd.ai.controller;

import com.tmd.ai.entity.vo.MessageVO;
import com.tmd.ai.repository.ChatHistoryRepository;
import com.tmd.ai.最新的聊天API.RedisChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai1")
public class ChatController {

    private final ChatClient chatClient;

    @Qualifier("redisChatHistoryRespository")
    private final ChatHistoryRepository chatHistoryRepository;

    private final RedisChatMemory chatMemory;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public String chat(String prompt, String chatId){
        //1.保存会话ID
        chatHistoryRepository.save("chat",chatId);
        //2.请求模型
        return chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatId))
                .call()
                .content();
    }

    @GetMapping("/history/{type}/{chatId}")
    public List<MessageVO> getHistory(@PathVariable String type, @PathVariable String chatId) {
        List<Message> messages = chatMemory.get(chatId, Integer.MAX_VALUE);
        if(messages==null){
            return List.of();
        }
        return messages.stream().map(message -> new MessageVO((message))).toList();
    }

    @GetMapping("/history/{type}")
    public List<String> getChatIds(@PathVariable String type) {
        return chatHistoryRepository.getChatIds(type);
    }
}