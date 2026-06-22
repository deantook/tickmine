package com.tickmine.domain.port;

import java.util.Map;

public interface PromptTemplateLoader {

    String load(String name, Map<String, Object> variables);
}
