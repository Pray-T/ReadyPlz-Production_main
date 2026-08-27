package io.readyplz.readyplz.service;

import io.readyplz.readyplz.domain.Game;
import io.readyplz.readyplz.dto.SteamGameDTO;
import io.readyplz.readyplz.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(GameImportBatchService.class)
class GameImportBatchServiceTest {

    @Autowired
    private GameImportBatchService gameImportBatchService;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void processBatch_persistsNewGamesInTransaction() {
        SteamGameDTO dto = new SteamGameDTO();
        dto.setAppId(100L);
        dto.setName("Test Game");

        int[] result = gameImportBatchService.processBatch(List.of(dto));

        assertThat(result[0]).isEqualTo(1);
        assertThat(gameRepository.findAll()).hasSize(1);
        Game saved = gameRepository.findAll().get(0);
        assertThat(saved.getAppid()).isEqualTo(100);
        assertThat(saved.getName()).isEqualTo("Test Game");
    }

    @Test
    void processBatch_skipsDuplicates() {
        SteamGameDTO dto = new SteamGameDTO();
        dto.setAppId(200L);
        dto.setName("Duplicate Game");
        gameImportBatchService.processBatch(List.of(dto));

        int[] secondRun = gameImportBatchService.processBatch(List.of(dto));

        assertThat(secondRun[0]).isZero();
        assertThat(secondRun[1]).isEqualTo(1);
        assertThat(gameRepository.findAll()).hasSize(1);
    }

    @Test
    void processBatch_skipsCaseInsensitiveNameDuplicatesInSameBatch() {
        SteamGameDTO upper = new SteamGameDTO();
        upper.setAppId(142200L);
        upper.setName("STREET FIGHTER X TEKKEN");

        SteamGameDTO mixed = new SteamGameDTO();
        mixed.setAppId(204120L);
        mixed.setName("Street Fighter X Tekken");

        int[] result = gameImportBatchService.processBatch(List.of(upper, mixed));

        assertThat(result[0]).isEqualTo(1);
        assertThat(result[1]).isEqualTo(1);
        assertThat(gameRepository.findAll()).hasSize(1);
        assertThat(gameRepository.findAll().get(0).getAppid()).isEqualTo(142200);
    }
}
