package Readyplz.io.ReadyPlz.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) { 
            return builder 
           
             // 기본 헤더 설정 (User-Agent는 그대로 두는 것이 좋습니다)
            .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36")

            // . 타임아웃 설정 (기존과 동일)
            .setConnectTimeout(Duration.ofSeconds(10)) 
            .setReadTimeout(Duration.ofSeconds(10)) 
            .build(); 
    }
}