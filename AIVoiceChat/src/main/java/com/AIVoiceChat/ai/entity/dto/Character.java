package com.AIVoiceChat.ai.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author 马卓航
 * @since 2025-09-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("`character`") // 👈 用反引号包裹
public class Character implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 姓名
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 图片url
     */
    private String image;

    /**
     * 角色提示词
     */
    private String promt;

    /**
     * 声音模型
     */
    private String voiceModel;

    /**
     * 音色
     */
    private String voice;


}
