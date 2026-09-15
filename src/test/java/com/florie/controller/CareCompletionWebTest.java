package com.florie.controller;

import com.florie.config.SecurityConfig;
import com.florie.service.CareService;
import com.florie.service.CareCompletionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CareController.class)
@Import(SecurityConfig.class)
class CareCompletionWebTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private CareService service;

    @Test
    void postUsesPrincipalAndRedirectsWithSuccessMessage() throws Exception {
        mvc.perform(post("/care-tasks/123/complete").with(user("own@example.com")).with(csrf())
                .param("completedOn", "2000-01-01").param("userId", "999"))
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("careCompleted", "お世話を記録しました。"));
        verify(service).completeCare("own@example.com", 123L);
    }

    @Test
    void rejectionReturnsHomeWithError() throws Exception {
        doThrow(new CareCompletionException("完了できません。"))
                .when(service).completeCare("own@example.com", 123L);
        mvc.perform(post("/care-tasks/123/complete").with(user("own@example.com")).with(csrf()))
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("careError", "完了できません。"));
    }

    @Test
    void databaseErrorDoesNotExposeInternalDetails() throws Exception {
        doThrow(new DataIntegrityViolationException("internal SQL detail"))
                .when(service).completeCare("own@example.com", 123L);
        mvc.perform(post("/care-tasks/123/complete").with(user("own@example.com")).with(csrf()))
                .andExpect(redirectedUrl("/home"))
                .andExpect(flash().attribute("careError", "記録できませんでした。ホームを確認してから、もう一度お試しください。"));
    }

    @Test
    void anonymousAndMissingCsrfCannotComplete() throws Exception {
        mvc.perform(post("/care-tasks/123/complete").with(csrf())).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/care-tasks/123/complete").with(user("own@example.com"))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void getAndInvalidIdCannotComplete() throws Exception {
        mvc.perform(get("/care-tasks/123/complete").with(user("own@example.com"))).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/care-tasks/invalid/complete").with(user("own@example.com")).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
