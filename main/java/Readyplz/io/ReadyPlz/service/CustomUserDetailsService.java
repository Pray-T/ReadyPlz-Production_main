package Readyplz.io.ReadyPlz.service;

import Readyplz.io.ReadyPlz.domain.Member;
import Readyplz.io.ReadyPlz.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
//Spring Security 인증 관련
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override 
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다: {}", username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });

        // Member의 roles를 Spring Security의 GrantedAuthority로 변환
        var authorities = member.getRoles().stream() 
                .map(role -> new SimpleGrantedAuthority(role.getName())) 
                .collect(Collectors.toList()); 

        return User.builder() 
                .username(member.getUsername()) 
                .password(member.getPassword()) 
                .authorities(authorities) 
                .build(); 
    }
} 