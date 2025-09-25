package com.AIVoiceChat.ai.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.AllArgsConstructor;
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
@TableName("chat_session")
@AllArgsConstructor
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "chatId", type = IdType.AUTO)
    private Integer chatId;

    private String chatName;

    private Integer characterId;

    private LocalDateTime creatTime;


}
