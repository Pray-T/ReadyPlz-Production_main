package Readyplz.io.ReadyPlz.service;

import Readyplz.io.ReadyPlz.domain.Game;
import Readyplz.io.ReadyPlz.dto.SteamGameDTO;
import Readyplz.io.ReadyPlz.repository.GameRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonToDbService {

    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper;
    
    // 배치 크기 설정
    private static final int BATCH_SIZE = 1000;

    public void saveDataFromJsonFile() throws IOException { 
        ClassPathResource resource = new ClassPathResource("steam_games_data.json"); 
        InputStream inputStream = resource.getInputStream(); 

        // JSON 파일을 JsonNode로 파싱
        JsonNode rootNode = objectMapper.readTree(inputStream); 

        // 파일 구조 유연 처리
        List<SteamGameDTO> games;
        if (rootNode.isArray()) {
            // 루트가 배열인 경우: [{"appId":..., "name":...}, ...]
            games = objectMapper.convertValue(rootNode, new TypeReference<List<SteamGameDTO>>() {});
        } else {
            // Steam Web API dump 형태: { "applist": { "apps": [ {"appid":..., "name":...}, ... ] } }
            JsonNode appsNode = rootNode.path("applist").path("apps");
            if (appsNode.isMissingNode() || !appsNode.isArray()) {
                throw new IllegalStateException("지원하지 않는 JSON 구조입니다. 배열 또는 {applist:{apps:[]}} 형식이어야 합니다.");
            }
            games = objectMapper.convertValue(appsNode, new TypeReference<List<SteamGameDTO>>() {});
        }

        if (games == null) {
            games = java.util.List.of();
        }

        log.info("{}개의 게임 데이터를 JSON 파일에서 읽었습니다. 배치 크기 {}로 DB 저장을 시작합니다.", games.size(), BATCH_SIZE);

        int totalSavedCount = 0;
        int totalSkippedCount = 0;
        int totalInvalidCount = 0;
        int batchNumber = 0;

        // 배치 단위로 처리
        for (int i = 0; i < games.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, games.size());
            List<SteamGameDTO> batch = games.subList(i, endIndex); 
            batchNumber++;

            try {
                int[] result = processBatch(batch, batchNumber);
                totalSavedCount += result[0];
                totalSkippedCount += result[1];
                totalInvalidCount += result[2];

                log.info("배치 {} 완료: 저장={}, 건너뜀={}, 유효하지 않음={}",
                        batchNumber, result[0], result[1], result[2]);

            } catch (Exception e) {
                log.error("배치 {} 처리 중 오류 발생: {}", batchNumber, e.getMessage());
                // 개별 배치 실패는 전체 프로세스를 중단하지 않음
                continue;
            }
        }

        log.info("전체 DB 저장 완료. 총 저장={}, 총 건너뜀={}, 총 유효하지 않음={}",
                totalSavedCount, totalSkippedCount, totalInvalidCount);
    }
    
    @Transactional
    protected int[] processBatch(List<SteamGameDTO> batch, int batchNumber) {
        int savedCount = 0;
        int skippedCount = 0;
        int invalidCount = 0;
        
        // 1) 유효한 DTO만 선별하고 배치 appid, name 목록 수집
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

        // 2) 단 한 번의 SELECT로 이미 존재하는 appid 목록 조회
        List<Integer> existingAppIds = gameRepository.findExistingAppids(batchAppIds);
        Set<Integer> existingAppIdSet = existingAppIds.stream().collect(Collectors.toSet());

        List<String> existingNames = gameRepository.findExistingNames(batchNames);
        Set<String> existingNameSet = existingNames.stream().collect(Collectors.toSet());

        // 배치 내에서도 중복 저장 방지: 이번 처리 중 이미 본 appId/name은 즉시 차단
        Set<Integer> seenAppIds = new HashSet<>(existingAppIdSet);
        Set<String> seenNames = new HashSet<>(existingNameSet);

        // 3) 메모리에서 비교 후 신규만 일괄 저장
        for (SteamGameDTO dto : validDtos) {
            Integer appId = dto.getAppId().intValue();
            String name = dto.getName();
            if (!seenAppIds.contains(appId) && !seenNames.contains(name)) {
                Game game = dto.toEntity();
                gameRepository.save(game);
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