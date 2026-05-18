package io.readyplz.readyplz;

import io.readyplz.readyplz.config.CorsProperties;
import io.readyplz.readyplz.config.GameDataImportProperties;
import io.readyplz.readyplz.config.JwtProperties;
import io.readyplz.readyplz.config.MessageRetentionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ CorsProperties.class, JwtProperties.class, GameDataImportProperties.class,
		MessageRetentionProperties.class })
public class ReadyPlzApplication {

	public static void main(String[] args) {
	
		
		SpringApplication.run(ReadyPlzApplication.class, args);
	}

}
