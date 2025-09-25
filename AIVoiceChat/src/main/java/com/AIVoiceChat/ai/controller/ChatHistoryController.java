package com.AIVoiceChat.ai.controller;


import com.AIVoiceChat.ai.entity.Result;
import com.AIVoiceChat.ai.entity.vo.MessageVO;
import com.AIVoiceChat.ai.repository.ChatHistoryRepository;
import com.AIVoiceChat.ai.repository.InMemoryChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {

    private final ChatHistoryRepository chatHistoryRepository;

    // 注入InMemoryChatHistoryRepository以访问语音历史记录
    private final InMemoryChatHistoryRepository inMemoryChatHistoryRepository;

    private final ChatMemory chatMemory;

    /**
     * 查询会话历史列表
     * @param type 业务类型，如：chat,service,pdf
     * @return chatId列表
     */
    @GetMapping("/{type}")
    public Result getChatIds(@PathVariable("type") String type) {
        return Result.success(chatHistoryRepository.getChatIds("chat"));
    }

    /**
     * 根据业务类型、chatId查询会话历史（包含语音历史记录）
     * @param type 业务类型，如：chat,service,pdf
     * @param chatId 会话id
     * @return 指定会话的历史消息（包含文本和语音）
     */
    @GetMapping("/{type}/{chatId}")
    public Result getChatHistory(@PathVariable("type") String type, @PathVariable("chatId") String chatId) {
        // 获取聊天历史记录
        List<Message> messages = chatMemory.get(chatId, Integer.MAX_VALUE);
        List<MessageVO> result = new ArrayList<>();

        if(messages != null) {
            result.addAll(messages.stream().map(MessageVO::new).toList());
        }

        // 获取语音历史记录并转换为MessageVO格式
        List<HashMap<String, Object>> voiceHistory = inMemoryChatHistoryRepository.getVoiceHistory(chatId);
        for (HashMap<String, Object> voiceRecord : voiceHistory) {
            // 创建一个表示语音记录的MessageVO
            String text = (String) voiceRecord.get("text");

            // 创建一个简单的消息VO来表示语音记录
            MessageVO voiceMessageVO = new MessageVO();
            voiceMessageVO.setRole("user"); // 默认设为user角色
            voiceMessageVO.setContent("[语音] " + (text != null ? text : "语音记录"));

            result.add(voiceMessageVO);
        }

        return Result.success(result);
    }

    /**
     * 根据chatId查询语音历史记录
     * @param chatId 会话id
     * @return 指定会话的语音历史记录
     */
    @GetMapping("/voice/{chatId}")
    public Result getVoiceHistory(@PathVariable("chatId") String chatId) {
        return Result.success(inMemoryChatHistoryRepository.getVoiceHistory(chatId));
    }
}