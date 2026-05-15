package com.competition.config;

import com.baidu.aip.ocr.AipOcr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaiduOcrConfig {

    @Value("${baidu.ocr.app-id}")
    private String appId;

    @Value("${baidu.ocr.api-key}")
    private String apiKey;

    @Value("${baidu.ocr.secret-key}")
    private String secretKey;

    @Bean
    public AipOcr aipOcr() {
        AipOcr client = new AipOcr(appId, apiKey, secretKey);
        client.setConnectionTimeoutInMillis(5000);
        client.setSocketTimeoutInMillis(30000);
        return client;
    }
}
