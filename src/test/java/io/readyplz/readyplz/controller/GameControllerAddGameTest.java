package io.readyplz.readyplz.controller;

import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.exception.AlreadyOwnedGameException;
import io.readyplz.readyplz.service.GameService;
import io.readyplz.readyplz.service.MemberGameService;
import io.readyplz.readyplz.service.MemberService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameControllerAddGameTest {

    @Mock
    private GameService gameService;

    @Mock
    private MemberService memberService;

    @Mock
    private MemberGameService memberGameService;

    @InjectMocks
    private GameController gameController;

    @Test
    void addGameToCollection_returnsSuccessFromSingleServiceCall() {
        when(memberService.findByUsername("collector")).thenReturn(member());

        ResponseEntity<String> response = gameController.addGameToCollection(3L, authentication());

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertEquals("게임이 컬렉션에 추가되었습니다.", response.getBody());
        verify(memberGameService).addGameToCollection(7L, 3L);
        verify(gameService, never()).findById(any());
    }

    @Test
    void addGameToCollection_returnsExistingDuplicateMessageWithoutExceptionText() {
        when(memberService.findByUsername("collector")).thenReturn(member());
        doThrow(new AlreadyOwnedGameException("Unique index or primary key violation: uk_member_game"))
                .when(memberGameService).addGameToCollection(7L, 3L);

        ResponseEntity<String> response = gameController.addGameToCollection(3L, authentication());

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
        assertEquals(MemberGameService.ALREADY_OWNED_MESSAGE, response.getBody());
        assertFalse(response.getBody().toLowerCase(Locale.ROOT).contains("uk_member_game"));
        assertFalse(response.getBody().contains("Exception"));
        assertFalse(response.getBody().toLowerCase(Locale.ROOT).contains("violation"));
    }

    @Test
    void addGameToCollection_returnsExistingLimitMessage() {
        when(memberService.findByUsername("collector")).thenReturn(member());
        doThrow(new IllegalArgumentException(MemberGameService.SELECTION_LIMIT_MESSAGE))
                .when(memberGameService).addGameToCollection(7L, 3L);

        ResponseEntity<String> response = gameController.addGameToCollection(3L, authentication());

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
        assertEquals(MemberGameService.SELECTION_LIMIT_MESSAGE, response.getBody());
    }

    @Test
    void addGameToCollection_hidesUnexpectedPersistenceExceptionText() {
        when(memberService.findByUsername("collector")).thenReturn(member());
        doThrow(new DataIntegrityViolationException(
                "could not execute statement; SQL [insert into member_game]; constraint [uk_member_game]"))
                .when(memberGameService).addGameToCollection(7L, 3L);

        ResponseEntity<String> response = gameController.addGameToCollection(3L, authentication());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
        assertEquals("게임 추가 중 오류가 발생했습니다.", response.getBody());
        assertFalse(response.getBody().toLowerCase(Locale.ROOT).contains("uk_member_game"));
        assertFalse(response.getBody().toLowerCase(Locale.ROOT).contains("insert into"));
        assertFalse(response.getBody().toLowerCase(Locale.ROOT).contains("constraint"));
    }

    private static Member member() {
        return Member.builder()
                .id(7L)
                .username("collector")
                .password("password")
                .email("collector@test.com")
                .country("KR")
                .build();
    }

    private static Authentication authentication() {
        return new UsernamePasswordAuthenticationToken("collector", "n/a");
    }
}
