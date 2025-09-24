package com.AIVoiceChat.ai.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Character {
    //角色性名
    private String name;
    //角色描述
    private String description;
    //角色图片url
    private String image;
    //角色语音模型
    private String voiceModel;
    //角色音色
    private String voice;
}
