package com.AIVoiceChat.ai.repository;

import com.AIVoiceChat.ai.entity.po.Msg;
import com.AIVoiceChat.ai.repository.ChatHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {

    private Map<String, List<String>> chatHistory;

    // 用于存储语音聊天历史记录
    private Map<String, List<HashMap<String, Object>>> voiceHistory;

    private final ObjectMapper objectMapper;

    private final ChatMemory chatMemory;

    @Override
    public void save(String type, String chatId) {
        /*if (!chatHistory.containsKey(type)) {
            chatHistory.put(type, new ArrayList<>());
        }
        List<String> chatIds = chatHistory.get(type);*/
        List<String> chatIds = chatHistory.computeIfAbsent(type, k -> new ArrayList<>());
        if (chatIds.contains(chatId)) {
            return;
        }
        chatIds.add(0, chatId);
    }

    @Override
    public List<String> getChatIds(String type) {
        /*List<String> chatIds = chatHistory.get(type);
        return chatIds == null ? List.of() : chatIds;*/
        return chatHistory.getOrDefault(type, List.of());
    }

    /**
     * 保存语音聊天历史记录
     * @param chatId
     * @param result
     */
    @Override
    public void saveVoice(String chatId, HashMap<String, Object> result) {
        // 初始化voiceHistory如果为null
        if (this.voiceHistory == null) {
            this.voiceHistory = new HashMap<>();
        }
        
        // 添加时间戳字段
        if (!result.containsKey("timestamp")) {
            result.put("timestamp", System.currentTimeMillis());
        }
        
        // 获取指定chatId的语音历史记录列表，如果不存在则创建新的列表
        List<HashMap<String, Object>> voiceList = this.voiceHistory.computeIfAbsent(chatId, k -> new ArrayList<>());
        
        // 将新的语音识别结果添加到列表中
        voiceList.add(result);
    }
    
    /**
     * 获取指定chatId的语音聊天历史记录
     * @param chatId
     * @return 语音历史记录列表
     */
    public List<HashMap<String, Object>> getVoiceHistory(String chatId) {
        if (this.voiceHistory == null) {
            return new ArrayList<>();
        }
        return this.voiceHistory.getOrDefault(chatId, new ArrayList<>());
    }

    @PostConstruct
    private void init() {
        // 1.初始化会话历史记录
        this.chatHistory = new HashMap<>();
        // 2.初始化语音历史记录
        this.voiceHistory = new HashMap<>();
        // 3.读取本地会话历史和会话记忆
        FileSystemResource historyResource = new FileSystemResource("chat-history.json");
        FileSystemResource memoryResource = new FileSystemResource("chat-memory.json");
        FileSystemResource voiceHistoryResource = new FileSystemResource("voice-history.json");
        
        try {
            // 会话历史
            if (historyResource.exists()) {
                Map<String, List<String>> chatIds = this.objectMapper.readValue(historyResource.getInputStream(), new TypeReference<>() {
                });
                if (chatIds != null) {
                    this.chatHistory = chatIds;
                }
            }
            
            // 会话记忆
            if (memoryResource.exists()) {
                Map<String, List<Msg>> memory = this.objectMapper.readValue(memoryResource.getInputStream(), new TypeReference<>() {
                });
                if (memory != null) {
                    memory.forEach(this::convertMsgToMessage);
                }
            }
            
            // 语音历史记录
            if (voiceHistoryResource.exists()) {
                Map<String, List<HashMap<String, Object>>> voiceHistory = this.objectMapper.readValue(voiceHistoryResource.getInputStream(), new TypeReference<>() {
                });
                if (voiceHistory != null) {
                    this.voiceHistory = voiceHistory;
                }
            }
        } catch (IOException ex) {
            log.error("读取历史记录文件时发生错误", ex);
            throw new RuntimeException(ex);
        }
    }

    private void convertMsgToMessage(String chatId, List<Msg> messages) {
        this.chatMemory.add(chatId, messages.stream().map(Msg::toMessage).toList());
    }

    @PreDestroy
    private void persistent() {
        //触发持久化
        log.info("持久化开始...");
        String history = toJsonString(this.chatHistory);
        String memory = getMemoryJsonString();
        String voiceHistory = toJsonString(this.voiceHistory);
        FileSystemResource historyResource = new FileSystemResource("chat-history.json");
        FileSystemResource memoryResource = new FileSystemResource("chat-memory.json");
        FileSystemResource voiceHistoryResource = new FileSystemResource("voice-history.json");
        try (
                PrintWriter historyWriter = new PrintWriter(historyResource.getOutputStream(), true, StandardCharsets.UTF_8);
                PrintWriter memoryWriter = new PrintWriter(memoryResource.getOutputStream(), true, StandardCharsets.UTF_8);
                PrintWriter voiceHistoryWriter = new PrintWriter(voiceHistoryResource.getOutputStream(), true, StandardCharsets.UTF_8)
        ) {
            historyWriter.write(history);
            memoryWriter.write(memory);
            voiceHistoryWriter.write(voiceHistory);
        } catch (IOException ex) {
            log.error("IOException occurred while saving vector store file.", ex);
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            log.error("SecurityException occurred while saving vector store file.", ex);
            throw new RuntimeException(ex);
        } catch (NullPointerException ex) {
            log.error("NullPointerException occurred while saving vector store file.", ex);
            throw new RuntimeException(ex);
        }
    }

    private String getMemoryJsonString() {
        Class<InMemoryChatMemory> clazz = InMemoryChatMemory.class;
        try {
            Field field = clazz.getDeclaredField("conversationHistory");
            field.setAccessible(true);
            Map<String, List<Message>> memory = (Map<String, List<Message>>) field.get(chatMemory);
            Map<String, List<Msg>> memoryToSave = new HashMap<>();
            memory.forEach((chatId, messages) -> memoryToSave.put(chatId, messages.stream().map(Msg::new).toList()));
            return toJsonString(memoryToSave);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String toJsonString(Object object) {
        ObjectWriter objectWriter = this.objectMapper.writerWithDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing documentMap to JSON.", e);
        }
    }
}
