package io.readyplz.readyplz.service;

import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.repository.MemberRepository;
import io.readyplz.readyplz.repository.MemberGameRepository;
import io.readyplz.readyplz.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MemberGameRepository memberGameRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public Long register(Member member) {
        
        // 중복 회원 검증
        validateDuplicateMember(member); 
        
        memberRepository.save(Objects.requireNonNull(member)); 
        return member.getId(); 
    }

    private void validateDuplicateMember(Member member) {
        // 이메일 중복 검증
        memberRepository.findByEmail(member.getEmail())
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 존재하는 이메일입니다.");
                });
        
        // 사용자명 중복 검증
        memberRepository.findByUsername(member.getUsername())
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 존재하는 사용자명입니다.");
                });

        if (member.getNickname() != null && !member.getNickname().isBlank()) {
            if (memberRepository.existsByNickname(member.getNickname())) {
                throw new IllegalStateException("이미 존재하는 닉네임입니다.");
            }
        }
    }

    public Member findById(Long id) {
        return memberRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }

    public Member findByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }

    @Transactional
    public void deleteMember(Long id) {
        memberRepository.deleteById(Objects.requireNonNull(id));
    }

    @Transactional
    public void createPasswordResetToken(String email) {
        // 사용자 열거형 공격 방지: 존재 여부와 무관하게 동일한 처리 흐름 유지
        String token = java.util.UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);

        //dirtyCheck를 통해 변경감지시 자동으로 save메서드 진행
        try {
            memberRepository.findByEmail(email).ifPresent(member -> {
                member.setResetToken(token);
                member.setResetTokenExpiry(expiry);
                // 토큰이 DB에 커밋된 뒤에만 메일을 발송한다.
                // (커밋 전 발송 시, 사용자가 즉시 링크를 클릭하면 토큰이 조회되지 않을 수 있음)
                scheduleResetMailAfterCommit(email, token);
            });
        } finally {
            // 메일 발송 유무에 따른 타이밍 기반 사용자 열거 완화를 위해 항상 동일한 CPU 연산을 수행한다.
            passwordEncoder.encode(token);
        }
    }

    /**
     * DB 커밋 성공 후에만 비동기 재설정 메일을 발송한다.
     * 트랜잭션 동기화가 비활성인 경우(테스트 등)에는 즉시 발송한다.
     */
    private void scheduleResetMailAfterCommit(String email, String token) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailService.sendResetMail(email, token);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.sendResetMail(email, token);
            }
        });
    }

    public Optional<Member> findByResetToken(String token) {
        return memberRepository.findByResetToken(token);
    }

    @Transactional
    public boolean isResetTokenValid(String token) {
        Optional<Member> memberOpt = memberRepository.findByResetToken(token);
        return memberOpt.isPresent() && memberOpt.get().getResetTokenExpiry() != null && memberOpt.get().getResetTokenExpiry().isAfter(LocalDateTime.now());
    }

    //비밀번호 재설정을 위한 메서드드
    @Transactional
    public void resetPassword(String token, String newPassword) {
        Member member = memberRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (member.getResetTokenExpiry() == null || member.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("토큰이 만료되었습니다.");
        }

        member.changePassword(passwordEncoder.encode(newPassword));
        member.setResetToken(null);
        member.setResetTokenExpiry(null);
        memberRepository.save(member);
    }
    
    @Transactional
    public Member save(Member member) {
        return memberRepository.save(Objects.requireNonNull(member));
    }

    @Transactional
    public void updateNickname(String username, String newNickname) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        // 중복 닉네임 체크
        if (newNickname != null && !newNickname.isBlank()
                && memberRepository.existsByNicknameAndIdNot(newNickname, member.getId())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }
        member.changeNickname(newNickname);
        memberRepository.save(member);
    }

    // 닉네임 유효성 검증은 컨트롤러 DTO에서 처리
    //프,로필 페이지에서 비밀번호 재설정을 위한 메서드
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword, String confirmPassword) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("password_input_error");
        }

        member.changePassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    /**
     * 계정 탈퇴(자기 자신): 비밀번호 확인, 연관 데이터 정리 후 회원 삭제
     */
    @Transactional
    public void deleteOwnAccount(String username, String rawPassword) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new IllegalArgumentException("password_input_error");
        }

        // 연관 데이터 삭제: 메시지(발신/수신), 멤버-게임 관계
        // 외래키 제약으로 수신 메시지 먼저 삭제
        messageRepository.deleteAllByReceiverId(member.getId());
        messageRepository.deleteAllBySenderId(member.getId());
        memberGameRepository.deleteByMember(member);

        // 최종 회원 삭제
        memberRepository.delete(member);
    }

    // ADMIN 계정 찾기 (DB에서 ROLE_ADMIN 한 명만 조회, 다수일 경우 가장 작은 member_id)
    public Member findAdminMember() {
        var admins = memberRepository.findMembersWithRoleNameOrdered("ROLE_ADMIN", PageRequest.of(0, 1));
        if (admins.isEmpty()) {
            throw new IllegalStateException("ADMIN 계정을 찾을 수 없습니다.");
        }
        return admins.get(0);
    }

}
