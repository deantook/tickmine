package com.tickmine.mcp;

import com.tickmine.domain.exception.TickTickTokenInvalidException;
import com.tickmine.domain.port.TickTickClient;
import com.tickmine.domain.port.TickTickTokenValidator;
import com.tickmine.mcp.exception.TickTickApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TickTickTokenValidatorImpl implements TickTickTokenValidator {

    private final TickTickClient tickTickClient;

    @Override
    public void validate(String token) {
        if (token == null || token.isBlank()) {
            throw new TickTickTokenInvalidException("API 口令不能为空");
        }
        try {
            tickTickClient.listProjects(token.trim());
        } catch (TickTickApiException e) {
            if (e.getStatusCode().value() == 401) {
                throw new TickTickTokenInvalidException(
                        "API 口令无效或已过期。请确认使用的是滴答清单（国内版）的 API 口令，并在「设置 → 账户与安全 → API 口令管理」重新生成后绑定。");
            }
            throw e;
        }
    }
}
