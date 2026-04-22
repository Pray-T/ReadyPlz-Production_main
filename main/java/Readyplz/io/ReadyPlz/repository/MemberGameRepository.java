package Readyplz.io.ReadyPlz.repository;


import Readyplz.io.ReadyPlz.domain.Game;
import Readyplz.io.ReadyPlz.domain.Member;
import Readyplz.io.ReadyPlz.domain.MemberGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

@Repository // Spring Bean으로 등록하기 위한 애노테이션 (선택적이지만 명시적으로 사용하는 것이 좋음)
public interface MemberGameRepository extends JpaRepository<MemberGame, Long> { // 엔티티 클래스와 ID 타입을 지정

    /**
     * 이 메서드는 특정 회원이 특정 게임을 소유하고 있는지 (또는 플레이했는지 등의 관계가 있는지) 확인하는 데 사용될 수 있습니다.
     * @param member 조회할 회원 엔티티
     * @param game 조회할 게임 엔티티
     * @return 존재할 경우 해당 MemberGame 엔티티를 담은 Optional, 없으면 Optional.empty()
     */
    Optional<MemberGame> findByMemberAndGame(Member member, Game game);
    
    boolean existsByMemberAndGame(Member member, Game game);

    /**
     * 특정 회원이 가진 모든 MemberGame 관계를 조회합니다.
     * 이 메서드는 특정 회원이 소유하거나 플레이한 모든 게임 목록을 가져오는 데 사용될 수 있습니다.
     * @param member 조회할 회원 엔티티
     * @return 해당 회원의 모든 MemberGame 엔티티를 담은 Set
     */
    Set<MemberGame> findByMember(Member member);

    //특정 회원이 가진 모든 MemberGame 관계의 수를 조회합니다.
    long countByMember(Member member);

     //단일 DELETE 쿼리로 회원-게임 관계를 삭제합니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MemberGame mg where mg.member = :member and mg.game = :game")
    int deleteByMemberAndGame(@Param("member") Member member, @Param("game") Game game);

    // 특정 회원의 모든 게임 연관관계 삭제
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MemberGame mg where mg.member = :member")
    int deleteByMember(@Param("member") Member member);

    /**
     * 특정 게임과 관련된 모든 MemberGame 관계를 조회합니다.
     * 이 메서드는 특정 게임을 어떤 회원들이 소유하고 있는지 등을 파악하는 데 사용될 수 있습니다.
     * @param game 조회할 게임 엔티티
     * @return 해당 게임의 모든 MemberGame 엔티티를 담은 Set
     */
    Set<MemberGame> findByGame(Game game);
    
    /**
     * 특정 게임 ID로 해당 게임을 소유한 모든 사용자 조회
     * @param gameId 조회할 게임 ID
     * @return 해당 게임을 소유한 모든 MemberGame 엔티티를 담은 Set
     */
    Set<MemberGame> findByGameId(Long gameId);

    @Query("SELECT mg FROM MemberGame mg JOIN FETCH mg.member JOIN FETCH mg.game WHERE mg.game.id IN :gameIds")
    Set<MemberGame> findByGameIdIn(@Param("gameIds") List<Long> gameIds);

    
     //회원이 가진 게임들을 DTO로 직접 조회하여 N+1 문제를 방지합니다.
    @Query("select new Readyplz.io.ReadyPlz.dto.SteamGameDetailDTO(g.appid, g.name, (case when g.releaseDate is not null then year(g.releaseDate) else null end), g.headerImageUrl) " +
           "from MemberGame mg join mg.game g where mg.member = :member")
    List<Readyplz.io.ReadyPlz.dto.SteamGameDetailDTO> findGameDetailsByMember(@Param("member") Member member);

    /** 특정 게임을 가진 회원들을 요약 DTO로 조회 (N+1 방지) */
    @Query("select new Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO(m.id, m.username, m.nickname, m.country) " +
           "from MemberGame mg join mg.member m where mg.game.id = :gameId")
    List<Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO> findMemberSummariesByGameId(@Param("gameId") Long gameId);

    /** 특정 게임을 가진 회원들을 요약 DTO로 조회하되 특정 사용자 제외 */
    @Query("select new Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO(m.id, m.username, m.nickname, m.country) " +
           "from MemberGame mg join mg.member m where mg.game.id = :gameId and m.id <> :currentUserId")
    List<Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO> findMemberSummariesByGameIdExcludingUser(@Param("gameId") Long gameId,
                                                                                                       @Param("currentUserId") Long currentUserId);

     //여러 게임에 대해, 각 게임을 가진 회원 정보를 DTO프로젝션을 통해 해결 (N+1 방지) 
    @Query("select new Readyplz.io.ReadyPlz.dto.SummaryDTO.GameUserSummaryDTO(g.id, m.id, m.username, m.nickname, m.country) " +
           "from MemberGame mg join mg.member m join mg.game g where g.id in :gameIds")
    List<Readyplz.io.ReadyPlz.dto.SummaryDTO.GameUserSummaryDTO> findGameUserSummariesByGameIds(@Param("gameIds") List<Long> gameIds);
}