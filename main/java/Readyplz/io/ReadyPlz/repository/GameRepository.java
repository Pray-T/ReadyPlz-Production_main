package Readyplz.io.ReadyPlz.repository;

import Readyplz.io.ReadyPlz.domain.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository // 이 어노테이션은 필수는 아니지만, 명시적으로 컴포넌트 스캔 대상으로 지정할 수 있습니다.
            // JpaRepository를 상속받는 인터페이스는 Spring Data JPA가 자동으로 빈으로 등록합니다.
public interface GameRepository extends JpaRepository<Game, Long> {
                                     // JpaRepository<Entity클래스, PK타입>

    // 기본적인 CRUD 메서드는 JpaRepository에서 이미 제공됩니다:
    // - save(S entity): 엔티티 저장 및 수정
    // - findById(ID id): ID로 엔티티 조회 (Optional<T> 반환)
    // - findAll(): 모든 엔티티 조회 (List<T> 반환)
    // - count(): 엔티티 총 개수 조회
    // - deleteById(ID id): ID로 엔티티 삭제
    // - delete(T entity): 엔티티로 삭제
    // - existsById(ID id): ID로 엔티티 존재 여부 확인

    // 사용자 정의 쿼리 메서드 (메서드 이름 규칙을 따르면 Spring Data JPA가 자동으로 쿼리 생성)

    /**
     * Steam Application ID로 게임을 조회합니다.
     * @param appid 조회할 게임의 Steam App ID
     * @return Optional<Game> 객체 (게임이 존재하면 Game 객체, 없으면 빈 Optional)
     */
    Optional<Game> findByAppid(Integer appid);

    /**
     * Steam Application ID로 게임의 존재 여부를 확인합니다.
     * @param appid 확인할 게임의 Steam App ID
     * @return boolean (게임이 존재하면 true, 없으면 false)
     */
    boolean existsByAppid(Integer appid);

    /**
     * 게임 이름에 특정 키워드가 포함된 게임들을 조회합니다. (예시)
     * @param nameKeyword 검색할 이름 키워드
     * @return List<Game> (조건에 맞는 게임 리스트)
     */
    List<Game> findByNameContainingIgnoreCase(String nameKeyword);

    /**
     * 게임 이름에 특정 키워드가 포함된 게임들을 페이지네이션과 함께 조회합니다.
     * @param nameKeyword 검색할 이름 키워드
     * @param pageable 페이지네이션 정보
     * @return Page<Game> (조건에 맞는 게임 페이지)
     */
    Page<Game> findByNameContainingIgnoreCase(String nameKeyword, Pageable pageable);

    // 필요에 따라 JPQL(@Query 어노테이션 사용) 또는 네이티브 쿼리를 사용할 수도 있습니다.
    // 예시:
    // @Query("SELECT g FROM Game g WHERE g.name = :name AND g.steamAppId > :appId")
    // List<Game> findGamesByNameAndAppIdGreaterThan(@Param("name") String name, @Param("appId") Integer appId);

     /**
     * 게임 이름으로 존재 여부를 확인합니다.
     * @param name 확인할 게임의 이름
     * @return boolean (게임이 존재하면 true, 없으면 false)
     */
    boolean existsByName(String name);

    /**
     * 주어진 appid 목록 중 이미 DB에 존재하는 appid들을 한 번의 쿼리로 조회합니다.
     */
    @Query("select g.appid from Game g where g.appid in :appids")
    List<Integer> findExistingAppids(@Param("appids") List<Integer> appids);

    /**
     * 주어진 이름 목록 중 이미 DB에 존재하는 게임 이름들을 한 번의 쿼리로 조회합니다.
     */
    @Query("select g.name from Game g where g.name in :names")
    List<String> findExistingNames(@Param("names") List<String> names);

}