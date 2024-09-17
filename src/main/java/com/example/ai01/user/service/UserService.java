package com.example.ai01.user.service;

import com.example.ai01.user.entity.User;
import com.example.ai01.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;


    // 사용자 이름으로 사용자 조회
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // 새로운 사용자 저장
    public User saveUser(String username) {
        User newUser = new User();
        newUser.setUsername(username);
        return userRepository.save(newUser);
    }

    // 사용자 조회 또는 없을 경우 생성
    public User findOrCreateUser(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> saveUser(username)); // 사용자 없으면 자동 생성
    }

    // 사용자 이름으로 존재 여부 확인
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
