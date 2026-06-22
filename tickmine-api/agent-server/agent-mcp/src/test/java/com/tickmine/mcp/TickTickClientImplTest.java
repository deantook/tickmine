package com.tickmine.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.tickmine.domain.port.TickTickTaskRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class TickTickClientImplTest {

    private MockWebServer server;
    private TickTickClientImpl client;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        meterRegistry = new SimpleMeterRegistry();
        String baseUrl = server.url("/open/v1/").toString().replaceAll("/$", "");
        client = new TickTickClientImpl(WebClient.builder(), meterRegistry, baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void createProject_returnsId() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"proj-1\",\"name\":\"Wedding Plan\"}")
                .addHeader("Content-Type", "application/json"));

        String id = client.createProject("Wedding Plan", "token-abc");

        assertThat(id).isEqualTo("proj-1");
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/open/v1/project");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer token-abc");
        assertThat(meterRegistry.counter("tickmine.ticktick.calls", "op", "createProject").count())
                .isEqualTo(1.0);
    }

    @Test
    void createTask_returnsId() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody("{\"id\":\"task-1\",\"title\":\"Book venue\"}")
                .addHeader("Content-Type", "application/json"));

        TickTickTaskRequest task = new TickTickTaskRequest(
                "Book venue", "proj-1", "Find a suitable venue", 3, null, null, true, "Asia/Shanghai", null, null, null);
        String id = client.createTask(task, "token-abc");

        assertThat(id).isEqualTo("task-1");
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/open/v1/task");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer token-abc");
        assertThat(meterRegistry.counter("tickmine.ticktick.calls", "op", "createTask").count())
                .isEqualTo(1.0);
    }
}
