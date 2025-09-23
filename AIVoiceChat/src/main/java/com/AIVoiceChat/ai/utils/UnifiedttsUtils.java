package com.AIVoiceChat.ai.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.io.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

/**
 * UnifiedTTS统一语音合成工具类
 * 支持模型列表查询、音色列表查询和文本转语音功能
 */
@Component
public class UnifiedttsUtils {
    
    @Value("${unifiedtts.api.base-url:https://unifiedtts.com}")
    private String baseUrl;
    
    @Value("${unifiedtts.api.key:}")
    private String apiKey;
    
    /**
     * 获取可用的TTS模型列表
     * @return 模型列表JSON响应
     */
    public JSONObject getModelList() {
        try {
            String url = baseUrl + "/api/v1/tools/models";
            
            HttpResponse response = HttpRequest.get(url)
                    .header("X-API-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(10000)
                    .execute();
            
            if (response.isOk()) {
                String body = response.body();
                return JSONUtil.parseObj(body);
            } else {
                return createErrorResponse("获取模型列表失败", response.getStatus(), response.body());
            }
            
        } catch (Exception e) {
            return createErrorResponse("请求异常: " + e.getMessage(), 500, null);
        }
    }
    
    /**
     * 获取指定模型的音色列表
     * @param model 模型名称，如 "edge-tts"
     * @return 音色列表JSON响应
     */
    public JSONObject getVoiceList(String model) {
        try {
            String url = baseUrl + "/api/v1/tools/voices/" + model;
            
            HttpResponse response = HttpRequest.get(url)
                    .header("X-API-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(10000)
                    .execute();
            
            if (response.isOk()) {
                String body = response.body();
                return JSONUtil.parseObj(body);
            } else {
                return createErrorResponse("获取音色列表失败", response.getStatus(), response.body());
            }
            
        } catch (Exception e) {
            return createErrorResponse("请求异常: " + e.getMessage(), 500, null);
        }
    }
    
    /**
     * 文本转语音（同步接口）
     * @param text 要转换的文本
     * @param model 使用的模型，如 "edge-tts"
     * @param voice 音色，如 "zh-CN-XiaoxiaoNeural"
     * @param speed 语速，范围通常是 0.5-2.0，默认 1.0
     * @param volume 音量，范围通常是 0.0-1.0，默认 1.0
     * @param outputPath 输出音频文件路径（可选）
     * @return TTS响应结果
     */
    public JSONObject textToSpeech(String text, String model, String voice, 
                                 Double speed, Double volume, String outputPath) {
        try {
            String url = baseUrl + "/api/v1/common/tts-sync";
            
            // 构建请求参数
            JSONObject requestBody = new JSONObject();
            requestBody.put("text", text);
            requestBody.put("model", model);
            requestBody.put("voice", voice);
            
            if (speed != null) {
                requestBody.put("speed", speed);
            }
            if (volume != null) {
                requestBody.put("volume", volume);
            }
            
            HttpResponse response = HttpRequest.post(url)
                    .header("X-API-Key", apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(30000) // TTS可能需要更长时间
                    .execute();
            
            if (response.isOk()) {
                String contentType = response.header("Content-Type");
                
                // 如果返回的是音频文件
                if (contentType != null && contentType.startsWith("audio/")) {
                    byte[] audioData = response.bodyBytes();
                    
                    // 保存音频文件
                    if (outputPath != null && !outputPath.isEmpty()) {
                        FileUtil.writeBytes(audioData, outputPath);
                    }
                    
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    result.put("message", "语音合成成功");
                    result.put("audioSize", audioData.length);
                    result.put("outputPath", outputPath);
                    result.put("audioData", audioData); // 可以直接使用的音频数据
                    
                    return result;
                } else {
                    // 返回JSON响应
                    String body = response.body();
                    return JSONUtil.parseObj(body);
                }
            } else {
                return createErrorResponse("语音合成失败", response.getStatus(), response.body());
            }
            
        } catch (Exception e) {
            return createErrorResponse("请求异常: " + e.getMessage(), 500, null);
        }
    }
    
    /**
     * 文本转语音（简化版本，使用默认参数）
     * @param text 要转换的文本
     * @param model 使用的模型
     * @param voice 音色
     * @return TTS响应结果
     */
    public JSONObject textToSpeech(String text, String model, String voice) {
        return textToSpeech(text, model, voice, null, null, null);
    }
    
    /**
     * 文本转语音并保存到文件
     * @param text 要转换的文本
     * @param model 使用的模型
     * @param voice 音色
     * @param outputPath 输出文件路径
     * @return TTS响应结果
     */
    public JSONObject textToSpeechWithFile(String text, String model, String voice, String outputPath) {
        return textToSpeech(text, model, voice, null, null, outputPath);
    }
    
    /**
     * 获取常用的音色列表（预定义）
     * @return 常用音色列表
     */
    public List<JSONObject> getCommonVoices() {
        List<JSONObject> voices = new ArrayList<>();
        
        // 中文音色
        voices.add(createVoiceInfo("zh-CN-XiaoxiaoNeural", "晓晓", "zh-CN", "female"));
        voices.add(createVoiceInfo("zh-CN-YunxiNeural", "云希", "zh-CN", "male"));
        voices.add(createVoiceInfo("zh-CN-YunyangNeural", "云扬", "zh-CN", "male"));
        voices.add(createVoiceInfo("zh-CN-XiaoyiNeural", "晓伊", "zh-CN", "female"));
        
        // 英文音色
        voices.add(createVoiceInfo("en-US-AriaNeural", "Aria", "en-US", "female"));
        voices.add(createVoiceInfo("en-US-DavisNeural", "Davis", "en-US", "male"));
        voices.add(createVoiceInfo("en-US-JennyNeural", "Jenny", "en-US", "female"));
        voices.add(createVoiceInfo("en-US-GuyNeural", "Guy", "en-US", "male"));
        
        return voices;
    }
    
    /**
     * 创建音色信息对象
     */
    private JSONObject createVoiceInfo(String voiceId, String displayName, String language, String gender) {
        JSONObject voice = new JSONObject();
        voice.put("voiceId", voiceId);
        voice.put("displayName", displayName);
        voice.put("language", language);
        voice.put("gender", gender);
        return voice;
    }
    
    /**
     * 创建错误响应
     */
    private JSONObject createErrorResponse(String message, int statusCode, String details) {
        JSONObject error = new JSONObject();
        error.put("success", false);
        error.put("message", message);
        error.put("statusCode", statusCode);
        if (details != null) {
            error.put("details", details);
        }
        return error;
    }
    
    /**
     * 验证API配置
     * @return 是否配置正确
     */
    public boolean validateConfig() {
        return apiKey != null && !apiKey.isEmpty() && baseUrl != null && !baseUrl.isEmpty();
    }
    
    /**
     * 测试API连接
     * @return 连接测试结果
     */
    public JSONObject testConnection() {
        if (!validateConfig()) {
            return createErrorResponse("API配置不完整，请检查apiKey和baseUrl", 400, null);
        }
        
        try {
            JSONObject result = getModelList();
            if (result.getBool("success", false)) {
                JSONObject testResult = new JSONObject();
                testResult.put("success", true);
                testResult.put("message", "API连接测试成功");
                testResult.put("modelsCount", result.getJSONArray("data").size());
                return testResult;
            } else {
                return result;
            }
        } catch (Exception e) {
            return createErrorResponse("连接测试失败: " + e.getMessage(), 500, null);
        }
    }
}