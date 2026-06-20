package com.pact.goal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoalConfig {

    @Bean
    public StreakCalculator streakCalculator() {
        return new StreakCalculator();
    }
}