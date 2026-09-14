package com.florie.service;

import com.florie.entity.User;
import com.florie.form.UserRegisterForm;
import com.florie.repository.UserRepository;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, Validator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional
    public void registerUser(UserRegisterForm form) {
        // Controller以外から呼ばれても、不正な入力を保存しない。
        if (!validator.validate(form).isEmpty()) {
            throw new IllegalArgumentException("登録内容を確認してください。");
        }
        String email = normalizeEmail(form.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User();
        user.setNickname(form.getNickname());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        // 同時登録で重複した場合も、DBの一意制約で拒否して画面へ戻せるようにする。
        userRepository.saveAndFlush(user);
    }

    @Transactional(readOnly = true)
    public String getNickname(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません。"));
        return user.getNickname();
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
