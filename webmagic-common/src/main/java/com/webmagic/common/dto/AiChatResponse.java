package com.webmagic.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * OpenAI 兼容 API — Chat Completion 响应体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatResponse {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private int index;
        private AiChatMessage message;
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }

    /** 获取第一个 choice 的文本内容；无结果返回 null */
    public String getFirstContent() {
        if (choices == null || choices.isEmpty()) return null;
        AiChatMessage msg = choices.get(0).getMessage();
        return msg != null ? msg.getContent() : null;
    }
}