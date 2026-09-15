package com.florie.service;

import com.florie.entity.*;
import com.florie.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// 一時的なメモリ内DBだけで実際のRepository検索を検証する。
// create-dropはこのテストだけに限定し、florieのMySQL・マスタには触れない。
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:care-list-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
@Transactional
class CareServiceTest {
    @Autowired private CareService service;
    @Autowired private UserRepository users;
    @Autowired private FlowerTypeRepository types;
    @Autowired private FlowerRepository flowers;
    @Autowired private CareTaskRepository tasks;
    @Autowired private CareTemplateRepository templates;
    @MockitoBean private Clock clock;
    private User ownUser;
    private FlowerType type;
    private final LocalDate today = LocalDate.of(2026, 10, 1);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-09-30T15:30:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Tokyo"));
        ownUser = createUser("own@example.com");
        type = new FlowerType();
        type.setFlowerName("テスト用の花");
        type.setIllustrationPath("");
        type.setDisplayOrder(1);
        types.saveAndFlush(type);
    }

    @Test
    void todayAndOverdueAreReturnedOnceButFutureOtherUserAndEndedAreExcluded() {
        Flower own = createFlower(ownUser, FlowerStatus.ACTIVE);
        CareTask dueToday = createTask(own, "今日", today);
        CareTask overdue = createTask(own, "一週間前", today.minusDays(7));
        createTask(own, "未来", today.plusDays(1));
        createTask(createFlower(createUser("other@example.com"), FlowerStatus.ACTIVE), "他ユーザー", today);
        createTask(createFlower(ownUser, FlowerStatus.ENDED), "終了済み", today.minusDays(1));

        List<CareTask> result = service.getDueTasks("own@example.com", service.getToday());
        assertThat(result).extracting(CareTask::getCareTaskId)
                .containsExactly(overdue.getCareTaskId(), dueToday.getCareTaskId());
        assertThat(tasks.count()).isEqualTo(5); // 閲覧による増減・日数分の複製がない。
        assertThat(overdue.getNextCareDate()).isEqualTo(today.minusDays(7));
    }

    @Test
    void noActiveFlowerReturnsEmptyEvenIfEndedTasksExist() {
        createTask(createFlower(ownUser, FlowerStatus.ENDED), "終了した予定", today);
        assertThat(service.getDueTasks("own@example.com", today)).isEmpty();
    }

    @Test
    void activeFlowerWithOnlyFutureTasksReturnsEmpty() {
        createTask(createFlower(ownUser, FlowerStatus.ACTIVE), "明日", today.plusDays(1));
        assertThat(service.getDueTasks("own@example.com", today)).isEmpty();
    }

    @Test
    void japanDateAndSameDateIdOrderAreStable() {
        assertThat(service.getToday()).isEqualTo(today);
        Flower own = createFlower(ownUser, FlowerStatus.ACTIVE);
        CareTask first = createTask(own, "一つ目", today);
        CareTask second = createTask(own, "二つ目", today);
        assertThat(service.getDueTasks("own@example.com", today)).extracting(CareTask::getCareTaskId)
                .containsExactly(first.getCareTaskId(), second.getCareTaskId());
    }

    @Test
    void descriptionsComeFromTheFlowersMaster() {
        Flower own = createFlower(ownUser, FlowerStatus.ACTIVE);
        CareTemplate template = new CareTemplate();
        template.setFlowerType(type);
        template.setCareName("水を替える");
        template.setIntervalDays(1);
        template.setDisplayOrder(1);
        template.setCareDescription("この花のお世話説明");
        templates.saveAndFlush(template);
        assertThat(service.getCareDescriptions(own)).containsEntry("水を替える", "この花のお世話説明");
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setNickname("テスト利用者");
        user.setPasswordHash("not-used-for-authentication");
        user.setCreatedAt(LocalDateTime.of(2026, 9, 1, 0, 0));
        return users.saveAndFlush(user);
    }

    private Flower createFlower(User user, FlowerStatus status) {
        Flower flower = new Flower();
        flower.setUser(user);
        flower.setFlowerType(type);
        flower.setFlowerNickname("テストの一輪");
        flower.setStartedOn(today.minusDays(10));
        flower.setStatus(status);
        if (status == FlowerStatus.ENDED) {
            flower.setEndedOn(today.minusDays(1));
        }
        return flowers.saveAndFlush(flower);
    }

    private CareTask createTask(Flower flower, String name, LocalDate date) {
        CareTask task = new CareTask();
        task.setFlower(flower);
        task.setCareName(name);
        task.setIntervalDays(1);
        task.setNextCareDate(date);
        return tasks.saveAndFlush(task);
    }
}
