package com.tickmine.executor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;

class PlanningHintExamplesTest {

    @RepeatedTest(20)
    void randomQuoted_returnsKnownExampleInQuotes() {
        String quoted = PlanningHintExamples.randomQuoted();

        assertThat(quoted).startsWith("「").endsWith("」");
        String inner = quoted.substring(1, quoted.length() - 1);
        assertThat(PlanningHintExamples.EXAMPLES).contains(inner);
    }
}
