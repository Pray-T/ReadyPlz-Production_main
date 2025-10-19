package Readyplz.io.ReadyPlz.controller;

import Readyplz.io.ReadyPlz.domain.Member;
import Readyplz.io.ReadyPlz.domain.Game;
import Readyplz.io.ReadyPlz.service.MemberService;
import Readyplz.io.ReadyPlz.service.GameService;
import Readyplz.io.ReadyPlz.service.MessageService;
import Readyplz.io.ReadyPlz.service.MemberGameService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MemberService memberService;
    private final GameService gameService;
    private final MemberGameService memberGameService;

    // 메시지 목록 페이지
    @GetMapping
    public String messageList(Authentication auth, Model model,
                              @PageableDefault(size = 20) Pageable pageable) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        org.springframework.data.domain.Pageable pageReq = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        var conversationsPage = messageService.getConversations(member.getId(), pageReq);
        model.addAttribute("conversations", conversationsPage.getContent());
        model.addAttribute("conversationsPage", conversationsPage);
        model.addAttribute("isLimitReached", messageService.isMessageLimitReached());

        return "messages/list";
    }

    // 대화 내용 페이지
    @GetMapping("/{otherMemberId}")
    public String conversation(@PathVariable("otherMemberId") Long otherMemberId, Authentication auth, Model model,
                               @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        Page<Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO> conversationPage = messageService.getConversation(member.getId(), otherMemberId, pageable);
        List<Readyplz.io.ReadyPlz.dto.SummaryDTO.MessageSummaryDTO> conversation = conversationPage.getContent();
        boolean isLimitReached = messageService.isMessageLimitReached();
        
        model.addAttribute("conversation", conversation);
        model.addAttribute("conversationPage", conversationPage);
        model.addAttribute("otherMemberId", otherMemberId);
        model.addAttribute("isLimitReached", isLimitReached);
        // Stateless 방식: 현재 사용자 ID를 모델에 추가 (세션 대신 사용)
        model.addAttribute("currentMemberId", member.getId());
        
        return "messages/conversation";
    }

    // 메시지 전송
    @PostMapping("/send")
    @ResponseBody
    public String sendMessage(Authentication auth,
                                                @RequestParam("receiverId") Long receiverId,
                                                @RequestParam("content") String content) {

        String username = auth.getName();
        Member member = memberService.findByUsername(username);

        try { 
            messageService.sendMessage(member.getId(), receiverId, content);
            return "success";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    
    // 게임별 사용자 목록 페이지
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
    
    // 문의하기 메시지 전송 (로그인 사용자용)
    @PostMapping("/inquiry")
    @ResponseBody
    public String sendInquiry(Authentication auth, @RequestParam("content") String content) {
        try {
            String username = auth.getName();
            Member member = memberService.findByUsername(username);
            
            // ADMIN 계정 찾기
            Member adminMember = memberService.findAdminMember();
            
            // 문의 내용을 ADMIN에게 전송
            messageService.sendMessage(member.getId(), adminMember.getId(), "[문의] " + content);
            
            return "success";
         } catch (Exception e) {
            return "문의 전송 중 오류가 발생했습니다: " + e.getMessage();
         }
    }
} 