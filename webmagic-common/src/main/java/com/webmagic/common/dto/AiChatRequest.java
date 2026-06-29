package com.webmagic.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAI 兼容 API — Chat Completion 请求体
 *
 * 参考: https://platform.openai.com/docs/api-reference/chat/create
 * 硅基流动兼容此格式: https://docs.siliconflow.cn/api-reference/chat-completions/chat-completions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiChatRequest {
    private String model;
    private List<AiChatMessage> messages;
    private Double temperature;
    private Integer maxTokens;
    /** 强制 JSON 输出（OpenAI 兼容模型支持） */
    private ResponseFormat responseFormat;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {
        private String type; // "json_object" or "text"
    }
}