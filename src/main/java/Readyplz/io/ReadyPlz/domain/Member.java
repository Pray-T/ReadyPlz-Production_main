package Readyplz.io.ReadyPlz.domain;

import lombok.*;

import jakarta.persistence.*; // Spring Boot 3 이상은 jakarta.persistence 사용

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
@Entity // 이 클래스가 JPA 엔티티임을 명시합니다. 데이터베이스 테이블과 매핑됩니다.
@Table(name = "members") // 매핑될 테이블의 이름을 지정합니다. (선택 사항이지만 명시하는 것이 좋습니다), 테이블 이름은 관례적으로 복수형을 사용합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"memberGames", "sentMessages", "receivedMessages"})
public class Member {

    @Id // Primary Key임을 명시합니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Primary Key 생성 전략을 설정합니다. IDENTITY는 DB가 ID를 자동 생성해주는 방식입니다.
    @Column(name = "member_id") // 데이터베이스 컬럼 이름을 지정합니다.
    private Long id; // 사용자의 고유 식별자 (Primary Key)

    @Column(name = "username", nullable = false, unique = true, length = 16) // 사용자 이름 또는 로그인 ID
    private String username;

    @Column(name = "password_hash", nullable = false) // 비밀번호. 실제 비밀번호가 아닌 해시된 값을 저장해야 합니다!
    private String password; // 보안을 위해 비밀번호는 반드시 해시해서 저장합니다.

    @Column(name = "email", nullable = false, unique = true, length = 32) // 이메일 (선택 사항일 수 있지만 보통 unique)
    private String email;
    
    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate; // 마지막 로그인 일시

    @Column(name = "nickname", length = 16) // 닉네임 (선택 사항일 수 있지만 보통 길이 제한)
    private String nickname;
  
    @Column(nullable = false)
    private String country;

    //비밀번호 재설정을 위한 토큰 필드드
    @Column(name = "reset_token")
    private String resetToken;
    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    // ★★★★★ Role과의 Many-to-Many 관계 추가 ★★★★★
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(
        name = "member_roles", // 중간 테이블의 이름
        joinColumns = @JoinColumn(name = "member_id"), // Member 테이블의 FK
        inverseJoinColumns = @JoinColumn(name = "role_id") // Role 테이블의 FK
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();



    // Member와 MemberGame의 연관관계 (Member 한 명은 여러 MemberGame을 가질 수 있음)
    // 필드명 및 클래스명 변경 (UserGame -> MemberGame)
    @OneToMany(mappedBy = "member", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MemberGame> memberGames = new ArrayList<>();

    // Member와 Message의 연관관계 (Member가 보낸 메시지와 받은 메시지를 관리)
    @OneToMany(mappedBy = "sender", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<Message> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<Message> receivedMessages = new ArrayList<>();

    // Auditing
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    /**
     * Member에 MemberGame을 추가하고, MemberGame에도 Member를 설정합니다 (양방향 연관관계 메서드).
     * 이미 해당 MemberGame이 리스트에 있다면 추가하지 않습니다.
     * @param memberGame 추가할 MemberGame 객체
     */
    public void addMemberGame(MemberGame memberGame) {
        if (memberGame != null && !this.memberGames.contains(memberGame)) {
            this.memberGames.add(memberGame);
            // MemberGame의 member 필드가 현재 Member 인스턴스와 다른 경우에만 설정 (무한루프 방지)
            if (memberGame.getMember() != this) {
                memberGame.setMember(this);
            }
        }
    }

    /**
     * Member에서 MemberGame을 제거하고, MemberGame에서도 Member 참조를 제거합니다 (양방향 연관관계 메서드).
     * 이미 해당 MemberGame이 리스트에 있다면 추가하지 않습니다.
     */
    public void removeMemberGame(MemberGame memberGame) {
        if (memberGame != null && this.memberGames.contains(memberGame)) {
            this.memberGames.remove(memberGame);
            // MemberGame의 member 필드가 현재 Member 인스턴스와 동일한 경우에만 null로 설정
            if (memberGame.getMember() == this) {
                memberGame.setMember(null); // 또는 memberGame.setMember(null); 이후 JPA orphanRemoval = true에 의해 처리
            }
        }
    }

    // equals와 hashCode 오버라이드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        // ID가 null이 아닐 때만 ID로 비교, ID가 null이면 객체 주소로 비교 (새로 생성된 엔티티)
        return id != null ? Objects.equals(id, member.id) : super.equals(o);
    }

    @Override
    public int hashCode() {
        // ID가 null이 아닐 때 ID의 해시코드 반환, null이면 기본 해시코드
        return id != null ? Objects.hash(id) : super.hashCode();
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }
}