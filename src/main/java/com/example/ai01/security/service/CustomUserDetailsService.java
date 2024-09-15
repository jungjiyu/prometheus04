package com.example.ai01.security.service;

import com.example.ai01.user.entity.Member;
import com.example.ai01.user.repository.MemberRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // DB에서 사용자 정보 조회
        Member member = memberRepository.findByUsername(username).orElseThrow(()-> new UsernameNotFoundException("User not found with username: " + username));

        // 사용자의 권한 설정
        String[] roles = member.getRoles() != null ? member.getRoles().split(",") : new String[]{"ROLE_USER"};

        return User.builder()
                .username(member.getUsername())
                .password(member.getPassword())
                .roles(roles)  // 사용자의 역할을 설정
                .build();
    }

}