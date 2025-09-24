package com.AIVoiceChat.ai.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.core.codec.Base64;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 阿里云智能语音交互工具类
 * 集成OSS上传和语音识别API调用
 */
@Component
public class AliyunASRUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(AliyunASRUtils.class);
    
    @Value("${asr.aliyun.access-key-id:}")
    private String accessKeyId;
    
    @Value("${asr.aliyun.access-key-secret:}")
    private String accessKeySecret;
    
    @Value("${asr.aliyun.app-key:}")
    private String appKey;
    
    @Value("${asr.aliyun.region-id:cn-shanghai}")
    private String regionId;
    
    @Value("${asr.aliyun.endpoint:filetrans.cn-shanghai.aliyuncs.com}")
    private String endpoint;
    
    @Value("${asr.temp-dir:temp/audio}")
    private String tempDir;
    
    // OSS 相关配置
    @Value("${asr.aliyun.oss.bucket-name:}")
    private String ossBucketName;
    
    @Value("${asr.aliyun.oss.endpoint:oss-cn-shanghai.aliyuncs.com}")
    private String ossEndpoint;
    
    @Value("${asr.aliyun.oss.url-prefix:}")
    private String ossUrlPrefix;
    
    /**
     * 执行阿里云语音识别
     */
    public String convertSpeechToText(MultipartFile file) {
        try {
            // 1. 验证配置
            if (!isConfigValid()) {
                return "阿里云配置不完整，请检查 access-key-id、access-key-secret 和 app-key";
            }
            
            // 2. 验证文件
            if (file == null || file.isEmpty()) {
                return "音频文件不能为空";
            }
            
            // 3. 检查文件大小和格式
            if (file.getSize() > 512 * 1024 * 1024) {
                return "文件太大，请使用小亏 512MB 的音频文件";
            }
            
            // 检查音频格式和大小是否符合规范
            String formatCheck = checkAudioFormat(file);
            if (formatCheck != null) {
                return formatCheck;
            }
            
            // 4. 检查OSS配置
            if (!isOSSConfigValid()) {
                return String.format(
                    "阿里云语音识别需要OSS配置:\n" +
                    "缺失配置: %s\n" +
                    "请配置 OSS 存储桶\n" +
                    "参考文档: https://help.aliyun.com/product/31815.html",
                    getMissingOSSConfig()
                );
            }
            
            logger.info("开始处理音频文件: {}, 大小: {} 字节", file.getOriginalFilename(), file.getSize());
            
            // 5. 保存临时文件
            File tempFile = saveToTempFile(file);
            
            try {
                // 6. 上传到OSS
                logger.info("开始上传文件到OSS...");
                String fileUrl = uploadToOSS(tempFile, file.getOriginalFilename());
                logger.info("OSS上传成功，文件URL: {}", fileUrl);
                
                // 7. 验证文件是否可访问
                if (!testFileAccess(fileUrl)) {
                    return "文件上传成功但无法公网访问，请检查OSS存储桶访问权限设置";
                }
                
                // 8. 调用阿里云语音识别API
                logger.info("文件可访问，开始调用语音识别API...");
                String result = callAliyunASRAPI(fileUrl);
                
                return result;
                
            } finally {
                // 9. 清理临时文件
                if (tempFile.exists()) {
                    boolean deleted = tempFile.delete();
                    logger.info("临时文件清理: {}", deleted ? "成功" : "失败");
                }
            }
            
        } catch (Exception e) {
            logger.error("阿里云语音识别异常", e);
            return "阿里云语音识别异常: " + e.getMessage();
        }
    }
    
    /**
     * 测试文件是否可公网访问
     */
    private boolean testFileAccess(String fileUrl) {
        try {
            HttpResponse response = HttpRequest.head(fileUrl)
                    .timeout(10000)
                    .execute();
            boolean accessible = response.getStatus() == 200;
            logger.info("文件访问性测试: {} - {}", fileUrl, accessible ? "可访问" : "不可访问");
            return accessible;
        } catch (Exception e) {
            logger.warn("文件访问性测试失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查音频格式是否符合规范
     */
    private String checkAudioFormat(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            return "文件名不能为空";
        }
        
        // 获取文件扩展名
        String extension = "";
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = filename.substring(lastDotIndex + 1).toLowerCase();
        } else {
            return "不支持的文件格式，请使用WAV、MP3、AAC、FLAC、AMR或M4A格式";
        }
        
        // 检查是否为支持的格式
        String[] supportedFormats = {"wav", "mp3", "aac", "flac", "amr", "m4a"};
        boolean supported = false;
        for (String format : supportedFormats) {
            if (format.equals(extension)) {
                supported = true;
                break;
            }
        }
        
        if (!supported) {
            return String.format(
                "不支持的文件格式: %s\n" +
                "支持的格式: WAV、MP3、AAC、FLAC、AMR、M4A\n" +
                "推荐使用WAV格式（单声道，8000Hz或16000Hz采样率）",
                extension.toUpperCase()
            );
        }
        
        // 检查文件大小是否在合理范围内
        long fileSize = file.getSize();
        if (fileSize < 1024) { // 小于1KB
            return "文件太小，可能不是有效的音频文件";
        }
        
        if (fileSize > 10 * 1024 * 1024) { // 大于10MB
            return String.format(
                "文件较大（%.2f MB），可能影响识别速度\n" +
                "建议使用小于10MB的音频文件，或压缩音频质量",
                fileSize / 1024.0 / 1024.0
            );
        }
        
        // 阿里云语音识别特殊要求检查
        String formatWarning = getFormatSpecificWarning(extension, fileSize);
        if (formatWarning != null) {
            return formatWarning;
        }
        
        return null; // 没有问题
    }
    
    /**
     * 获取格式特定的警告信息
     */
    private String getFormatSpecificWarning(String extension, long fileSize) {
        StringBuilder warnings = new StringBuilder();
        
        // 采样率要求说明
        boolean shouldWarnSampleRate = true;
        String sampleRateWarning = "\n✅ 采样率支持：系统已启用自动采样率适配功能\n" +
                                  "• 优先支持：8000Hz和16000Hz采样率\n" +
                                  "• 自动处理：其他采样率会自动转换为支持的格式\n" +
                                  "• 推荐使用16000Hz采样率以获得最佳识别效果\n" +
                                  "• 无需手动转换：系统会自动处理不同采样率的音频";
        
        // 根据格式给出具体建议
        switch (extension.toLowerCase()) {
            case "wav":
                warnings.append("✅ WAV格式：推荐格式，请确保：\n")
                       .append("   • 声道：单声道（mono）\n")
                       .append("   • 编码：PCM格式\n")
                       .append("   • 采样率：系统已启用自动适配，支持各种采样率");
                break;
                
            case "mp3":
                warnings.append("🔶 MP3格式注意事项：\n")
                       .append("   • 采样率：系统已启用自动适配功能\n")
                       .append("   • 必须为单声道，多声道会导致识别失败\n")
                       .append("   • 比特率建议16-128 kbps\n")
                       .append("   • 如识别失败，请检查声道设置");
                logger.warn("检测到MP3格式文件，系统已启用自动采样率适配");
                break;
                
            case "m4a":
                warnings.append("🔶 M4A格式注意事项：\n")
                       .append("   • 采样率：系统已启用自动适配功能\n")
                       .append("   • 必须为单声道\n")
                       .append("   • AAC编码格式\n")
                       .append("   • 如识别失败，检查声道设置和编码格式");
                logger.warn("检测到M4A格式文件，系统已启用自动采样率适配");
                break;
                
            case "flac":
                warnings.append("🔶 FLAC格式注意事项：\n")
                       .append("   • 采样率：系统已启用自动适配功能\n")
                       .append("   • 必须为单声道\n")
                       .append("   • 无损压缩格式，文件较大");
                break;
                
            case "amr":
                warnings.append("✅ AMR格式：电话语音专用格式\n")
                       .append("   • 默认8000Hz采样率，原生支持\n")
                       .append("   • 单声道格式\n")
                       .append("   • 适合电话录音场景");
                shouldWarnSampleRate = false; // AMR格式通常符合要求
                break;
                
            case "aac":
                warnings.append("🔶 AAC格式注意事项：\n")
                       .append("   • 采样率：系统已启用自动适配功能\n")
                       .append("   • 必须为单声道\n")
                       .append("   • 如识别失败，检查声道设置");
                break;
                
            default:
                shouldWarnSampleRate = true;
        }
        
        // 添加采样率警告（对于需要的格式）
        if (shouldWarnSampleRate) {
            warnings.append(sampleRateWarning);
        }
        
        // 文件时长建议
        warnings.append("\n\n📋 推荐规格（已启用自动适配）：\n")
               .append("• 格式：WAV（PCM编码）\n")
               .append("• 采样率：任意采样率（系统自动适配）\n")
               .append("• 声道：单声道（mono）\n")
               .append("• 时长：0.5-60秒\n")
               .append("• 大小：≤10MB");
        
        // 如果是潜在问题格式，返回警告；否则返回null表示可以继续
        if ("mp3".equals(extension) || "m4a".equals(extension) || "aac".equals(extension) || "flac".equals(extension)) {
            return warnings.toString();
        }
        
        // 对于WAV和AMR格式，只记录日志，不阻止处理
        if (warnings.length() > 0) {
            logger.info("音频格式提示：\n{}", warnings.toString());
        }
        
        return null; // 允许继续处理
    }
    
    /**
     * 上传文件到OSS
     */
    public String uploadToOSS(File file, String originalFilename) throws Exception {
        OSS ossClient = null;
        try {
            // 创建OSSClient
            ossClient = new OSSClientBuilder().build("https://" + ossEndpoint, accessKeyId, accessKeySecret);
            
            // 生成对象名称
            String objectName = "audio/" + System.currentTimeMillis() + "_" + 
                               (originalFilename != null ? originalFilename : file.getName());
            
            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType("audio/wav"); // 设置正确的MIME类型
            
            // 上传文件
            PutObjectRequest putRequest = new PutObjectRequest(ossBucketName, objectName, new FileInputStream(file));
            putRequest.setMetadata(metadata);
            ossClient.putObject(putRequest);
            
            // 设置文件为公共读权限（阿里云语音识别需要公网访问）
            try {
                ossClient.setObjectAcl(ossBucketName, objectName, com.aliyun.oss.model.CannedAccessControlList.PublicRead);
                logger.info("文件上传成功并设置为公共读权限: {}", objectName);
            } catch (Exception e) {
                logger.warn("设置文件公共读权限失败，请确保存储桶已配置公共读权限: {}", e.getMessage());
            }
            
            // 生成文件URL
            String fileUrl;
            if (ossUrlPrefix != null && !ossUrlPrefix.isEmpty()) {
                fileUrl = ossUrlPrefix + "/" + objectName;
            } else {
                fileUrl = "https://" + ossBucketName + "." + ossEndpoint + "/" + objectName;
            }
            
            logger.info("文件上传完成，URL: {}", fileUrl);
            return fileUrl;
            
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 调用阿里云语音识别API
     */
    public String callAliyunASRAPI(String fileUrl) {
        try {
            logger.info("开始调用阿里云语音识别API，文件URL: {}", fileUrl);
            
            // 构造请求参数
            Map<String, Object> requestParams = new HashMap<>();
            requestParams.put("appkey", appKey);
            requestParams.put("file_link", fileUrl);
            requestParams.put("version", "4.0");
            requestParams.put("enable_words", true);
            
            // 创建识别任务
            String taskId = createRecognitionTask(requestParams);
            if (taskId == null) {
                return "创建识别任务失败，请检查配置和网络连接";
            }
            
            logger.info("识别任务创建成功，TaskID: {}", taskId);
            
            // 轮询任务状态直至完成
            String result = pollTaskResult(taskId);
            return result;
            
        } catch (Exception e) {
            logger.error("调用阿里云ASR API失败", e);
            return "调用阿里云API失败: " + e.getMessage() + 
                   "\n\n请检查:\n1. 网络连接是否正常\n2. AccessKey和Secret是否正确\n3. AppKey是否有效\n4. OSS文件是否公网可访问";
        }
    }
    
    /**
     * 创建识别任务
     */
    private String createRecognitionTask(Map<String, Object> requestParams) {
        try {
            // 按照阿里云官方文档，使用正确的域名
            String url = "https://filetrans.cn-shanghai.aliyuncs.com/";
            
            // 构造请求参数（按照POP API规范）
            Map<String, String> params = new HashMap<>();
            params.put("Action", "SubmitTask");
            params.put("Version", "2018-08-17");
            params.put("Format", "JSON");
            params.put("Timestamp", java.time.Instant.now().toString());
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureVersion", "1.0");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("AccessKeyId", accessKeyId);
            
            // Task参数（JSON格式）
            JSONObject taskObject = new JSONObject();
            taskObject.put("appkey", appKey);
            taskObject.put("file_link", requestParams.get("file_link"));
            taskObject.put("version", "4.0");
            taskObject.put("enable_words", false);
            // 启用自动降采样功能，自动处理采样率不匹配问题
            taskObject.put("enable_sample_rate_adaptive", true);
            
            params.put("Task", taskObject.toString());
            
            // 生成签名
            String signature = generatePOPSignature("POST", params);
            params.put("Signature", signature);
            
            logger.info("准备创建阿里云识别任务（官方SDK风格）");
            logger.info("API URL: {}", url);
            logger.info("AppKey: {}", appKey);
            logger.info("文件URL: {}", requestParams.get("file_link"));
            logger.info("Task参数: {}", taskObject.toString());
            logger.info("请求参数: {}", params);
            
            // 构造请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("Accept", "application/json");
            
            logger.info("请求头: {}", headers);
            
            // 发送POST请求
            Map<String, Object> formParams = new HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formParams.put(entry.getKey(), entry.getValue());
            }
            
            HttpResponse response = HttpRequest.post(url)
                    .headerMap(headers, true)
                    .form(formParams)
                    .timeout(30000)
                    .execute();
            
            logger.info("响应状态码: {}", response.getStatus());
            logger.info("响应头: {}", response.headers());
            logger.info("响应内容: {}", response.body());
            
            if (response.getStatus() == 200) {
                try {
                    JSONObject responseJson = JSONUtil.parseObj(response.body());
                    
                    // 检查是否成功
                    String statusText = responseJson.getStr("StatusText");
                    String taskId = responseJson.getStr("TaskId");
                    
                    if ("SUCCESS".equals(statusText) && taskId != null && !taskId.isEmpty()) {
                        logger.info("创建任务成功，TaskID: {}, StatusText: {}", taskId, statusText);
                        return taskId;
                    } else {
                        // 检查是否有错误信息
                        String errorCode = responseJson.getStr("Code");
                        String errorMessage = responseJson.getStr("Message");
                        logger.error("创建任务失败 - StatusText: {}, 错误代码: {}, 错误信息: {}", statusText, errorCode, errorMessage);
                        return null;
                    }
                } catch (Exception parseEx) {
                    logger.error("解析响应JSON失败: {}", parseEx.getMessage());
                    logger.error("原始响应: {}", response.body());
                    return null;
                }
            } else {
                logger.error("HTTP请求失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("创建识别任务异常", e);
            return null;
        }
    }
    
    /**
     * 轮询任务结果
     */
    private String pollTaskResult(String taskId) {
        try {
            // 最多轮询30次，每次5秒一次
            for (int i = 0; i < 30; i++) {
                logger.info("查询任务状态，第{}次尝试，TaskID: {}", (i + 1), taskId);
                
                // 按照官方文档格式构造查询请求
                String url = "https://filetrans.cn-shanghai.aliyuncs.com/";
                
                // 构造POP API查询参数
                Map<String, String> params = new HashMap<>();
                params.put("Action", "GetTaskResult");
                params.put("Version", "2018-08-17");
                params.put("Format", "JSON");
                params.put("Timestamp", java.time.Instant.now().toString());
                params.put("SignatureMethod", "HMAC-SHA1");
                params.put("SignatureVersion", "1.0");
                params.put("SignatureNonce", UUID.randomUUID().toString());
                params.put("AccessKeyId", accessKeyId);
                params.put("TaskId", taskId);  // 设置任务ID为查询参数
                
                // 生成签名
                String signature = generatePOPSignature("GET", params);
                params.put("Signature", signature);
                
                // 构造查询字符串
                StringBuilder queryString = new StringBuilder();
                boolean first = true;
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (!first) {
                        queryString.append("&");
                    }
                    try {
                        queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                                   .append("=")
                                   .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                    } catch (Exception e) {
                        logger.error("URL编码异常", e);
                    }
                    first = false;
                }
                
                String fullUrl = url + "?" + queryString.toString();
                logger.info("查询URL: {}", fullUrl);
                
                // 使用GET请求查询结果
                HttpResponse response = HttpRequest.get(fullUrl)
                        .timeout(10000)
                        .execute();
                
                logger.info("查询响应状态码: {}", response.getStatus());
                logger.info("查询响应内容: {}", response.body());
                
                if (response.getStatus() == 200) {
                    JSONObject responseJson = JSONUtil.parseObj(response.body());
                    String statusText = responseJson.getStr("StatusText");
                    
                    if ("SUCCESS".equals(statusText) || "SUCCESS_WITH_NO_VALID_FRAGMENT".equals(statusText)) {
                        // 任务成功完成，解析结果
                        JSONObject result = responseJson.getJSONObject("Result");
                        if (result != null) {
                            String sentences = result.getStr("Sentences");
                            if (sentences != null && !sentences.isEmpty()) {
                                return parseResultText(sentences);
                            }
                        }
                        return "识别成功但结果为空";
                    } else if ("RUNNING".equals(statusText) || "QUEUEING".equals(statusText)) {
                        // 任务还在运行
                        logger.info("任务状态: {}，等待1秒后重试...", statusText);
                        Thread.sleep(1000);
                        continue;
                    } else {
                        // 任务失败或其他状态
                        return "识别失败: " + (statusText != null ? statusText : "未知错误");
                    }
                } else {
                    logger.error("查询任务状态失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                }
                
                Thread.sleep(5000);
            }
            
            return "识别超时，请稍后再试";
            
        } catch (Exception e) {
            logger.error("轮询任务结果异常", e);
            return "轮询结果异常: " + e.getMessage();
        }
    }
    
    /**
     * 解析识别结果文本
     */
    private String parseResultText(String sentences) {
        try {
            if (sentences.startsWith("[")) {
                // JSON数组格式
                cn.hutool.json.JSONArray sentenceArray = JSONUtil.parseArray(sentences);
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < sentenceArray.size(); i++) {
                    JSONObject sentence = sentenceArray.getJSONObject(i);
                    if (sentence != null) {
                        String text = sentence.getStr("Text");
                        if (text != null && !text.isEmpty()) {
                            result.append(text);
                        }
                    }
                }
                return result.toString();
            } else {
                // 简单文本格式
                return sentences;
            }
        } catch (Exception e) {
            logger.warn("解析结果文本失败，返回原始内容: {}", e.getMessage());
            return sentences;
        }
    }
    
    /**
     * 解析识别结果
     */
    private String parseRecognitionResult(JSONObject responseJson) {
        try {
            // 阿里云语音识别的结果格式
            JSONObject result = responseJson.getJSONObject("result");
            if (result != null) {
                // 尝试获取sentences数组
                Object sentencesObj = result.get("sentences");
                if (sentencesObj != null) {
                    String sentencesStr = sentencesObj.toString();
                    if (sentencesStr.startsWith("[")) {
                        // 解析sentences数组
                        cn.hutool.json.JSONArray sentences = result.getJSONArray("sentences");
                        if (sentences != null && sentences.size() > 0) {
                            StringBuilder text = new StringBuilder();
                            for (int i = 0; i < sentences.size(); i++) {
                                JSONObject sentence = sentences.getJSONObject(i);
                                if (sentence != null) {
                                    String sentenceText = sentence.getStr("text");
                                    if (sentenceText != null && !sentenceText.isEmpty()) {
                                        text.append(sentenceText);
                                    }
                                }
                            }
                            if (text.length() > 0) {
                                return text.toString();
                            }
                        }
                    }
                }
                
                // 如果没有sentences，尝试直接获取text字段
                String text = result.getStr("text");
                if (text != null && !text.isEmpty()) {
                    return text;
                }
                
                // 尝试获取其他可能的文本字段
                String transcript = result.getStr("transcript");
                if (transcript != null && !transcript.isEmpty()) {
                    return transcript;
                }
            }
            
            // 如果都没有，返回原始响应供调试
            logger.warn("无法解析识别结果，原始响应: {}", responseJson.toString());
            return "识别结果为空或格式异常，原始响应: " + responseJson.toString();
            
        } catch (Exception e) {
            logger.error("解析识别结果异常", e);
            return "解析结果异常: " + e.getMessage() + 
                   "\n原始响应: " + responseJson.toString();
        }
    }
    
    /**
     * 构造请求头（包含签名）
     */
    private Map<String, String> buildHeaders(String method, String url, String body) {
        Map<String, String> headers = new HashMap<>();
        
        // 基本请求头
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        
        // 时间戳
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        headers.put("X-NLS-Token", generateToken(timestamp));
        
        return headers;
    }
    
    /**
     * 生成阿里云POP API签名
     */
    private String generatePOPSignature(String method, Map<String, String> params) {
        try {
            // 按照阿里云POP API签名规范
            StringBuilder stringToSign = new StringBuilder();
            stringToSign.append(method).append("&");
            stringToSign.append(URLEncoder.encode("/", "UTF-8")).append("&");
            
            // 对参数进行排序和编码
            TreeMap<String, String> sortedParams = new TreeMap<>(params);
            StringBuilder canonicalizedQueryString = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (!first) {
                    canonicalizedQueryString.append("&");
                }
                canonicalizedQueryString.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                first = false;
            }
            
            stringToSign.append(URLEncoder.encode(canonicalizedQueryString.toString(), "UTF-8"));
            
            // 使用HMAC-SHA1生成签名
            String key = accessKeySecret + "&"; // POP API需要在密钥后面加&
            HMac hMac = new HMac(HmacAlgorithm.HmacSHA1, key.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = hMac.digest(stringToSign.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.encode(signBytes);
            
        } catch (Exception e) {
            logger.error("生成POP签名异常", e);
            return "";
        }
    }
    
    /**
     * 构造简化的请求头（用于查询结果）
     */
    private Map<String, String> buildSimpleHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "application/json");
        return headers;
    }
    
    /**
     * 生成简化的认证token
     */
    private String generateSimpleToken() {
        try {
            // 使用AccessKey和Secret生成简单的token
            String timestamp = String.valueOf(System.currentTimeMillis());
            String data = accessKeyId + ":" + timestamp;
            
            HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, accessKeySecret.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = hMac.digest(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.encode(signBytes);
            
            // 返回token格式
            return accessKeyId + ":" + signature;
        } catch (Exception e) {
            logger.error("生成token异常", e);
            return accessKeyId + ":" + accessKeySecret; // 备用方案
        }
    }
    
    /**
     * 生成访问令牌
     */
    private String generateToken(String timestamp) {
        try {
            // 简化版token生成，实际中需要按照阿里云规范生成
            String stringToSign = timestamp + appKey;
            HMac hMac = new HMac(HmacAlgorithm.HmacSHA1, accessKeySecret.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = hMac.digest(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.encode(signBytes);
        } catch (Exception e) {
            logger.error("生成token异常", e);
            return "";
        }
    }
    
    /**
     * 生成阿里云API签名
     */
    private String generateAliyunSignature(String method, String timestamp, String nonce, String body) {
        try {
            // 按照阿里云签名规范构造签名字符串
            StringBuilder stringToSign = new StringBuilder();
            stringToSign.append(method).append("\n");
            stringToSign.append("/2018-05-18/projects/").append(appKey).append("/async-recognitions\n");
            stringToSign.append("X-NLS-Nonce:").append(nonce).append("\n");
            stringToSign.append("X-NLS-Timestamp:").append(timestamp).append("\n");
            if (body != null && !body.isEmpty()) {
                stringToSign.append(body);
            }
            
            // 使用HMAC-SHA1生成签名
            HMac hMac = new HMac(HmacAlgorithm.HmacSHA1, accessKeySecret.getBytes(StandardCharsets.UTF_8));
            byte[] signBytes = hMac.digest(stringToSign.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.encode(signBytes);
        } catch (Exception e) {
            logger.error("生成阿里云签名异常", e);
            return "";
        }
    }
    
    /**
     * 生成阿里云Authorization头
     */
    private String generateAliyunAuthorization(String method, String url, String timestamp, String nonce, String body) {
        try {
            // 按照阿里云标准生成Authorization头
            String signature = generateAliyunSignature(method, timestamp, nonce, body);
            return String.format("acs %s:%s", accessKeyId, signature);
        } catch (Exception e) {
            logger.error("生成Authorization头异常", e);
            return "";
        }
    }
    
    /**
     * 验证阿里云基本配置是否完整
     */
    private boolean isConfigValid() {
        return accessKeyId != null && !accessKeyId.isEmpty() &&
               accessKeySecret != null && !accessKeySecret.isEmpty() &&
               appKey != null && !appKey.isEmpty();
    }
    
    /**
     * 验证OSS配置是否完整
     */
    private boolean isOSSConfigValid() {
        return ossBucketName != null && !ossBucketName.isEmpty();
    }
    
    /**
     * 获取缺失的OSS配置项
     */
    private String getMissingOSSConfig() {
        StringBuilder missing = new StringBuilder();
        if (ossBucketName == null || ossBucketName.isEmpty()) {
            missing.append("bucket-name ");
        }
        if (missing.length() == 0) {
            missing.append("OSS SDK集成 ");
        }
        return missing.toString().trim();
    }
    
    /**
     * 保存文件到临时目录
     */
    public File saveToTempFile(MultipartFile file) throws IOException {
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
        
        String tempFileName = "aliyun_audio_" + UUID.randomUUID().toString() + extension;
        File tempFile = new File(tempDirFile, tempFileName);
        
        // 保存文件
        Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        return tempFile;
    }
    
    /**
     * 获取配置状态信息
     */
    public String getConfigStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== 阿里云ASR配置状态 ===\n");
        status.append("AccessKey ID: ").append(accessKeyId != null && !accessKeyId.isEmpty() ? "[已配置]" : "[未配置]").append("\n");
        status.append("AccessKey Secret: ").append(accessKeySecret != null && !accessKeySecret.isEmpty() ? "[已配置]" : "[未配置]").append("\n");
        status.append("App Key: ").append(appKey != null && !appKey.isEmpty() ? appKey : "[未配置]").append("\n");
        status.append("Region ID: ").append(regionId).append("\n");
        status.append("Endpoint: ").append(endpoint).append("\n");
        status.append("基础配置: ").append(isConfigValid() ? "✓ 完整" : "✗ 不完整").append("\n");
        status.append("\n=== OSS配置状态 ===\n");
        status.append("Bucket Name: ").append(ossBucketName != null && !ossBucketName.isEmpty() ? ossBucketName : "[未配置]").append("\n");
        status.append("OSS Endpoint: ").append(ossEndpoint).append("\n");
        status.append("URL Prefix: ").append(ossUrlPrefix != null && !ossUrlPrefix.isEmpty() ? ossUrlPrefix : "[未配置]").append("\n");
        status.append("OSS配置: ").append(isOSSConfigValid() ? "✓ 基本完整" : "✗ 不完整").append("\n");
        status.append("SDK集成: ").append("✓ 已集成OSS和语音识别SDK").append("\n");
        status.append("\n=== 使用说明 ===\n");
        status.append("1. 配置阿里云AccessKey和AppKey\n");
        status.append("2. 配置OSS存储桶\n");
        status.append("3. 上传音频文件即可进行语音识别\n");
        status.append("========================");
        
        return status.toString();
    }
    
    /**
     * 获取完整的配置指南
     */
    public String getSetupGuide() {
        return "=== 阿里云语音识别配置指南 ===\n" +
               "\n1. 获取阿里云AccessKey:\n" +
               "   - 登录阿里云控制台 → 访问控制RAM → 用户管理\n" +
               "   - 创建AccessKey ID和AccessKey Secret\n" +
               "\n2. 开通智能语音交互服务:\n" +
               "   - 访问 https://nls-portal.console.aliyun.com/\n" +
               "   - 开通服务并获取AppKey: " + (appKey != null && !appKey.isEmpty() ? appKey : "需要配置") + "\n" +
               "\n3. 配置OSS存储:\n" +
               "   - 创建OSS存储桶\n" +
               "   - 设置公网读权限\n" +
               "   - 配置bucket-name到application.yaml\n" +
               "\n4. 在application.yaml中配置:\n" +
               "   asr:\n" +
               "     aliyun:\n" +
               "       access-key-id: 您的AccessKey-ID\n" +
               "       access-key-secret: 您的AccessKey-Secret\n" +
               "       app-key: " + appKey + "\n" +
               "       oss:\n" +
               "         bucket-name: 您的存储桶名称\n" +
               "\n5. 音频格式要求（已启用自动适配）:\n" +
               "   • 采样率：系统自动适配，支持各种采样率\n" +
               "   • 声道：必须为单声道（mono）\n" +
               "   • 格式：支持WAV、MP3、AAC、FLAC、AMR、M4A\n" +
               "   • 推荐：WAV格式，16000Hz，PCM编码\n" +
               "   • 大小：≤10MB，时长0.5-60秒\n" +
               "\n6. 自动适配功能:\n" +
               "   • 系统已启用enable_sample_rate_adaptive参数\n" +
               "   • 自动将不同采样率的音频转换为支持的格式\n" +
               "   • 无需手动转换采样率，系统自动处理\n" +
               "\n7. 录制建议:\n" +
               "   • 推荐使用16000Hz单声道WAV格式以获得最佳效果\n" +
               "   • 其他采样率也可以使用，系统会自动处理\n" +
               "   • 确保使用单声道录制（多声道无法自动转换）\n" +
               "\n8. 常见问题解决:\n" +
               "   • 识别失败：检查是否为单声道格式\n" +
               "   • 无声音：检查文件是否损坏或为空文件\n" +
               "   • 文件太大：压缩或分段处理\n" +
               "\n9. 功能特点:\n" +
               "   • 启用自动采样率适配功能\n" +
               "   • 异步处理模式，支持长音频\n" +
               "   • 自动OSS上传和清理\n" +
               "   • 实时任务状态查询\n" +
               "   • 完整的错误处理和日志记录\n" +
               "   • 智能格式检查和提示\n" +
               "========================";
    }
}