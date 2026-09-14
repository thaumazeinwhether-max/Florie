package com.florie.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DateTimeConfig {
    @Bean
    public Clock applicationClock() {
        // サーバーの設置場所にかかわらず、日本時間の日付で花の登録日を決める。
        return Clock.system(ZoneId.of("Asia/Tokyo"));
    }
}
