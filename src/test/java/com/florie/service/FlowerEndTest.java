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

// 実MySQLには接続しない。テスト自体を@Transactionalにせず、Serviceのコミット・ロールバックを確認する。
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flower-end-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
class FlowerEndTest {
    @Autowired private FlowerService service;
    @Autowired private CareService cares;
    @Autowired private CareTemplateRepository templates;
    @Autowired private UserRepository users;
    @Autowired private FlowerTypeRepository types;
    @MockitoSpyBean private FlowerRepository flowers;
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
        type.setFlowerName(UUID.randomUUID().toString());
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
    void confirmationOnlyReadsAndDoesNotEndFlower() {
        Flower shown = service.getFlowerForEnd(user.getEmail(), flower.getFlowerId());
        assertThat(shown.getFlowerId()).isEqualTo(flower.getFlowerId());
        assertThat(reload().getStatus()).isEqualTo(FlowerStatus.ACTIVE);
        assertThat(reload().getEndedOn()).isNull();
    }

    @Test
    void endsOnTokyoTodayAndPreservesTasksAndRecords() {
        CareTask task = new CareTask();
        task.setFlower(flower);
        task.setCareName("水を替える");
        task.setIntervalDays(1);
        task.setNextCareDate(today.minusDays(1));
        tasks.saveAndFlush(task);
        CareRecord record = new CareRecord();
        record.setCareTask(task);
        record.setCompletedOn(today.minusDays(2));
        records.saveAndFlush(record);
        service.endFlower(user.getEmail(), flower.getFlowerId());
        assertThat(reload().getStatus()).isEqualTo(FlowerStatus.ENDED);
        assertThat(reload().getEndedOn()).isEqualTo(today);
        assertThat(reload().getStartedOn()).isEqualTo(flower.getStartedOn());
        assertThat(tasks.findByFlowerFlowerId(flower.getFlowerId())).hasSize(1);
        assertThat(tasks.findById(task.getCareTaskId()).orElseThrow().getNextCareDate()).isEqualTo(today.minusDays(1));
        assertThat(records.findById(record.getCareRecordId()).orElseThrow().getCompletedOn()).isEqualTo(today.minusDays(2));
        assertThat(service.getCurrentFlower(user.getEmail())).isEmpty();
        assertThat(cares.getDueTasks(user.getEmail(), today)).isEmpty();
        assertThatThrownBy(() -> cares.completeCare(user.getEmail(), task.getCareTaskId())).isInstanceOf(CareCompletionException.class);
    }

    @Test
    void canRegisterAnotherFlowerAfterEnding() {
        for (int interval : new int[]{1, 3}) {
            CareTemplate template = new CareTemplate();
            template.setFlowerType(flower.getFlowerType());
            template.setCareName(interval == 1 ? "水を替える" : "茎を確認して整える");
            template.setIntervalDays(interval);
            template.setDisplayOrder(interval);
            template.setCareDescription("一時DBの説明");
            templates.saveAndFlush(template);
        }
        service.endFlower(user.getEmail(), flower.getFlowerId());
        com.florie.form.FlowerRegisterForm form = new com.florie.form.FlowerRegisterForm();
        form.setFlowerTypeId(flower.getFlowerType().getFlowerTypeId());
        form.setFlowerNickname("新しい一輪");
        service.registerFlower(user.getEmail(), form);
        Flower current = service.getCurrentFlower(user.getEmail()).orElseThrow();
        assertThat(current.getFlowerId()).isNotEqualTo(flower.getFlowerId());
        assertThat(current.getStatus()).isEqualTo(FlowerStatus.ACTIVE);
        assertThat(tasks.findByFlowerFlowerId(current.getFlowerId())).hasSize(2);
        assertThat(reload().getStatus()).isEqualTo(FlowerStatus.ENDED);
    }

    @Test
    void anotherUserCannotReadOrEndFlower() {
        User other = new User();
        other.setEmail(UUID.randomUUID() + "@example.com");
        other.setNickname("別の利用者");
        other.setPasswordHash("unused");
        other.setCreatedAt(LocalDateTime.now());
        users.saveAndFlush(other);
        assertThatThrownBy(() -> service.getFlowerForEnd(other.getEmail(), flower.getFlowerId())).isInstanceOf(FlowerEndException.class);
        assertThatThrownBy(() -> service.endFlower(other.getEmail(), flower.getFlowerId())).isInstanceOf(FlowerEndException.class);
        assertThat(reload().getStatus()).isEqualTo(FlowerStatus.ACTIVE);
        assertThat(reload().getEndedOn()).isNull();
    }

    @Test
    void repeatedEndDoesNotOverwriteOriginalEndDate() {
        service.endFlower(user.getEmail(), flower.getFlowerId());
        when(clock.instant()).thenReturn(Instant.parse("2026-09-16T15:30:00Z"));
        assertThatThrownBy(() -> service.endFlower(user.getEmail(), flower.getFlowerId())).isInstanceOf(FlowerEndException.class);
        assertThatThrownBy(() -> service.getFlowerForEnd(user.getEmail(), flower.getFlowerId())).isInstanceOf(FlowerEndException.class);
        assertThat(reload().getEndedOn()).isEqualTo(today);
    }

    @Test
    void missingFlowerIsRejected() {
        assertThatThrownBy(() -> service.endFlower(user.getEmail(), Long.MAX_VALUE)).isInstanceOf(FlowerEndException.class);
        assertThatThrownBy(() -> service.getFlowerForEnd(user.getEmail(), Long.MAX_VALUE)).isInstanceOf(FlowerEndException.class);
        assertThat(reload().getStatus()).isEqualTo(FlowerStatus.ACTIVE);
    }

    @Test
    void saveFailureRollsBackStatusAndEndDate() {
        doThrow(new DataIntegrityViolationException("test save failure")).when(flowers).saveAndFlush(any(Flower.class));
        assertThatThrownBy(() -> service.endFlower(user.getEmail(), flower.getFlowerId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(reload().getStatus()).isEqualTo(FlowerStatus.ACTIVE);
        assertThat(reload().getEndedOn()).isNull();
    }

    private Flower reload() {
        return flowers.findById(flower.getFlowerId()).orElseThrow();
    }
}
