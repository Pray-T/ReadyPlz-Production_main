package io.readyplz.readyplz.service;

import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.domain.Game;
import io.readyplz.readyplz.domain.MemberGame;
import io.readyplz.readyplz.dto.SteamGameDetailDTO;
import io.readyplz.readyplz.repository.MemberGameRepository;
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

    private final MemberGameRepository memberGameRepository;

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
} 