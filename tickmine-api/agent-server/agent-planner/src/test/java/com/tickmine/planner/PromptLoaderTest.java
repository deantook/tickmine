package com.tickmine.planner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

class PromptLoaderTest {

    private PromptLoader promptLoader;

    @BeforeEach
    void setUp() {
        promptLoader = new PromptLoader(new DefaultResourceLoader());
    }

    @Test
    void load_goalAnalyzer_rendersVariables() {
        String rendered = promptLoader.load("goal-analyzer.st", Map.of(
                "title", "策划婚礼",
                "description", "2026年秋季上海婚礼",
                "attributes", "city: 上海\nguestCount: 150",
                "todayDate", "2026-06-22",
                "conversation", "user: 我们想在秋天办婚礼"));

        assertThat(rendered).contains("策划婚礼");
        assertThat(rendered).contains("2026年秋季上海婚礼");
        assertThat(rendered).contains("city: 上海");
        assertThat(rendered).contains("user: 我们想在秋天办婚礼");
    }

    @Test
    void load_planner_rendersTargetDate() {
        String rendered = promptLoader.load("planner.st", Map.of(
                "title", "蜜月旅行",
                "description", "日本七日游",
                "attributes", "budget: 3万",
                "targetDate", "2026-10-01",
                "todayDate", "2026-06-22"));

        assertThat(rendered).contains("蜜月旅行");
        assertThat(rendered).contains("2026-10-01");
        assertThat(rendered).contains("budget: 3万");
    }

    @Test
    void load_followUp_rendersMissingFields() {
        String rendered = promptLoader.load("follow-up.st", Map.of(
                "title", "策划婚礼",
                "missingFields", "[budget, guestCount]",
                "attributes", "city: 上海"));

        assertThat(rendered).contains("策划婚礼");
        assertThat(rendered).contains("[budget, guestCount]");
        assertThat(rendered).contains("city: 上海");
    }
}
