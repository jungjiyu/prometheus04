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


    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User saveUser(String username) {
        User newUser = new User();
        newUser.setUsername(username);
        return userRepository.save(newUser);
    }

    public User findOrCreateUser(String username) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> saveUser(username)); // 사용자 없으면 자동 생성
    }
}
