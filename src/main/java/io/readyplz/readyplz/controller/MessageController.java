package io.readyplz.readyplz.controller;

import io.readyplz.readyplz.domain.Member;
import io.readyplz.readyplz.domain.Game;
import io.readyplz.readyplz.dto.request.SendMessageRequest;
import io.readyplz.readyplz.service.MemberService;
import io.readyplz.readyplz.service.GameService;
import io.readyplz.readyplz.service.MessageService;
import io.readyplz.readyplz.service.MemberGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MemberService memberService;
    private final GameService gameService;
    private final MemberGameService memberGameService;

    @GetMapping
    public String messageList(Authentication auth, Model model,
                              @PageableDefault(size = 20) Pageable pageable) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        var pageReq = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        var conversationsPage = messageService.getConversations(member.getId(), pageReq);
        model.addAttribute("conversations", conversationsPage.getContent());
        model.addAttribute("conversationsPage", conversationsPage);

        return "messages/list";
    }

    @GetMapping("/{otherMemberId}")
    public String conversation(@PathVariable("otherMemberId") Long otherMemberId, Authentication auth, Model model,
                               @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        try {
            messageService.assertCanViewConversation(member.getId(), otherMemberId);
        } catch (IllegalArgumentException e) {
            return "redirect:/messages?error=access_denied";
        }

        Page<io.readyplz.readyplz.dto.summary.MessageSummaryDTO> conversationPage =
                messageService.getConversation(member.getId(), otherMemberId, pageable);
        List<io.readyplz.readyplz.dto.summary.MessageSummaryDTO> conversation = conversationPage.getContent();
        boolean isLimitReached = messageService.isConversationLimitReached(member.getId(), otherMemberId);

        model.addAttribute("conversation", conversation);
        model.addAttribute("conversationPage", conversationPage);
        model.addAttribute("otherMemberId", otherMemberId);
        model.addAttribute("isLimitReached", isLimitReached);
        model.addAttribute("currentMemberId", member.getId());

        return "messages/conversation";
    }

    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendMessage(Authentication auth,
            @Valid @ModelAttribute SendMessageRequest request) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        try {
            messageService.sendMessage(member.getId(), request.getReceiverId(), request.getContent());
            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/game/{gameId}/users")
    public String gameUsers(@PathVariable("gameId") Long gameId, Authentication auth, Model model) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        Game game = gameService.findById(gameId);

        var gameUsers = memberGameService.getMembersByGameIdExcludingCurrentUser(gameId, member.getId());

        model.addAttribute("game", game);
        model.addAttribute("gameUsers", gameUsers);
        model.addAttribute("currentUser", member);

        return "messages/game-users";
    }

    @PostMapping("/inquiry")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendInquiry(Authentication auth, @RequestParam("content") String content) {
        try {
            String username = auth.getName();
            Member member = memberService.findByUsername(username);

            Member adminMember = memberService.findAdminMember();

            messageService.sendMessage(member.getId(), adminMember.getId(), "[문의] " + content);

            return ResponseEntity.ok(Map.of("message", "success"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "문의 전송 중 오류가 발생했습니다."));
        }
    }
}
