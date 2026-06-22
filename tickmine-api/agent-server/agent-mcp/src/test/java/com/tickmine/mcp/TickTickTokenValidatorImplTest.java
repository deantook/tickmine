package com.tickmine.mcp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tickmine.domain.exception.TickTickTokenInvalidException;
import com.tickmine.domain.port.TickTickClient;
import com.tickmine.mcp.exception.TickTickApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TickTickTokenValidatorImplTest {

    @Mock
    private TickTickClient tickTickClient;

    @InjectMocks
    private TickTickTokenValidatorImpl validator;

    @Test
    void validate_validToken_callsListProjects() {
        when(tickTickClient.listProjects("dp_test")).thenReturn(List.of());

        validator.validate("  dp_test  ");

        verify(tickTickClient).listProjects("dp_test");
    }

    @Test
    void validate_unauthorized_throwsInvalidToken() {
        when(tickTickClient.listProjects("dp_bad"))
                .thenThrow(new TickTickApiException(HttpStatus.UNAUTHORIZED, "invalid_token"));

        assertThatThrownBy(() -> validator.validate("dp_bad"))
                .isInstanceOf(TickTickTokenInvalidException.class)
                .hasMessageContaining("无效或已过期");
    }
}
