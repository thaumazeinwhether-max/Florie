package com.florie.service;

import com.florie.entity.CareTask;
import com.florie.entity.CareTemplate;
import com.florie.entity.Flower;
import com.florie.entity.FlowerStatus;
import com.florie.entity.FlowerType;
import com.florie.entity.User;
import com.florie.form.FlowerRegisterForm;
import com.florie.repository.CareTaskRepository;
import com.florie.repository.CareTemplateRepository;
import com.florie.repository.FlowerRepository;
import com.florie.repository.FlowerTypeRepository;
import com.florie.repository.UserRepository;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlowerService {
    private final UserRepository userRepository;
    private final FlowerRepository flowerRepository;
    private final FlowerTypeRepository flowerTypeRepository;
    private final CareTemplateRepository careTemplateRepository;
    private final CareTaskRepository careTaskRepository;
    private final Validator validator;
    private final Clock clock;

    public FlowerService(UserRepository userRepository, FlowerRepository flowerRepository,
                         FlowerTypeRepository flowerTypeRepository, CareTemplateRepository careTemplateRepository,
                         CareTaskRepository careTaskRepository, Validator validator, Clock clock) {
        this.userRepository = userRepository;
        this.flowerRepository = flowerRepository;
        this.flowerTypeRepository = flowerTypeRepository;
        this.careTemplateRepository = careTemplateRepository;
        this.careTaskRepository = careTaskRepository;
        this.validator = validator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FlowerType> getFlowerTypes() {
        return flowerTypeRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Flower> getCurrentFlower(String email) {
        User user = userRepository.findByEmail(UserService.normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません。"));
        return flowerRepository.findByUserUserIdAndStatus(user.getUserId(), FlowerStatus.ACTIVE);
    }

    @Transactional
    public void endFlower(String email, Long flowerId) {
        // 登録・お世話完了と同じ順番でロックし、同じ利用者の更新を順番に処理する。
        User user = userRepository.findByEmailForUpdate(UserService.normalizeEmail(email))
                .orElseThrow(() -> new FlowerEndException("お別れできるお花が見つかりません。"));
        Flower flower = flowerRepository.findByFlowerIdAndUserUserId(flowerId, user.getUserId())
                .orElseThrow(() -> new FlowerEndException("お別れできるお花が見つかりません。"));
        checkActiveFlower(flower);
        flower.setStatus(FlowerStatus.ENDED);
        flower.setEndedOn(LocalDate.now(clock));
        // 花と関連する予定・履歴は残し、状態と終了日だけを同時に保存する。
        flowerRepository.saveAndFlush(flower);
    }

    @Transactional(readOnly = true)
    public Flower getFlowerForEnd(String email, Long flowerId) {
        User user = userRepository.findByEmail(UserService.normalizeEmail(email))
                .orElseThrow(() -> new FlowerEndException("お別れできるお花が見つかりません。"));
        Flower flower = flowerRepository.findByFlowerIdAndUserUserId(flowerId, user.getUserId())
                .orElseThrow(() -> new FlowerEndException("お別れできるお花が見つかりません。"));
        checkActiveFlower(flower);
        return flower;
    }

    private void checkActiveFlower(Flower flower) {
        if (flower.getStatus() != FlowerStatus.ACTIVE) {
            throw new FlowerEndException("このお花とのお別れは完了しています。");
        }
    }

    @Transactional
    public void registerFlower(String email, FlowerRegisterForm form) {
        if (!validator.validate(form).isEmpty()) {
            throw new FlowerRegistrationException("入力内容を確認してください。");
        }

        // このトランザクションの最初のDB操作でロックする。
        // 後から届いた登録は先の登録完了を待ち、保存済みのACTIVEを確認する。
        User user = userRepository.findByEmailForUpdate(UserService.normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません。"));
        if (flowerRepository.existsByUserUserIdAndStatus(user.getUserId(), FlowerStatus.ACTIVE)) {
            throw new FlowerRegistrationException("お世話中のお花がいるため、新しいお花は登録できません。");
        }

        FlowerType flowerType = flowerTypeRepository.findById(form.getFlowerTypeId())
                .orElseThrow(() -> new FlowerRegistrationException("一覧からお花を選び直してください。"));
        List<CareTemplate> templates = careTemplateRepository
                .findByFlowerTypeFlowerTypeIdOrderByDisplayOrderAsc(flowerType.getFlowerTypeId());
        validateCareTemplates(templates);

        // 日付が途中で変わっても、花と2件の予定は同じ登録日を基準にする。
        LocalDate registeredOn = LocalDate.now(clock);
        Flower flower = new Flower();
        flower.setUser(user);
        flower.setFlowerType(flowerType);
        flower.setFlowerNickname(form.getFlowerNickname());
        flower.setStartedOn(registeredOn);
        flower.setStatus(FlowerStatus.ACTIVE);
        flowerRepository.save(flower);

        for (CareTemplate template : templates) {
            CareTask task = new CareTask();
            task.setFlower(flower);
            task.setCareName(template.getCareName());
            task.setIntervalDays(template.getIntervalDays());
            task.setNextCareDate(registeredOn.plusDays(template.getIntervalDays()));
            careTaskRepository.save(task);
        }
        // DB制約違反もこの処理内で検知し、花を含めてロールバックする。
        careTaskRepository.flush();
    }

    private void validateCareTemplates(List<CareTemplate> templates) {
        boolean hasWaterCare = false;
        boolean hasStemCare = false;
        for (CareTemplate template : templates) {
            if ("水を替える".equals(template.getCareName()) && Integer.valueOf(1).equals(template.getIntervalDays())) {
                hasWaterCare = true;
            }
            if ("茎を確認して整える".equals(template.getCareName()) && Integer.valueOf(3).equals(template.getIntervalDays())) {
                hasStemCare = true;
            }
            if (template.getCareDescription() == null || template.getCareDescription().isBlank()) {
                throw new FlowerRegistrationException("お世話の設定が揃っていないため、今は登録できません。");
            }
        }
        // 不足した設定を推測で補わず、確定した2件がある場合だけ登録する。
        if (templates.size() != 2 || !hasWaterCare || !hasStemCare) {
            throw new FlowerRegistrationException("お世話の設定が揃っていないため、今は登録できません。");
        }
    }
}
