package com.AIVoiceChat.ai.controller;


import com.AIVoiceChat.ai.entity.Result;
import com.AIVoiceChat.ai.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制类
 * @ClassName OssController
 * @Description OSS上传文件
 * @Author kczx
 * @Date 2023/5/5 17:05
 * @Version 1.0
 */
@CrossOrigin
@RestController
@RequestMapping("/oss")
public class OssController {

    @Autowired
    private OssUtil ossUtil;

    /**
     *上传文件
     * @param file 文件
     * @return 返回图片路径
     */
    @PostMapping("/upload")
    public Result uploadFile(@RequestParam("file") MultipartFile file) {
        String url = ossUtil.uploadFile(file, "uploads/");
        return Result.success( url);
    }
    /**
     *删除文件
     * @param fileUrl 文件路径
     * @return 删除成功或者失败
     */
    @DeleteMapping("/delete")
    public Result deleteFile(String fileUrl) {
        ossUtil.deleteFile(fileUrl);
        return Result.success();
    }
}
