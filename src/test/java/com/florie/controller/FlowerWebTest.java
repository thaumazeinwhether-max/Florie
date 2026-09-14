package com.florie.controller;

import com.florie.config.SecurityConfig;
import com.florie.entity.Flower;
import com.florie.entity.FlowerType;
import com.florie.form.FlowerRegisterForm;
import com.florie.service.FlowerRegistrationException;
import com.florie.service.FlowerService;
import com.florie.service.UserService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({FlowerController.class, HomeController.class})
@Import(SecurityConfig.class)
class FlowerWebTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private FlowerService flowerService;
    @MockitoBean private UserService userService;

    @BeforeEach
    void setUp() {
        String[] names = {"ガーベラ", "バラ", "チューリップ", "カーネーション", "ひまわり", "ダリア", "アネモネ", "ラナンキュラス"};
        List<FlowerType> types = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            FlowerType type = mock(FlowerType.class);
            when(type.getFlowerTypeId()).thenReturn(20L + i);
            when(type.getFlowerName()).thenReturn(names[i]);
            when(type.getCareGuidance()).thenReturn("水は少なめに。茎先が3～5cmほどつかる量を目安にし、水切れに気をつけましょう。");
            types.add(type);
        }
        when(flowerService.getFlowerTypes()).thenReturn(types);
        when(userService.getNickname("test@example.com")).thenReturn("利用者さん");
    }

    @Test
    void registrationPageShowsEightTypesGuidancePreparationAndCsrf() throws Exception {
        MvcResult result = mvc.perform(get("/flowers/register").with(user("test@example.com")))
                .andExpect(status().isOk())
                
                .andExpect(content().string(containsString("ラナンキュラス")))
                .andExpect(content().string(containsString("水は少なめに。")))
                .andExpect(content().string(containsString("すでに準備が済んでいる場合は、やり直さなくて大丈夫です。")))
                .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
        assertThat(result.getResponse().getContentAsString().split("type=\"radio\"").length - 1).isEqualTo(8);
        savePreview("flower-register", result);
    }

    @Test
    void successfulRegistrationUsesLoggedInUserAndRedirectsHome() throws Exception {
        mvc.perform(post("/flowers").with(user("test@example.com")).with(csrf())
                .param("flowerTypeId", "21").param("flowerNickname", "ばらちゃん")
                .param("userId", "999").param("status", "ENDED").param("startedOn", "2000-01-01"))
                .andExpect(redirectedUrl("/home?flowerRegistered"));
        ArgumentCaptor<FlowerRegisterForm> form = ArgumentCaptor.forClass(FlowerRegisterForm.class);
        verify(flowerService).registerFlower(eq("test@example.com"), form.capture());
        assertThat(form.getValue().getFlowerTypeId()).isEqualTo(21L);
        assertThat(form.getValue().getFlowerNickname()).isEqualTo("ばらちゃん");
    }

    @Test
    void missingInputsAndLongNicknameReturnValidationErrors() throws Exception {
        mvc.perform(post("/flowers").with(user("test@example.com")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("flowerRegisterForm", "flowerTypeId", "flowerNickname"));
        mvc.perform(post("/flowers").with(user("test@example.com")).with(csrf())
                .param("flowerTypeId", "21").param("flowerNickname", "あ".repeat(21)))
                .andExpect(model().attributeHasFieldErrors("flowerRegisterForm", "flowerNickname"))
                .andExpect(content().string(matchesPattern("(?s).*<input[^>]*value=\"21\"[^>]*checked=\"checked\"[^>]*>.*")));
        verify(flowerService, never()).registerFlower(anyString(), any());
    }

    @Test
    void invalidTypeNumberHasJapaneseError() throws Exception {
        mvc.perform(post("/flowers").with(user("test@example.com")).with(csrf())
                .param("flowerTypeId", "invalid").param("flowerNickname", "はな"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("一覧からお花を選択してください。")));
        verify(flowerService, never()).registerFlower(anyString(), any());
    }

    @Test
    void activeFlowerBlocksFormAndDirectPostShowsServiceError() throws Exception {
        when(flowerService.getCurrentFlower("test@example.com")).thenReturn(Optional.of(new Flower()));
        mvc.perform(get("/flowers/register").with(user("test@example.com")))
                .andExpect(redirectedUrl("/home?alreadyRegistered"));
        doThrow(new FlowerRegistrationException("お世話中のお花がいるため、新しいお花は登録できません。"))
                .when(flowerService).registerFlower(anyString(), any());
        mvc.perform(post("/flowers").with(user("test@example.com")).with(csrf())
                .param("flowerTypeId", "21").param("flowerNickname", "はな"))
                .andExpect(status().isOk()).andExpect(model().hasErrors());
    }

    @Test
    void homeShowsEmptyOrCurrentFlowerAndEscapesNickname() throws Exception {
        MvcResult empty = mvc.perform(get("/home").with(user("test@example.com")))
                .andExpect(content().string(containsString("お花がいません")))
                .andExpect(content().string(containsString("href=\"/flowers/register\""))).andReturn();
        savePreview("home-empty", empty);
        Flower flower = new Flower();
        flower.setFlowerType(flowerService.getFlowerTypes().get(1));
        flower.setFlowerNickname("<ばらちゃん>");
        when(flowerService.getCurrentFlower("test@example.com")).thenReturn(Optional.of(flower));
        MvcResult active = mvc.perform(get("/home").with(user("test@example.com")))
                .andExpect(content().string(containsString("&lt;ばらちゃん&gt;")))
                .andExpect(content().string(not(containsString("href=\"/flowers/register\""))))
                .andExpect(content().string(not(containsString("care-tasks")))).andReturn();
        savePreview("home-active", active);
        verify(flowerService, times(2)).getCurrentFlower("test@example.com");
    }

    @Test
    void authenticationAndCsrfProtectFlowerRegistration() throws Exception {
        mvc.perform(get("/flowers/register")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/flowers").with(csrf())).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/flowers").with(user("test@example.com"))).andExpect(status().isForbidden());
        verify(flowerService, never()).registerFlower(anyString(), any());
    }

    private void savePreview(String name, MvcResult result) throws Exception {
        Path folder = Path.of("target", "ui-preview");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(name + ".html"), result.getResponse().getContentAsString());
    }
}
