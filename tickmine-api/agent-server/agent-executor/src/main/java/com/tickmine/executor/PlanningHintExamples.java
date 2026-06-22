package com.tickmine.executor;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class PlanningHintExamples {

    static final List<String> EXAMPLES = List.of(
            "帮我策划一场婚礼",
            "帮我规划一场毕业典礼",
            "帮我拆分需求文档进行工作安排",
            "帮我规划一次周末短途旅行",
            "下午三点去大润发买菜",
            "帮我制定两周的备考计划",
            "帮我安排明天的工作重点",
            "帮我把今天的事理一理");

    private PlanningHintExamples() {}

    static String randomQuoted() {
        return "「" + EXAMPLES.get(ThreadLocalRandom.current().nextInt(EXAMPLES.size())) + "」";
    }
}
