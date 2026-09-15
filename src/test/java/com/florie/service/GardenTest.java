package com.florie.service;

import com.florie.entity.*;
import com.florie.repository.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.test.web.servlet.MvcResult;

// 実MySQLには接続しない。テスト自体を@Transactionalにせず、Serviceのコミット・ロールバックを確認する。
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:garden-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class GardenTest {
    @Autowired private FlowerService service;
    @Autowired private CareService cares;
    @Autowired private org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private FlowerTypeRepository types;
    @Autowired private FlowerRepository flowers;
    @Autowired private CareRecordRepository records;
    @Autowired private CareTaskRepository tasks;
    @MockitoBean private Clock clock;
    private User user;
    private Flower flower;
    private final LocalDate today = LocalDate.of(2026, 9, 15);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-09-14T15:30:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Tokyo"));
        user = new User();
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setNickname("テスト利用者");
        user.setPasswordHash("not-used-for-authentication");
        user.setCreatedAt(LocalDateTime.of(2026, 9, 1, 0, 0));
        users.saveAndFlush(user);
        FlowerType type = new FlowerType();
        type.setFlowerName("ガーベラ" + UUID.randomUUID().toString().substring(0, 4));
        type.setIllustrationPath("");
        type.setDisplayOrder(1);
        types.saveAndFlush(type);
        flower = new Flower();
        flower.setUser(user);
        flower.setFlowerType(type);
        flower.setFlowerNickname("一時DBの花");
        flower.setStartedOn(today.minusDays(10));
        flower.setStatus(FlowerStatus.ACTIVE);
        flowers.saveAndFlush(flower);
    }

    @Test
    void listsOnlyOwnEndedFlowersInDescendingEndDateOrder() throws Exception {
        Flower old = endedFlower(user, "古い花", today.minusDays(2));
        Flower recent = endedFlower(user, "新しい花", today);
        endedFlower(otherUser(), "他人の秘密の花", today);
        assertThat(service.getEndedFlowers(user.getEmail())).extracting(Flower::getFlowerId)
                .containsExactly(recent.getFlowerId(), old.getFlowerId());
        mvc.perform(get("/garden").with(user(user.getEmail())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("新しい花")))
                .andExpect(content().string(not(containsString("他人の秘密の花"))))
                .andExpect(content().string(not(containsString("一時DBの花"))));
    }

    @Test
    void emptyGardenShowsGuidance() throws Exception {
        MvcResult result = mvc.perform(get("/garden").with(user(user.getEmail())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("まだお花がいません")))
                .andExpect(content().string(containsString("ここに並びます"))).andReturn();
        preview("garden-empty", result);
    }

    @Test
    void selectedFlowerShowsCorrectDatesAndEscapesNickname() throws Exception {
        Flower ended = endedFlower(user, "<思い出の花>", today);
        MvcResult result = mvc.perform(get("/garden/" + ended.getFlowerId()).with(user(user.getEmail())))
                .andExpect(status().isOk()).andExpect(view().name("memory"))
                .andExpect(content().string(containsString("&lt;思い出の花&gt;")))
                .andExpect(content().string(containsString(ended.getFlowerType().getFlowerName())))
                .andExpect(content().string(containsString("2026/09/05")))
                .andExpect(content().string(containsString("2026/09/15")))
                .andExpect(content().string(not(containsString("<form"))))
                .andExpect(content().string(not(containsString("care-records")))).andReturn();
        preview("memory", result);
    }

    @Test
    void anotherUsersFlowerCannotBeOpenedDirectly() throws Exception {
        Flower other = endedFlower(otherUser(), "秘密", today);
        rejected(other.getFlowerId());
    }

    @Test
    void activeFlowerCannotBeOpenedAsMemory() throws Exception {
        rejected(flower.getFlowerId());
    }

    @Test
    void missingFlowerIsHandledSafely() throws Exception {
        rejected(Long.MAX_VALUE);
    }

    @Test
    void anonymousCannotOpenGardenOrMemory() throws Exception {
        mvc.perform(get("/garden")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/garden/" + flower.getFlowerId())).andExpect(redirectedUrl("/login"));
    }

    @Test
    void repeatedGetDoesNotChangeFlowersTasksOrRecords() throws Exception {
        Flower ended = endedFlower(user, "保持する花", today);
        CareTask task = new CareTask();
        task.setFlower(ended);
        task.setCareName("保持する予定");
        task.setIntervalDays(3);
        task.setNextCareDate(today.plusDays(3));
        tasks.saveAndFlush(task);
        CareRecord record = new CareRecord();
        record.setCareTask(task);
        record.setCompletedOn(today);
        records.saveAndFlush(record);
        long flowerCount = flowers.count();
        long taskCount = tasks.count();
        long recordCount = records.count();
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/garden").with(user(user.getEmail()))).andExpect(status().isOk());
            mvc.perform(get("/garden/" + ended.getFlowerId()).with(user(user.getEmail()))).andExpect(status().isOk());
        }
        assertThat(flowers.count()).isEqualTo(flowerCount);
        assertThat(tasks.count()).isEqualTo(taskCount);
        assertThat(records.count()).isEqualTo(recordCount);
        assertThat(flowers.findById(ended.getFlowerId()).orElseThrow())
                .usingRecursiveComparison().isEqualTo(ended);
        assertThat(tasks.findById(task.getCareTaskId()).orElseThrow().getNextCareDate()).isEqualTo(today.plusDays(3));
        assertThat(records.findById(record.getCareRecordId()).orElseThrow().getCompletedOn()).isEqualTo(today);
    }

    @Test
    void homeLinksToGardenAndEachFlowerLinksToItsMemory() throws Exception {
        Flower one = endedFlower(user, "一輪目", today);
        endedFlower(user, "二輪目", today.minusDays(1));
        endedFlower(user, "三輪目", today.minusDays(2));
        endedFlower(user, "四輪目", today.minusDays(3));
        mvc.perform(get("/home").with(user(user.getEmail())))
                .andExpect(content().string(containsString("href=\"/garden\"")));
        MvcResult result = mvc.perform(get("/garden").with(user(user.getEmail())))
                .andExpect(content().string(containsString("href=\"/garden/" + one.getFlowerId() + "\"")))
                .andExpect(content().string(containsString("href=\"/home\""))).andReturn();
        preview("garden", result);
    }

    @Test
    void gardenHasNoPostOperationAndInvalidIdIsRejected() throws Exception {
        mvc.perform(post("/garden").with(user(user.getEmail())).with(csrf())).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/garden/1").with(user(user.getEmail())).with(csrf())).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/garden/invalid").with(user(user.getEmail()))).andExpect(status().isBadRequest());
    }

    private Flower endedFlower(User owner, String nickname, LocalDate endedOn) {
        Flower ended = new Flower();
        ended.setUser(owner);
        ended.setFlowerType(flower.getFlowerType());
        ended.setFlowerNickname(nickname);
        ended.setStartedOn(today.minusDays(10));
        ended.setStatus(FlowerStatus.ENDED);
        ended.setEndedOn(endedOn);
        return flowers.saveAndFlush(ended);
    }

    private User otherUser() {
        User other = new User();
        other.setEmail(UUID.randomUUID() + "@example.com");
        other.setNickname("別の利用者");
        other.setPasswordHash("unused");
        other.setCreatedAt(LocalDateTime.now());
        return users.saveAndFlush(other);
    }

    private void rejected(Long id) throws Exception {
        assertThat(service.getMemoryFlower(user.getEmail(), id)).isEmpty();
        mvc.perform(get("/garden/" + id).with(user(user.getEmail())))
                .andExpect(redirectedUrl("/garden"))
                .andExpect(flash().attribute("gardenError", "このお花の思い出は表示できません。"));
    }

    private void preview(String name, MvcResult result) throws Exception {
        Files.createDirectories(Path.of("target/ui-preview"));
        Files.writeString(Path.of("target/ui-preview/" + name + ".html"), result.getResponse().getContentAsString());
    }
}
