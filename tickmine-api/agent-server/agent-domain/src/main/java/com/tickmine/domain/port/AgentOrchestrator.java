package com.tickmine.domain.port;

import com.tickmine.domain.model.AgentRunRequest;
import com.tickmine.domain.model.AgentRunResult;
import java.util.function.Consumer;

public interface AgentOrchestrator {

    AgentRunResult run(AgentRunRequest request);

    void streamRun(AgentRunRequest request, Consumer<String> onDelta);
}
