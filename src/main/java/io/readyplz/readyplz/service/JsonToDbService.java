package io.readyplz.readyplz.service;

import io.readyplz.readyplz.config.GameDataImportProperties;
import io.readyplz.readyplz.domain.Game;
import io.readyplz.readyplz.dto.SteamGameDTO;
import io.readyplz.readyplz.repository.GameRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonToDbService {

	private final GameRepository gameRepository;
	private final ObjectMapper objectMapper;
	private final ResourceLoader resourceLoader;
	private final GameDataImportProperties gameDataImportProperties;

	public void saveDataFromJsonFile() throws IOException {
		Resource resource = resourceLoader.getResource(gameDataImportProperties.getDataLocation());
		if (!resource.exists()) {
			throw new IllegalStateException(
					"게임 JSON 리소스를 찾을 수 없습니다. app.games.import.data-location 확인: "
							+ gameDataImportProperties.getDataLocation());
		}

		try (InputStream inputStream = resource.getInputStream()) {

			JsonNode rootNode = objectMapper.readTree(inputStream);

			List<SteamGameDTO> games;
			if (rootNode.isArray()) {
				games = objectMapper.convertValue(rootNode, new TypeReference<List<SteamGameDTO>>() {});
			} else {
				JsonNode appsNode = rootNode.path("applist").path("apps");
				if (appsNode.isMissingNode() || !appsNode.isArray()) {
					throw new IllegalStateException(
							"지원하지 않는 JSON 구조입니다. 배열 또는 {applist:{apps:[]}} 형식이어야 합니다.");
				}
				games = objectMapper.convertValue(appsNode, new TypeReference<List<SteamGameDTO>>() {});
			}

			if (games == null) {
				games = List.of();
			}

			int batchSize = gameDataImportProperties.getBatchSize();
			log.info("{}개의 게임 데이터를 JSON에서 읽었습니다. 배치 크기 {}로 DB 저장을 시작합니다.", games.size(),
					batchSize);

			int totalSavedCount = 0;
			int totalSkippedCount = 0;
			int totalInvalidCount = 0;
			int batchNumber = 0;

			for (int i = 0; i < games.size(); i += batchSize) {
				int endIndex = Math.min(i + batchSize, games.size());
				List<SteamGameDTO> batch = games.subList(i, endIndex);
				batchNumber++;

				try {
					int[] result = processBatch(batch);
					totalSavedCount += result[0];
					totalSkippedCount += result[1];
					totalInvalidCount += result[2];

					log.info("배치 {} 완료: 저장={}, 건너뜀={}, 유효하지 않음={}",
							batchNumber, result[0], result[1], result[2]);

				} catch (Exception e) {
					log.error("배치 {} 처리 중 오류 발생: {}", batchNumber, e.getMessage());
					continue;
				}
			}

			log.info("전체 DB 저장 완료. 총 저장={}, 총 건너뜀={}, 총 유효하지 않음={}",
					totalSavedCount, totalSkippedCount, totalInvalidCount);
		}
	}

	@Transactional
	protected int[] processBatch(List<SteamGameDTO> batch) {
		int savedCount = 0;
		int skippedCount = 0;
		int invalidCount = 0;

		List<SteamGameDTO> validDtos = batch.stream()
				.filter(dto -> dto.getAppId() != null && dto.getName() != null && !dto.getName().isEmpty())
				.collect(Collectors.toList());

		invalidCount = batch.size() - validDtos.size();

		if (validDtos.isEmpty()) {
			return new int[]{0, 0, invalidCount};
		}

		List<Integer> batchAppIds = validDtos.stream()
				.map(dto -> dto.getAppId().intValue())
				.collect(Collectors.toList());
		List<String> batchNames = validDtos.stream()
				.map(SteamGameDTO::getName)
				.collect(Collectors.toList());

		List<Integer> existingAppIds = gameRepository.findExistingAppids(batchAppIds);
		Set<Integer> existingAppIdSet = existingAppIds.stream().collect(Collectors.toSet());

		List<String> existingNames = gameRepository.findExistingNames(batchNames);
		Set<String> existingNameSet = existingNames.stream().collect(Collectors.toSet());

		Set<Integer> seenAppIds = new HashSet<>(existingAppIdSet);
		Set<String> seenNames = new HashSet<>(existingNameSet);

		for (SteamGameDTO dto : validDtos) {
			Integer appId = dto.getAppId().intValue();
			String name = dto.getName();
			if (!seenAppIds.contains(appId) && !seenNames.contains(name)) {
				Game game = dto.toEntity();
				gameRepository.save(Objects.requireNonNull(game));
				savedCount++;
				log.debug("게임 저장됨: AppID={}, 이름={}", dto.getAppId(), name);
				seenAppIds.add(appId);
				seenNames.add(name);
			} else {
				skippedCount++;
				log.debug("게임 중복으로 건너뜀: AppID={}, 이름={}", dto.getAppId(), name);
			}
		}

		return new int[]{savedCount, skippedCount, invalidCount};
	}
}
