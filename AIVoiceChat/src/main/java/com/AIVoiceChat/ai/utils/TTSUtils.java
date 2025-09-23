package com.AIVoiceChat.ai.utils;

import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 用于处理将语音转换为文字的工具类
 */
//TODO:工具类待完善
    @Component
public class TTSUtils {
    /**
     * 将语音转换为文字
     *
     * @param file 语音文件的路径
     * @return 转换后的文字
     */
    public static String convertSpeechToText(File file) {
        return "转换后的文字";
    }

    /**
     * 将文字转换为语音
     *
     * @param text 要转换的文字
     * @return 转换后的语音文件
     */
    public static File convertTextToSpeech(String text) {
        return new File("转换后的语音文件");
    }
}
