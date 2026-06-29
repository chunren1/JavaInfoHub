package com.webmagic.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * AI 从 HTML 中提取的文章结构化数据
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractionResult {

    private String title;
    private String summary;
    private String author;
    private String contentUrl;
    private List<String> tags;
    private String publishTime;

    /** 验证必要字段是否有效 */
    public boolean isValid() {
        return title != null && !title.isBlank();
    }
}