package com.florie.service;

import com.florie.entity.CareTask;
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

    public CareService(UserRepository userRepository, CareTaskRepository careTaskRepository,
                       CareTemplateRepository careTemplateRepository, Clock clock) {
        this.userRepository = userRepository;
        this.careTaskRepository = careTaskRepository;
        this.careTemplateRepository = careTemplateRepository;
        this.clock = clock;
    }

    public LocalDate getToday() {
        return LocalDate.now(clock);
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
