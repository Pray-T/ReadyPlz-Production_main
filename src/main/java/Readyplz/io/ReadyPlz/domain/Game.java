package Readyplz.io.ReadyPlz.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.LocalDate;

@Getter 
@NoArgsConstructor(access = AccessLevel.PROTECTED) 
@ToString(exclude = {"memberGames"}) // 양방향 연관관계 시 순환참조 방지를 위해 userGames 제외
@Entity
@Table(name = "game") // 데이터베이스 테이블명을 "game"으로 지정
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB에 ID 생성을 위임 (auto_increment)
    @Column(name = "game_id")
    private Long id;

    @Column(unique = true, nullable = false, name = "STEAM_APP_ID") // Steam App ID, 고유해야 함
    private Integer appid;

    @Column(nullable = false, name = "name", unique = true) // Game name, 이름은 중복될 수 없음
    private String name;
 
    @Column(name = "header_image_url", length = 512) // 게임 대표 이미지 URL
    private String headerImageUrl;
  
    @Column(name = "release_date")
    private LocalDate releaseDate;

    // 사용자가 해당 게임을 가지고 있는지 표시하는 임시 필드 (DB에 저장되지 않음)
    @Transient
    @Setter
    private boolean userHasGame = false;

    // Game과 MemberGame의 연관관계 (Game 하나는 여러 MemberGame을 가질 수 있음)
    // 이 게임을 소유한 사용자 목록을 가져오기 위한 관계
    @OneToMany(mappedBy = "game", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<MemberGame> memberGames = new ArrayList<>();

    // Auditing을 위한 필드 (BaseEntity로 분리 가능)
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist // 엔티티가 저장되기 전에 호출
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate // 엔티티가 업데이트되기 전에 호출
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Game(Integer appid, String name, String headerImageUrl, LocalDate releaseDate) {
        this.appid = appid;
        this.name = name;
        this.headerImageUrl = headerImageUrl;
        this.releaseDate = releaseDate;
      
    }

    // == 연관관계 편의 메서드 == //

    /**
     * Game에 MemberGame을 추가하고, MemberGame에도 Game을 설정합니다 (양방향).
     * 이미 해당 MemberGame이 리스트에 있다면 추가하지 않습니다.
     * @param memberGame 추가할 MemberGame 객체
     */
    public void addMemberGame(MemberGame memberGame) {
        if (memberGame != null && !this.memberGames.contains(memberGame)) {
            this.memberGames.add(memberGame);
            // MemberGame의 game 필드가 현재 Game 인스턴스와 다른 경우에만 설정 (무한루프 방지)
            if (memberGame.getGame() != this) {
                memberGame.setGame(this); // MemberGame 클래스에 setGame(Game game) 메서드 필요
            }
        }
    }

    /**
     * Game에서 MemberGame을 제거하고, MemberGame에서도 Game 참조를 제거합니다 (양방향).
     * @param memberGame 제거할 MemberGame 객체
     */
    public void removeMemberGame(MemberGame memberGame) {
        if (memberGame != null && this.memberGames.contains(memberGame)) {
            this.memberGames.remove(memberGame);
            // MemberGame의 game 필드가 현재 Game 인스턴스와 동일한 경우에만 null로 설정
            if (memberGame.getGame() == this) {
                memberGame.setGame(null); // MemberGame 클래스에 setGame(Game game) 메서드 필요
            }
        }
    }

    // JPA 연관관계 편의 메서드를 위해 equals와 hashCode를 올바르게 구현하는 것이 좋습니다.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return id != null ? Objects.equals(id, game.id) : super.equals(o);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : super.hashCode();
    }

    // Setter는 Lombok @Setter를 사용하거나 필요에 따라 직접 만들 수 있지만,
    // Entity의 상태 변경은 신중해야 하므로 Builder 패턴이나 특정 로직을 가진 메서드를 사용하는 것이 좋습니다.
    // 예: public void updateGameDetails(String name, String genres, String tags) { ... }

    // 게임 정보 업데이트
    public void updateGameInfo(String name, LocalDate releaseDate, String headerImageUrl) {
        this.name = name;
        this.releaseDate = releaseDate;
        this.headerImageUrl = headerImageUrl;
    }

    public Integer getAppid() {
        return appid;
    }
    
}