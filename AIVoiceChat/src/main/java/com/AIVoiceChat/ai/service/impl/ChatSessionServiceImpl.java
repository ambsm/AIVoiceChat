package com.AIVoiceChat.ai.service.impl;

import com.AIVoiceChat.ai.entity.Result;
import com.AIVoiceChat.ai.entity.dto.ChatSession;
import com.AIVoiceChat.ai.mapper.ChatSessionMapper;
import com.AIVoiceChat.ai.service.IChatSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 马卓航
 * @since 2025-09-25
 */
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements IChatSessionService {

    /**
     * 根据人格前缀创建聊天ID
     *
     * @param character The name of the character.
     * @return A unique chat ID.
     */
    public Result generateChatId(int character) {
        String sessionName = character + "-" + System.currentTimeMillis();
        boolean result =this.save(new ChatSession(null, sessionName, character, null));
        if(result)return Result.success(sessionName);
        return Result.error("创建聊天ID失败");
    }
}
