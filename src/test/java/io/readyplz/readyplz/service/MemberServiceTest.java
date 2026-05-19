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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
}
