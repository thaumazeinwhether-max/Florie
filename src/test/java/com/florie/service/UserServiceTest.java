package com.florie.service;

import com.florie.entity.User;
import com.florie.form.UserRegisterForm;
import com.florie.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private UserRepository repository;
    private UserService service;
    private ValidatorFactory validatorFactory;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        validatorFactory = Validation.buildDefaultValidatorFactory();
        service = new UserService(repository, encoder, validatorFactory.getValidator());
    }

    @AfterEach
    void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void registrationStoresSaltedHashAndNormalizedEmail() {
        UserRegisterForm form = validForm();
        service.registerUser(form);
        service.registerUser(form);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(repository, times(2)).saveAndFlush(saved.capture());
        User first = saved.getAllValues().get(0);
        User second = saved.getAllValues().get(1);
        assertThat(first.getEmail()).isEqualTo("test@example.com");
        assertThat(first.getNickname()).isEqualTo("はな");
        assertThat(first.getCreatedAt()).isNotNull();
        assertThat(first.getPasswordHash()).isNotEqualTo(form.getPassword());
        assertThat(first.getPasswordHash()).isNotEqualTo(second.getPasswordHash());
        assertThat(encoder.matches(form.getPassword(), first.getPasswordHash())).isTrue();
    }

    @Test
    void duplicateEmailIsRejectedBeforeSaving() {
        when(repository.existsByEmail("test@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.registerUser(validForm()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void invalidInputNeverReachesDatabase() {
        UserRegisterForm form = validForm();
        form.setNickname(" ");
        form.setEmail("invalid");
        form.setPassword("short");
        assertThatThrownBy(() -> service.registerUser(form)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void multibytePasswordOverBcryptLimitIsRejected() {
        UserRegisterForm form = validForm();
        form.setPassword("あ".repeat(25));
        form.setPasswordConfirmation(form.getPassword());
        assertThatThrownBy(() -> service.registerUser(form)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void exactPasswordAndNicknameLimitsAreAccepted() {
        UserRegisterForm form = validForm();
        form.setNickname("あ".repeat(20));
        form.setPassword("a".repeat(72));
        form.setPasswordConfirmation(form.getPassword());
        service.registerUser(form);
        verify(repository).saveAndFlush(any(User.class));
    }

    private UserRegisterForm validForm() {
        UserRegisterForm form = new UserRegisterForm();
        form.setNickname("はな");
        form.setEmail(" Test@EXAMPLE.com ");
        form.setPassword("test-password");
        form.setPasswordConfirmation("test-password");
        return form;
    }
}
