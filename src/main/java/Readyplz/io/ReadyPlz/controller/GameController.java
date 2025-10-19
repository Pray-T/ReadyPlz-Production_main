package Readyplz.io.ReadyPlz.controller;

import Readyplz.io.ReadyPlz.domain.Game;
import Readyplz.io.ReadyPlz.domain.Member;
import Readyplz.io.ReadyPlz.domain.MemberGame;
import Readyplz.io.ReadyPlz.service.MemberGameService;
import Readyplz.io.ReadyPlz.service.GameService;
import Readyplz.io.ReadyPlz.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final MemberService memberService;
    private final MemberGameService memberGameService; 

    // 최대 선택 가능 게임 수
    private final int MAX_SELECTED_GAMES = 5;

    @GetMapping("/collection") // 이 메서드는 사용자의 게임 컬렉션 페이지를 렌더링하는 역할을 합니다. 사용자가 가진 게임 목록, 게임 검색 기능 및 결과, 그리고 각 게임을 가진 다른 사용자 목록을 화면에 표시하기 위한 데이터를 준비합니다.
    public String gameCollection(@RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size,
                                @RequestParam(value = "search", defaultValue = "") String search,
                                Authentication auth,
                                Model model) {
        

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        // 사용자가 가진 게임 목록 조회
        Set<MemberGame> memberGames = memberGameService.findByMember(member);
        List<Game> userGames = memberGames.stream()
                .map(MemberGame::getGame)
                .collect(Collectors.toList());
        List<Long> userGameIds = memberGames.stream()
                .map(memberGame -> memberGame.getGame().getId())
                .collect(Collectors.toList());

        model.addAttribute("userGames", userGames);
        model.addAttribute("member", member);
        model.addAttribute("search", search);

        // N+1 쿼리 문제를 해결하기 위해 모든 게임에 대한 '같은 게임을 가진 유저' 목록을 한 번의 쿼리로 조회, N+1방지를 위해 DTO프로젝션으로 해결
        if (!userGameIds.isEmpty()) {
            Map<Long, List<Readyplz.io.ReadyPlz.dto.SummaryDTO.MemberSummaryDTO>> sameGameUsersMap = memberGameService.getSameGameUsersForAllGames(userGameIds, member.getId());
            model.addAttribute("sameGameUsersMap", sameGameUsersMap);
        } else {
            model.addAttribute("sameGameUsersMap", new HashMap<>());
        }

        if (search != null && !search.trim().isEmpty()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending()); 
            Page<Game> gamesPage = gameService.findByName(search.trim(), pageable);
            List<Game> games = gamesPage.getContent();
            
            // 디버깅 로그 추가
            // System.out.println("=== 게임 컬렉션 페이지 디버깅 ===");
            // System.out.println("검색어: " + search);
            // System.out.println("현재 페이지: " + page);
            // System.out.println("페이지 크기: " + size);
            // System.out.println("검색 결과 개수: " + games.size());
            // System.out.println("전체 결과 개수: " + gamesPage.getTotalElements());
            // System.out.println("전체 페이지 수: " + gamesPage.getTotalPages());
            // System.out.println("================================");
            
            games.forEach(game -> {
                game.setUserHasGame(userGameIds.contains(game.getId()));  //검색된 각 게임에 대해, 현재 사용자가 이미 가지고 있는 게임인지 여부를 나타내는 boolean 플래그를 설정합니다. 이는 뷰에서 "추가" 또는 "삭제" 버튼을 동적으로 표시하는 데 사용됩니다.
            });
            
            model.addAttribute("games", games);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", gamesPage.getTotalPages());
            model.addAttribute("totalElements", gamesPage.getTotalElements());
            model.addAttribute("hasResults", true); // 검색 결과가 있음을 명시
        } else {
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            Page<Game> gamesPage = gameService.findAll(pageable);
            List<Game> games = gamesPage.getContent();

            games.forEach(game -> {
                game.setUserHasGame(userGameIds.contains(game.getId()));
            });

            model.addAttribute("games", games);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", gamesPage.getTotalPages());
            model.addAttribute("totalElements", gamesPage.getTotalElements());
            model.addAttribute("hasResults", !games.isEmpty());
        }
        return "games/collection"; //templates/games/collection.html (Thymeleaf 기준) 뷰를 렌더링하도록 지시합니다.
    }

    @PostMapping("/collection/add-game")
    @ResponseBody
    public ResponseEntity<String> addGameToCollection(@RequestParam("gameId") Long gameId, Authentication auth) {
        try {

            // 인증된 사용자의 정보를 조회합니다.
            String username = auth.getName(); 
            Member member = memberService.findByUsername(username); 
            
            Game game = gameService.findById(gameId);

            // 이미 가진 게임인지 확인
            if (memberGameService.existsByMemberAndGame(member, game)) { 
                return ResponseEntity.ok("이미 보유한 게임입니다.");
            }
            // 8개 초과 제한
            long selectedCount = memberGameService.countByMember(member);
            if (selectedCount >= MAX_SELECTED_GAMES) {
                return ResponseEntity.badRequest().body("'최대 5개까지 선택 가능합니다, 기존 게임을 삭제하고 다시 추가해주세요");
            }

            // MemberGame 생성 및 저장
            MemberGame memberGame = MemberGame.builder()
                    .member(member)
                    .game(game)
                    .build();
            
            memberGameService.save(memberGame);

            return ResponseEntity.ok("게임이 컬렉션에 추가되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("게임 추가 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/collection/remove-game")
    @ResponseBody
    public ResponseEntity<String> removeGameFromCollection(@RequestParam("gameId") Long gameId, Authentication auth) {
        try {

            String username = auth.getName();
            Member member = memberService.findByUsername(username);
            
            Game game = gameService.findById(gameId);

            // MemberGame 관계 삭제
            memberGameService.deleteByMemberAndGame(member, game);

            return ResponseEntity.ok("게임이 컬렉션에서 제거되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("게임 제거 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 