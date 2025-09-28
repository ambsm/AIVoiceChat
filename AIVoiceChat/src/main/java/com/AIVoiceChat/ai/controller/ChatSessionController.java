package com.AIVoiceChat.ai.controller;

import com.AIVoiceChat.ai.entity.Result;
import com.AIVoiceChat.ai.service.impl.ChatSessionServiceImpl;
import com.AIVoiceChat.ai.utils.UnifiedttsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionServiceImpl chatService;
    private final UnifiedttsUtils unifiedttsUtils;
    
    /**
     * 生成会话id
     * @param CharacterId 人物ID
     * @return chatId
     */
    @GetMapping("/generateChatId")
    public Result generateChatId(@RequestParam int CharacterId) {
        return chatService.generateChatId(CharacterId);
    }

    /**
     * 根据会话ID获取角色信息
     * @param chatName 会话名
     * @return 会话列表
     */
    @GetMapping("/getChatSessionList")
    public Result getChatSessionList(@RequestParam String chatName) {
        return chatService.getChatSessionList(chatName);
    }
    
    /**
     * 获取TTS模型列表
     * @return TTS模型列表
     */
    @GetMapping("/tts/models")
    public Result getTTSModels() {
        try {
            Map<String, Object> result = unifiedttsUtils.getModelList();
            if (result != null && (Boolean) result.getOrDefault("success", false)) {
                return Result.success(result);
            } else {
                return Result.error("获取TTS模型列表失败: " + (result != null ? result.get("message") : "空响应"));
            }
        } catch (Exception e) {
            return Result.error("获取TTS模型列表异常: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定模型的音色列表
     * @param model 模型名称
     * @return 音色列表
     */
    @GetMapping("/tts/voices/{model}")
    public Result getTTSVoices(@PathVariable String model) {
        try {
            Map<String, Object> result = unifiedttsUtils.getVoiceList(model);
            if (result != null && (Boolean) result.getOrDefault("success", false)) {
                return Result.success(result);
            } else {
                return Result.error("获取音色列表失败: " + (result != null ? result.get("message") : "空响应"));
            }
        } catch (Exception e) {
            return Result.error("获取音色列表异常: " + e.getMessage());
        }
    }
    
    /**
     * 文本转语音
     * @param text 要转换的文本
     * @param model 使用的模型
     * @param voice 音色
     * @param speed 语速(可选)
     * @param volume 音量(可选)
     * @return TTS结果
     */
    @PostMapping("/tts/convert")
    public Result textToSpeech(
            @RequestParam String text,
            @RequestParam String model,
            @RequestParam String voice,
            @RequestParam(required = false, defaultValue = "1.0") Double speed,
            @RequestParam(required = false, defaultValue = "1.0") Double volume) {
        try {
            Map<String, Object> result = unifiedttsUtils.textToSpeech(text, model, voice, speed, volume, null);
            if (result != null && (Boolean) result.getOrDefault("success", false)) {
                return Result.success(result);
            } else {
                return Result.error("语音合成失败: " + (result != null ? result.get("message") : "空响应"));
            }
        } catch (Exception e) {
            return Result.error("语音合成异常: " + e.getMessage());
        }
    }
    
    /**
     * 测试API连接
     * @return 连接测试结果
     */
    @GetMapping("/tts/test")
    public Result testTTSConnection() {
        try {
            Map<String, Object> result = unifiedttsUtils.testConnection();
            if (result != null && (Boolean) result.getOrDefault("success", false)) {
                return Result.success(result);
            } else {
                return Result.error("API连接测试失败: " + (result != null ? result.get("message") : "空响应"));
            }
        } catch (Exception e) {
            return Result.error("API连接测试异常: " + e.getMessage());
        }
    }
}