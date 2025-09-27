/*
 Navicat Premium Dump SQL

 Source Server         : 主机
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : mou

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 27/09/2025 22:38:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for character
-- ----------------------------
DROP TABLE IF EXISTS `character`;
CREATE TABLE `character`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '姓名',
  `description` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '角色描述',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '图片url',
  `promt` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '角色提示词',
  `voice_model` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '音色\r\n声音模型',
  `voice` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '音色',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for chat_session
-- ----------------------------
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session`  (
  `chat_id` int NOT NULL AUTO_INCREMENT,
  `character_id` int NULL DEFAULT NULL COMMENT '和人物的外键绑定',
  `creat_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `chat_name` varchar(25) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT 'session名',
  PRIMARY KEY (`chat_id`, `chat_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
