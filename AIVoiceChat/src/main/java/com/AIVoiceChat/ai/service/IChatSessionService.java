package com.AIVoiceChat.ai.service;

import com.AIVoiceChat.ai.entity.Result;
import com.AIVoiceChat.ai.entity.dto.ChatSession;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 马卓航
 * @since 2025-09-25
 */
public interface IChatSessionService extends IService<ChatSession> {
    public Result generateChatId(int characterId);

    String getStytem(String characterId);

    ChatSession getByChatName(String chatId);
}
