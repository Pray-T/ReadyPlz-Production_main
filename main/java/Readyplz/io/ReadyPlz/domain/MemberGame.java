package Readyplz.io.ReadyPlz.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@ToString(exclude = {"member", "game"}) // 순환참조 방지 (user -> member)
@Entity
@Table(
        name = "member_game", // 테이블명 변경 (user_game -> member_game)
        uniqueConstraints = { // 멤버와 게임의 조합은 유일해야 함
            @UniqueConstraint(
                name = "uk_member_game", // 유니크 제약조건 이름 변경
                columnNames = {"member_id", "game_id"} // 컬럼명 변경 (user_id -> member_id)
                )
        }
)
public class MemberGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_game_id") // PK 컬럼명 변경
    private Long id;

    // Member와의 다대일 연관관계
    // MemberGame은 Member에 속한다 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false) // 외래 키 (FK) 및 컬럼명 변경
    private Member member;

    // Game과의 다대일 연관관계 (Game 엔티티는 그대로 사용)
    // MemberGame은 Game에 속한다 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false) // 외래 키 (FK)
    private Game game;

    

    // Auditing (생성/수정 시간)
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 빌더 패턴
    @Builder
    public MemberGame(@NonNull Member member, @NonNull Game game) {
        this.member = member;
        this.game = game;
        this.updatedAt = LocalDateTime.now();
    }
       

    // 도메인 내부 연관관계 설정용 (외부 계층 직접 호출 금지)
    void setMember(Member member) {
        this.member = member;
        // 필요하다면 Member 객체의 컬렉션에도 추가하는 로직이 Member.addMemberGame()에 구현되어 있어야 함
        // member.getMemberGames().add(this); // 직접 추가보다는 Member쪽에 위임
    }

    // 도메인 내부 연관관계 설정용 (외부 계층 직접 호출 금지)
    void setGame(Game game) {
        this.game = game;
        // 필요하다면 Game 객체의 컬렉션에도 추가 (Game 엔티티에 List<MemberGame> gameMembers 필드가 있다고 가정)
        // game.getGameMembers().add(this); // Game 엔티티에 해당 컬렉션과 편의 메서드 필요
    }

    

  
}