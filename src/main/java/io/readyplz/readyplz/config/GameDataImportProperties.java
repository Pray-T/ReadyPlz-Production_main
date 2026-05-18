package io.readyplz.readyplz.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.games.import")
@Validated
public class GameDataImportProperties {

	/**
	 * Steam 게임 덤프 JSON 위치. {@code classpath:...}, {@code file:...} 등 Spring 리소스 URI.
	 */
	@NotBlank
	private String dataLocation = "classpath:steam_games_data.json";

	@Min(1)
	private int batchSize = 1000;

	public String getDataLocation() {
		return dataLocation;
	}

	public void setDataLocation(String dataLocation) {
		this.dataLocation = dataLocation;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}
}
