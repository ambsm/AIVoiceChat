package com.AIVoiceChat.ai.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 音频格式转换工具类
 * 提供音频格式检测和转换建议
 */
@Component
public class AudioConversionHelper {

    /**
     * 检测音频文件的声道数（简单检测）
     * 注意：这是一个简化的检测方法，主要针对常见格式
     */
    public AudioInfo detectAudioInfo(MultipartFile file) {
        AudioInfo info = new AudioInfo();
        info.setFilename(file.getOriginalFilename());
        info.setFileSize(file.getSize());
        info.setFormat(getFormatFromFilename(file.getOriginalFilename()));
        
        try {
            // 对于 WAV 文件，可以读取头部信息
            if (info.getFormat().equalsIgnoreCase("wav")) {
                info = detectWavInfo(file, info);
            } else {
                // 对于其他格式，提供一般性建议
                info.setChannels(-1); // 无法确定
                info.setSampleRate(-1); // 无法确定
                info.setNeedsConversion(true);
                info.addRecommendation("建议转换为 WAV 格式以获得最佳兼容性");
            }
        } catch (Exception e) {
            info.addRecommendation("无法检测音频详细信息，建议使用标准格式");
        }
        
        return info;
    }
    
    /**
     * 检测 WAV 文件信息
     */
    private AudioInfo detectWavInfo(MultipartFile file, AudioInfo info) throws IOException {
        byte[] header = new byte[44];
        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.read(header);
            if (bytesRead >= 44) {
                // 检查 WAV 文件头
                if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F') {
                    // 读取声道数（字节 22-23）
                    int channels = (header[23] & 0xFF) << 8 | (header[22] & 0xFF);
                    // 读取采样率（字节 24-27）
                    int sampleRate = (header[27] & 0xFF) << 24 | (header[26] & 0xFF) << 16 | 
                                   (header[25] & 0xFF) << 8 | (header[24] & 0xFF);
                    
                    info.setChannels(channels);
                    info.setSampleRate(sampleRate);
                    
                    // 根据检测结果提供建议
                    if (channels > 1) {
                        info.setNeedsConversion(true);
                        info.addRecommendation("检测到多声道音频，需要转换为单声道");
                    }
                    if (sampleRate != 8000 && sampleRate != 16000) {
                        info.setNeedsConversion(true);
                        info.addRecommendation("建议采样率设置为 8000Hz 或 16000Hz");
                    }
                }
            }
        }
        return info;
    }
    
    /**
     * 生成音频转换命令
     */
    public List<String> generateConversionCommands(MultipartFile file) {
        List<String> commands = new ArrayList<>();
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return commands;
        }
        
        String nameWithoutExt = getFilenameWithoutExtension(filename);
        
        // FFmpeg 转换命令
        commands.add("# 使用 FFmpeg 转换为单声道 WAV (推荐)");
        commands.add(String.format("ffmpeg -i \"%s\" -ar 16000 -ac 1 \"%s_mono.wav\"", 
                                 filename, nameWithoutExt));
        
        commands.add("# 转换为单声道 MP3");
        commands.add(String.format("ffmpeg -i \"%s\" -ar 16000 -ac 1 \"%s_mono.mp3\"", 
                                 filename, nameWithoutExt));
        
        // Audacity 批处理建议
        commands.add("# 或使用 Audacity:");
        commands.add("# 1. 打开音频文件");
        commands.add("# 2. 选择 轨道 -> 立体声轨道转单声道");
        commands.add("# 3. 选择 轨道 -> 重新取样... -> 16000Hz");
        commands.add("# 4. 导出为 WAV 格式");
        
        return commands;
    }
    
    /**
     * 根据文件名获取格式
     */
    private String getFormatFromFilename(String filename) {
        if (filename == null) return "unknown";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "unknown";
    }
    
    /**
     * 获取不含扩展名的文件名
     */
    private String getFilenameWithoutExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }
    
    /**
     * 音频信息类
     */
    public static class AudioInfo {
        private String filename;
        private long fileSize;
        private String format;
        private int channels = -1;
        private int sampleRate = -1;
        private boolean needsConversion = false;
        private List<String> recommendations = new ArrayList<>();
        
        // Getters and Setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public int getChannels() { return channels; }
        public void setChannels(int channels) { this.channels = channels; }
        
        public int getSampleRate() { return sampleRate; }
        public void setSampleRate(int sampleRate) { this.sampleRate = sampleRate; }
        
        public boolean isNeedsConversion() { return needsConversion; }
        public void setNeedsConversion(boolean needsConversion) { this.needsConversion = needsConversion; }
        
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        public void addRecommendation(String recommendation) { this.recommendations.add(recommendation); }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("音频文件信息:\n");
            sb.append("  文件名: ").append(filename).append("\n");
            sb.append("  大小: ").append(fileSize).append(" bytes\n");
            sb.append("  格式: ").append(format.toUpperCase()).append("\n");
            if (channels > 0) {
                sb.append("  声道数: ").append(channels).append(channels == 1 ? " (单声道)" : " (多声道)").append("\n");
            }
            if (sampleRate > 0) {
                sb.append("  采样率: ").append(sampleRate).append(" Hz\n");
            }
            sb.append("  需要转换: ").append(needsConversion ? "是" : "否").append("\n");
            if (!recommendations.isEmpty()) {
                sb.append("  建议:\n");
                for (String rec : recommendations) {
                    sb.append("    - ").append(rec).append("\n");
                }
            }
            return sb.toString();
        }
    }
}