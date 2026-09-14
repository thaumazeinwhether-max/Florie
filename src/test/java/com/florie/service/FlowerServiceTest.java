package com.florie.service;

import com.florie.entity.*;
import com.florie.form.FlowerRegisterForm;
import com.florie.repository.*;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlowerServiceTest {
    private UserRepository users;
    private FlowerRepository flowers;
    private FlowerTypeRepository types;
    private CareTemplateRepository templates;
    private CareTaskRepository tasks;
    private ValidatorFactory validators;
    private FlowerService service;
    private User user;
    private FlowerType type;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        flowers = mock(FlowerRepository.class);
        types = mock(FlowerTypeRepository.class);
        templates = mock(CareTemplateRepository.class);
        tasks = mock(CareTaskRepository.class);
        validators = Validation.buildDefaultValidatorFactory();
        // UTCでは9月30日でも、日本では10月1日。日付境界も固定して確認する。
        Clock clock = Clock.fixed(Instant.parse("2026-09-30T15:30:00Z"), ZoneId.of("Asia/Tokyo"));
        service = new FlowerService(users, flowers, types, templates, tasks, validators.getValidator(), clock);
        user = mock(User.class);
        when(user.getUserId()).thenReturn(42L);
        when(users.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(user));
        type = mock(FlowerType.class);
        when(type.getFlowerTypeId()).thenReturn(15L);
        when(types.findById(15L)).thenReturn(Optional.of(type));
        when(templates.findByFlowerTypeFlowerTypeIdOrderByDisplayOrderAsc(15L))
                .thenReturn(List.of(template("水を替える", 1), template("茎を確認して整える", 3)));
    }

    @AfterEach
    void closeValidator() {
        validators.close();
    }

    @Test
    void registrationSavesCurrentUserFlowerAndTwoFutureTasks() {
        service.registerFlower("test@example.com", form());
        ArgumentCaptor<Flower> flowerArgument = ArgumentCaptor.forClass(Flower.class);
        verify(flowers).save(flowerArgument.capture());
        Flower saved = flowerArgument.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getFlowerType()).isSameAs(type);
        assertThat(saved.getFlowerNickname()).isEqualTo("はなちゃん");
        assertThat(saved.getStatus()).isEqualTo(FlowerStatus.ACTIVE);
        assertThat(saved.getEndedOn()).isNull();
        assertThat(saved.getStartedOn()).isEqualTo(LocalDate.of(2026, 10, 1));

        ArgumentCaptor<CareTask> taskArgument = ArgumentCaptor.forClass(CareTask.class);
        verify(tasks, times(2)).save(taskArgument.capture());
        CareTask water = taskArgument.getAllValues().get(0);
        CareTask stem = taskArgument.getAllValues().get(1);
        assertThat(water.getFlower()).isSameAs(saved);
        assertThat(stem.getFlower()).isSameAs(saved);
        assertThat(water.getCareName()).isEqualTo("水を替える");
        assertThat(stem.getCareName()).isEqualTo("茎を確認して整える");
        assertThat(water.getIntervalDays()).isEqualTo(1);
        assertThat(stem.getIntervalDays()).isEqualTo(3);
        assertThat(water.getNextCareDate()).isEqualTo(saved.getStartedOn().plusDays(1));
        assertThat(stem.getNextCareDate()).isEqualTo(saved.getStartedOn().plusDays(3));

        InOrder order = inOrder(users, flowers, tasks);
        order.verify(users).findByEmailForUpdate("test@example.com");
        order.verify(flowers).existsByUserUserIdAndStatus(42L, FlowerStatus.ACTIVE);
        order.verify(flowers).save(saved);
        order.verify(tasks, times(2)).save(any(CareTask.class));
        order.verify(tasks).flush();
    }

    @Test
    void blankNicknameIsRejected() {
        FlowerRegisterForm form = form();
        form.setFlowerNickname("　 ");
        assertThatThrownBy(() -> service.registerFlower("test@example.com", form))
                .isInstanceOf(FlowerRegistrationException.class);
        verifyNoInteractions(users, flowers, tasks);
    }

    @Test
    void nicknameOverTwentyCharactersIsRejected() {
        FlowerRegisterForm form = form();
        form.setFlowerNickname("あ".repeat(21));
        assertThatThrownBy(() -> service.registerFlower("test@example.com", form))
                .isInstanceOf(FlowerRegistrationException.class);
        verifyNoInteractions(users, flowers, tasks);
    }

    @Test
    void twentyCharacterNicknameIsAccepted() {
        FlowerRegisterForm form = form();
        form.setFlowerNickname("あ".repeat(20));
        service.registerFlower("test@example.com", form);
        verify(flowers).save(any(Flower.class));
    }

    @Test
    void missingFlowerTypeIsRejected() {
        FlowerRegisterForm form = form();
        form.setFlowerTypeId(null);
        assertThatThrownBy(() -> service.registerFlower("test@example.com", form))
                .isInstanceOf(FlowerRegistrationException.class);
        verifyNoInteractions(users, flowers, tasks);
    }

    @Test
    void nonexistentFlowerTypeIsRejected() {
        FlowerRegisterForm form = form();
        form.setFlowerTypeId(999L);
        assertThatThrownBy(() -> service.registerFlower("test@example.com", form))
                .isInstanceOf(FlowerRegistrationException.class);
        verify(flowers, never()).save(any());
        verifyNoInteractions(tasks);
    }

    @Test
    void secondSubmissionIsRejectedAfterLockingUserAgain() {
        when(flowers.existsByUserUserIdAndStatus(42L, FlowerStatus.ACTIVE)).thenReturn(false, true);
        service.registerFlower("test@example.com", form());
        assertThatThrownBy(() -> service.registerFlower("test@example.com", form()))
                .isInstanceOf(FlowerRegistrationException.class).hasMessageContaining("お世話中");
        verify(users, times(2)).findByEmailForUpdate("test@example.com");
        verify(flowers, times(1)).save(any());
        verify(tasks, times(2)).save(any());
    }

    @Test
    void existingActiveFlowerPreventsAllInserts() {
        when(flowers.existsByUserUserIdAndStatus(42L, FlowerStatus.ACTIVE)).thenReturn(true);
        assertThatThrownBy(() -> service.registerFlower("test@example.com", form()))
                .isInstanceOf(FlowerRegistrationException.class);
        verify(flowers, never()).save(any());
        verifyNoInteractions(tasks);
    }

    @Test
    void missingOrIncorrectCareMasterPreventsFlowerInsert() {
        when(templates.findByFlowerTypeFlowerTypeIdOrderByDisplayOrderAsc(15L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.registerFlower("test@example.com", form()))
                .isInstanceOf(FlowerRegistrationException.class);
        when(templates.findByFlowerTypeFlowerTypeIdOrderByDisplayOrderAsc(15L))
                .thenReturn(List.of(template("水を替える", 1), template("茎を確認して整える", 1)));
        assertThatThrownBy(() -> service.registerFlower("test@example.com", form()))
                .isInstanceOf(FlowerRegistrationException.class);
        verify(flowers, never()).save(any());
        verifyNoInteractions(tasks);
    }

    @Test
    void databaseFailureIsPropagatedForTransactionRollback() {
        doThrow(new DataIntegrityViolationException("test failure")).when(tasks).flush();
        assertThatThrownBy(() -> service.registerFlower("test@example.com", form()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void homeSearchUsesOnlyLoggedInUserAndActiveStatus() {
        when(users.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Flower ownFlower = new Flower();
        when(flowers.findByUserUserIdAndStatus(42L, FlowerStatus.ACTIVE))
                .thenReturn(Optional.of(ownFlower));

        assertThat(service.getCurrentFlower("test@example.com")).contains(ownFlower);
        verify(flowers).findByUserUserIdAndStatus(42L, FlowerStatus.ACTIVE);
        verifyNoInteractions(tasks);
    }

    private FlowerRegisterForm form() {
        FlowerRegisterForm form = new FlowerRegisterForm();
        form.setFlowerTypeId(15L);
        form.setFlowerNickname("はなちゃん");
        return form;
    }

    private CareTemplate template(String name, int interval) {
        CareTemplate template = new CareTemplate();
        template.setCareName(name);
        template.setIntervalDays(interval);
        template.setCareDescription("テスト用のお世話説明");
        return template;
    }
}
