package com.tickmine.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tickmine.domain.port.TickTickClient;
import com.tickmine.domain.port.TickTickProjectResponse;
import com.tickmine.domain.port.TickTickTaskResponse;
import com.tickmine.infra.service.UserService;
import com.tickmine.domain.util.TickTickDates;
import com.tickmine.mcp.exception.TickTickApiException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TickTickTaskQueryServiceTest {

    @Mock
    private TickTickClient tickTickClient;

    @Mock
    private UserService userService;

    @InjectMocks
    private TickTickTaskQueryService taskQueryService;

    @Test
    void answerQuery_todayTasks_filtersByDueDateInShanghaiTimezone() {
        when(userService.getDecryptedToken("user-1")).thenReturn("token");
        when(tickTickClient.listProjects("token")).thenReturn(List.of());
        LocalDate today = LocalDate.now(TickTickDates.DEFAULT_ZONE);
        String todayInstant = TickTickDates.toTickTickInstant(today, null, TickTickDates.DEFAULT_ZONE);
        String overdueInstant =
                TickTickDates.toTickTickInstant(today.minusDays(10), null, TickTickDates.DEFAULT_ZONE);
        when(tickTickClient.getProjectTasks("inbox", "收集箱", "token"))
                .thenReturn(List.of(
                        new TickTickTaskResponse(
                                "1", "inbox", "收集箱", "写报告", todayInstant, todayInstant, "Asia/Shanghai", true, 0),
                        new TickTickTaskResponse(
                                "2",
                                "inbox",
                                "收集箱",
                                "旧任务",
                                overdueInstant,
                                overdueInstant,
                                "Asia/Shanghai",
                                true,
                                0),
                        new TickTickTaskResponse(
                                "3",
                                "inbox",
                                "收集箱",
                                "已完成",
                                todayInstant,
                                todayInstant,
                                "Asia/Shanghai",
                                true,
                                2)));

        String reply = taskQueryService.answerQuery("user-1", "我今天有哪些任务");

        assertThat(reply).contains("写报告");
        assertThat(reply).contains("旧任务");
        assertThat(reply).contains("已逾期");
        assertThat(reply).doesNotContain("已完成");
    }

    @Test
    void answerQuery_todayUndatedTasks_returnsFallbackList() {
        when(userService.getDecryptedToken("user-1")).thenReturn("token");
        when(tickTickClient.listProjects("token")).thenReturn(List.of());
        when(tickTickClient.getProjectTasks("inbox", "收集箱", "token"))
                .thenReturn(List.of(new TickTickTaskResponse("1", "inbox", "收集箱", "去大润发买菜", null, 0)));

        String reply = taskQueryService.answerQuery("user-1", "我今天有哪些任务");

        assertThat(reply).contains("去大润发买菜");
        assertThat(reply).contains("没有设置到期日");
    }

    @Test
    void answerQuery_allTasks_withoutTodayKeyword() {
        when(userService.getDecryptedToken("user-1")).thenReturn("token");
        when(tickTickClient.listProjects("token")).thenReturn(List.of());
        when(tickTickClient.getProjectTasks("inbox", "收集箱", "token"))
                .thenReturn(List.of(new TickTickTaskResponse("1", "inbox", "收集箱", "整理文档", null, 0)));

        String reply = taskQueryService.answerQuery("user-1", "我有哪些待办");

        assertThat(reply).contains("整理文档");
        assertThat(reply).contains("共有 1 项");
    }

    @Test
    void answerQuery_noToken_returnsBindHint() {
        when(userService.getDecryptedToken("user-1"))
                .thenThrow(new com.tickmine.domain.exception.TickTickNotConnectedException("user-1"));

        String reply = taskQueryService.answerQuery("user-1", "今天有什么任务");

        assertThat(reply).contains("绑定");
    }

    @Test
    void answerQuery_invalidToken_returnsRebindHint() {
        when(userService.getDecryptedToken("user-1")).thenReturn("dp_bad");
        when(tickTickClient.getProjectTasks("inbox", "收集箱", "dp_bad"))
                .thenThrow(new TickTickApiException(HttpStatus.UNAUTHORIZED, "invalid_token"));

        String reply = taskQueryService.answerQuery("user-1", "今天有什么任务");

        assertThat(reply).contains("失效");
        org.mockito.Mockito.verify(userService).invalidateToken("user-1");
    }
}
