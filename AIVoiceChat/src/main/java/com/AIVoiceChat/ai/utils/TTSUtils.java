package com.AIVoiceChat.ai.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.io.FileUtil;
import com.AIVoiceChat.ai.entity.dto.Character;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用于处理语音转文字和文字转语音的工具类
 * 支持多种语音识别和合成服务
 */
@Component
public class TTSUtils {
    
    @Autowired
    private UnifiedttsUtils unifiedttsUtils;
    // 配置文件中的 ASR 服务配置
    @Value("${asr.service.type:baidu}")
    private String asrServiceType;
    
    @Value("${asr.baidu.api-key:}")
    private String baiduApiKey;
    
    @Value("${asr.baidu.secret-key:}")
    private String baiduSecretKey;
    
    @Value("${asr.azure.subscription-key:}")
    private String azureSubscriptionKey;
    
    @Value("${asr.azure.region:eastus}")
    private String azureRegion;
    
    @Value("${asr.temp-dir:temp/audio}")
    private String tempDir;

    @Autowired
    private AliyunASRUtils aliyunASRUtils;

    /**
     * 智能语音识别接口 - 根据用户偏好自动处理多声道问题
     * 优先使用百度，如果遇到声道问题自动切换到 Azure
     */
    public String convertSpeechToTextIntelligent(MultipartFile file) {
        try {
            // 首先尝试百度 ASR
            String baiduResult = convertSpeechToTextByBaidu(file);
            
            // 检查是否是声道问题
            if (baiduResult.contains("3312") || baiduResult.contains("音频道数不正确")) {
                System.out.println("检测到百度 ASR 声道问题，自动切换到 Azure Speech Services...");
                
                // 根据项目技术栈规范，自动切换到 Azure
                try {
                    String azureResult = convertSpeechToTextByAzure(file);
                    return "[Azure ASR] " + azureResult;
                } catch (Exception azureException) {
                    // 如果 Azure 也失败，提供完整的解决方案
                    return String.format(
                        "🤖 智能处理结果：\n" +
                        "✗ 百度 ASR: 声道问题\n" +
                        "✗ Azure ASR: %s\n\n" +
                        "💡 建议解决方案：\n" +
                        "系统无法自动处理您的多声道音频。\n" +
                        "建议在录音或TTS生成时直接使用单声道格式。",
                        azureException.getMessage()
                    );
                }
            } else {
                // 百度识别成功
                return "[百度 ASR] " + baiduResult;
            }
            
        } catch (Exception e) {
            return "智能语音识别失败: " + e.getMessage();
        }
    }
    
    /**
     * 使用百度 ASR 服务进行语音识别
     */
    public String convertSpeechToTextByBaidu(MultipartFile file) {
        File processedFile = null;
        
        try {
            // 1. 验证文件
            if (file == null || file.isEmpty()) {
                return "音频文件不能为空";
            }
            
            // 2. 检查文件大小（百度 API 限制 10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                return "文件太大，请使用小于 10MB 的音频文件";
            }
            
            // 3. 获取 Access Token
            String accessToken = getBaiduAccessToken();
            if (accessToken == null) {
                return "获取百度访问令牌失败，请检查 API Key 和 Secret Key";
            }
            
            // 4. 根据项目规范进行音频预处理，确保单声道
            processedFile = null;
            byte[] audioData;
            try {
                processedFile = preprocessAudioForASR(file);
                audioData = Files.readAllBytes(processedFile.toPath());
            } catch (Exception e) {
                // 如枟预处理失败，使用原文件并警告用户
                System.err.println("音频预处理失败，使用原文件: " + e.getMessage());
                audioData = file.getBytes();
            }
            
            // 5. 将文件转换为 Base64
            String base64Audio = Base64.getEncoder().encodeToString(audioData);
            
            // 6. 获取音频格式
            String format = getAudioFormat(file.getOriginalFilename());
            
            // 7. 根据格式设置采样率
            int sampleRate = getSampleRateByFormat(format);
            
            // 8. 构建请求参数
            JSONObject requestBody = new JSONObject();
            requestBody.put("format", format);
            requestBody.put("rate", sampleRate);
            requestBody.put("channel", 1); // 强制设置为单声道
            requestBody.put("cuid", "java-client-" + System.currentTimeMillis());
            requestBody.put("token", accessToken);
            requestBody.put("speech", base64Audio);
            requestBody.put("len", audioData.length);
            requestBody.put("dev_pid", 1537); // 普通话（支持简单的英文识别）
            
            // 9. 发送请求
            String apiUrl = "https://vop.baidu.com/server_api";
            HttpResponse response = HttpRequest.post(apiUrl)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .body(requestBody.toString())
                    .timeout(30000)
                    .execute();
            
            // 10. 解析响应
            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());
                int errNo = result.getInt("err_no", -1);
                
                if (errNo == 0) {
                    // 识别成功
                    Object resultObj = result.get("result");
                    if (resultObj instanceof cn.hutool.json.JSONArray) {
                        cn.hutool.json.JSONArray resultArray = (cn.hutool.json.JSONArray) resultObj;
                        if (resultArray.size() > 0) {
                            return resultArray.getStr(0);
                        }
                    }
                    return "识别结果为空";
                } else {
                    // 识别失败，返回详细错误信息
                    String errMsg = result.getStr("err_msg", "未知错误");
                    return String.format("识别失败 [%d]: %s", errNo, getDetailedErrorMessage(errNo, errMsg));
                }
            } else {
                return "请求失败: HTTP " + response.getStatus() + " - " + response.body();
            }
            
        } catch (Exception e) {
            return "语音识别异常: " + e.getMessage();
        } finally {
            // 清理临时文件
            if (processedFile != null && processedFile.exists()) {
                try {
                    processedFile.delete();
                } catch (Exception e) {
                    System.err.println("删除临时文件失败: " + e.getMessage());
                }
            }
        }
    }
    /**
     * 使用阿里云 TTS 服务进行语音识别
     */
    public String convertSpeechToTextByAliyun(MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

            // 使用新的阿里云工具类
            String result = aliyunASRUtils.convertSpeechToText(file);

            response.put("success", true);
            response.put("result", result);
            response.put("service", "阿里云智能语音交互");
            response.put("filename", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("supportedFormats", new String[]{"wav", "mp3", "aac", "flac", "amr", "m4a"});
            response.put("maxFileSize", "512MB");
            response.put("apiType", "录音文件识别");
            response.put("configStatus", aliyunASRUtils.getConfigStatus());

            return response.get("result").toString();
    }
    /**
     * 使用 Azure Speech Services 进行语音识别
     */
    public String convertSpeechToTextByAzure(MultipartFile file) {
        try {
            // 1. 保存临时文件
            File tempFile = saveToTempFile(file);
            
            // 2. 构建 Azure 请求
            String region = azureRegion;
            String apiUrl = String.format("https://%s.stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=zh-CN", region);
            
            // 3. 发送请求
            HttpResponse response = HttpRequest.post(apiUrl)
                    .header("Ocp-Apim-Subscription-Key", azureSubscriptionKey)
                    .header("Content-Type", "audio/wav; codecs=audio/pcm; samplerate=16000")
                    .header("Accept", "application/json")
                    .form("file", tempFile)
                    .timeout(30000)
                    .execute();
            
            // 4. 清理临时文件
            tempFile.delete();
            
            // 5. 解析响应
            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());
                if ("Success".equals(result.getStr("RecognitionStatus"))) {
                    return result.getStr("DisplayText");
                } else {
                    return "识别失败: " + result.getStr("RecognitionStatus");
                }
            } else {
                return "请求失败: " + response.body();
            }
            
        } catch (Exception e) {
            return "语音识别异常: " + e.getMessage();
        }
    }
    
    /**
     * 本地简单的语音识别（模拟实现）
     */
    public String convertSpeechToTextByLocal(MultipartFile file) {
        try {
            // 这里是一个模拟实现，实际项目中可以集成 Whisper 或其他本地模型
            String filename = file.getOriginalFilename();
            long fileSize = file.getSize();
            
            // 模拟识别结果（实际应用中需要集成真实的 ASR 模型）
            if (filename != null && filename.toLowerCase().contains("hello")) {
                return "你好，这是一个测试音频。";
            } else if (fileSize > 100000) {
                return "这是一段较长的语音内容。";
            } else {
                return "短语音识别结果。";
            }
            
        } catch (Exception e) {
            return "本地识别异常: " + e.getMessage();
        }
    }
    
    /**
     * 获取百度 Access Token
     */
    private String getBaiduAccessToken() {
        try {
            String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token";
            
            HttpResponse response = HttpRequest.post(tokenUrl)
                    .form("grant_type", "client_credentials")
                    .form("client_id", baiduApiKey)
                    .form("client_secret", baiduSecretKey)
                    .timeout(10000)
                    .execute();
            
            if (response.isOk()) {
                JSONObject result = JSONUtil.parseObj(response.body());
                return result.getStr("access_token");
            }
            
        } catch (Exception e) {
            System.err.println("获取百度 Token 失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 根据文件名获取音频格式（百度 API 专用）
     */
    private String getAudioFormat(String filename) {
        if (filename == null) {
            return "wav";
        }
        
        String extension = filename.toLowerCase();
        if (extension.endsWith(".wav")) {
            return "wav";
        } else if (extension.endsWith(".mp3")) {
            return "mp3";
        } else if (extension.endsWith(".m4a")) {
            return "m4a";
        } else if (extension.endsWith(".flac")) {
            return "flac";
        } else if (extension.endsWith(".amr")) {
            return "amr";
        } else if (extension.endsWith(".pcm")) {
            return "pcm";
        } else {
            return "wav"; // 默认格式
        }
    }
    
    /**
     * 将 MultipartFile 保存为临时文件
     */
    private File saveToTempFile(MultipartFile file) throws IOException {
        // 创建临时目录
        File tempDirFile = new File(tempDir);
        if (!tempDirFile.exists()) {
            tempDirFile.mkdirs();
        }
        
        // 生成临时文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String tempFileName = "temp_audio_" + UUID.randomUUID().toString() + extension;
        File tempFile = new File(tempDirFile, tempFileName);
        
        // 保存文件
        Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        return tempFile;
    }
    
    /**
     * 根据音频格式获取推荐的采样率
     */
    private int getSampleRateByFormat(String format) {
        switch (format.toLowerCase()) {
            case "amr":
                return 8000;  // AMR 格式通常使用 8kHz
            case "pcm":
                return 16000; // PCM 格式推荐 16kHz
            case "wav":
            case "flac":
            case "mp3":
            case "m4a":
            default:
                return 16000; // 大多数格式默认使用 16kHz
        }
    }
    
    /**
     * 获取详细的错误信息并提供智能解决方案
     */
    private String getDetailedErrorMessage(int errNo, String originalMsg) {
        switch (errNo) {
            case 3300:
                return "输入参数不正确，请检查音频格式、采样率等参数";
            case 3301:
                return "音频质量过差，请使用更高质量的音频";
            case 3302:
                return "鉴权失败，请检查 API Key 和 Secret Key";
            case 3303:
                return "语音识别服务未开通或者试用期已结束";
            case 3307:
                return "语音太短，请使用更长的音频（建议大于 1.5 秒）";
            case 3308:
                return "音频时长超过限制（最长 60 秒）";
            case 3309:
                return "音频文件过大（最大 10MB）";
            case 3310:
                return "音频格式不支持，请使用 wav、mp3、amr、m4a 等支持的格式";
            case 3311:
                return "采样率不支持，请使用 8000 或 16000 Hz";
            case 3312:
                return "音频道数不正确。\n\n🛠️ 智能解决方案：\n" +
                       "您可以尝试使用我们的自动转换接口：\n" +
                       "curl -X POST http://localhost:8080/debug/asr/smart-convert -F \"audio=@您的文件名\"\n\n" +
                       "或者使用以下命令手动转换：\n" +
                       "ffmpeg -i 您的文件名 -ar 16000 -ac 1 转换后的文件名.wav";
            case 5000:
                return "服务器内部错误，请稍后重试";
            default:
                return originalMsg + " (错误代码: " + errNo + ")";
        }
    }
    
    /**
     * 打印调试信息，帮助定位问题
     */
    public String debugBaiduASR(MultipartFile file) {
        try {
            StringBuilder debug = new StringBuilder();
            debug.append("=== 百度 ASR 调试信息 ===\n");
            
            // 文件信息
            debug.append("1. 文件信息:\n");
            debug.append("   - 文件名: ").append(file.getOriginalFilename()).append("\n");
            debug.append("   - 文件大小: ").append(file.getSize()).append(" bytes\n");
            debug.append("   - 内容类型: ").append(file.getContentType()).append("\n");
            
            // 格式和参数
            String format = getAudioFormat(file.getOriginalFilename());
            int sampleRate = getSampleRateByFormat(format);
            debug.append("2. 解析参数:\n");
            debug.append("   - 格式: ").append(format).append("\n");
            debug.append("   - 采样率: ").append(sampleRate).append(" Hz\n");
            
            // Token 获取
            debug.append("3. Token 获取:\n");
            String accessToken = getBaiduAccessToken();
            if (accessToken != null) {
                debug.append("   - Token 获取成功: ").append(accessToken.substring(0, Math.min(20, accessToken.length()))).append("...\n");
            } else {
                debug.append("   - Token 获取失败\n");
                debug.append("   - 请检查 API Key: ").append(baiduApiKey != null ? "[***已配置***]" : "[未配置]").append("\n");
                debug.append("   - 请检查 Secret Key: ").append(baiduSecretKey != null ? "[***已配置***]" : "[未配置]").append("\n");
            }
            
            // 构建请求参数（不包含音频数据）
            debug.append("4. 请求参数:\n");
            JSONObject requestBody = new JSONObject();
            requestBody.put("format", format);
            requestBody.put("rate", sampleRate);
            requestBody.put("channel", 1);
            requestBody.put("cuid", "java-client-debug");
            requestBody.put("token", accessToken);
            requestBody.put("len", file.getSize());
            requestBody.put("dev_pid", 1537);
            
            debug.append("   - 参数结构: ").append(requestBody.toString()).append("\n");
            
            debug.append("=== 调试信息结束 ===\n");
            
            return debug.toString();
            
        } catch (Exception e) {
            return "调试信息获取失败: " + e.getMessage();
        }
    }
    
    /**
     * 增强的音频预处理方法，根据项目规范处理音频格式
     * 尝试使用 Java 内置功能自动转换，如果失败则提供明确指导
     */
    private File preprocessAudioForASR(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        String extension = filename.toLowerCase();
        
        // 根据项目规范，优先处理WAV格式
        if (extension.endsWith(".wav")) {
            return convertWavToMono(file);
        }
        
        // 对于MP3等其他格式，尝试直接使用（限制功能）
        if (extension.endsWith(".mp3") || extension.endsWith(".m4a") || extension.endsWith(".flac")) {
            // 先记录警告，但尝试继续处理
            System.out.println("警告：检测到 " + getFileExtension(filename) + " 格式文件，可能需要手动转换为单声道");
            // 尝试直接使用，让 API 处理
            return saveToTempFile(file);
        }
        
        // AMR格式通常已经是单声道
        if (extension.endsWith(".amr")) {
            return saveToTempFile(file);
        }
        
        // 默认情况，尝试直接使用
        return saveToTempFile(file);
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
    
    /**
     * 获取不含扩展名的文件名
     */
    private String getFileNameWithoutExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }
    
    /**
     * 将 WAV 文件转换为单声道
     */
    private File convertWavToMono(MultipartFile file) throws Exception {
        File tempInputFile = saveToTempFile(file);
        
        try {
            // 读取音频文件
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(tempInputFile);
            AudioFormat originalFormat = audioInputStream.getFormat();
            
            // 检查是否已经是单声道
            if (originalFormat.getChannels() == 1) {
                audioInputStream.close();
                return tempInputFile; // 已经是单声道，直接返回
            }
            
            // 创建单声道格式
            AudioFormat monoFormat = new AudioFormat(
                originalFormat.getEncoding(),
                originalFormat.getSampleRate(),
                originalFormat.getSampleSizeInBits(),
                1, // 单声道
                originalFormat.getFrameSize() / originalFormat.getChannels(),
                originalFormat.getFrameRate(),
                originalFormat.isBigEndian()
            );
            
            // 转换为单声道
            AudioInputStream monoStream = AudioSystem.getAudioInputStream(monoFormat, audioInputStream);
            
            // 创建输出文件
            File outputFile = new File(tempInputFile.getParent(), "mono_" + tempInputFile.getName());
            AudioSystem.write(monoStream, AudioFileFormat.Type.WAVE, outputFile);
            
            // 关闭流
            monoStream.close();
            audioInputStream.close();
            
            // 删除临时文件
            tempInputFile.delete();
            
            return outputFile;
            
        } catch (UnsupportedAudioFileException | IOException e) {
            // 如果转换失败，返回原文件
            System.err.println("音频转换失败，使用原文件: " + e.getMessage());
            return tempInputFile;
        }
    }

    /**
     * 通过李白的tts来转换文字为语音
     */
    public  JSONObject convertTextToSpeechByLiba(String text, Character character) {
        JSONObject result = unifiedttsUtils.textToSpeech(
                text,
                character.getVoiceModel(),
                character.getVoice(),
                1.0,
                1.0,
                ""

        );
        return result;

    }

}
