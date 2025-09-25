package com.AIVoiceChat.ai.utils;

import com.aliyun.oss.OSS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class OssUtil {

    @Autowired
    private OSS ossClient;

    @Value("${asr.aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${asr.aliyun.oss.url-expire-time}")
    private Long expireTime;

    /**
     * 上传文件
     * @param file 文件对象
     * @param path 存储路径（如 "images/"）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String path) {
        try {
            // 生成唯一文件名
            String fileName = path + UUID.randomUUID() + "." +
                    StringUtils.getFilenameExtension(file.getOriginalFilename());

            ossClient.putObject(bucketName, fileName,
                    new ByteArrayInputStream(file.getBytes()));

            // 生成访问URL
            return generateUrl(fileName);
        } catch (IOException e) {
            log.error("文件上传失败：", e);
            throw new RuntimeException("文件上传失败");
        }
    }

    /**
     * 生成文件访问URL
     */
    private String generateUrl(String fileName) {
        Date expiration = new Date(System.currentTimeMillis() + expireTime * 1000);
        return ossClient.generatePresignedUrl(bucketName, fileName, expiration).toString();
    }

    /**
     * 删除文件
     */
    public void deleteFile(String fileUrl) {

        String fileName = extractFileName(fileUrl);
        ossClient.deleteObject(bucketName, fileName);
    }
    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new IllegalArgumentException("File URL cannot be empty.");
        }

        try {
            URI uri = new URI(fileUrl);
            String path = uri.getPath(); // 获取路径部分，如：/uploads/xxx.jpg

            // 去掉开头的斜杠（如果有的话）
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            return URLDecoder.decode(path, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid file URL: " + fileUrl, e);
        }
    }

    // 其他工具方法...
}
