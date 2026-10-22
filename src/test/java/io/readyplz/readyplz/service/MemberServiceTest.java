package io.readyplz.readyplz.service;

import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.repository.MemberGameRepository;
import io.readyplz.readyplz.repository.MemberRepository;
import io.readyplz.readyplz.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private MemberGameRepository memberGameRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private MemberService memberService;

    @Test
    void updateNickname_allowsKeepingSameNickname() {
        Member member = Member.builder().id(1L).username("user1").nickname("gamer").build();
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndIdNot("gamer", 1L)).thenReturn(false);

        assertDoesNotThrow(() -> memberService.updateNickname("user1", "gamer"));

        verify(memberRepository).save(member);
    }

    @Test
    void updateNickname_rejectsDuplicateNicknameFromOtherMember() {
        Member member = Member.builder().id(1L).username("user1").nickname("old").build();
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(memberRepository.existsByNicknameAndIdNot("taken", 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> memberService.updateNickname("user1", "taken"));

        verify(memberRepository, never()).save(any());
    }

    @Test
    void changePassword_deletesOnlyThatUsersTokensAfterSavingHash() {
        Member member = member("user1", "encoded-old");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

        memberService.changePassword("user1", "old-password", "new-password", "new-password");

        assertEquals("encoded-new", member.getPassword());
        InOrder order = inOrder(memberRepository, tokenService);
        order.verify(memberRepository).save(member);
        order.verify(memberRepository).flush();
        order.verify(tokenService).deleteUserTokens("user1");
        verify(tokenService, times(1)).deleteUserTokens("user1");
        verify(tokenService, never()).deleteUserTokens("user2");
    }

    @Test
    void changePassword_doesNotDeleteTokensWhenCurrentPasswordIsWrong() {
        Member member = member("user1", "encoded-old");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> memberService.changePassword("user1", "wrong", "new-password", "new-password"));

        assertEquals("encoded-old", member.getPassword());
        verify(memberRepository, never()).save(any());
        verify(memberRepository, never()).flush();
        verify(tokenService, never()).deleteUserTokens(anyString());
    }

    @Test
    void changePassword_propagatesWhenTokenDeleteFails() {
        Member member = member("user1", "encoded-old");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        doThrow(new IllegalStateException("redis")).when(tokenService).deleteUserTokens("user1");

        assertThrows(IllegalStateException.class,
                () -> memberService.changePassword("user1", "old-password", "new-password", "new-password"));

        verify(memberRepository).flush();
        verify(tokenService).deleteUserTokens("user1");
    }

    @Test
    void resetPassword_deletesOnlyThatUsersTokensAfterSavingHash() {
        Member member = member("user1", "encoded-old");
        member.setResetToken("tok");
        member.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        when(memberRepository.findByResetToken("tok")).thenReturn(Optional.of(member));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");

        memberService.resetPassword("tok", "new-password");

        assertEquals("encoded-new", member.getPassword());
        InOrder order = inOrder(memberRepository, tokenService);
        order.verify(memberRepository).save(member);
        order.verify(memberRepository).flush();
        order.verify(tokenService).deleteUserTokens("user1");
        verify(tokenService, never()).deleteUserTokens("user2");
    }

    @Test
    void resetPassword_doesNotDeleteTokensWhenTokenIsInvalidOrExpired() {
        when(memberRepository.findByResetToken("missing")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> memberService.resetPassword("missing", "new-password"));

        Member expired = member("user1", "encoded-old");
        expired.setResetToken("old");
        expired.setResetTokenExpiry(LocalDateTime.now().minusMinutes(1));
        when(memberRepository.findByResetToken("old")).thenReturn(Optional.of(expired));
        assertThrows(IllegalArgumentException.class, () -> memberService.resetPassword("old", "new-password"));

        assertEquals("encoded-old", expired.getPassword());
        verify(tokenService, never()).deleteUserTokens(anyString());
        verify(memberRepository, never()).flush();
    }

    private static Member member(String username, String encodedPassword) {
        return Member.builder()
                .id(1L)
                .username(username)
                .password(encodedPassword)
                .email(username + "@test.com")
                .country("KR")
                .build();
    }
}
