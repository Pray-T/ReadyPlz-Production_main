package Readyplz.io.ReadyPlz.service;

import Readyplz.io.ReadyPlz.domain.Game;
import Readyplz.io.ReadyPlz.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;

    public Game findById(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게임을 찾을 수 없습니다."));
    }

    public Page<Game> findAll(Pageable pageable) {
        return gameRepository.findAll(pageable);
    }

    public Page<Game> findByName(String nameKeyword, Pageable pageable) {
        return gameRepository.findByNameContainingIgnoreCase(nameKeyword, pageable);
    }
}


