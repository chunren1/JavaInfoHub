package com.webmagic.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI 兼容 API — 单条消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatMessage {
    private String role;
    private String content;

    public static AiChatMessage system(String content) {
        return new AiChatMessage("system", content);
    }

    public static AiChatMessage user(String content) {
        return new AiChatMessage("user", content);
    }
}