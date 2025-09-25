package com.AIVoiceChat.ai.controller;


import com.AIVoiceChat.ai.entity.Result;
import com.AIVoiceChat.ai.entity.dto.Character;
import com.AIVoiceChat.ai.service.impl.CharacterServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 马卓航
 * @since 2025-09-25
 */
@RestController
@RequestMapping("/character")
public class CharacterController {

    @Autowired
    private CharacterServiceImpl characterService;
    /**
     * 分页获取所有角色
     */
    @RequestMapping("/getPage")
    public Result getPage(int currentPage, int pageSize) {
        Page<Character> page = new Page<>(currentPage, pageSize);
        return Result.success(characterService.page(page));
    }
    /**
     * 创建角色
     */
    @RequestMapping("/create")
    public Result create(Character character) {
        return characterService.save(character) ? Result.success() : Result.error("创建角色失败");
    }
}
