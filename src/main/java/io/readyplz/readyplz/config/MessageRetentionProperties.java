package io.readyplz.readyplz.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.messages")
@Validated
public class MessageRetentionProperties {

	/** 두 회원 간 1:1 대화당 허용되는 최대 메시지 수 */
	@Min(1)
	private int maxPerConversation = 1000;

	/** 한도 초과 시 한 번에 제거할 오래된 메시지 개수 */
	@Min(1)
	private int trimBatchSize = 10;

	public int getMaxPerConversation() {
		return maxPerConversation;
	}

	public void setMaxPerConversation(int maxPerConversation) {
		this.maxPerConversation = maxPerConversation;
	}

	public int getTrimBatchSize() {
		return trimBatchSize;
	}

	public void setTrimBatchSize(int trimBatchSize) {
		this.trimBatchSize = trimBatchSize;
	}
}
