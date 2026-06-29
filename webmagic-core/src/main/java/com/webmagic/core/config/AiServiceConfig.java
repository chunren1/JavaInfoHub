package com.webmagic.core.config;

import com.webmagic.common.service.AiService;
import com.webmagic.common.service.impl.OpenAiCompatibleAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI Service Bean 装配
 *
 * webmagic-common 不依赖 Spring，Service 实现在 common 中。
 * 此处通过 @Bean 将配置注入并暴露为 Spring Bean。
 */
@Slf4j
@Configuration
public class AiServiceConfig {

    @Autowired
    private AiProperties aiProperties;

    @Bean
    public AiService aiService() {
        AiProperties.Provider p = aiProperties.getProvider();
        AiProperties.RateLimit rl = aiProperties.getRateLimit();

        String apiKey = p.getApiKey();
        // 尝试从环境变量读取（优先于 yml 中的值）
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("AI_API_KEY");
            if (apiKey != null && !apiKey.isBlank()) {
                p.setApiKey(apiKey);
            }
        }

        log.info("Initializing AI service: base-url={}, model={}, enabled={}",
                p.getBaseUrl(), p.getModel(), aiProperties.isEnabled());

        return new OpenAiCompatibleAiService(
                p.getBaseUrl(),
                p.getApiKey(),
                p.getModel(),
                p.getTemperature(),
                p.getMaxTokens(),
                p.getTimeoutSeconds(),
                rl.getMaxRequestsPerCrawl(),
                rl.getDelayBetweenCallsMs()
        );
    }
}