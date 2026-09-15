package com.florie.service;

import com.florie.entity.*;
import com.florie.repository.*;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
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
        "spring.datasource.url=jdbc:h2:mem:care-completion-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never"
})
class CareCompletionTest {
    @Autowired private CareService service;
    @Autowired private UserRepository users;
    @Autowired private FlowerTypeRepository types;
    @Autowired private FlowerRepository flowers;
    @Autowired private CareRecordRepository records;
    @MockitoSpyBean private CareTaskRepository tasks;
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
    void completesTodayAndSavesOneDateRecordAndDisappearsFromList() {
        CareTask task = createTask(today, 1);
        service.completeCare(user.getEmail(), task.getCareTaskId());
        assertThat(recordsFor(task)).hasSize(1);
        assertThat(recordsFor(task).getFirst().getCompletedOn()).isEqualTo(today);
        assertThat(reload(task).getNextCareDate()).isEqualTo(today.plusDays(1));
        assertThat(service.getDueTasks(user.getEmail(), today)).isEmpty();
    }

    @Test
    void overdueTaskUsesCompletionDateAndCreatesOnlyOneRecord() {
        CareTask task = createTask(today.minusDays(8), 3);
        service.completeCare(user.getEmail(), task.getCareTaskId());
        assertThat(recordsFor(task)).hasSize(1);
        assertThat(reload(task).getNextCareDate()).isEqualTo(LocalDate.of(2026, 9, 18));
    }

    @Test
    void rejectsFutureTask() {
        CareTask task = createTask(today.plusDays(1), 1);
        assertRejected(task, user.getEmail());
    }

    @Test
    void rejectsAnotherUsersTaskEvenWithCorrectId() {
        CareTask task = createTask(today, 1);
        User other = new User();
        other.setEmail(UUID.randomUUID() + "@example.com");
        other.setNickname("別の利用者");
        other.setPasswordHash("unused");
        other.setCreatedAt(LocalDateTime.now());
        users.saveAndFlush(other);
        assertRejected(task, other.getEmail());
    }

    @Test
    void rejectsEndedFlower() {
        flower.setStatus(FlowerStatus.ENDED);
        flower.setEndedOn(today);
        flowers.saveAndFlush(flower);
        assertRejected(createTask(today, 1), user.getEmail());
    }

    @Test
    void rejectsMissingTaskId() {
        long before = records.count();
        assertThatThrownBy(() -> service.completeCare(user.getEmail(), Long.MAX_VALUE))
                .isInstanceOf(CareCompletionException.class);
        assertThat(records.count()).isEqualTo(before);
    }

    @Test
    void repeatedPostDoesNotCreateSecondRecord() {
        CareTask task = createTask(today, 1);
        service.completeCare(user.getEmail(), task.getCareTaskId());
        assertThatThrownBy(() -> service.completeCare(user.getEmail(), task.getCareTaskId()))
                .isInstanceOf(CareCompletionException.class);
        assertThat(recordsFor(task)).hasSize(1);
        assertThat(reload(task).getNextCareDate()).isEqualTo(today.plusDays(1));
    }

    @Test
    void failureAfterHistoryInsertRollsBackBothChanges() {
        CareTask task = createTask(today.minusDays(1), 3);
        // 履歴INSERTを実DBへflushした後、次の保存で失敗させる（H2だけ）。
        doThrow(new DataIntegrityViolationException("test update failure"))
                .when(tasks).saveAndFlush(any(CareTask.class));
        assertThatThrownBy(() -> service.completeCare(user.getEmail(), task.getCareTaskId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(recordsFor(task)).isEmpty();
        assertThat(reload(task).getNextCareDate()).isEqualTo(task.getNextCareDate());
    }

    @Test
    void simultaneousRequestsCreateOnlyOneRecord() throws Exception {
        CareTask task = createTask(today, 1);
        // 同時送信を再現するため、このテストだけ2スレッドを使う。アプリには並列処理を追加しない。
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> request = () -> {
            start.await();
            try {
                service.completeCare(user.getEmail(), task.getCareTaskId());
                return true;
            } catch (CareCompletionException exception) {
                return false;
            }
        };
        try {
            Future<Boolean> first = executor.submit(request);
            Future<Boolean> second = executor.submit(request);
            start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(recordsFor(task)).hasSize(1);
            assertThat(reload(task).getNextCareDate()).isEqualTo(today.plusDays(1));
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertRejected(CareTask task, String email) {
        assertThatThrownBy(() -> service.completeCare(email, task.getCareTaskId()))
                .isInstanceOf(CareCompletionException.class);
        assertThat(recordsFor(task)).isEmpty();
        assertThat(reload(task).getNextCareDate()).isEqualTo(task.getNextCareDate());
    }

    private CareTask createTask(LocalDate date, int interval) {
        CareTask task = new CareTask();
        task.setFlower(flower);
        task.setCareName("テストのお世話");
        task.setIntervalDays(interval);
        task.setNextCareDate(date);
        return tasks.saveAndFlush(task);
    }

    private CareTask reload(CareTask task) {
        return tasks.findById(task.getCareTaskId()).orElseThrow();
    }

    private List<CareRecord> recordsFor(CareTask task) {
        List<CareRecord> result = new ArrayList<>();
        for (CareRecord record : records.findAll()) {
            if (record.getCareTask().getCareTaskId().equals(task.getCareTaskId())) {
                result.add(record);
            }
        }
        return result;
    }
}
