package com.florie.controller;

import com.florie.config.SecurityConfig;
import com.florie.entity.CareTask;
import com.florie.entity.Flower;
import com.florie.entity.FlowerType;
import com.florie.service.CareService;
import com.florie.service.FlowerService;
import com.florie.service.UserService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeCareWebTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private UserService users;
    @MockitoBean private FlowerService flowers;
    @MockitoBean private CareService cares;
    private final LocalDate today = LocalDate.of(2026, 10, 1);
    private Flower flower;

    @BeforeEach
    void setUp() {
        FlowerType type = new FlowerType();
        type.setFlowerName("バラ");
        type.setCareGuidance("水はやや多めに。花瓶の半分程度を目安にし、水につかる葉は取り除きましょう。");
        flower = new Flower();
        flower.setFlowerType(type);
        flower.setFlowerNickname("ばらちゃん");
        flower.setStartedOn(LocalDate.of(2026, 9, 14));
        when(users.getNickname("own@example.com")).thenReturn("はな");
        when(flowers.getCurrentFlower("own@example.com")).thenReturn(Optional.of(flower));
        when(cares.getToday()).thenReturn(today);
        when(cares.getCareDescriptions(flower)).thenReturn(Map.of("水を替える", "<説明>水を替えましょう。"));
    }

    @Test
    void rendersOneCardPerTaskWithDueLabelsAndCompletionForms() throws Exception {
        List<CareTask> dueTasks = List.of(task("茎を確認して整える", today.minusDays(7)), task("水を替える", today));
        when(cares.getDueTasks("own@example.com", today)).thenReturn(dueTasks);
        MvcResult result = mvc.perform(get("/home").with(user("own@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("今日が予定日")))
                .andExpect(content().string(containsString("予定日を過ぎています")))
                .andExpect(content().string(containsString("&lt;説明&gt;")))
                .andExpect(content().string(containsString("/care-tasks/1/complete")))
                .andExpect(content().string(containsString("できた"))).andReturn();
        assertThat(result.getResponse().getContentAsString().split("class=\"today-care-card\"").length - 1).isEqualTo(2);
        verify(cares).getDueTasks("own@example.com", today);
        savePreview("home-due", result);
    }

    @Test
    void activeFlowerWithNoDueTasksShowsClearMessage() throws Exception {
        MvcResult result = mvc.perform(get("/home").with(user("own@example.com")))
                .andExpect(content().string(containsString("今日のお世話はありません。")))
                .andExpect(content().string(not(containsString("class=\"today-care-card\"")))).andReturn();
        savePreview("home-no-due", result);
    }

    @Test
    void activeFlowerShowsStoredStartDateAndCommonGuidanceWithOrWithoutDueTasks() throws Exception {
        // 案内はお世話の有無に依存せず、開始日は今日ではなく保存済みの日付を表示する。
        for (List<CareTask> dueTasks : List.of(List.<CareTask>of(), List.of(task("水を替える", today)))) {
            when(cares.getDueTasks("own@example.com", today)).thenReturn(dueTasks);
            mvc.perform(get("/home").with(user("own@example.com")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("お迎えした日：")))
                    .andExpect(content().string(containsString("<time datetime=\"2026-09-14\">2026/09/14</time>")))
                    .andExpect(content().string(containsString("予定を待たず、水が濁ったり、ぬめりが出たりしたら水替え・洗浄を行う。")))
                    .andExpect(content().string(containsString("水が減って切り口が出そうなら、次の予定まで放置しない。")))
                    .andExpect(content().string(containsString("茎が短くなったら、切り口が水につく小さな花瓶に替える。")))
                    .andExpect(content().string(containsString(flower.getFlowerType().getCareGuidance())));
        }
    }

    @Test
    void noFlowerKeepsEmptyHomeAndDoesNotSearchTasks() throws Exception {
        when(flowers.getCurrentFlower("own@example.com")).thenReturn(Optional.empty());
        mvc.perform(get("/home").with(user("own@example.com")))
                .andExpect(content().string(containsString("お花がいません")))
                .andExpect(content().string(not(containsString("お迎えした日"))))
                .andExpect(content().string(not(containsString("水替え・洗浄を行う"))))
                .andExpect(content().string(not(containsString("次の予定まで放置しない"))))
                .andExpect(content().string(not(containsString("小さな花瓶に替える"))))
                .andExpect(content().string(not(containsString("今日のお世話"))));
        verifyNoInteractions(cares);
    }

    @Test
    void anonymousUserCannotReadHome() throws Exception {
        mvc.perform(get("/home")).andExpect(redirectedUrl("/login"));
        verifyNoInteractions(cares, flowers, users);
    }

    private CareTask task(String name, LocalDate date) {
        CareTask task = mock(CareTask.class);
        when(task.getCareTaskId()).thenReturn(name.equals("水を替える") ? 1L : 2L);
        when(task.getCareName()).thenReturn(name);
        when(task.getNextCareDate()).thenReturn(date);
        return task;
    }

    private void savePreview(String name, MvcResult result) throws Exception {
        Path folder = Path.of("target", "ui-preview");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(name + ".html"), result.getResponse().getContentAsString());
    }
}
