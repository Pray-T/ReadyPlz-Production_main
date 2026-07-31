package io.readyplz.readyplz.service;

import io.readyplz.readyplz.domain.Game;
import io.readyplz.readyplz.dto.SteamGameDTO;
import io.readyplz.readyplz.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameImportBatchService {

    private final GameRepository gameRepository;

    @Transactional
    public int[] processBatch(List<SteamGameDTO> batch) {
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
        Set<Integer> existingAppIdSet = new HashSet<>(existingAppIds);

        List<String> existingNames = gameRepository.findExistingNames(batchNames);
        Set<String> existingNameSet = new HashSet<>(existingNames);

        Set<Integer> seenAppIds = new HashSet<>(existingAppIdSet);
        Set<String> seenNames = new HashSet<>(existingNameSet);

        List<Game> toSave = new ArrayList<>();
        for (SteamGameDTO dto : validDtos) {
            Integer appId = dto.getAppId().intValue();
            String name = dto.getName();
            if (!seenAppIds.contains(appId) && !seenNames.contains(name)) {
                toSave.add(dto.toEntity());
                seenAppIds.add(appId);
                seenNames.add(name);
            } else {
                skippedCount++;
            }
        }

        if (!toSave.isEmpty()) {
            List<Game> saved = gameRepository.saveAll(toSave);
            savedCount = saved.size();
        }

        return new int[]{savedCount, skippedCount, invalidCount};
    }
}
