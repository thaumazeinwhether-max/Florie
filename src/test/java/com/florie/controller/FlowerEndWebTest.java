package com.florie.controller;

import com.florie.config.SecurityConfig;
import com.florie.entity.Flower;
import com.florie.entity.FlowerType;
import com.florie.service.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({FlowerController.class, HomeController.class})
@Import(SecurityConfig.class)
class FlowerEndWebTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private FlowerService flowers;
    @MockitoBean private UserService users;
    @MockitoBean private CareService cares;

    @Test
    void confirmationRendersFlowerAndPostFormButDoesNotUpdate() throws Exception {
        Flower flower = mock(Flower.class);
        FlowerType type = new FlowerType();
        type.setFlowerName("ガーベラ");
        when(flower.getFlowerId()).thenReturn(123L);
        when(flower.getFlowerType()).thenReturn(type);
        when(flower.getFlowerNickname()).thenReturn("がーべらちゃん");
        when(flowers.getFlowerForEnd("own@example.com", 123L)).thenReturn(flower);
        MvcResult result = mvc.perform(get("/flowers/123/end").with(user("own@example.com")))
                .andExpect(status().isOk()).andExpect(view().name("flower-end"))
                .andExpect(content().string(containsString("ガーベラ")))
                .andExpect(content().string(containsString("がーべらちゃん")))
                .andExpect(content().string(containsString("action=\"/flowers/123/end\" method=\"post\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("href=\"/home\" aria-label=\"キャンセルしてHomeへ戻る\""))).andReturn();
        verify(flowers, never()).endFlower(anyString(), anyLong());
        Files.createDirectories(Path.of("target/ui-preview"));
        Files.writeString(Path.of("target/ui-preview/flower-end.html"), result.getResponse().getContentAsString());
    }

    @Test
    void homeLinkCancelsWithoutCallingEndAndEmptyHomeAllowsRegistration() throws Exception {
        when(flowers.getCurrentFlower("own@example.com")).thenReturn(Optional.empty());
        mvc.perform(get("/home").with(user("own@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("お花がいません")))
                .andExpect(content().string(containsString("href=\"/flowers/register\"")));
        verify(flowers, never()).endFlower(anyString(), anyLong());
        verifyNoInteractions(cares);
    }

    @Test
    void postUsesPrincipalAndIgnoresSubmittedStatusAndDate() throws Exception {
        mvc.perform(post("/flowers/123/end").with(user("own@example.com")).with(csrf())
                .param("userId", "999").param("endedOn", "2000-01-01").param("status", "ACTIVE"))
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("flowerEnded", "お花とのお別れが完了しました。"));
        verify(flowers).endFlower("own@example.com", 123L);
    }

    @Test
    void invalidTargetReturnsHomeFromGetAndPost() throws Exception {
        when(flowers.getFlowerForEnd("own@example.com", 123L)).thenThrow(new FlowerEndException("対象がありません。"));
        doThrow(new FlowerEndException("対象がありません。")).when(flowers).endFlower("own@example.com", 123L);
        mvc.perform(get("/flowers/123/end").with(user("own@example.com")))
                .andExpect(redirectedUrl("/home")).andExpect(flash().attribute("flowerError", "対象がありません。"));
        mvc.perform(post("/flowers/123/end").with(user("own@example.com")).with(csrf()))
                .andExpect(redirectedUrl("/home")).andExpect(flash().attribute("flowerError", "対象がありません。"));
    }

    @Test
    void anonymousCannotOpenOrSubmit() throws Exception {
        mvc.perform(get("/flowers/123/end")).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/flowers/123/end").with(csrf())).andExpect(redirectedUrl("/login"));
        verifyNoInteractions(flowers);
    }

    @Test
    void missingCsrfIsRejected() throws Exception {
        mvc.perform(post("/flowers/123/end").with(user("own@example.com"))).andExpect(status().isForbidden());
        verifyNoInteractions(flowers);
    }

    @Test
    void databaseErrorIsReportedWithoutInternalDetails() throws Exception {
        doThrow(new DataIntegrityViolationException("internal SQL")) .when(flowers).endFlower("own@example.com", 123L);
        mvc.perform(post("/flowers/123/end").with(user("own@example.com")).with(csrf()))
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("flowerError", "お別れできませんでした。ホームでお花の状態を確認してから、もう一度お試しください。"));
    }
}
