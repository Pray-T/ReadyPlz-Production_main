package io.readyplz.readyplz.service;

import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.domain.Game;
import io.readyplz.readyplz.domain.MemberGame;
import io.readyplz.readyplz.dto.SteamGameDetailDTO;
import io.readyplz.readyplz.exception.AlreadyOwnedGameException;
import io.readyplz.readyplz.repository.GameRepository;
import io.readyplz.readyplz.repository.MemberGameRepository;
import io.readyplz.readyplz.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberGameService {

    public static final int MAX_SELECTED_GAMES = 5;

    public static final String ALREADY_OWNED_MESSAGE = "이미 보유한 게임입니다.";

    public static final String SELECTION_LIMIT_MESSAGE =
            "최대 " + MAX_SELECTED_GAMES + "개까지 선택 가능합니다. 기존 게임을 삭제하고 다시 추가해주세요.";

    private final MemberGameRepository memberGameRepository;
    private final MemberRepository memberRepository;
    private final GameRepository gameRepository;

    @Transactional(readOnly = true) 
    public List<SteamGameDetailDTO> getMemberGames(Member member) { 
        return memberGameRepository.findGameDetailsByMember(member); 
    }

    @Transactional(readOnly = true)
    public List<io.readyplz.readyplz.dto.summary.MemberSummaryDTO> getMembersByGameId(Long gameId) {
        return memberGameRepository.findMemberSummariesByGameId(gameId);
    }

    @Transactional(readOnly = true)
    public List<io.readyplz.readyplz.dto.summary.MemberSummaryDTO> getMembersByGameIdExcludingCurrentUser(Long gameId, Long currentUserId) {
        return memberGameRepository.findMemberSummariesByGameIdExcludingUser(gameId, currentUserId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<io.readyplz.readyplz.dto.summary.MemberSummaryDTO>> getSameGameUsersForAllGames(List<Long> gameIds, Long currentUserId) {
        List<io.readyplz.readyplz.dto.summary.GameUserSummaryDTO> rows = memberGameRepository.findGameUserSummariesByGameIds(gameIds);
        Map<Long, List<io.readyplz.readyplz.dto.summary.MemberSummaryDTO>> sameGameUsersMap = rows.stream()
                .filter(row -> !row.getMemberId().equals(currentUserId))
                .collect(Collectors.groupingBy(
                        io.readyplz.readyplz.dto.summary.GameUserSummaryDTO::getGameId,
                        Collectors.mapping(row -> new io.readyplz.readyplz.dto.summary.MemberSummaryDTO(
                                row.getMemberId(), row.getUsername(), row.getNickname(), row.getCountry()
                        ), Collectors.toList())
                ));
        gameIds.forEach(gameId -> sameGameUsersMap.putIfAbsent(gameId, List.of()));
        return sameGameUsersMap;
    }

    @Transactional(readOnly = true) 
    public Set<MemberGame> findByMember(Member member) {
        return memberGameRepository.findByMember(member);
    }

    @Transactional(readOnly = true) 
    public Optional<MemberGame> findByMemberAndGame(Member member, Game game) { 
        return memberGameRepository.findByMemberAndGame(member, game); 
    }

    @Transactional(readOnly = true)
    public boolean existsByMemberAndGame(Member member, Game game) { 
        return memberGameRepository.existsByMemberAndGame(member, game);
    }

    @Transactional(readOnly = true) 
    public long countByMember(Member member) {
        return memberGameRepository.countByMember(member);
    }

    @Transactional 
    public MemberGame save(MemberGame memberGame) {
        return memberGameRepository.save(Objects.requireNonNull(memberGame));
    }

    @Transactional
    public void deleteByMemberAndGame(Member member, Game game) { 
        memberGameRepository.deleteByMemberAndGame(member, game);
    }

    /**
     * 보유 확인, 개수 확인, 저장을 한 트랜잭션에서 처리한다.
     * 회원 행을 잠근 뒤에 중복과 상한을 본다. InnoDB REPEATABLE READ에서도
     * 잠금 대기 중에 커밋된 추가가 개수 조회에 포함된다.
     */
    @Transactional
    public void addGameToCollection(Long memberId, Long gameId) {
        Long requiredMemberId = Objects.requireNonNull(memberId);
        Long requiredGameId = Objects.requireNonNull(gameId);

        Member member = memberRepository.findByIdForUpdate(requiredMemberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Game game = gameRepository.findById(requiredGameId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게임을 찾을 수 없습니다."));

        if (memberGameRepository.existsByMemberIdAndGameId(requiredMemberId, requiredGameId)) {
            throw new AlreadyOwnedGameException(ALREADY_OWNED_MESSAGE);
        }
        if (memberGameRepository.countByMemberId(requiredMemberId) >= MAX_SELECTED_GAMES) {
            throw new IllegalArgumentException(SELECTION_LIMIT_MESSAGE);
        }

        memberGameRepository.save(MemberGame.builder()
                .member(member)
                .game(game)
                .build());
    }
} 