package com.AIVoiceChat.ai.controller;


import com.AIVoiceChat.ai.entity.Result;
import com.AIVoiceChat.ai.service.IChatSessionService;
import com.AIVoiceChat.ai.service.impl.ChatSessionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 马卓航
 * @since 2025-09-25
 */
@RestController
@RequestMapping("/chat-session")
public class ChatSessionController {

    @Autowired
    private ChatSessionServiceImpl chatService;
    /**
     * 生成会话id
     * @param CharacterId 人物
     * @return chatId
     */
    @GetMapping("/generateChatId")
    public Result generateChatId(@RequestParam int CharacterId) {
        return chatService.generateChatId( CharacterId);
    }
}
