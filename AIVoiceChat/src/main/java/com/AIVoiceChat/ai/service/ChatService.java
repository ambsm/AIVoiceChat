package com.AIVoiceChat.ai.service;

import org.springframework.stereotype.Service;

@Service
public class ChatService {
    /**
     * 根据人格前缀创建聊天ID
     *
     * @param character The name of the character.
     * @return A unique chat ID.
     */
    public String generateChatId(String character) {
        return character + "-" + System.currentTimeMillis();
    }
}
