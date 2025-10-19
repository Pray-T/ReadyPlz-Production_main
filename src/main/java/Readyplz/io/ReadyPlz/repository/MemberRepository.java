package Readyplz.io.ReadyPlz.repository;

import Readyplz.io.ReadyPlz.domain.Member; // Member 엔티티 클래스를 임포트합니다.
import org.springframework.data.jpa.repository.JpaRepository; // JpaRepository를 임포트합니다.
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository; // (선택 사항이지만 명시하는 것이 좋습니다)

import java.util.List;
import java.util.Optional; // 결과가 없을 수도 있는 경우 Optional 사용

@Repository // 이 인터페이스가 Repository 계층의 컴포넌트임을 나타냅니다. (필수 아님, 가독성 목적)
public interface MemberRepository extends JpaRepository<Member, Long> {

    // username으로 회원 찾기 (unique 제약조건이 있으므로 결과는 0개 또는 1개)
    @Query("select distinct m from Member m left join fetch m.roles where m.username = :username")
    Optional<Member> findByUsername(@Param("username") String username);

    // email로 회원 찾기 (unique 제약조건이 있으므로 결과는 0개 또는 1개)
    Optional<Member> findByEmail(String email);

    // 특정 키워드를 포함하는 username을 가진 회원 목록 찾기 (부분 검색)
     List<Member> findByUsernameContaining(String keyword);

    // username을 통한 회원 존재 여부 확인 
    boolean existsByUsername(String username);

    // email을 통한  회원 존재 여부 확인 
    boolean existsByEmail(String email);

    // nickname 중복 여부 확인
    boolean existsByNickname(String nickname);

    Optional<Member> findByResetToken(String resetToken);

    // --- 커스텀 쿼리가 필요한 경우 @Query 어노테이션 사용 예시 ---
    // @Query("SELECT m FROM Member m WHERE m.email = :email")
    // Optional<Member> findByEmailCustomQuery(@Param("email") String email);
}