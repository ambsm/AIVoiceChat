package com.AIVoiceChat.ai.controller;

import cn.hutool.json.JSONObject;
import com.AIVoiceChat.ai.repository.ChatHistoryRepository;
import com.AIVoiceChat.ai.utils.AliyunASRUtils;
import com.AIVoiceChat.ai.utils.TTSUtils;
import com.AIVoiceChat.ai.utils.UnifiedttsUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {
    @Autowired
    private TTSUtils ttsUtils;

    @Autowired
    AliyunASRUtils aliyunASRUtils;

    private final ChatClient chatClient;

    private final ChatHistoryRepository chatHistoryRepository;

    @RequestMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(
            @RequestParam("prompt") String prompt,
            @RequestParam("chatId") String chatId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        // 1.保存会话id
        chatHistoryRepository.save("chat", chatId);
        // 2.请求模型
        if (files == null || files.isEmpty()) {
            // 没有附件，纯文本聊天
            return textChat(prompt, chatId);
        } else {
            // 有附件，多模态聊天
            return multiModalChat(prompt, chatId, files);
        }

    }
    /**
     * 语音聊天
     * @param voiceFile
     * @param chatId
     * @param files
     * @return
     */
    @RequestMapping(value = "/voiceChat")
    public HashMap<String, Object> voiceChat(
            @RequestParam("prompt") MultipartFile  voiceFile,
            @RequestParam("chatId") String chatId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) throws Exception {
        // 1.保存会话id
        chatHistoryRepository.save("chat", chatId);
        //用户语音转换链接
        File tempFile = aliyunASRUtils.saveToTempFile(voiceFile);
        String fileUrl = aliyunASRUtils.uploadToOSS(tempFile, voiceFile.getOriginalFilename());
        String prompt = aliyunASRUtils.callAliyunASRAPI(fileUrl);
        //此处也使用流式传输时因为防止过长时间无响应导致连接断开
        Flux<String> stringFlux = textChat(prompt, chatId);
        String fullResponse = stringFlux.collect(StringBuilder::new,
                        (sb, s) -> sb.append(s))
                .map(StringBuilder::toString)
                .block(); // ⚠️ 仍然阻塞，但你明确需要完整结果
        JSONObject entries = ttsUtils.convertTextToSpeechByLiba(fullResponse);
        HashMap<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> dataMap = entries.getBean("data", Map.class);
            Object o = dataMap.get("audio_url");
            result.put("agentVoice", o);
            result.put("userVoice", fileUrl);
            chatHistoryRepository.saveVoice(chatId, result);
            return result;
        } catch (Exception e){
            e.printStackTrace();
        }
        return result;

    }

    private Flux<String> multiModalChat(String prompt, String chatId, List<MultipartFile> files) {
        // 1.解析多媒体
        List<Media> medias = files.stream()
                .map(file -> new Media(
                                MimeType.valueOf(Objects.requireNonNull(file.getContentType())),
                                file.getResource()
                        )
                )
                .toList();
        // 2.请求模型
        return chatClient.prompt()
                .user(p -> p.text(prompt).media(medias.toArray(Media[]::new)))
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .stream()
                .content();
    }

    private Flux<String> textChat(String prompt, String chatId) {
        return chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .stream()
                .content();
    }
}
