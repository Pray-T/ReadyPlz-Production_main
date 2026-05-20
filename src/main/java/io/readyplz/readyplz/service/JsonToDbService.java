package io.readyplz.readyplz.service;

import io.readyplz.readyplz.config.GameDataImportProperties;
import io.readyplz.readyplz.dto.SteamGameDTO;
import io.readyplz.readyplz.exception.GameImportException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonToDbService {

    private final GameImportBatchService gameImportBatchService;
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

        int batchSize = gameDataImportProperties.getBatchSize();
        int totalSavedCount = 0;
        int totalSkippedCount = 0;
        int totalInvalidCount = 0;
        int batchNumber = 0;

        try (InputStream inputStream = resource.getInputStream()) {
            JsonFactory factory = objectMapper.getFactory();
            try (JsonParser parser = factory.createParser(inputStream)) {
                JsonToken firstToken = parser.nextToken();
                if (firstToken == JsonToken.START_ARRAY) {
                    int[] totals = importFromArrayParser(parser, batchSize);
                    totalSavedCount = totals[0];
                    totalSkippedCount = totals[1];
                    totalInvalidCount = totals[2];
                } else {
                    JsonNode rootNode = objectMapper.readTree(parser);
                    List<SteamGameDTO> games = extractGamesFromRoot(rootNode);
                    int[] totals = importFromList(games, batchSize);
                    totalSavedCount = totals[0];
                    totalSkippedCount = totals[1];
                    totalInvalidCount = totals[2];
                }
            }
        }

        log.info("전체 DB 저장 완료. 총 저장={}, 총 건너뜀={}, 총 유효하지 않음={}",
                totalSavedCount, totalSkippedCount, totalInvalidCount);
    }

    private int[] importFromList(List<SteamGameDTO> games, int batchSize) {
        if (games == null) {
            games = List.of();
        }
        log.info("{}개의 게임 데이터를 JSON에서 읽었습니다. 배치 크기 {}로 DB 저장을 시작합니다.", games.size(), batchSize);

        int totalSavedCount = 0;
        int totalSkippedCount = 0;
        int totalInvalidCount = 0;
        int batchNumber = 0;

        for (int i = 0; i < games.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, games.size());
            List<SteamGameDTO> batch = games.subList(i, endIndex);
            batchNumber++;
            int[] result = processBatchOrThrow(batch, batchNumber);
            totalSavedCount += result[0];
            totalSkippedCount += result[1];
            totalInvalidCount += result[2];
            log.info("배치 {} 완료: 저장={}, 건너뜀={}, 유효하지 않음={}",
                    batchNumber, result[0], result[1], result[2]);
        }
        return new int[]{totalSavedCount, totalSkippedCount, totalInvalidCount};
    }

    private int[] importFromArrayParser(JsonParser parser, int batchSize) throws IOException {
        List<SteamGameDTO> batch = new ArrayList<>(batchSize);
        int totalSavedCount = 0;
        int totalSkippedCount = 0;
        int totalInvalidCount = 0;
        int batchNumber = 0;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            SteamGameDTO dto = objectMapper.readValue(parser, SteamGameDTO.class);
            batch.add(dto);
            if (batch.size() >= batchSize) {
                batchNumber++;
                int[] result = processBatchOrThrow(batch, batchNumber);
                totalSavedCount += result[0];
                totalSkippedCount += result[1];
                totalInvalidCount += result[2];
                log.info("배치 {} 완료: 저장={}, 건너뜀={}, 유효하지 않음={}",
                        batchNumber, result[0], result[1], result[2]);
                batch = new ArrayList<>(batchSize);
            }
        }

        if (!batch.isEmpty()) {
            batchNumber++;
            int[] result = processBatchOrThrow(batch, batchNumber);
            totalSavedCount += result[0];
            totalSkippedCount += result[1];
            totalInvalidCount += result[2];
            log.info("배치 {} 완료: 저장={}, 건너뜀={}, 유효하지 않음={}",
                    batchNumber, result[0], result[1], result[2]);
        }

        log.info("스트리밍 JSON import 완료. 배치 수={}", batchNumber);
        return new int[]{totalSavedCount, totalSkippedCount, totalInvalidCount};
    }

    private int[] processBatchOrThrow(List<SteamGameDTO> batch, int batchNumber) {
        try {
            return gameImportBatchService.processBatch(batch);
        } catch (Exception e) {
            throw new GameImportException(batchNumber,
                    "배치 " + batchNumber + " 처리 중 오류가 발생했습니다.", e);
        }
    }

    private List<SteamGameDTO> extractGamesFromRoot(JsonNode rootNode) {
        if (rootNode.isArray()) {
            return objectMapper.convertValue(rootNode, new TypeReference<List<SteamGameDTO>>() {});
        }
        JsonNode appsNode = rootNode.path("applist").path("apps");
        if (appsNode.isMissingNode() || !appsNode.isArray()) {
            throw new IllegalStateException(
                    "지원하지 않는 JSON 구조입니다. 배열 또는 {applist:{apps:[]}} 형식이어야 합니다.");
        }
        return objectMapper.convertValue(appsNode, new TypeReference<List<SteamGameDTO>>() {});
    }
}
