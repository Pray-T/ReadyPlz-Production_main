package io.readyplz.readyplz.controller;

import io.readyplz.readyplz.domain.Game;
import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.domain.MemberGame;
import io.readyplz.readyplz.dto.SteamGameDetailDTO;
import io.readyplz.readyplz.service.MemberGameService;
import io.readyplz.readyplz.service.GameService;
import io.readyplz.readyplz.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final MemberService memberService;
    private final MemberGameService memberGameService;

    private static final int MAX_SELECTED_GAMES = 5;

    @GetMapping("/collection")
    public String gameCollection(@RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                @RequestParam(value = "search", defaultValue = "") String search,
                                Authentication auth,
                                Model model) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        List<SteamGameDetailDTO> userGames = memberGameService.getMemberGames(member);
        List<Long> userGameIds = userGames.stream()
                .map(SteamGameDetailDTO::getId)
                .collect(Collectors.toList());

        model.addAttribute("userGames", userGames);
        model.addAttribute("member", member);
        model.addAttribute("search", search);

        if (!userGameIds.isEmpty()) {
            Map<Long, List<io.readyplz.readyplz.dto.summary.MemberSummaryDTO>> sameGameUsersMap =
                    memberGameService.getSameGameUsersForAllGames(userGameIds, member.getId());
            model.addAttribute("sameGameUsersMap", sameGameUsersMap);
        } else {
            model.addAttribute("sameGameUsersMap", new HashMap<>());
        }

        if (search != null && !search.trim().isEmpty()) {
            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("name").ascending());
            Page<Game> gamesPage = gameService.findByName(search.trim(), pageable);
            List<Game> games = gamesPage.getContent();

            games.forEach(game -> game.setUserHasGame(userGameIds.contains(game.getId())));

            model.addAttribute("games", games);
            model.addAttribute("currentPage", safePage);
            model.addAttribute("totalPages", gamesPage.getTotalPages());
            model.addAttribute("totalElements", gamesPage.getTotalElements());
            model.addAttribute("hasResults", true);
        } else {
            Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("name").ascending());
            Page<Game> gamesPage = gameService.findAll(pageable);
            List<Game> games = gamesPage.getContent();

            games.forEach(game -> game.setUserHasGame(userGameIds.contains(game.getId())));

            model.addAttribute("games", games);
            model.addAttribute("currentPage", safePage);
            model.addAttribute("totalPages", gamesPage.getTotalPages());
            model.addAttribute("totalElements", gamesPage.getTotalElements());
            model.addAttribute("hasResults", !games.isEmpty());
        }
        return "games/collection";
    }

    @PostMapping("/collection/add-game")
    @ResponseBody
    public ResponseEntity<String> addGameToCollection(@RequestParam("gameId") Long gameId, Authentication auth) {
        try {
            String username = auth.getName();
            Member member = memberService.findByUsername(username);

            Game game = gameService.findById(gameId);

            if (memberGameService.existsByMemberAndGame(member, game)) {
                return ResponseEntity.ok("이미 보유한 게임입니다.");
            }

            long selectedCount = memberGameService.countByMember(member);
            if (selectedCount >= MAX_SELECTED_GAMES) {
                return ResponseEntity.badRequest().body("최대 5개까지 선택 가능합니다. 기존 게임을 삭제하고 다시 추가해주세요.");
            }

            MemberGame memberGame = MemberGame.builder()
                    .member(member)
                    .game(game)
                    .build();

            memberGameService.save(memberGame);

            return ResponseEntity.ok("게임이 컬렉션에 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("게임 추가 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/collection/remove-game")
    @ResponseBody
    public ResponseEntity<String> removeGameFromCollection(@RequestParam("gameId") Long gameId, Authentication auth) {
        try {
            String username = auth.getName();
            Member member = memberService.findByUsername(username);

            Game game = gameService.findById(gameId);

            memberGameService.deleteByMemberAndGame(member, game);

            return ResponseEntity.ok("게임이 컬렉션에서 제거되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("게임 제거 중 오류가 발생했습니다.");
        }
    }
}
