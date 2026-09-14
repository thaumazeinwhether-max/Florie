package com.florie.controller;

import com.florie.config.SecurityConfig;
import com.florie.entity.User;
import com.florie.repository.UserRepository;
import com.florie.service.CustomUserDetailsService;
import com.florie.service.UserService;
import com.florie.service.FlowerService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// DBだけを置き換え、Controller・Service・認証フィルター・HTMLを一緒に確認する。
@WebMvcTest({AuthController.class, HomeController.class})
@Import({SecurityConfig.class, UserService.class, CustomUserDetailsService.class})
class AuthWebTest {
    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;
    @MockitoBean private UserRepository repository;
    @MockitoBean private FlowerService flowerService;

    @Test
    void pagesRenderWithCsrfAndConfirmationField() throws Exception {
        MvcResult login = mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        MvcResult register = mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"passwordConfirmation\""))).andReturn();
        // 画面の見た目を確認するための出力。targetはGitの管理対象外。
        Path preview = Path.of("target", "ui-preview");
        Files.createDirectories(preview);
        Files.writeString(preview.resolve("login.html"), login.getResponse().getContentAsString());
        Files.writeString(preview.resolve("register.html"), register.getResponse().getContentAsString());
    }

    @Test
    void successfulRegistrationReturnsToLoginWithoutAutoLogin() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("nickname", "はな").param("email", "test@example.com")
                .param("password", "test-password").param("passwordConfirmation", "test-password"))
                .andExpect(redirectedUrl("/login?registered")).andExpect(unauthenticated());
        verify(repository).saveAndFlush(any(User.class));
    }

    @Test
    void mismatchedPasswordRetainsSafeFieldsButNotPasswords() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("nickname", "はな").param("email", "test@example.com")
                .param("password", "do-not-echo-this").param("passwordConfirmation", "different-secret"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("userRegisterForm", "passwordsMatching"))
                .andExpect(content().string(containsString("test@example.com")))
                .andExpect(content().string(not(containsString("do-not-echo-this"))))
                .andExpect(content().string(not(containsString("different-secret"))));
        verifyNoInteractions(repository);
    }

    @Test
    void requiredAndLengthAndEmailChecksAreDisplayed() throws Exception {
        mvc.perform(post("/register").with(csrf()))
                .andExpect(model().attributeHasFieldErrors("userRegisterForm",
                        "nickname", "email", "password", "passwordConfirmation"));
        mvc.perform(post("/register").with(csrf())
                .param("nickname", "あ".repeat(21)).param("email", "invalid")
                .param("password", "short").param("passwordConfirmation", "short"))
                .andExpect(model().attributeHasFieldErrors("userRegisterForm", "nickname", "email", "password"));
        verifyNoInteractions(repository);
    }

    @Test
    void duplicateAndConcurrentDuplicateReturnRegistrationErrors() throws Exception {
        when(repository.existsByEmail("test@example.com")).thenReturn(true);
        mvc.perform(post("/register").with(csrf())
                .param("nickname", "はな").param("email", "test@example.com")
                .param("password", "test-password").param("passwordConfirmation", "test-password"))
                .andExpect(model().attributeHasFieldErrors("userRegisterForm", "email"));
        when(repository.existsByEmail("test@example.com")).thenReturn(false);
        when(repository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("internal-sql-detail"));
        mvc.perform(post("/register").with(csrf())
                .param("nickname", "はな").param("email", "test@example.com")
                .param("password", "test-password").param("passwordConfirmation", "test-password"))
                .andExpect(status().isOk()).andExpect(model().hasErrors())
                .andExpect(content().string(not(containsString("internal-sql-detail"))));
    }

    @Test
    void loginKeepsSessionErasesCredentialsAndLogoutInvalidatesIt() throws Exception {
        prepareUser();
        MockHttpSession beforeLogin = new MockHttpSession();
        String oldId = beforeLogin.getId();
        MvcResult login = mvc.perform(post("/login").session(beforeLogin).with(csrf())
                .param("email", " Test@EXAMPLE.com ").param("password", "test-password"))
                .andExpect(redirectedUrl("/home"))
                .andExpect(authenticated().withUsername("test@example.com")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session.getId()).isNotEqualTo(oldId);
        SecurityContext context = (SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        assertThat(context.getAuthentication().getCredentials()).isNull();
        assertThat(((UserDetails) context.getAuthentication().getPrincipal()).getPassword()).isNull();
        mvc.perform(get("/home").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;はな&gt;")));
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/home")).andExpect(status().is3xxRedirection());
    }

    @Test
    void failedAndUnknownAndOversizedPasswordsHaveSameFailureDestination() throws Exception {
        prepareUser();
        mvc.perform(post("/login").with(csrf()).param("email", "test@example.com").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        mvc.perform(post("/login").with(csrf()).param("email", "unknown@example.com").param("password", "wrong"))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        mvc.perform(post("/login").with(csrf()).param("email", "test@example.com").param("password", "あ".repeat(100)))
                .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
        mvc.perform(get("/login?error")).andExpect(content().string(
                containsString("メールアドレスまたはパスワードが正しくありません。")));
    }

    @Test
    void authenticationAndCsrfAreRequired() throws Exception {
        mvc.perform(get("/home")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/register")).andExpect(status().isForbidden());
        mvc.perform(post("/login")).andExpect(status().isForbidden());
        mvc.perform(post("/logout")).andExpect(status().isForbidden());
    }

    private void prepareUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setNickname("<はな>");
        user.setPasswordHash(encoder.encode("test-password"));
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    }
}
