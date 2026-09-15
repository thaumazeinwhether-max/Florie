package com.florie.service;

import com.florie.entity.CareTask;
import com.florie.entity.CareRecord;
import com.florie.repository.CareRecordRepository;
import com.florie.entity.CareTemplate;
import com.florie.entity.Flower;
import com.florie.entity.FlowerStatus;
import com.florie.entity.User;
import com.florie.repository.CareTaskRepository;
import com.florie.repository.CareTemplateRepository;
import com.florie.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareService {
    private final UserRepository userRepository;
    private final CareTaskRepository careTaskRepository;
    private final CareTemplateRepository careTemplateRepository;
    private final Clock clock;
    private final CareRecordRepository careRecordRepository;

    public CareService(UserRepository userRepository, CareTaskRepository careTaskRepository,
                       CareTemplateRepository careTemplateRepository, Clock clock,
                       CareRecordRepository careRecordRepository) {
        this.userRepository = userRepository;
        this.careTaskRepository = careTaskRepository;
        this.careTemplateRepository = careTemplateRepository;
        this.clock = clock;
        this.careRecordRepository = careRecordRepository;
    }

    public LocalDate getToday() {
        return LocalDate.now(clock);
    }

    @Transactional
    public void completeCare(String email, Long taskId) {
        // 花登録と同じ順序でユーザーをロックし、同じ人の更新を順番に行う。
        User user = userRepository.findByEmailForUpdate(UserService.normalizeEmail(email))
                .orElseThrow(() -> new CareCompletionException("お世話を完了できませんでした。"));
        CareTask task = careTaskRepository.findOwnedTaskForUpdate(taskId, user.getUserId())
                .orElseThrow(() -> new CareCompletionException("対象のお世話を完了できません。ホームを確認してください。"));

        // ロック待ちの間に日付が変わった場合も、処理時点の日本時間を使う。
        LocalDate completedOn = getToday();
        if (task.getFlower().getStatus() != FlowerStatus.ACTIVE) {
            throw new CareCompletionException("終了したお花のお世話は完了できません。");
        }
        if (task.getNextCareDate().isAfter(completedOn)) {
            throw new CareCompletionException("このお世話は完了済み、またはまだ予定日前です。");
        }
        if (task.getIntervalDays() == null || task.getIntervalDays() < 1) {
            throw new CareCompletionException("お世話の周期を確認できないため、完了できません。");
        }

        LocalDate nextCareDate = completedOn.plusDays(task.getIntervalDays());
        CareRecord record = new CareRecord();
        record.setCareTask(task);
        record.setCompletedOn(completedOn);
        careRecordRepository.saveAndFlush(record);

        task.setNextCareDate(nextCareDate);
        careTaskRepository.saveAndFlush(task);
        // 途中で失敗した例外は外へ返し、履歴INSERTと予定UPDATEを両方取り消す。
    }

    @Transactional(readOnly = true)
    public List<CareTask> getDueTasks(String email, LocalDate today) {
        User user = userRepository.findByEmail(UserService.normalizeEmail(email))
                .orElseThrow(() -> new IllegalStateException("ユーザーが見つかりません。"));
        // 日数分の予定を生成せず、DBにある予定を1件ずつ取得する。
        return careTaskRepository.findDueTasks(user.getUserId(), FlowerStatus.ACTIVE, today);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getCareDescriptions(Flower flower) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        List<CareTemplate> templates = careTemplateRepository
                .findByFlowerTypeFlowerTypeIdOrderByDisplayOrderAsc(flower.getFlowerType().getFlowerTypeId());
        for (CareTemplate template : templates) {
            descriptions.put(template.getCareName(), template.getCareDescription());
        }
        return descriptions;
    }
}
