package Readyplz.io.ReadyPlz.service;

import Readyplz.io.ReadyPlz.domain.Member;
import Readyplz.io.ReadyPlz.domain.Game;
import Readyplz.io.ReadyPlz.domain.MemberGame;
import Readyplz.io.ReadyPlz.dto.SteamGameDetailDTO;
import Readyplz.io.ReadyPlz.repository.MemberGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberGameService {

    private final MemberGameRepository memberGameRepository;

    @Transactional(readOnly = true) 
    public List<SteamGameDetailDTO> getMemberGames(Member member) { 
        return memberGameRepository.findGameDetailsByMember(member); 
    }

    @Transactional(readOnly = true)
    public List<Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO> getMembersByGameId(Long gameId) {
        return memberGameRepository.findMemberSummariesByGameId(gameId);
    }

    @Transactional(readOnly = true)
    public List<Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO> getMembersByGameIdExcludingCurrentUser(Long gameId, Long currentUserId) {
        return memberGameRepository.findMemberSummariesByGameIdExcludingUser(gameId, currentUserId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO>> getSameGameUsersForAllGames(List<Long> gameIds, Long currentUserId) {
        List<Readyplz.io.ReadyPlz.dto.SummaryDTO.GameUserSummaryDTO> rows = memberGameRepository.findGameUserSummariesByGameIds(gameIds);
        Map<Long, List<Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO>> sameGameUsersMap = rows.stream()
                .filter(row -> !row.getMemberId().equals(currentUserId))
                .collect(Collectors.groupingBy(
                        Readyplz.io.ReadyPlz.dto.SummaryDTO.GameUserSummaryDTO::getGameId,
                        Collectors.mapping(row -> new Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO(
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
        return memberGameRepository.save(memberGame);
    }

    @Transactional
    public void deleteByMemberAndGame(Member member, Game game) { 
        memberGameRepository.deleteByMemberAndGame(member, game);
    }
} 